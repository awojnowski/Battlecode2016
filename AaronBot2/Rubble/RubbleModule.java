package AaronBot2.Rubble;

import battlecode.common.*;

public class RubbleModule {

    public Direction getAnyRubbleClearanceDirectionFromDirection(final Direction direction, final RobotController robotController) throws GameActionException {

        final MapLocation mapLocation = robotController.getLocation();
        Direction rubbleClearanceDirection = direction;
        for (int i = 0; i < 8; i++) {

            final MapLocation rubbleLocation = mapLocation.add(rubbleClearanceDirection);
            if (robotController.onTheMap(rubbleLocation) && robotController.senseRubble(rubbleLocation) > 0) {

                break;

            }
            rubbleClearanceDirection = robotController.getID() % 2 == 0 ? rubbleClearanceDirection.rotateLeft() : rubbleClearanceDirection.rotateRight();

        }
        return rubbleClearanceDirection;

    }

    public Direction getRubbleClearanceDirectionFromTargetDirection(final Direction direction, final RobotController robotController) throws GameActionException {

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
