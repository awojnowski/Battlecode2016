package AaronBot3;

import AaronBot3.Cartography.*;
import AaronBot3.Information.*;
import AaronBot3.Movement.*;
import battlecode.common.*;
import java.util.*;

public class RobotScout implements Robot {

    public void run(final RobotController robotController) throws GameActionException {

        final CartographyModule cartographyModule = new CartographyModule();
        final PoliticalAgenda politicalAgenda = new PoliticalAgenda();
        final MovementModule movementModule = new MovementModule();
        final Random random = new Random(robotController.getID());

        final MapLocation spawnLocation = robotController.getLocation();
        final Team team = robotController.getTeam();

        boolean returnToSpawnLocation = false;
        Direction movementDirection = null;

        politicalAgenda.determineMapMirroring(robotController);

        while (true) {

            MapLocation currentLocation = robotController.getLocation();
            final RobotInfo[] enemies = robotController.senseHostileRobots(currentLocation, robotController.getType().sensorRadiusSquared);

            robotController.setIndicatorString(0, "Map information: N: " + politicalAgenda.mapBoundaryNorth + " E: " + politicalAgenda.mapBoundaryEast + " S: " + politicalAgenda.mapBoundarySouth + " W: " + politicalAgenda.mapBoundaryWest);
            robotController.setIndicatorString(1, "Signal distance: " + politicalAgenda.maximumBroadcastRangeForLocation(currentLocation));

            // process incoming communications

            politicalAgenda.processIncomingSignalsFromRobotController(robotController);

            // let's verify information

            int zombieDenCount = politicalAgenda.zombieDens.size();
            for (int i = 0; i < zombieDenCount; i++) {

                final InformationSignal signal = politicalAgenda.zombieDens.get(i);
                if (!politicalAgenda.verifyZombieDenSignal(signal, robotController)) {

                    signal.action = PoliticalAgenda.SignalActionErase;
                    politicalAgenda.enqueueSignalForBroadcast(signal);

                    zombieDenCount --;
                    i--;

                }

            }

            // let's try identify what we can see

            for (int i = 0; i < enemies.length; i++) {

                final RobotInfo enemy = enemies[i];
                if (enemy.type == RobotType.ZOMBIEDEN) {

                    if (politicalAgenda.zombieDens.contains(enemy.ID)) {

                        continue;

                    }

                    final InformationSignal signal = politicalAgenda.generateZombieDenInformationSignal(enemy.location, enemy.ID);
                    politicalAgenda.enqueueSignalForBroadcast(signal);

                    // TODO: add mirrored signal

                }

            }

            if (politicalAgenda.isInformationSynced) {

                if (!politicalAgenda.hasAllMapBoundaries()) {

                    boolean hasEast = politicalAgenda.mapBoundaryEast != PoliticalAgenda.UnknownValue;
                    boolean hasNorth = politicalAgenda.mapBoundaryNorth != PoliticalAgenda.UnknownValue;
                    boolean hasWest = politicalAgenda.mapBoundaryWest != PoliticalAgenda.UnknownValue;
                    boolean hasSouth = politicalAgenda.mapBoundarySouth != PoliticalAgenda.UnknownValue;

                    cartographyModule.probeAndUpdatePoliticalAgenda(politicalAgenda, currentLocation, robotController);
                    if (politicalAgenda.hasAllMapBoundaries()) {

                        final InformationSignal signal = politicalAgenda.generateMapInfoInformationSignal();
                        politicalAgenda.enqueueSignalForBroadcast(signal);

                    } else {

                        if (!hasEast && politicalAgenda.mapBoundaryEast != PoliticalAgenda.UnknownValue) {

                            final InformationSignal signal = politicalAgenda.generateMapWallInformationSignal(PoliticalAgenda.SignalTypeMapWallEast, politicalAgenda.mapBoundaryEast);
                            politicalAgenda.enqueueSignalForBroadcast(signal);

                            if (politicalAgenda.mapBoundaryWest == PoliticalAgenda.UnknownValue) {

                                final InformationSignal mirroredSignal = politicalAgenda.getMirroredBoundarySignal(signal);
                                if (mirroredSignal != null) {

                                    politicalAgenda.mapBoundaryWest = mirroredSignal.data;
                                    politicalAgenda.enqueueSignalForBroadcast(mirroredSignal);
                                    hasWest = true;

                                }

                            }

                        }
                        if (!hasNorth && politicalAgenda.mapBoundaryNorth != PoliticalAgenda.UnknownValue) {

                            final InformationSignal signal = politicalAgenda.generateMapWallInformationSignal(PoliticalAgenda.SignalTypeMapWallNorth, politicalAgenda.mapBoundaryNorth);
                            politicalAgenda.enqueueSignalForBroadcast(signal);

                            if (politicalAgenda.mapBoundarySouth == PoliticalAgenda.UnknownValue) {

                                final InformationSignal mirroredSignal = politicalAgenda.getMirroredBoundarySignal(signal);
                                if (mirroredSignal != null) {

                                    politicalAgenda.mapBoundarySouth = mirroredSignal.data;
                                    politicalAgenda.enqueueSignalForBroadcast(mirroredSignal);
                                    hasSouth = true;

                                }

                            }

                        }
                        if (!hasWest && politicalAgenda.mapBoundaryWest != PoliticalAgenda.UnknownValue) {

                            final InformationSignal signal = politicalAgenda.generateMapWallInformationSignal(PoliticalAgenda.SignalTypeMapWallWest, politicalAgenda.mapBoundaryWest);
                            politicalAgenda.enqueueSignalForBroadcast(signal);

                            if (politicalAgenda.mapBoundaryEast == PoliticalAgenda.UnknownValue) {

                                final InformationSignal mirroredSignal = politicalAgenda.getMirroredBoundarySignal(signal);
                                if (mirroredSignal != null) {

                                    politicalAgenda.mapBoundaryEast = mirroredSignal.data;
                                    politicalAgenda.enqueueSignalForBroadcast(mirroredSignal);
                                    hasEast = true;

                                }

                            }

                        }
                        if (!hasSouth && politicalAgenda.mapBoundarySouth != PoliticalAgenda.UnknownValue) {
                            final InformationSignal signal = politicalAgenda.generateMapWallInformationSignal(PoliticalAgenda.SignalTypeMapWallSouth, politicalAgenda.mapBoundarySouth);
                            politicalAgenda.enqueueSignalForBroadcast(signal);

                            if (politicalAgenda.mapBoundaryNorth == PoliticalAgenda.UnknownValue) {

                                final InformationSignal mirroredSignal = politicalAgenda.getMirroredBoundarySignal(signal);
                                if (mirroredSignal != null) {

                                    politicalAgenda.mapBoundaryNorth = mirroredSignal.data;
                                    politicalAgenda.enqueueSignalForBroadcast(mirroredSignal);
                                    hasNorth = true;

                                }

                            }

                        }

                    }

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

            if (robotController.isCoreReady() && politicalAgenda.isInformationSynced && enemies.length > 0) {

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

            if (politicalAgenda.hasEnqueuedSignalsForBroadcast()) {

                politicalAgenda.broadcastEnqueuedSignals(robotController, politicalAgenda.maximumBroadcastRangeForLocation(currentLocation));

            }

            // now let's try move to see more

            if (robotController.isCoreReady() && politicalAgenda.isInformationSynced) {

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

                        movementDirection = RobotScout.rotateDirection(movementDirection, currentLocation, robotController);

                    }

                }

            }

            if (movementDirection != null) {

                robotController.setIndicatorLine(currentLocation, currentLocation.add(movementDirection, 10000), 255, 255, 255);

            }

            // show what we know

            for (int i = 0; i < politicalAgenda.zombieDens.size(); i++) {

                final InformationSignal signal = politicalAgenda.zombieDens.get(i);
                robotController.setIndicatorLine(currentLocation, signal.location, 0, 255, 0);

            }

            if (politicalAgenda.mapBoundaryEast != PoliticalAgenda.UnknownValue) {

                robotController.setIndicatorLine(currentLocation, new MapLocation(politicalAgenda.mapBoundaryEast - 1, currentLocation.y), 255, 125, 0);

            }
            if (politicalAgenda.mapBoundaryNorth != PoliticalAgenda.UnknownValue) {

                robotController.setIndicatorLine(currentLocation, new MapLocation(currentLocation.x, politicalAgenda.mapBoundaryNorth + 1), 255, 125, 0);

            }
            if (politicalAgenda.mapBoundaryWest != PoliticalAgenda.UnknownValue) {

                robotController.setIndicatorLine(currentLocation, new MapLocation(politicalAgenda.mapBoundaryWest + 1, currentLocation.y), 255, 125, 0);

            }
            if (politicalAgenda.mapBoundarySouth != PoliticalAgenda.UnknownValue) {

                robotController.setIndicatorLine(currentLocation, new MapLocation(currentLocation.x, politicalAgenda.mapBoundarySouth - 1), 255, 125, 0);

            }

            Clock.yield();

        }

    }

    private static Direction rotateDirection(final Direction direction, final MapLocation location, final RobotController robotController) {

        if (robotController.getID() % 2 == 0) {

            return direction.rotateLeft();

        } else {

            return direction.rotateRight();

        }

    }

}
