package AaronBot3.Cartography;

import AaronBot3.Information.*;
import battlecode.common.*;

public class CartographyModule {

    public void probeAndUpdatePoliticalAgenda(final PoliticalAgenda politicalAgenda, final MapLocation location, final RobotController robotController) throws GameActionException {

        final int probeDistance = (int)Math.floor(Math.sqrt(robotController.getType().sensorRadiusSquared));
        if (politicalAgenda.mapBoundaryEast == PoliticalAgenda.UnknownValue) {

            final MapLocation foundProbeLocation = this.probeDirection(Direction.EAST, probeDistance, location, robotController);
            if (foundProbeLocation != null) {

                politicalAgenda.mapBoundaryEast = foundProbeLocation.x;

                if (robotController.getID() == 3007) {

                    System.out.println("Updating east boundary: " + politicalAgenda.mapBoundaryEast);

                }

            }

        }
        if (politicalAgenda.mapBoundaryWest == PoliticalAgenda.UnknownValue) {

            final MapLocation foundProbeLocation = this.probeDirection(Direction.WEST, probeDistance, location, robotController);
            if (foundProbeLocation != null) {

                politicalAgenda.mapBoundaryWest = foundProbeLocation.x;

                if (robotController.getID() == 3007) {

                    System.out.println("Updating west boundary: " + politicalAgenda.mapBoundaryWest);

                }

            }

        }
        if (politicalAgenda.mapBoundaryNorth == PoliticalAgenda.UnknownValue) {

            final MapLocation foundProbeLocation = this.probeDirection(Direction.NORTH, probeDistance, location, robotController);
            if (foundProbeLocation != null) {

                politicalAgenda.mapBoundaryNorth = foundProbeLocation.y;

                if (robotController.getID() == 3007) {

                    System.out.println("Updating north boundary: " + politicalAgenda.mapBoundaryNorth);

                }

            }
        }
        if (politicalAgenda.mapBoundarySouth == PoliticalAgenda.UnknownValue) {

            final MapLocation foundProbeLocation = this.probeDirection(Direction.SOUTH, probeDistance, location, robotController);
            if (foundProbeLocation != null) {

                politicalAgenda.mapBoundarySouth = foundProbeLocation.y;

                if (robotController.getID() == 3007) {

                    System.out.println("Updating south boundary: " + politicalAgenda.mapBoundarySouth);

                }

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
