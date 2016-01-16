package TheHair.Turtle;

import TheHair.Movement.DirectionModule;
import battlecode.common.*;

public class TurtlePlacementModule {

    public MapLocation fetchBestTurretLocation(final MapLocation currentLocation, final RobotController robotController, final MapLocation turtleLocation, final TurtleInfo turtleInfo, final int scanDistance) throws GameActionException {

        MapLocation bestLocation = null;
        int bestLocationDistance = -1;

        for (int i = -scanDistance; i <= scanDistance; i++) {

            for (int j = -scanDistance; j <= scanDistance; j++) {

                final MapLocation location = new MapLocation(currentLocation.x + i, currentLocation.y + j);
                if (!robotController.canSenseLocation(location)) {

                    continue;

                }
                if (!robotController.onTheMap(location)) {

                    continue;

                }
                if (robotController.senseRubble(location) >= 100) {

                    continue;

                }
                if (robotController.senseRobotAtLocation(location) != null) {

                    continue;

                }

                final int distance = turtleLocation.distanceSquaredTo(location);
                if (distance > bestLocationDistance && distance <= turtleInfo.distance) {

                    bestLocation = location;
                    bestLocationDistance = distance;

                }

            }

        }
        final int currentLocationDistance = turtleLocation.distanceSquaredTo(currentLocation);
        if (bestLocationDistance <= currentLocationDistance) {

            return null;

        }

        return bestLocation;

    }

}
