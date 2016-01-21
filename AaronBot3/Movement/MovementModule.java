package AaronBot3.Movement;

import battlecode.common.*;
import java.util.ArrayList;

public class MovementModule {

    private ArrayList<MapLocation> previousMovementLocations = new ArrayList<MapLocation>();
    private int lastMovementLocationTurnNumber = 0;

    /*
    MOVEMENT LOCATIONS
     */

    // call this when you move somewhere
    public void addMovementLocation(final MapLocation location, final RobotController robotController) {

        if (robotController.getRoundNum() - this.lastMovementLocationTurnNumber > 10) {

            this.previousMovementLocations.clear();

        }
        this.lastMovementLocationTurnNumber = robotController.getRoundNum();
        this.previousMovementLocations.add(location);

    }

    // .... self explanatory
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

    // used if you make some action that isn't a movement to get to the target location
    // (like clearing rubble)
    public void extendLocationInvalidationTurn(final RobotController robotController) {

        this.lastMovementLocationTurnNumber = robotController.getRoundNum();

    }

}
