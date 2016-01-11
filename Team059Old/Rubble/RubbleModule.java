package Team059Old.Rubble;

import battlecode.common.*;

public class RubbleModule {

    public Direction rubbleClearanceDirectionFromTargetDirection(final Direction direction, final RobotController robotController) throws GameActionException {

        final MapLocation mapLocation = robotController.getLocation();

        Direction scanDirection = direction;
        MapLocation newLocation = mapLocation.add(scanDirection);
        if (robotController.onTheMap(newLocation) && robotController.senseRubble(newLocation) > 0) {

            return direction;

        }

        final boolean divisible = robotController.getID() % 2 == 0;

        scanDirection = divisible ? direction.rotateLeft() : direction.rotateRight();
        newLocation = mapLocation.add(scanDirection);
        if (robotController.onTheMap(newLocation) && robotController.senseRubble(newLocation) > 0) {

            return scanDirection;

        }
        scanDirection = divisible ? direction.rotateRight() : direction.rotateLeft();
        newLocation = mapLocation.add(scanDirection);
        if (robotController.onTheMap(newLocation) && robotController.senseRubble(newLocation) > 0) {

            return scanDirection;

        }
        scanDirection = divisible ? direction.rotateLeft().rotateLeft() : direction.rotateRight().rotateRight();
        newLocation = mapLocation.add(scanDirection);
        if (robotController.onTheMap(newLocation) && robotController.senseRubble(newLocation) > 0) {

            return scanDirection;

        }
        scanDirection = divisible ? direction.rotateRight().rotateRight() : direction.rotateLeft().rotateLeft();
        newLocation = mapLocation.add(scanDirection);
        if (robotController.onTheMap(newLocation) && robotController.senseRubble(newLocation) > 0) {

            return scanDirection;

        }
        return null;


    }

}
