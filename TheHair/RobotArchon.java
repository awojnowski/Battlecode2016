package TheHair;

import TheHair.Cartography.*;
import TheHair.Map.*;
import TheHair.Movement.*;
import TheHair.Rubble.RubbleModule;
import TheHair.Signals.*;
import TheHair.Signals.CommunicationModule;
import TheHair.Signals.CommunicationModuleSignal;
import TheHair.Signals.CommunicationModuleSignalCollection;
import TheHair.ZombieSpawns.ZombieSpawnsModule;
import battlecode.common.*;
import java.util.*;

public class RobotArchon implements Robot {

    private static int InitialMessageUpdateLength = 2;

    enum State {
        UNKNOWN,
        ARCHON_RENDEZVOUS,
        INITIAL_UNIT_BUILD,
        TURTLE
    }

    public void run(final RobotController robotController) throws GameActionException {

        final MapInfoModule mapInfoModule = new MapInfoModule();
        final CartographyModule cartographyModule = new CartographyModule();
        final CommunicationModule communicationModule = new CommunicationModule(mapInfoModule);
        final DirectionModule directionModule = new DirectionModule(robotController.getID());
        final MovementModule movementModule = new MovementModule();
        final RubbleModule rubbleModule = new RubbleModule();

        // GLOBAL CONSTANTS

        final RobotType currentType = robotController.getType();
        final int totalArchons = robotController.getInitialArchonLocations(robotController.getTeam()).length;

        // GLOBAL FLAGS

        State currentState = State.UNKNOWN;

        RobotType currentBuildingUnitType = null;
        CommunicationModuleSignalCollection buildingUnitUpdateSignalCollection = null;
        int guardsBuilt = 0;
        int scoutsBuilt = 0;
        int soldiersBuilt = 0;

        // ARCHON_RENDEZVOUS

        MapLocation archonRendezvousLocation = null;

        // run

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

            // we must figure out the initial archon state

            if (currentState == State.UNKNOWN) {

                final MapLocation[] archonLocations = robotController.getInitialArchonLocations(robotController.getTeam());
                final MapLocation rendezvousLocation = movementModule.getArchonRendezvousLocation(currentLocation, archonLocations);
                if (rendezvousLocation != null && !rendezvousLocation.equals(currentLocation)) {

                    archonRendezvousLocation = rendezvousLocation;
                    currentState = State.ARCHON_RENDEZVOUS;

                } else {

                    currentState = State.INITIAL_UNIT_BUILD;

                }

            }

            // process communication

            communicationModule.processIncomingSignals(robotController);

            // check if we are done building a unit

            if (robotController.isCoreReady()) {

                if (currentBuildingUnitType != null) {

                    buildingUnitUpdateSignalCollection = communicationModule.allCommunicationModuleSignals();

                }
                currentBuildingUnitType = null;

            }

            // let's broadcast any remaining information that we have to the nearby, recently created units

            if (buildingUnitUpdateSignalCollection != null) {

                boolean signalsSendingDone = true;
                int totalSignalsSent = 0;
                while (buildingUnitUpdateSignalCollection.hasMoreElements()) {

                    if (totalSignalsSent >= GameConstants.MESSAGE_SIGNALS_PER_TURN - 1) {

                        signalsSendingDone = false;
                        break;

                    }

                    final CommunicationModuleSignal communicationModuleSignal = buildingUnitUpdateSignalCollection.nextElement();
                    communicationModule.broadcastSignal(communicationModuleSignal, robotController, RobotArchon.InitialMessageUpdateLength);
                    totalSignalsSent ++;

                }
                if (signalsSendingDone) {

                    final CommunicationModuleSignal signal = new CommunicationModuleSignal();
                    signal.action = CommunicationModuleSignal.ACTION_INITIAL_UPDATE_COMPLETE;
                    signal.location = robotController.getLocation();
                    signal.data = robotController.getID();
                    signal.type = CommunicationModuleSignal.TYPE_NONE;
                    communicationModule.broadcastSignal(signal, robotController, RobotArchon.InitialMessageUpdateLength);

                    buildingUnitUpdateSignalCollection = null;

                }

            }

            // check map boundaries

            if (!mapInfoModule.hasAllBoundaries() && false) { // intentionally disabled

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

            if (currentState == State.ARCHON_RENDEZVOUS) {

                desiredMovementDirection = currentLocation.directionTo(archonRendezvousLocation);

            } else if (currentState == State.INITIAL_UNIT_BUILD) {

                RobotType unitBuildType = null;
                if (scoutsBuilt == 0) {

                    unitBuildType = RobotType.SCOUT;

                } else if (guardsBuilt * 2 < soldiersBuilt) {

                    unitBuildType = RobotType.GUARD;

                } else {

                    unitBuildType = RobotType.SOLDIER;

                }
                if (robotController.hasBuildRequirements(unitBuildType)) {

                    Direction unitBuildDirection = directionModule.randomDirection();
                    for (int i = 0; i < 8; i ++) {

                        if (robotController.canBuild(unitBuildDirection, unitBuildType)) {

                            desiredUnitBuildDirection = unitBuildDirection;
                            desiredUnitBuildType = unitBuildType;
                            break;

                        }
                        unitBuildDirection = unitBuildDirection.rotateRight();

                    }

                    if (desiredUnitBuildType == null) {

                        desiredRubbleClearanceDirection = directionModule.randomDirection();
                        canClearAnyRubbleDirection = true;

                    }

                }

            } else if (currentState == State.TURTLE) {

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

            // attempt to build units

            if (robotController.isCoreReady() && desiredUnitBuildType != null && desiredUnitBuildDirection != null) {

                if (desiredUnitBuildType == RobotType.GUARD) {

                    guardsBuilt ++;

                } else if (desiredUnitBuildType == RobotType.SCOUT) {

                    scoutsBuilt ++;

                } else if (desiredUnitBuildType == RobotType.SOLDIER) {

                    soldiersBuilt ++;

                }

                robotController.build(desiredUnitBuildDirection, desiredUnitBuildType);
                currentBuildingUnitType = desiredUnitBuildType;

            }

            // check if we should clear rubble

            if (robotController.isCoreReady() && desiredRubbleClearanceDirection != null) {

                Direction rubbleClearanceDirection = null;
                if (canClearAnyRubbleDirection) {

                    rubbleClearanceDirection = rubbleModule.getAnyRubbleClearanceDirectionFromDirection(desiredRubbleClearanceDirection, robotController);

                } else {

                    rubbleClearanceDirection = rubbleModule.getRubbleClearanceDirectionFromTargetDirection(desiredRubbleClearanceDirection, robotController);

                }
                if (rubbleClearanceDirection != null) {

                    robotController.clearRubble(rubbleClearanceDirection);

                }

            }

            // confirm states

            if (currentState == State.ARCHON_RENDEZVOUS) {

                final int distance = currentLocation.distanceSquaredTo(archonRendezvousLocation);
                if (distance < 16) {

                    archonRendezvousLocation = null;
                    currentState = State.INITIAL_UNIT_BUILD;

                }

            } else if (currentState == State.INITIAL_UNIT_BUILD) {

                if (robotController.getRoundNum() >= 300) {

                    currentState = State.TURTLE;

                }

            } else if (currentState == State.TURTLE) {

                ;

            }

            // broadcast enqueued signals

            if (communicationModule.hasEnqueuedSignalsForBroadcast()) {

                if (directionModule.isMapLocationSafe(currentLocation, enemies, 1)) {

                    communicationModule.broadcastEnqueuedSignals(robotController, CommunicationModule.maximumBroadcastRange(mapInfoModule));

                }

            }

            // update indicators

            final CommunicationModuleSignalCollection communicationModuleSignalCollection = communicationModule.allCommunicationModuleSignals();
            final MapLocation location = robotController.getLocation();
            while (communicationModuleSignalCollection.hasMoreElements()) {

                final CommunicationModuleSignal communicationModuleSignal = communicationModuleSignalCollection.nextElement();
                int[] color = new int[]{255, 255, 255};
                if (communicationModuleSignal.type == CommunicationModuleSignal.TYPE_ZOMBIEDEN) {

                    color = new int[]{50, 255, 50};

                } else if (communicationModuleSignal.type == CommunicationModuleSignal.TYPE_ENEMY_ARCHON) {

                    color = new int[]{255, 0, 0};

                }
                if (communicationModuleSignal.location != null) {

                    robotController.setIndicatorLine(location, communicationModuleSignal.location, color[0], color[1], color[2]);

                }

            }

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

            // done!

            Clock.yield();

        }

    }

}
