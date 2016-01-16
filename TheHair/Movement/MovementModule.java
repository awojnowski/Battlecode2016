package TheHair.Movement;

import battlecode.common.*;

public class MovementModule {

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
