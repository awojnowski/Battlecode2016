package AaronBot3_Kiting;

import AaronBot3_Kiting.Cartography.*;
import AaronBot3_Kiting.Map.*;
import AaronBot3_Kiting.Movement.*;
import AaronBot3_Kiting.Signals.*;
import battlecode.common.*;
import java.util.*;

public class RobotScout implements Robot {

    public void run(final RobotController robotController) throws GameActionException {

        final MapInfoModule mapInfoModule = new MapInfoModule();

        final CartographyModule cartographyModule = new CartographyModule();
        final CommunicationModule communicationModule = new CommunicationModule(mapInfoModule);
        final MovementModule movementModule = new MovementModule();
        final Random random = new Random(robotController.getID());

        final MapLocation spawnLocation = robotController.getLocation();
        final Team team = robotController.getTeam();

        boolean returnToSpawnLocation = false;
        Direction movementDirection = null;
        int cantSeeShitTurns = 0;

        while (true) {

            MapLocation currentLocation = robotController.getLocation();
            final RobotInfo[] enemies = robotController.senseHostileRobots(currentLocation, robotController.getType().sensorRadiusSquared);

            robotController.setIndicatorString(1, "Signal distance: " + CommunicationModule.maximumBroadcastRange(mapInfoModule, currentLocation) + "; Queue size: " + communicationModule.communicationModuleSignalQueue.size());

            // process incoming communications

            communicationModule.processIncomingSignals(robotController);

            // let's check up on existing communications to verify the information, if we can

            final ArrayList<CommunicationModuleSignal> verifiedSignals = communicationModule.verifyCommunicationsInformation(robotController, enemies, true);
            for (int i = 0; i < verifiedSignals.size(); i++) {

                communicationModule.enqueueSignalForBroadcast(verifiedSignals.get(i));

            }

            // let's try identify what we can see

            for (int i = 0; i < enemies.length; i++) {

                final RobotInfo enemy = enemies[i];
                if (enemy.type == RobotType.ZOMBIEDEN) {

                    final CommunicationModuleSignal existingSignal = communicationModule.zombieDens.get(CommunicationModule.communicationsIndexFromLocation(enemy.location));
                    if (existingSignal != null) {

                        continue;

                    }

                    final CommunicationModuleSignal signal = new CommunicationModuleSignal();
                    signal.action = CommunicationModuleSignal.ACTION_SEEN;
                    signal.location = enemy.location;
                    signal.data = enemy.ID;
                    signal.type = CommunicationModuleSignal.TYPE_ZOMBIEDEN;
                    communicationModule.enqueueSignalForBroadcast(signal);

                } else if (enemy.type == RobotType.ARCHON) {

                    final ArrayList<CommunicationModuleSignal> existingSignals = communicationModule.getCommunicationModuleSignalsNearbyLocation(communicationModule.enemyArchons, currentLocation, CommunicationModule.DefaultApproximateNearbyLocationRange);
                    if (existingSignals.size() > 0) {

                        continue;

                    }

                    final CommunicationModuleSignal signal = new CommunicationModuleSignal();
                    signal.action = CommunicationModuleSignal.ACTION_SEEN;
                    signal.location = enemy.location;
                    signal.data = enemy.ID;
                    signal.type = CommunicationModuleSignal.TYPE_ENEMY_ARCHON;
                    communicationModule.enqueueSignalForBroadcast(signal);

                } else if (enemy.type == RobotType.TURRET) {

                    final ArrayList<CommunicationModuleSignal> existingSignals = communicationModule.getCommunicationModuleSignalsNearbyLocation(communicationModule.enemyTurrets, currentLocation, CommunicationModule.DefaultApproximateNearbyLocationRange);
                    if (existingSignals.size() > 0) {

                        continue;

                    }

                    final CommunicationModuleSignal signal = new CommunicationModuleSignal();
                    signal.action = CommunicationModuleSignal.ACTION_SEEN;
                    signal.location = enemy.location;
                    signal.data = enemy.ID;
                    signal.type = CommunicationModuleSignal.TYPE_ENEMY_TURRET;
                    communicationModule.enqueueSignalForBroadcast(signal);

                    robotController.setIndicatorLine(currentLocation, signal.location, 0, 255, 255);

                } else {

                    if (enemy.type != RobotType.SCOUT && cantSeeShitTurns > 100) {

                        final ArrayList<CommunicationModuleSignal> existingSignals = communicationModule.getCommunicationModuleSignalsNearbyLocation(communicationModule.enemyArchons, currentLocation, CommunicationModule.DefaultApproximateNearbyLocationRange);
                        if (existingSignals.size() > 0) {

                            continue;

                        }

                        final CommunicationModuleSignal signal = new CommunicationModuleSignal();
                        signal.action = CommunicationModuleSignal.ACTION_SEEN;
                        signal.location = enemy.location;
                        signal.data = enemy.ID;
                        signal.type = CommunicationModuleSignal.TYPE_ENEMY_ARCHON;
                        communicationModule.enqueueSignalForBroadcast(signal);

                    }

                }

            }

            if (communicationModule.initialInformationReceived) {

                if (!mapInfoModule.hasAllBoundaries()) {

                    final boolean hasEast = mapInfoModule.eastBoundaryValue != MapInfoModule.UnknownValue;
                    final boolean hasNorth = mapInfoModule.northBoundaryValue != MapInfoModule.UnknownValue;
                    final boolean hasWest = mapInfoModule.westBoundaryValue != MapInfoModule.UnknownValue;
                    final boolean hasSouth = mapInfoModule.southBoundaryValue != MapInfoModule.UnknownValue;

                    cartographyModule.probeAndUpdateMapInfoModule(mapInfoModule, currentLocation, robotController);
                    if (mapInfoModule.hasAllBoundaries()) {

                        final CommunicationModuleSignal signal = new CommunicationModuleSignal();
                        signal.action = CommunicationModuleSignal.ACTION_SEEN;
                        mapInfoModule.fillCommunicationModuleSignalWithMapSizeData(signal);
                        communicationModule.enqueueSignalForBroadcast(signal);

                    } else {

                        if (!hasEast && mapInfoModule.eastBoundaryValue != MapInfoModule.UnknownValue) {

                            final CommunicationModuleSignal signal = new CommunicationModuleSignal();
                            signal.action = CommunicationModuleSignal.ACTION_SEEN;
                            signal.type = CommunicationModuleSignal.TYPE_MAP_WALL_EAST;
                            signal.data = mapInfoModule.eastBoundaryValue;
                            communicationModule.enqueueSignalForBroadcast(signal);

                        }
                        if (!hasNorth && mapInfoModule.northBoundaryValue != MapInfoModule.UnknownValue) {

                            final CommunicationModuleSignal signal = new CommunicationModuleSignal();
                            signal.action = CommunicationModuleSignal.ACTION_SEEN;
                            signal.type = CommunicationModuleSignal.TYPE_MAP_WALL_NORTH;
                            signal.data = mapInfoModule.northBoundaryValue;
                            communicationModule.enqueueSignalForBroadcast(signal);

                        }
                        if (!hasWest && mapInfoModule.westBoundaryValue != MapInfoModule.UnknownValue) {

                            final CommunicationModuleSignal signal = new CommunicationModuleSignal();
                            signal.action = CommunicationModuleSignal.ACTION_SEEN;
                            signal.type = CommunicationModuleSignal.TYPE_MAP_WALL_WEST;
                            signal.data = mapInfoModule.westBoundaryValue;
                            communicationModule.enqueueSignalForBroadcast(signal);

                        }
                        if (!hasSouth && mapInfoModule.southBoundaryValue != MapInfoModule.UnknownValue) {

                            final CommunicationModuleSignal signal = new CommunicationModuleSignal();
                            signal.action = CommunicationModuleSignal.ACTION_SEEN;
                            signal.type = CommunicationModuleSignal.TYPE_MAP_WALL_SOUTH;
                            signal.data = mapInfoModule.southBoundaryValue;
                            communicationModule.enqueueSignalForBroadcast(signal);

                        }

                    }

                }

            }

            if (!mapInfoModule.hasAllBoundaries()) {

                cartographyModule.probeAndUpdateMapInfoModule(mapInfoModule, currentLocation, robotController);
                if (mapInfoModule.hasAllBoundaries()) {

                    final CommunicationModuleSignal signal = new CommunicationModuleSignal();
                    signal.action = CommunicationModuleSignal.ACTION_SEEN;
                    mapInfoModule.fillCommunicationModuleSignalWithMapSizeData(signal);
                    communicationModule.enqueueSignalForBroadcast(signal);

                }

            }

            // let's try to flee if we aren't safe

            final DirectionController directionController = new DirectionController(robotController);
            directionController.currentLocation = currentLocation;
            directionController.enemyBufferDistance = 2;
            directionController.nearbyEnemies = enemies;
            directionController.random = random;
            directionController.shouldAvoidEnemies = true;

            final Direction enemiesDirectionOutput = directionController.getAverageDirectionTowardsEnemies(enemies, true);
            if (enemiesDirectionOutput != null) {

                robotController.setIndicatorLine(currentLocation, currentLocation.add(enemiesDirectionOutput, 1000), 50, 25, 25);

            }

            if (robotController.isCoreReady() && communicationModule.initialInformationReceived && enemies.length > 0) {

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

            // now let's see if we should send our signals

            if (communicationModule.hasEnqueuedSignalsForBroadcast()) {

                communicationModule.broadcastEnqueuedSignals(robotController, CommunicationModule.maximumBroadcastRange(mapInfoModule, currentLocation));

            }

            // now let's try move to see more

            if (robotController.isCoreReady() && communicationModule.initialInformationReceived) {

                final int roundNumber = robotController.getRoundNum();
                final int freedomRoundNumber = 300;
                final int spawnDistance = currentLocation.distanceSquaredTo(spawnLocation);
                if (roundNumber < freedomRoundNumber && spawnDistance > roundNumber * 1.5) {

                    if (movementDirection == null) {

                        movementDirection = directionController.getRandomDirection();

                    }
                    movementDirection = robotController.getID() % 2 == 0 ? movementDirection.rotateLeft().rotateLeft() : movementDirection.rotateRight().rotateRight();
                    returnToSpawnLocation = true;

                } else if (roundNumber >= freedomRoundNumber || spawnDistance < 64) {

                    returnToSpawnLocation = false;

                }

                if (returnToSpawnLocation) {

                    final Direction returnDirection = currentLocation.directionTo(spawnLocation);
                    final DirectionController.Result returnMovementResult = directionController.getDirectionResultFromDirection(returnDirection, DirectionController.ADJUSTMENT_THRESHOLD_MEDIUM);
                    if (returnMovementResult.direction != null) {

                        robotController.move(returnMovementResult.direction);
                        currentLocation = robotController.getLocation();

                    }

                } else {

                    // let's see if we have a movement direction before moving (if not, create one)
                    if (movementDirection == null || !robotController.onTheMap(currentLocation.add(movementDirection))) {

                        movementDirection = directionController.getRandomDirection();

                    }

                    final DirectionController.Result movementResult = directionController.getDirectionResultFromDirection(movementDirection, DirectionController.ADJUSTMENT_THRESHOLD_MEDIUM);
                    if (movementResult.direction != null) {

                        robotController.move(movementResult.direction);
                        currentLocation = robotController.getLocation();

                    } else {

                        movementDirection = RobotScout.rotateDirection(movementDirection, currentLocation, robotController, mapInfoModule);

                    }

                }

            }

            if (movementDirection != null) {

                robotController.setIndicatorLine(currentLocation, currentLocation.add(movementDirection, 10000), 255, 255, 255);

            }

            // show what we know

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

            // check if we haven't been producing results

            if (communicationModule.zombieDens.size() == 0 && communicationModule.enemyArchons.size() == 0) {

                cantSeeShitTurns++;

            } else {

                cantSeeShitTurns = 0;

            }

            // update indicators

            if (mapInfoModule.eastBoundaryValue != MapInfoModule.UnknownValue) {

                robotController.setIndicatorLine(currentLocation, currentLocation.add(Direction.EAST, 1000), 255, 125, 0);

            }
            if (mapInfoModule.northBoundaryValue != MapInfoModule.UnknownValue) {

                robotController.setIndicatorLine(currentLocation, currentLocation.add(Direction.NORTH, 1000), 255, 125, 0);

            }
            if (mapInfoModule.westBoundaryValue != MapInfoModule.UnknownValue) {

                robotController.setIndicatorLine(currentLocation, currentLocation.add(Direction.WEST, 1000), 255, 125, 0);

            }
            if (mapInfoModule.southBoundaryValue != MapInfoModule.UnknownValue) {

                robotController.setIndicatorLine(currentLocation, currentLocation.add(Direction.SOUTH, 1000), 255, 125, 0);

            }

            Clock.yield();

        }

    }

    private static Direction rotateDirection(final Direction direction, final MapLocation location, final RobotController robotController, final MapInfoModule mapInfo) {

        if (robotController.getID() % 2 == 0) {

            return direction.rotateLeft();

        } else {

            return direction.rotateRight();

        }

    }

}
