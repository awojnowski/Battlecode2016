package AaronBot2;

import AaronBot2.Signals.*;
import battlecode.common.*;
import java.util.*;

public class RobotArchon implements Robot {

    public void run(final RobotController robotController) throws GameActionException {

        final CommunicationModule communicationModule = new CommunicationModule();
        final Direction[] directions = { Direction.EAST, Direction.NORTH_EAST, Direction.NORTH, Direction.NORTH_WEST, Direction.WEST, Direction.SOUTH_WEST, Direction.SOUTH, Direction.SOUTH_EAST };

        int soldiersBuilt = 0;
        int scoutsBuilt = 0;
        boolean isBuildingUnit = false;

        while (true) {

            communicationModule.processIncomingSignals(robotController);

            if (robotController.isCoreReady()) {

                if (isBuildingUnit == true) {

                    communicationModule.broadcastCommunicationsDump(robotController, CommunicationModule.maximumFreeBroadcastRangeForRobotType(RobotType.ARCHON));

                }
                isBuildingUnit = false;

                if (robotController.isCoreReady()) {

                    RobotType typeToBuild = RobotType.SOLDIER;
                    if (scoutsBuilt * 25 < soldiersBuilt || scoutsBuilt == 0) {

                        typeToBuild = RobotType.SCOUT;

                    }
                    if (robotController.getTeamParts() >= typeToBuild.partCost) {

                        for (int i = 0; i < directions.length; i++) {

                            if (robotController.canBuild(directions[i], typeToBuild)) {

                                isBuildingUnit = true;
                                if (typeToBuild == RobotType.SOLDIER) {

                                    soldiersBuilt ++;

                                } else if (typeToBuild == RobotType.SCOUT) {

                                    scoutsBuilt ++;

                                }
                                robotController.build(directions[i], typeToBuild);
                                break;

                            }

                        }

                    }

                }

            }

            Clock.yield();

        }

    }

}
