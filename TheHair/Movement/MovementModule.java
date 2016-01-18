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

    public MapLocation getArchonRendezvousLocation(final RobotController robotController) {

        int furthestArchonDistance = 0;
        MapLocation furthestArchonLocation = null;

        MapLocation[] friendlyArchonLocation = robotController.getInitialArchonLocations(robotController.getTeam());
        MapLocation[] enemyArchonLocation = robotController.getInitialArchonLocations(robotController.getTeam().opponent());

        for (int i = 0; i < friendlyArchonLocation.length; i++) {

            final MapLocation testLocation = friendlyArchonLocation[i];
            int distanceTotal = 0;
            for (int j = 0; j < enemyArchonLocation.length; j++) {

                distanceTotal += testLocation.distanceSquaredTo(enemyArchonLocation[j]);

            }
            if (distanceTotal > furthestArchonDistance) {

                furthestArchonDistance = distanceTotal;
                furthestArchonLocation = testLocation;

            }

        }

        return furthestArchonLocation;

    }

    /*
    LOCATIONS / SIDES
     */

    public boolean isLocationOnOurSide(final RobotController robotController, final MapLocation denLocation) {

        int totalDistanceToUs = 0;
        int totalDistanceToThem = 0;

        MapLocation[] friendlyArchonLocations = robotController.getInitialArchonLocations(robotController.getTeam());
        MapLocation[] enemyArchonLocations = robotController.getInitialArchonLocations(robotController.getTeam().opponent());

        for (int i = 0; i < friendlyArchonLocations.length; i++) {

            totalDistanceToUs += friendlyArchonLocations[i].distanceSquaredTo(denLocation);

        }

        for (int i = 0; i < enemyArchonLocations.length; i++) {

            totalDistanceToThem += enemyArchonLocations[i].distanceSquaredTo(denLocation);

        }

        robotController.setIndicatorString(1, totalDistanceToUs + " " + totalDistanceToThem);

        return totalDistanceToUs * 2.5 < totalDistanceToThem;

    }

}
