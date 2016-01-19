package team059;

import team059.Combat.*;
import team059.Map.MapInfoModule;
import team059.Movement.*;
import team059.Signals.*;
import team059.Rubble.*;
import battlecode.common.*;

import java.util.*;

public class RobotSoldier implements Robot {

    public void run(final RobotController robotController) throws GameActionException {

        final MapInfoModule mapInfoModule = new MapInfoModule();

        final CombatModule combatModule = new CombatModule();
        final CommunicationModule communicationModule = new CommunicationModule(mapInfoModule);
        final DirectionModule directionModule = new DirectionModule(robotController.getID());
        final MovementModule movementModule = new MovementModule();
        final RubbleModule rubbleModule = new RubbleModule();

        final Team currentTeam = robotController.getTeam();
        int turnsStuck = 0;
        boolean stop = false;

        final RobotType type = robotController.getType();

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

            // let's prepare our actions for this turn

            final RobotInfo[] enemies = robotController.senseHostileRobots(currentLocation, robotController.getType().attackRadiusSquared);

            RobotInfo bestEnemy = this.getBestEnemyToAttackFromEnemies(enemies);
            boolean shouldMove = true;
            Direction desiredMovementDirection = null;
            Direction targetRubbleClearanceDirection = null;

            // check if there are nearby signals

            if (desiredMovementDirection == null) {

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

                    desiredMovementDirection = currentLocation.directionTo(closestSignalLocation);

                }

            }

            if (bestEnemy != null && bestEnemy.type == RobotType.ZOMBIEDEN && desiredMovementDirection != null) {

                bestEnemy = null;

            }

            // handle attacking

            if (bestEnemy != null) {

                // either we attack the enemy or we can kite away from it

                if (this.shouldKiteEnemy(bestEnemy) && currentLocation.distanceSquaredTo(bestEnemy.location) < 9) {

                    // we need to kite away from the enemy
                    desiredMovementDirection = currentLocation.directionTo(bestEnemy.location).opposite();

                } else {

                    if (robotController.isWeaponReady()) {

                        // we can attack the enemy

                        robotController.attackLocation(bestEnemy.location);
                        if (bestEnemy.type != RobotType.ZOMBIEDEN) {

                            communicationModule.broadcastSignal(robotController, CommunicationModule.maximumFreeBroadcastRangeForRobotType(robotController.getType()));

                        }

                    }

                    if (!this.shouldMoveTowardsEnemy(bestEnemy, currentLocation)) {

                        shouldMove = false;

                    }

                }

            }

            // now let's try move toward an assignment

            if (robotController.isCoreReady() && communicationModule.initialInformationReceived && shouldMove) {

                // check if we have an objective

                if (desiredMovementDirection == null) {

                    if (objectiveSignal != null) {

                        final MapLocation objectiveLocation = objectiveSignal.location;
                        if (objectiveLocation.distanceSquaredTo(currentLocation) >= 8) {

                            desiredMovementDirection = currentLocation.directionTo(objectiveLocation);

                        }

                    }

                }

                // try move towards archon starting positions

                if (desiredMovementDirection == null) {

                    int closestArchonDistance = Integer.MAX_VALUE;
                    MapLocation closestArchonLocation = null;

                    final MapLocation[] archonLocations = robotController.getInitialArchonLocations(robotController.getTeam().opponent());
                    for (int i = 0; i < archonLocations.length; i++) {

                        final MapLocation location = archonLocations[i];
                        final int distance = currentLocation.distanceSquaredTo(location);
                        if (distance < closestArchonDistance) {

                            closestArchonDistance = distance;
                            closestArchonLocation = location;

                        }

                    }
                    if (closestArchonLocation != null) {

                        desiredMovementDirection = currentLocation.directionTo(closestArchonLocation);

                    }

                }

                // process movement

                if (desiredMovementDirection != null) {

                    final Direction recommendedMovementDirection = directionModule.recommendedMovementDirectionForDirection(desiredMovementDirection, robotController, false);
                    if (recommendedMovementDirection != null) {

                        robotController.move(recommendedMovementDirection);
                        currentLocation = robotController.getLocation();
                        turnsStuck = 0;

                    } else {

                        targetRubbleClearanceDirection = desiredMovementDirection;

                    }

                }

            }

            // we can try clear rubble if we didn't move

            if (robotController.isCoreReady() && communicationModule.initialInformationReceived) {

                if (targetRubbleClearanceDirection != null) {

                    final Direction rubbleClearanceDirection = rubbleModule.getRubbleClearanceDirectionFromTargetDirection(targetRubbleClearanceDirection, robotController);
                    if (rubbleClearanceDirection != null) {

                        robotController.clearRubble(rubbleClearanceDirection);

                        if (turnsStuck != 0) {

                            turnsStuck = 0;

                        }

                        // otherwise they didn't move or clear rubble, check if they're stuck

                    } else if (communicationModule.notifications.size() == 0 && objectiveSignal != null) {

                        turnsStuck++;

                        if (turnsStuck > 5) {

                            communicationModule.clearSignal(objectiveSignal, communicationModule.enemyArchons);
                            communicationModule.clearSignal(objectiveSignal, communicationModule.zombieDens);
                            turnsStuck = 0;

                        }

                    } else if (turnsStuck != 0) {

                        turnsStuck = 0;

                    }

                }

            }

            // finish up

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
