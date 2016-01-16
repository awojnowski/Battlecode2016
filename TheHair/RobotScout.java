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
        TURRET_VISION
    }

    public void run(final RobotController robotController) throws GameActionException {

        final MapInfoModule mapInfoModule = new MapInfoModule();
        final CartographyModule cartographyModule = new CartographyModule();
        final CommunicationModule communicationModule = new CommunicationModule(mapInfoModule);
        final DirectionModule directionModule = new DirectionModule(robotController.getID());
        final RubbleModule rubbleModule = new RubbleModule();

        // GLOBAL CONSTANTS

        final RobotType currentType = robotController.getType();

        // GLOBAL FLAGS

        State currentState = State.UNKNOWN;

        while (true) {

            // ROUND FLAGS

            MapLocation currentLocation = robotController.getLocation();
            Direction desiredMovementDirection = null;

            Direction desiredUnitBuildDirection = null;
            RobotType desiredUnitBuildType = null;

            Direction desiredRubbleClearanceDirection = null;
            boolean canClearAnyRubbleDirection = false;

            // ROUND CONSTANTS

            final RobotInfo[] enemies = robotController.senseHostileRobots(currentLocation, currentType.sensorRadiusSquared);

            // begin...

            // we need to figure out the initial state

            if (currentState == State.UNKNOWN) {

                currentState = State.INFO_GATHER;

            }

            // process communication

            communicationModule.processIncomingSignals(robotController);

            // check map boundaries

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

            // handle states

            if (currentState == State.INFO_GATHER) {

                desiredMovementDirection = directionModule.randomDirection();

            } else if (currentState == State.TURRET_VISION) {

                ;

            }

            // process flags

            // attempt to move to the desired movement location

            if (robotController.isCoreReady() && desiredMovementDirection != null) {

                final Direction movementDirection = directionModule.recommendedMovementDirectionForDirection(desiredMovementDirection, robotController, false);
                if (movementDirection != null) {

                    robotController.move(movementDirection);
                    currentLocation = robotController.getLocation();

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
