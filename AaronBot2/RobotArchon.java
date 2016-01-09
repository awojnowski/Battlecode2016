package AaronBot2;

import AaronBot2.Movement.*;
import AaronBot2.Signals.*;
import battlecode.common.*;
import java.util.*;

public class RobotArchon implements Robot {

    public void run(final RobotController robotController) throws GameActionException {

        // instance variables

        // modules

        final CommunicationModule communicationModule = new CommunicationModule();
        final DirectionModule directionModule = new DirectionModule(robotController.getID());

        // unit building

        int scoutsBuilt = 0;
        RobotType buildingUnitType = null;
        Enumeration<CommunicationModuleSignal> buildingUpdateSignals = null;

        // loop

        while (true) {

            communicationModule.processIncomingSignals(robotController);

            // check if we are done building a unit
            // if so, we should broadcast relevant information to that unit

            if (robotController.isCoreReady()) {

                if (buildingUnitType != null) {

                    if (buildingUpdateSignals == null) {

                        buildingUpdateSignals = communicationModule.communications.elements();

                    }

                }
                buildingUnitType = null;

            }

            if (buildingUpdateSignals != null) {

                boolean signalsSendingDone = true;
                int totalSignalsSent = 0;
                while (buildingUpdateSignals.hasMoreElements()) {

                    if (totalSignalsSent >= GameConstants.MESSAGE_SIGNALS_PER_TURN) {

                        signalsSendingDone = false;
                        break;

                    }

                    final CommunicationModuleSignal communicationModuleSignal = buildingUpdateSignals.nextElement();
                    if (this.shouldBroadcastCommunicationModuleSignalToRobotType(communicationModuleSignal.type, buildingUnitType)) {

                        communicationModule.broadcastSignal(communicationModuleSignal, robotController, 3);
                        totalSignalsSent ++;

                    }

                }
                if (signalsSendingDone) {

                    final CommunicationModuleSignal signal = new CommunicationModuleSignal();
                    signal.action = CommunicationModuleSignal.ACTION_INITIAL_UPDATE_COMPLETE;
                    signal.location = robotController.getLocation();
                    signal.robotIdentifier = robotController.getID();
                    signal.type = CommunicationModuleSignal.TYPE_NONE;
                    communicationModule.broadcastSignal(signal, robotController, 3);

                    buildingUpdateSignals = null;

                }

            }

            // attempt to build new units

            if (robotController.isCoreReady()) {

                if (scoutsBuilt < 2) {

                    RobotType typeToBuild = RobotType.SCOUT;
                    if (robotController.getTeamParts() >= typeToBuild.partCost) {

                        for (int i = 0; i < directionModule.directions.length; i++) {

                            if (robotController.canBuild(directionModule.directions[i], typeToBuild)) {

                                buildingUnitType = typeToBuild;
                                scoutsBuilt ++;
                                robotController.build(directionModule.directions[i], typeToBuild);
                                break;

                            }

                        }

                    }

                }

            }

            final Enumeration<CommunicationModuleSignal> communicationModuleSignals = communicationModule.communications.elements();
            final MapLocation location = robotController.getLocation();
            while (communicationModuleSignals.hasMoreElements()) {

                final CommunicationModuleSignal communicationModuleSignal = communicationModuleSignals.nextElement();
                int[] color = new int[]{255, 255, 255};
                if (communicationModuleSignal.type == CommunicationModuleSignal.TYPE_ZOMBIEDEN) {

                    color = new int[]{50, 255, 50};

                } else if (communicationModuleSignal.type == CommunicationModuleSignal.TYPE_ENEMY_ARCHON) {

                    color = new int[]{255, 0, 0};

                } else if (communicationModuleSignal.type == CommunicationModuleSignal.TYPE_MAP_CORNER) {

                    color = new int[]{0, 0, 0};

                } else if (communicationModuleSignal.type == CommunicationModuleSignal.TYPE_MAP_CORNER) {

                    color = new int[]{0, 255, 0};

                }
                robotController.setIndicatorLine(location, communicationModuleSignal.location, color[0], color[1], color[2]);

            }
            robotController.setIndicatorString(0, "I know of " + communicationModule.communications.size() + " communications.");

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
