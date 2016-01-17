package TheHair;

import TheHair.Cartography.*;
import TheHair.Map.*;
import TheHair.Movement.*;
import TheHair.Rubble.RubbleModule;
import TheHair.Signals.*;
import TheHair.Signals.CommunicationModule;
import TheHair.Signals.CommunicationModuleSignal;
import TheHair.Signals.CommunicationModuleSignalCollection;
import TheHair.Turtle.TurtleInfo;
import TheHair.Turtle.TurtlePlacementModule;
import TheHair.ZombieSpawns.ZombieSpawnsModule;
import battlecode.common.*;
import java.util.*;

public class RobotArchon implements Robot {

    private static int InitialMessageUpdateLength = 2;
    private static int TurtleRoundNumber = 300;

    enum State {
        UNKNOWN,
        ARCHON_RENDEZVOUS,
        INITIAL_UNIT_BUILD,
        TURTLE_CLEARING,
        TURTLE_STAGING,
        TURTLE_BUILDING
    }

    public void run(final RobotController robotController) throws GameActionException {

        final MapInfoModule mapInfoModule = new MapInfoModule();
        final CartographyModule cartographyModule = new CartographyModule();
        final CommunicationModule communicationModule = new CommunicationModule(mapInfoModule);
        final DirectionModule directionModule = new DirectionModule(robotController.getID());
        final MovementModule movementModule = new MovementModule();
        final RubbleModule rubbleModule = new RubbleModule();
        final TurtlePlacementModule turtlePlacementModule = new TurtlePlacementModule();

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
        int turretsBuilt = 0;

        // ARCHON_RENDEZVOUS

        final MapLocation archonRendezvousLocation = movementModule.getArchonRendezvousLocation(robotController.getLocation(), robotController.getInitialArchonLocations(robotController.getTeam()));

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

                if (archonRendezvousLocation != null && !archonRendezvousLocation.equals(currentLocation) && robotController.getRoundNum() < RobotArchon.TurtleRoundNumber) {

                    currentState = State.ARCHON_RENDEZVOUS;

                } else {

                    currentState = State.INITIAL_UNIT_BUILD;

                }

            }

            // process communication

            communicationModule.processIncomingSignals(robotController);

            final ArrayList<CommunicationModuleSignal> verifiedSignals = communicationModule.verifyCommunicationsInformation(robotController, enemies, true);
            for (int i = 0; i < verifiedSignals.size(); i++) {

                communicationModule.enqueueSignalForBroadcast(verifiedSignals.get(i));

            }

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

            } else if (currentState == State.TURTLE_CLEARING) {

                final MapLocation turtleLocation = communicationModule.turtleInfo.location;
                final int distance = currentLocation.distanceSquaredTo(turtleLocation);
                if (distance > 35) {

                    desiredMovementDirection = currentLocation.directionTo(turtleLocation);

                }

            } else if (currentState == State.TURTLE_STAGING) {

                desiredMovementDirection = currentLocation.directionTo(communicationModule.turtleInfo.location);

            } else if (currentState == State.TURTLE_BUILDING) {

                // check if we need to expand the distance from the turtle location

                final MapLocation bestTurretLocation = turtlePlacementModule.fetchBestTurretLocation(currentLocation, robotController, communicationModule.turtleInfo.location, communicationModule.turtleInfo, communicationModule.turtleInfo.distance);
                if (bestTurretLocation == null) {

                    communicationModule.turtleInfo.distance ++;

                    final CommunicationModuleSignal signal = new CommunicationModuleSignal();
                    signal.action = CommunicationModuleSignal.ACTION_SEEN;
                    signal.type = CommunicationModuleSignal.TYPE_TURTLE_INFO;
                    signal.data = communicationModule.turtleInfo.serialize();
                    signal.location = communicationModule.turtleInfo.location;
                    communicationModule.broadcastSignal(signal, robotController, CommunicationModule.maximumBroadcastRange(mapInfoModule));

                }

                // try to build a unit

                final RobotType unitBuildType = RobotType.TURRET;
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

            }

            // process flags

            // attempt to move to the desired movement location

            if (robotController.isCoreReady() && desiredMovementDirection != null) {

                final Direction movementDirection = directionModule.recommendedMovementDirectionForDirection(desiredMovementDirection, robotController, false);
                if (movementDirection != null && !movementModule.isMovementLocationRepetitive(currentLocation.add(movementDirection), robotController)) {

                    robotController.move(movementDirection);
                    currentLocation = robotController.getLocation();
                    movementModule.addMovementLocation(currentLocation, robotController);

                } else {

                    if (movementDirection != null) {

                        movementModule.extendLocationInvalidationTurn(robotController);

                    }
                    desiredRubbleClearanceDirection = desiredMovementDirection;

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

                } else if (desiredUnitBuildType == RobotType.TURRET) {

                    turretsBuilt ++;

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

                    currentState = State.INITIAL_UNIT_BUILD;

                }

            } else if (currentState == State.INITIAL_UNIT_BUILD) {

                if (robotController.getRoundNum() >= RobotArchon.TurtleRoundNumber) {

                    currentState = State.TURTLE_CLEARING;

                    if (!communicationModule.turtleInfo.hasLocation) {

                        // we need to find the best corner, otherwise we stay where we are

                        final int eastBoundary = mapInfoModule.eastBoundaryValue;
                        final int northBoundary = mapInfoModule.northBoundaryValue;
                        final int westBoundary = mapInfoModule.westBoundaryValue;
                        final int southBoundary = mapInfoModule.southBoundaryValue;

                        final MapLocation topLeftCorner     = (westBoundary != MapInfoModule.UnknownValue && northBoundary != MapInfoModule.UnknownValue) ? new MapLocation(westBoundary + 1, northBoundary + 1) : null;
                        final MapLocation topRightCorner    = (eastBoundary != MapInfoModule.UnknownValue && northBoundary != MapInfoModule.UnknownValue) ? new MapLocation(eastBoundary - 1, northBoundary + 1) : null;
                        final MapLocation bottomLeftCorner  = (westBoundary != MapInfoModule.UnknownValue && southBoundary != MapInfoModule.UnknownValue) ? new MapLocation(westBoundary + 1, southBoundary - 1) : null;
                        final MapLocation bottomRightCorner = (eastBoundary != MapInfoModule.UnknownValue && southBoundary != MapInfoModule.UnknownValue) ? new MapLocation(eastBoundary - 1, southBoundary - 1) : null;

                        MapLocation bestLocation = null;
                        int bestLocationDistance = Integer.MAX_VALUE;
                        if (topLeftCorner != null) {

                            final int distance = topLeftCorner.distanceSquaredTo(archonRendezvousLocation);
                            if (distance < bestLocationDistance) {

                                bestLocation = topLeftCorner;
                                bestLocationDistance = distance;

                            }

                        }
                        if (topRightCorner != null) {

                            final int distance = topRightCorner.distanceSquaredTo(archonRendezvousLocation);
                            if (distance < bestLocationDistance) {

                                bestLocation = topRightCorner;
                                bestLocationDistance = distance;

                            }

                        }
                        if (bottomLeftCorner != null) {

                            final int distance = bottomLeftCorner.distanceSquaredTo(archonRendezvousLocation);
                            if (distance < bestLocationDistance) {

                                bestLocation = bottomLeftCorner;
                                bestLocationDistance = distance;

                            }

                        }
                        if (bottomRightCorner != null) {

                            final int distance = bottomRightCorner.distanceSquaredTo(archonRendezvousLocation);
                            if (distance < bestLocationDistance) {

                                bestLocation = bottomRightCorner;
                                bestLocationDistance = distance;

                            }

                        }
                        if (bestLocation == null) {

                            bestLocation = archonRendezvousLocation;

                        }

                        communicationModule.turtleInfo.status = TurtleInfo.StatusSiteClearance;
                        communicationModule.turtleInfo.hasLocation = true;
                        communicationModule.turtleInfo.location = bestLocation;

                        final CommunicationModuleSignal signal = new CommunicationModuleSignal();
                        signal.action = CommunicationModuleSignal.ACTION_SEEN;
                        signal.type = CommunicationModuleSignal.TYPE_TURTLE_INFO;
                        signal.data = communicationModule.turtleInfo.serialize();
                        signal.location = communicationModule.turtleInfo.location;
                        communicationModule.broadcastSignal(signal, robotController, CommunicationModule.maximumBroadcastRange(mapInfoModule));

                    }

                }

            } else if (currentState == State.TURTLE_CLEARING) {

                if (communicationModule.turtleInfo.status == TurtleInfo.StatusSiteStaging) {

                    currentState = State.TURTLE_STAGING;

                } else {

                    final MapLocation turtleLocation = communicationModule.turtleInfo.location;
                    if (robotController.canSenseLocation(turtleLocation)) {

                        final RobotInfo robot = robotController.senseRobotAtLocation(turtleLocation);
                        if (robot == null || (robot != null && robot.team == robotController.getTeam())) {

                            currentState = State.TURTLE_STAGING;

                            communicationModule.turtleInfo.status = TurtleInfo.StatusSiteStaging;

                            final CommunicationModuleSignal signal = new CommunicationModuleSignal();
                            signal.action = CommunicationModuleSignal.ACTION_SEEN;
                            signal.type = CommunicationModuleSignal.TYPE_TURTLE_INFO;
                            signal.data = communicationModule.turtleInfo.serialize();
                            signal.location = communicationModule.turtleInfo.location;
                            communicationModule.broadcastSignal(signal, robotController, CommunicationModule.maximumBroadcastRange(mapInfoModule));

                        }

                    }

                }

            } else if (currentState == State.TURTLE_STAGING) {

                final MapLocation turtleLocation = communicationModule.turtleInfo.location;
                if (currentLocation.equals(turtleLocation) || currentLocation.distanceSquaredTo(turtleLocation) < 4) {

                    currentState = State.TURTLE_BUILDING;

                    if (communicationModule.turtleInfo.status != TurtleInfo.StatusSiteEstablished) {

                        communicationModule.turtleInfo.status = TurtleInfo.StatusSiteEstablished;

                        final CommunicationModuleSignal signal = new CommunicationModuleSignal();
                        signal.action = CommunicationModuleSignal.ACTION_SEEN;
                        signal.type = CommunicationModuleSignal.TYPE_TURTLE_INFO;
                        signal.data = communicationModule.turtleInfo.serialize();
                        signal.location = communicationModule.turtleInfo.location;
                        communicationModule.broadcastSignal(signal, robotController, CommunicationModule.maximumBroadcastRange(mapInfoModule));

                    }

                }

            } else if (currentState == State.TURTLE_BUILDING) {

                ;

            }

            // try to repair any units nearby

            RobotInfo injuredUnit = null;
            double injuredUnitHealth = Integer.MAX_VALUE;
            final RobotInfo[] friendlyRepairableUnits = robotController.senseNearbyRobots(currentType.attackRadiusSquared, robotController.getTeam());
            for (int i = 0; i < friendlyRepairableUnits.length; i++) {

                final RobotInfo friendly = friendlyRepairableUnits[i];
                if (friendly.type == RobotType.ARCHON) {

                    continue;

                }
                if (friendly.health < injuredUnitHealth && friendly.health < friendly.maxHealth) {

                    injuredUnit = friendly;
                    injuredUnitHealth = friendly.health;

                }

            }
            if (injuredUnit != null) {

                if (robotController.senseRobotAtLocation(injuredUnit.location) != null) {

                    robotController.repair(injuredUnit.location);

                }

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

            robotController.setIndicatorString(0, "State: " + currentState.name());

            // done!

            Clock.yield();

        }

    }

    /*
    TURTLE STAGING
     */

    private ArrayList<MapLocation> fetchAvailabileTurtleMapLocations(final MapLocation turtleLocation, final int ring, final int[] xMultipliers, final int[] yMultipliers) {

        final ArrayList<MapLocation> availableMapLocations = new ArrayList<MapLocation>();

        return availableMapLocations;

    }

}
