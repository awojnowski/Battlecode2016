package AaronBot2;

import AaronBot2.Map.*;
import AaronBot2.Movement.*;
import AaronBot2.Rubble.RubbleModule;
import AaronBot2.Signals.*;
import battlecode.common.*;
import java.util.*;

public class RobotArchon implements Robot {

    private static int InitialMessageUpdateLength = 2;

    public void run(final RobotController robotController) throws GameActionException {

        // modules

        final MapInfoModule mapInfoModule = new MapInfoModule();
        mapInfoModule.detectWhetherToThrowGame(robotController);

        final CommunicationModule communicationModule = new CommunicationModule(mapInfoModule);
        final DirectionModule directionModule = new DirectionModule(robotController.getID());
        final RubbleModule rubbleModule = new RubbleModule();

        // unit building

        int scoutsBuilt = 0;
        int soldiersBuilt = 0;
        RobotType buildingUnitType = null;
        CommunicationModuleSignalCollection buildingUpdateSignalCollection = null;

        // loop

        while (true) {

            communicationModule.processIncomingSignals(robotController);

            if (mapInfoModule.shouldThrowGame) {

                final int throwRound = robotController.getID() % 200;
                if (robotController.getRoundNum() == throwRound) {

                    robotController.disintegrate();

                }

            }

            // check if we are done building a unit
            // if so, we should broadcast relevant information to that unit

            if (robotController.isCoreReady()) {

                if (buildingUnitType != null) {

                    if (buildingUpdateSignalCollection == null) {

                        buildingUpdateSignalCollection = communicationModule.allCommunicationModuleSignals();

                    }

                }
                buildingUnitType = null;

            }

            if (buildingUpdateSignalCollection != null) {

                boolean signalsSendingDone = true;
                int totalSignalsSent = 0;
                while (buildingUpdateSignalCollection.hasMoreElements()) {

                    if (totalSignalsSent >= GameConstants.MESSAGE_SIGNALS_PER_TURN - 1) {

                        signalsSendingDone = false;
                        break;

                    }

                    final CommunicationModuleSignal communicationModuleSignal = buildingUpdateSignalCollection.nextElement();
                    if (this.shouldBroadcastCommunicationModuleSignalToRobotType(communicationModuleSignal.type, buildingUnitType)) {

                        communicationModule.broadcastSignal(communicationModuleSignal, robotController, RobotArchon.InitialMessageUpdateLength);
                        totalSignalsSent ++;

                    }

                }
                if (signalsSendingDone) {

                    final CommunicationModuleSignal signal = new CommunicationModuleSignal();
                    signal.action = CommunicationModuleSignal.ACTION_INITIAL_UPDATE_COMPLETE;
                    signal.location = robotController.getLocation();
                    signal.data = robotController.getID();
                    signal.type = CommunicationModuleSignal.TYPE_NONE;
                    communicationModule.broadcastSignal(signal, robotController, RobotArchon.InitialMessageUpdateLength);

                    buildingUpdateSignalCollection = null;

                }

            }

            // we should try activate robots

            if (robotController.isCoreReady()) {

                final RobotInfo[] neutrals = robotController.senseNearbyRobots(3, Team.NEUTRAL);
                for (int i = 0; i < neutrals.length; i++) {

                    robotController.activate(neutrals[i].location);
                    break;

                }

            }

            // check to make sure we are safe

            MapLocation currentLocation = robotController.getLocation();
            final RobotInfo[] enemies = robotController.senseHostileRobots(currentLocation, robotController.getType().sensorRadiusSquared);

            if (enemies.length > 0 && robotController.isCoreReady()) {

                final Direction fleeDirection = directionModule.averageDirectionTowardDangerousRobotsAndOuterBounds(robotController, enemies);
                if (fleeDirection != null) {

                    final Direction fleeMovementDirection = directionModule.recommendedMovementDirectionForDirection(fleeDirection.opposite(), robotController, false);
                    if (fleeMovementDirection != null) {

                        robotController.move(fleeMovementDirection);
                        currentLocation = robotController.getLocation();
                        robotController.setIndicatorString(1, fleeDirection.name() + " " + fleeMovementDirection.name());

                    }

                }

            }

            // let's check up on existing communications to verify the information, if we can

            communicationModule.verifyCommunicationsInformation(robotController, enemies, true);

            // attempt to build new units

            if (robotController.isCoreReady()) {

                RobotType typeToBuild = RobotType.SOLDIER;
                if (scoutsBuilt == 0 || (int)Math.pow(scoutsBuilt + 1, 3) < soldiersBuilt) {

                    typeToBuild = RobotType.SCOUT;

                }
                if (robotController.getTeamParts() >= typeToBuild.partCost) {

                    for (int i = 0; i < directionModule.directions.length; i++) {

                        if (robotController.canBuild(directionModule.directions[i], typeToBuild)) {

                            buildingUnitType = typeToBuild;
                            if (typeToBuild == RobotType.SCOUT) {

                                scoutsBuilt ++;

                            }
                            if (typeToBuild == RobotType.SOLDIER) {

                                soldiersBuilt ++;

                            }
                            robotController.build(directionModule.directions[i], typeToBuild);
                            break;

                        }

                    }

                }

            }

            // try to move toward some spare parts

            Direction targetRubbleClearanceDirection = null;
            if (robotController.isCoreReady()) {

                MapLocation nearestPartsLocation = null;

                final MapLocation[] partsLocations = robotController.sensePartLocations(-1);
                if (partsLocations.length > 0) {

                    double nearestPartsRanking = 0;

                    for (int i = 0; i < partsLocations.length; i++) {

                        final MapLocation partsLocation = partsLocations[i];
                        final double partsTotal = robotController.senseParts(partsLocation);
                        final int distance = partsLocation.distanceSquaredTo(currentLocation);

                        final double ranking = partsTotal / distance;
                        if (ranking > nearestPartsRanking) {

                            nearestPartsLocation = partsLocation;
                            nearestPartsRanking = ranking;

                        }

                    }

                } else {

                    int nearestPartsDistance = Integer.MAX_VALUE;

                    final Enumeration<CommunicationModuleSignal> sparePartsCommunicationModuleSignals = communicationModule.spareParts.elements();
                    while (sparePartsCommunicationModuleSignals.hasMoreElements()) {

                        final CommunicationModuleSignal signal = sparePartsCommunicationModuleSignals.nextElement();
                        final int distance = signal.location.distanceSquaredTo(currentLocation);
                        if (distance < nearestPartsDistance) {

                            nearestPartsLocation = signal.location;
                            nearestPartsDistance = distance;

                        }

                    }

                }

                if (nearestPartsLocation != null) {

                    final Direction nearestPartsDirection = currentLocation.directionTo(nearestPartsLocation);
                    final Direction nearestPartsMovementDirection = directionModule.recommendedSafeMovementDirectionForDirection(nearestPartsDirection, robotController, enemies, 1, true);
                    if (nearestPartsMovementDirection != null) {

                        robotController.move(nearestPartsMovementDirection);
                        currentLocation = robotController.getLocation();

                    } else {

                        targetRubbleClearanceDirection = nearestPartsDirection;

                    }

                }

            }

            // we can try clear rubble if we didn't move

            if (robotController.isCoreReady()) {

                if (targetRubbleClearanceDirection != null) {

                    final Direction rubbleClearanceDirection = rubbleModule.getRubbleClearanceDirectionFromTargetDirection(targetRubbleClearanceDirection, robotController);
                    if (rubbleClearanceDirection != null) {

                        robotController.clearRubble(rubbleClearanceDirection);

                    }

                }

            }

            // try to repair any units nearby

            RobotInfo injuredUnit = null;
            double injuredUnitHealth = Integer.MAX_VALUE;
            final RobotInfo[] friendlyRepairableUnits = robotController.senseNearbyRobots(robotController.getType().attackRadiusSquared, robotController.getTeam());
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

                robotController.repair(injuredUnit.location);

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

                } else if (communicationModuleSignal.type == CommunicationModuleSignal.TYPE_SPARE_PARTS) {

                    color = new int[]{255, 255, 255};

                }
                robotController.setIndicatorLine(location, communicationModuleSignal.location, color[0], color[1], color[2]);

            }

            Clock.yield();

        }

    }

    /*
    SIGNALS
     */

    public boolean shouldBroadcastCommunicationModuleSignalToRobotType(final int broadcastType, final RobotType robotType) {

        return true;

    }

}
