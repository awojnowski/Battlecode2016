package AaronBot2Old.Cartography;

import AaronBot2Old.Map.*;
import battlecode.common.*;

public class CartographyModule {

    public void probeAndUpdateMapInfoModule(final MapInfoModule mapInfoModule, final MapLocation location, final RobotController robotController) throws GameActionException {

        final int probeDistance = (int)Math.floor(Math.sqrt(robotController.getType().sensorRadiusSquared));
        if (mapInfoModule.eastBoundaryValue == MapInfoModule.UnknownValue) {

            final MapLocation foundProbeLocation = this.probeDirection(Direction.EAST, probeDistance, location, robotController);
            if (foundProbeLocation != null) {

                mapInfoModule.eastBoundaryValue = foundProbeLocation.x;

            }

        }
        if (mapInfoModule.westBoundaryValue == MapInfoModule.UnknownValue) {

            final MapLocation foundProbeLocation = this.probeDirection(Direction.WEST, probeDistance, location, robotController);
            if (foundProbeLocation != null) {

                mapInfoModule.westBoundaryValue = foundProbeLocation.x;

            }

        }
        if (mapInfoModule.northBoundaryValue == MapInfoModule.UnknownValue) {

            final MapLocation foundProbeLocation = this.probeDirection(Direction.NORTH, probeDistance, location, robotController);
            if (foundProbeLocation != null) {

                mapInfoModule.northBoundaryValue = foundProbeLocation.y;

            }
        }
        if (mapInfoModule.southBoundaryValue == MapInfoModule.UnknownValue) {

            final MapLocation foundProbeLocation = this.probeDirection(Direction.SOUTH, probeDistance, location, robotController);
            if (foundProbeLocation != null) {

                mapInfoModule.southBoundaryValue = foundProbeLocation.y;

            }

        }

    }

    public MapLocation probeDirection(final Direction direction, int probeDistance, final MapLocation location, final RobotController robotController) throws GameActionException {

        MapLocation lastOffMapLocation = null;
        while (probeDistance >= 0) {

            final MapLocation checkLocation = probeDistance > 0 ? location.add(direction, probeDistance) : location;
            if (!robotController.onTheMap(checkLocation)) {

                lastOffMapLocation = checkLocation;

            } else if (lastOffMapLocation != null) {

                return lastOffMapLocation;

            }
            probeDistance --;

        }
        return null;

    }

}
