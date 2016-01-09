package AaronBot2.Rubble;

import battlecode.common.*;

public class RubbleModule {

    public Direction rubbleClearanceDirectionFromTargetDirection(final Direction direction, final RobotController robotController) {

        final MapLocation mapLocation = robotController.getLocation();
        if (robotController.canSenseLocation(mapLocation) && robotController.senseRubble(mapLocation) > 0) {

            return direction;

        }

        final boolean divisible = robotController.getID() % 2 == 0;
        Direction scanDirection = divisible ? direction.rotateLeft() : direction.rotateRight();
        MapLocation newLocation = mapLocation.add(scanDirection);
        if (robotController.canSenseLocation(newLocation) && robotController.senseRubble(newLocation) > 0) {

            return scanDirection;

        }
        scanDirection = divisible ? direction.rotateRight() : direction.rotateLeft();
        newLocation = mapLocation.add(scanDirection);
        if (robotController.canSenseLocation(newLocation) && robotController.senseRubble(newLocation) > 0) {

            return scanDirection;

        }
        scanDirection = divisible ? direction.rotateLeft().rotateLeft() : direction.rotateRight().rotateRight();
        newLocation = mapLocation.add(scanDirection);
        if (robotController.canSenseLocation(newLocation) && robotController.senseRubble(newLocation) > 0) {

            return scanDirection;

        }
        scanDirection = divisible ? direction.rotateRight().rotateRight() : direction.rotateLeft().rotateLeft();
        newLocation = mapLocation.add(scanDirection);
        if (robotController.canSenseLocation(newLocation) && robotController.senseRubble(newLocation) > 0) {

            return scanDirection;

        }
        return null;


    }

}
