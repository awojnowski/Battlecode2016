package TheHair;

import TheHair.Cartography.*;
import TheHair.Map.*;
import TheHair.Movement.*;
import TheHair.Rubble.RubbleModule;
import TheHair.Signals.*;
import battlecode.common.*;
import java.util.*;

public class RobotScout implements Robot {

    enum State {
        UNKNOWN,
        INFO_GATHER,
        TURTLE_STAGING,
        TURRET_VISION
    }

    public void run(final RobotController robotController) throws GameActionException {

        final MapInfoModule mapInfoModule = new MapInfoModule();
        final CartographyModule cartographyModule = new CartographyModule();
        final CommunicationModule communicationModule = new CommunicationModule(mapInfoModule);
        final DirectionModule directionModule = new DirectionModule(robotController.getID());
        final MovementModule movementModule = new MovementModule();
        final RubbleModule rubbleModule = new RubbleModule();

        // GLOBAL CONSTANTS

        final MapLocation archonRendezvousLocation = movementModule.getArchonRendezvousLocation(robotController.getLocation(), robotController.getInitialArchonLocations(robotController.getTeam()));
        final RobotType currentType = robotController.getType();

        int consecutiveInvalidMovementTurns = 0;

        // GLOBAL FLAGS

        State currentState = State.UNKNOWN;

        Direction infoGatherDirection = null;
        boolean returnToRendezvous = false;

        while (true) {

            // ROUND FLAGS

            MapLocation currentLocation = robotController.getLocation();
            Direction desiredMovementDirection = null;
            boolean moveSafely = false;

            // ROUND CONSTANTS

            final RobotInfo[] enemies = robotController.senseHostileRobots(currentLocation, currentType.sensorRadiusSquared);

            // begin...

            // we need to figure out the initial state

            if (currentState == State.UNKNOWN) {

                currentState = State.INFO_GATHER;

            }

            // process communication

            communicationModule.processIncomingSignals(robotController);

            final ArrayList<CommunicationModuleSignal> verifiedSignals = communicationModule.verifyCommunicationsInformation(robotController, enemies, true);
            for (int i = 0; i < verifiedSignals.size(); i++) {

                communicationModule.enqueueSignalForBroadcast(verifiedSignals.get(i));

            }

            // check map boundaries

            if (communicationModule.initialInformationReceived) {

                if (!mapInfoModule.hasAllBoundaries()) {

                    final boolean hasEast  = mapInfoModule.eastBoundaryValue != MapInfoModule.UnknownValue;
                    final boolean hasNorth = mapInfoModule.northBoundaryValue != MapInfoModule.UnknownValue;
                    final boolean hasWest  = mapInfoModule.westBoundaryValue != MapInfoModule.UnknownValue;
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

                    }

                }

            }

            // see if we are in danger

            if (enemies.length > 0) {

                final Direction fleeDirection = directionModule.averageDirectionTowardDangerousRobotsAndOuterBounds(robotController, enemies);
                if (fleeDirection != null) {

                    desiredMovementDirection = fleeDirection.opposite();

                }

            }

            // handle states

            if (currentState == State.INFO_GATHER) {

                if (desiredMovementDirection == null) {

                    if (infoGatherDirection == null) {

                        infoGatherDirection = directionModule.randomDirection();

                    }
                    if (returnToRendezvous) {

                        if (currentLocation.distanceSquaredTo(archonRendezvousLocation) < 64) {

                            returnToRendezvous = false;
                            infoGatherDirection = infoGatherDirection.rotateLeft().rotateLeft();

                        }

                    } else {

                        if (currentLocation.distanceSquaredTo(archonRendezvousLocation) > robotController.getRoundNum() * 2) {

                            returnToRendezvous = true;

                        }

                    }
                    final MapLocation targetLocation = currentLocation.add(infoGatherDirection);
                    if (!robotController.onTheMap(targetLocation)) {

                        infoGatherDirection = infoGatherDirection.rotateLeft().rotateLeft();

                    }
                    if (returnToRendezvous) {

                        desiredMovementDirection = currentLocation.directionTo(archonRendezvousLocation);
                        moveSafely = true;

                    } else {

                        if (consecutiveInvalidMovementTurns > 3) {

                            infoGatherDirection = infoGatherDirection.rotateLeft().rotateLeft();

                        }

                        desiredMovementDirection = infoGatherDirection;
                        moveSafely = true;

                    }

                }

            } else if (currentState == State.TURTLE_STAGING) {

                ;

            } else if (currentState == State.TURRET_VISION) {

                ;

            }

            // process flags

            // attempt to move to the desired movement location

            if (robotController.isCoreReady() && communicationModule.initialInformationReceived && desiredMovementDirection != null) {

                Direction movementDirection = null;
                if (moveSafely) {

                    movementDirection = directionModule.recommendedSafeMovementDirectionForDirection(desiredMovementDirection, robotController, enemies, 2, true);

                } else {

                    movementDirection = directionModule.recommendedMovementDirectionForDirection(desiredMovementDirection, robotController, false);

                }
                if (movementDirection != null) {

                    robotController.move(movementDirection);
                    currentLocation = robotController.getLocation();

                    consecutiveInvalidMovementTurns = 0;

                } else {

                    consecutiveInvalidMovementTurns ++;

                }

            }

            // confirm states

            if (currentState == State.INFO_GATHER) {

                ;

            } else if (currentState == State.TURRET_VISION) {

                ;

            }

            // broadcast enqueued signals

            if (communicationModule.hasEnqueuedSignalsForBroadcast()) {

                if (directionModule.isMapLocationSafe(currentLocation, enemies, 1)) {

                    communicationModule.broadcastEnqueuedSignals(robotController, CommunicationModule.maximumBroadcastRange(mapInfoModule));

                }

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

}
