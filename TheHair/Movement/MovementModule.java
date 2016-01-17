package TheHair.Movement;

import battlecode.common.*;
import java.util.*;

public class MovementModule {

    private ArrayList<MapLocation> previousMovementLocations = new ArrayList<MapLocation>();
    private int lastMovementLocationTurnNumber = 0;

    /*
    MOVEMENT LOCATIONS
     */

    public void addMovementLocation(final MapLocation location, final RobotController robotController) {

        if (robotController.getRoundNum() - this.lastMovementLocationTurnNumber > 10) {

            this.previousMovementLocations.clear();

        }
        this.lastMovementLocationTurnNumber = robotController.getRoundNum();
        this.previousMovementLocations.add(location);

    }

    public boolean isMovementLocationRepetitive(final MapLocation location, final RobotController robotController) {

        if (robotController.getRoundNum() - this.lastMovementLocationTurnNumber > 10) {

            this.previousMovementLocations.clear();
            return false;

        }

        final int size = this.previousMovementLocations.size();
        if (size < 2) {

            return false;

        }
        return this.previousMovementLocations.get(size - 2).equals(location);

    }

    public void extendLocationInvalidationTurn(final RobotController robotController) {

        this.lastMovementLocationTurnNumber = robotController.getRoundNum();

    }

    /*
    RENDEZVOUS
     */

    public MapLocation getArchonRendezvousLocation(final MapLocation currentLocation, final MapLocation[] archonLocations) {

        int nearestArchonDistance = Integer.MAX_VALUE;
        MapLocation nearestArchonLocation = null;

        for (int i = 0; i < archonLocations.length; i++) {

            final MapLocation testLocation = archonLocations[i];
            int distanceTotal = 0;
            for (int j = 0; j < archonLocations.length; j++) {

                if (i == j) {

                    continue;

                }
                distanceTotal += testLocation.distanceSquaredTo(archonLocations[j]);

            }
            if (distanceTotal < nearestArchonDistance) {

                nearestArchonDistance = distanceTotal;
                nearestArchonLocation = testLocation;

            }

        }

        return nearestArchonLocation;

    }

}
