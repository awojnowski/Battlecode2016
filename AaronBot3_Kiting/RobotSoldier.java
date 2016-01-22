package AaronBot3_Kiting;

import AaronBot3_Kiting.Combat.*;
import AaronBot3_Kiting.Map.MapInfoModule;
import AaronBot3_Kiting.Movement.*;
import AaronBot3_Kiting.Signals.*;
import AaronBot3_Kiting.Rubble.*;
import battlecode.common.*;

import java.util.*;

public class RobotSoldier implements Robot {

    public void run(final RobotController robotController) throws GameActionException {

        final MapInfoModule mapInfoModule = new MapInfoModule();
        final CombatModule combatModule = new CombatModule();
        final CommunicationModule communicationModule = new CommunicationModule(mapInfoModule);
        final MovementModule movementModule = new MovementModule();
        final Random random = new Random(robotController.getID());
        final RubbleModule rubbleModule = new RubbleModule();

        final Team currentTeam = robotController.getTeam();
        final RobotType type = robotController.getType();

        boolean requiresHealing = false;

        while (true) {

            MapLocation currentLocation = robotController.getLocation();

            // update communication

            communicationModule.processIncomingSignals(robotController);

            // let's verify existing information

            communicationModule.verifyCommunicationsInformation(robotController, null, false);

            // let's get the best assignment

            CommunicationModuleSignal objectiveSignal = null;
            int closestObjectiveLocationDistance = Integer.MAX_VALUE;

            final Enumeration<CommunicationModuleSignal> zombieDenCommunicationModuleSignals = communicationModule.zombieDens.elements();
            while (zombieDenCommunicationModuleSignals.hasMoreElements()) {

                final CommunicationModuleSignal signal = zombieDenCommunicationModuleSignals.nextElement();
                final int distance = signal.location.distanceSquaredTo(currentLocation);
                if (distance < closestObjectiveLocationDistance) {

                    objectiveSignal = signal;
                    closestObjectiveLocationDistance = distance;

                }

            }

            final Enumeration<CommunicationModuleSignal> enemyArchonCommunicationModuleSignals = communicationModule.enemyArchons.elements();
            while (enemyArchonCommunicationModuleSignals.hasMoreElements()) {

                final CommunicationModuleSignal signal = enemyArchonCommunicationModuleSignals.nextElement();
                final int distance = signal.location.distanceSquaredTo(currentLocation) * 6; // multiplying by 6 to prioritize the dens
                if (distance < closestObjectiveLocationDistance) {

                    objectiveSignal = signal;
                    closestObjectiveLocationDistance = distance;

                }

            }

            final Enumeration<CommunicationModuleSignal> enemyTurretCommunicationModuleSignals = communicationModule.enemyTurrets.elements();
            while (enemyTurretCommunicationModuleSignals.hasMoreElements()) {

                final CommunicationModuleSignal signal = enemyTurretCommunicationModuleSignals.nextElement();
                final int distance = signal.location.distanceSquaredTo(currentLocation) * 20;
                if (distance < closestObjectiveLocationDistance) {

                    objectiveSignal = signal;
                    closestObjectiveLocationDistance = distance;

                }

            }

            // see if we can attack anything this turn

            robotController.setIndicatorString(0, "");

            final RobotInfo[] enemies = robotController.senseHostileRobots(currentLocation, -1);
            final RobotInfo[] friendlies = robotController.senseNearbyRobots(-1, robotController.getTeam());

            if (robotController.isWeaponReady()) {

                final RobotInfo[] attackableEnemies = robotController.senseHostileRobots(currentLocation, robotController.getType().attackRadiusSquared);
                final RobotInfo bestEnemy = this.getBestEnemyToAttackFromEnemies(attackableEnemies);
                if (bestEnemy != null) {

                    robotController.attackLocation(bestEnemy.location);

                    if (bestEnemy.type != RobotType.ZOMBIEDEN) {

                        communicationModule.broadcastSignal(robotController, CommunicationModule.maximumFreeBroadcastRangeForRobotType(robotController.getType()));

                    }

                }

            }

            // handle movement

            Direction desiredRubbleClearanceDirection = null;

            final DirectionController directionController = new DirectionController(robotController);
            directionController.currentLocation = currentLocation;
            directionController.nearbyEnemies = enemies;
            directionController.random = random;
            directionController.shouldAvoidEnemies = true;
            directionController.enemyBufferDistance = 1;
            directionController.minimumEnemyAttackRadiusSquared = robotController.getType().attackRadiusSquared;

            if (robotController.getHealth() < 15.0) {

                directionController.enemyBufferDistance = 2;
                requiresHealing = true;

            } else if (robotController.getHealth() / robotController.getType().maxHealth > 0.90) {

                requiresHealing = false;

            }

            // kite from nearby enemies

            if (robotController.isCoreReady()) {

                final Direction kiteDirection = directionController.getAverageDirectionTowardsEnemies(enemies, true);
                if (kiteDirection != null) {

                    directionController.shouldAvoidEnemies = false;
                    final DirectionController.Result kiteDirectionResult = directionController.getDirectionResultFromDirection(kiteDirection.opposite(), DirectionController.ADJUSTMENT_THRESHOLD_MEDIUM);
                    directionController.shouldAvoidEnemies = true;

                    if (kiteDirectionResult.direction != null) {

                        robotController.move(kiteDirectionResult.direction);
                        currentLocation = robotController.getLocation();

                    }

                }

                if (desiredRubbleClearanceDirection != null) {

                    desiredRubbleClearanceDirection = kiteDirection;

                }

            }

            // try find an archon to heal up

            if (robotController.isCoreReady() && requiresHealing) {

                RobotInfo nearestArchon = null;
                int nearestArchonDistance = Integer.MAX_VALUE;

                for (int i = 0; i < friendlies.length; i++) {

                    final RobotInfo robot = friendlies[i];
                    if (robot.type != RobotType.ARCHON) {

                        continue;

                    }
                    final int distance = currentLocation.distanceSquaredTo(robot.location);
                    if (distance < nearestArchonDistance) {

                        nearestArchon = robot;
                        nearestArchonDistance = distance;

                    }

                }

                if (nearestArchon != null) {

                    final Direction nearestArchonDirection = currentLocation.directionTo(nearestArchon.location);

                    directionController.shouldAvoidEnemies = false;
                    final DirectionController.Result nearestArchonResult = directionController.getDirectionResultFromDirection(nearestArchonDirection, DirectionController.ADJUSTMENT_THRESHOLD_MEDIUM);
                    directionController.shouldAvoidEnemies = true;

                    if (nearestArchonResult.direction != null) {

                        robotController.move(nearestArchonResult.direction);
                        currentLocation = robotController.getLocation();

                    }

                    if (desiredRubbleClearanceDirection != null) {

                        desiredRubbleClearanceDirection = nearestArchonDirection;

                    }

                }

            }

            // respond to nearby unit pings

            if (robotController.isCoreReady() && !requiresHealing) {

                int closestSignalDistance = Integer.MAX_VALUE;
                MapLocation closestSignalLocation = null;

                final ArrayList<Signal> notifications = communicationModule.notifications;
                for (int i = 0; i < notifications.size(); i++) {

                    final Signal signal = notifications.get(i);
                    final int distance = currentLocation.distanceSquaredTo(signal.getLocation());
                    if (distance < closestSignalDistance) {

                        closestSignalDistance = distance;
                        closestSignalLocation = signal.getLocation();

                    }

                }
                if (closestSignalLocation != null) {

                    final Direction closestSignalDirection = currentLocation.directionTo(closestSignalLocation);
                    final DirectionController.Result closestSignalResult = directionController.getDirectionResultFromDirection(closestSignalDirection, DirectionController.ADJUSTMENT_THRESHOLD_LOW);
                    if (closestSignalResult.direction != null && !movementModule.isMovementLocationRepetitive(currentLocation.add(closestSignalResult.direction), robotController)) {

                        robotController.move(closestSignalResult.direction);
                        currentLocation = robotController.getLocation();
                        movementModule.addMovementLocation(currentLocation, robotController);

                    } else if (closestSignalResult.error == DirectionController.ErrorType.BLOCKED_FRIENDLIES) {

                        final DirectionController.Result closestSignalResultExtended = directionController.getDirectionResultFromDirection(closestSignalDirection, DirectionController.ADJUSTMENT_THRESHOLD_MEDIUM);
                        if (closestSignalResultExtended.direction != null && !movementModule.isMovementLocationRepetitive(currentLocation.add(closestSignalResultExtended.direction), robotController)) {

                            robotController.move(closestSignalResultExtended.direction);
                            currentLocation = robotController.getLocation();
                            movementModule.addMovementLocation(currentLocation, robotController);

                        }

                    }

                    if (desiredRubbleClearanceDirection != null) {

                        desiredRubbleClearanceDirection = closestSignalDirection;

                    }

                }

            }

            // move to an objective

            if (robotController.isCoreReady() && objectiveSignal != null) {

                boolean shouldMoveTowardsObjective = true;
                if (objectiveSignal.type == CommunicationModuleSignal.TYPE_ZOMBIEDEN) {

                    final int distance = currentLocation.distanceSquaredTo(objectiveSignal.location);
                    if (distance < 9) {

                        shouldMoveTowardsObjective = false;

                    }

                }

                if (shouldMoveTowardsObjective) {

                    final Direction signalDirection = currentLocation.directionTo(objectiveSignal.location);
                    final DirectionController.Result signalResult = directionController.getDirectionResultFromDirection(signalDirection, DirectionController.ADJUSTMENT_THRESHOLD_LOW);
                    if (signalResult.direction != null && !movementModule.isMovementLocationRepetitive(currentLocation.add(signalResult.direction), robotController)) {

                        robotController.move(signalResult.direction);
                        currentLocation = robotController.getLocation();
                        movementModule.addMovementLocation(currentLocation, robotController);

                    }

                    if (desiredRubbleClearanceDirection != null) {

                        desiredRubbleClearanceDirection = signalDirection;

                    }

                }

            }

            // try clear rubble

            if (robotController.isCoreReady()) {

                int rubbleAdjustmentThreshold = RubbleModule.ADJUSTMENT_THRESHOLD_MEDIUM;
                if (desiredRubbleClearanceDirection == null) {

                    desiredRubbleClearanceDirection = directionController.getRandomDirection();
                    rubbleAdjustmentThreshold = RubbleModule.ADJUSTMENT_THRESHOLD_ALL;

                }

                final Direction rubbleClearanceDirection = rubbleModule.getRubbleClearanceDirectionFromDirection(desiredRubbleClearanceDirection, robotController, rubbleAdjustmentThreshold);
                if (rubbleClearanceDirection != null) {

                    robotController.clearRubble(rubbleClearanceDirection);

                }

            }

            // finish up

            /*

            final CommunicationModuleSignalCollection communicationModuleSignalCollection = communicationModule.allCommunicationModuleSignals();
            final MapLocation location = robotController.getLocation();
            while (communicationModuleSignalCollection.hasMoreElements()) {

                final CommunicationModuleSignal communicationModuleSignal = communicationModuleSignalCollection.nextElement();
                int[] color = new int[]{255, 255, 255};
                if (communicationModuleSignal.type == CommunicationModuleSignal.TYPE_ZOMBIEDEN) {

                    color = new int[]{50, 255, 50};

                } else if (communicationModuleSignal.type == CommunicationModuleSignal.TYPE_ENEMY_ARCHON) {

                    color = new int[]{255, 0, 0};

                } else if (communicationModuleSignal.type == CommunicationModuleSignal.TYPE_ENEMY_TURRET) {

                    color = new int[]{255, 50, 100};

                } else {

                    continue;

                }
                robotController.setIndicatorLine(location, communicationModuleSignal.location, color[0], color[1], color[2]);

            }

            if (objectiveSignal != null) {

                robotController.setIndicatorLine(objectiveSignal.location, robotController.getLocation(), 125, 0, 0);

            }

            */

            Clock.yield();

        }

    }

    /*
    COMBAT
     */

    private RobotInfo getBestEnemyToAttackFromEnemies(final RobotInfo[] enemies) {

        RobotInfo bestEnemy = null;
        for (int i = 0; i < enemies.length; i++) {

            final RobotInfo enemy = enemies[i];
            if (bestEnemy == null) {

                bestEnemy = enemy;
                continue;

            }
            if (enemy.type != RobotType.ZOMBIEDEN && bestEnemy.type == RobotType.ZOMBIEDEN) {

                bestEnemy = enemy;
                continue;

            }
            if (enemy.type == RobotType.ZOMBIEDEN && bestEnemy.type != RobotType.ZOMBIEDEN) {

                continue;

            }
            if (enemy.health < bestEnemy.health) {

                bestEnemy = enemy;
                continue;

            }

        }
        return bestEnemy;

    }

    private boolean shouldKiteEnemy(final RobotInfo enemy) {

        return enemy.type == RobotType.BIGZOMBIE || enemy.type == RobotType.STANDARDZOMBIE;

    }

    private boolean shouldMoveTowardsEnemy(final RobotInfo enemy, final MapLocation currentLocation) {

        if (enemy.type == RobotType.TURRET || enemy.type == RobotType.ARCHON || enemy.type == RobotType.SCOUT) {

            return true;

        }
        if (enemy.type == RobotType.ZOMBIEDEN) {

            final int distance = currentLocation.distanceSquaredTo(enemy.location);
            if (distance > 8) {

                return true;

            }

        }
        return false;

    }

}
