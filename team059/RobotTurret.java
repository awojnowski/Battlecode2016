package team059;

import team059.Combat.*;
import team059.Information.*;
import team059.Movement.*;
import team059.Rubble.*;
import battlecode.common.*;
import java.util.*;

public class RobotTurret implements Robot {

    public void run(final RobotController robotController) throws GameActionException {

        robotController.emptySignalQueue();

        final CombatModule combatModule = new CombatModule();
        final MovementModule movementModule = new MovementModule();
        final PoliticalAgenda politicalAgenda = new PoliticalAgenda();
        final Random random = new Random(robotController.getID());
        final RubbleModule rubbleModule = new RubbleModule();

        final Team currentTeam = robotController.getTeam();
        int turnsStuck = 0;

        while (true) {

            // update communication

            politicalAgenda.processIncomingSignalsFromRobotController(robotController);
            if (!politicalAgenda.isInformationSynced) {

                Clock.yield();
                continue;

            }

            // begin

            RobotType type = robotController.getType();
            MapLocation currentLocation = robotController.getLocation();

            if (type == RobotType.TTM) {

                // MOVE

                // let's get the best assignment

                InformationSignal objectiveSignal = null;
                int closestObjectiveLocationDistance = Integer.MAX_VALUE;

                int zombieDenCount = politicalAgenda.zombieDens.size();
                for (int i = 0; i < zombieDenCount; i++) {

                    final InformationSignal signal = politicalAgenda.zombieDens.get(i);
                    final int distance = currentLocation.distanceSquaredTo(signal.location);
                    if (distance < closestObjectiveLocationDistance) {

                        if (politicalAgenda.verifyZombieDenSignal(signal, robotController)) {

                            objectiveSignal = signal;
                            closestObjectiveLocationDistance = distance;

                        } else {

                            zombieDenCount --;
                            i--;

                        }

                    }

                }

                // process movement

                if (robotController.isCoreReady()) {

                    Direction desiredMovementDirection = null;

                    final DirectionController directionController = new DirectionController(robotController);
                    directionController.currentLocation = currentLocation;
                    directionController.enemyBufferDistance = 2;
                    directionController.random = random;
                    directionController.shouldAvoidEnemies = true;

                    final RobotInfo[] enemies = robotController.senseHostileRobots(currentLocation, robotController.getType().sensorRadiusSquared);
                    directionController.nearbyEnemies = enemies;

                    // run away from nearby enemies

                    if (desiredMovementDirection == null && enemies.length > 0) {

                        final Direction enemiesDirection = directionController.getAverageDirectionTowardsEnemies(enemies, true);
                        if (enemiesDirection != null) {

                            directionController.shouldAvoidEnemies = false;
                            final DirectionController.Result enemiesMovementResult = directionController.getDirectionResultFromDirection(enemiesDirection.opposite(), DirectionController.ADJUSTMENT_THRESHOLD_MEDIUM);
                            directionController.shouldAvoidEnemies = true;

                            if (enemiesMovementResult.direction != null) {

                                robotController.move(enemiesMovementResult.direction);
                                currentLocation = robotController.getLocation();

                            }

                        }

                    }

                    // check nearby signals

                    if (desiredMovementDirection == null) {

                        int closestSignalDistance = Integer.MAX_VALUE;
                        MapLocation closestSignalLocation = null;

                        final ArrayList<Signal> notifications = politicalAgenda.notifications;
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

                        final DirectionController.Result directionResult = directionController.getDirectionResultFromDirection(desiredMovementDirection, DirectionController.ADJUSTMENT_THRESHOLD_LOW);
                        if (directionResult.direction != null && !movementModule.isMovementLocationRepetitive(currentLocation.add(directionResult.direction), robotController)) {

                            robotController.move(directionResult.direction);
                            currentLocation = robotController.getLocation();
                            movementModule.addMovementLocation(currentLocation, robotController);

                        }

                    }

                }

                // unpack if we're safe

                final RobotInfo[] nearbyTeammates = robotController.senseNearbyRobots(8, currentTeam);
                final RobotInfo[] nearbySoldiers = combatModule.robotsOfTypesFromRobots(nearbyTeammates, new RobotType[]{RobotType.SOLDIER});

                if (nearbySoldiers.length > 2) {

                    robotController.unpack();

                }

            } else {

                // ATTACK

                final RobotInfo[] enemies = robotController.senseHostileRobots(currentLocation, robotController.getType().attackRadiusSquared);
                final RobotInfo bestEnemy = this.getBestEnemyToAttackFromEnemies(robotController, enemies);

                // handle attacking

                if (bestEnemy != null) {

                    if (robotController.isWeaponReady()) {

                        // we can attack the enemy

                        robotController.attackLocation(bestEnemy.location);
                        if (bestEnemy.type != RobotType.ZOMBIEDEN) {

                            politicalAgenda.broadcastSignal(robotController, politicalAgenda.maximumFreeBroadcastRangeForType(robotController.getType()));

                        }

                    }

                }

                // pack if we aren't near soldiers

                final RobotInfo[] nearbyTeammates = robotController.senseNearbyRobots(type.sensorRadiusSquared, currentTeam);
                final RobotInfo[] nearbySoldiers = combatModule.robotsOfTypesFromRobots(nearbyTeammates, new RobotType[]{RobotType.SOLDIER});

                if (nearbySoldiers.length < 3) {

                    robotController.pack();

                }

            }

            // finish up

            // show what we know

            for (int i = 0; i < politicalAgenda.archonLocations.size(); i++) {

                final MapLocation archonLocation = politicalAgenda.archonLocations.get(i);
                robotController.setIndicatorLine(currentLocation, archonLocation, 25, 25, 255);

            }

            for (int i = 0; i < politicalAgenda.enemies.size(); i++) {

                final EnemyInfo enemy = politicalAgenda.enemies.get(i);
                robotController.setIndicatorLine(currentLocation, enemy.location, 255, 0, 255);

            }

            for (int i = 0; i < politicalAgenda.zombieDens.size(); i++) {

                final InformationSignal signal = politicalAgenda.zombieDens.get(i);
                robotController.setIndicatorLine(currentLocation, signal.location, 0, 255, 0);

            }

            for (int i = 0; i < politicalAgenda.enemyClumps.size(); i++) {

                final ClumpInfo clumpInfo = politicalAgenda.enemyClumps.get(i);
                robotController.setIndicatorLine(currentLocation, clumpInfo.location, 120, 0, 0);

            }

            for (int i = 0; i < politicalAgenda.friendlyClumps.size(); i++) {

                final ClumpInfo clumpInfo = politicalAgenda.friendlyClumps.get(i);
                robotController.setIndicatorLine(currentLocation, clumpInfo.location, 0, 120, 0);

            }

            Clock.yield();

        }

    }

    /*
    COMBAT
     */

    private RobotInfo getBestEnemyToAttackFromEnemies(final RobotController robotController, final RobotInfo[] enemies) {

        MapLocation currentLocaiton = robotController.getLocation();
        RobotInfo bestEnemy = null;
        for (int i = 0; i < enemies.length; i++) {

            final RobotInfo enemy = enemies[i];
            if (currentLocaiton.distanceSquaredTo(enemy.location) < GameConstants.TURRET_MINIMUM_RANGE) {

                continue;

            }
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

}
