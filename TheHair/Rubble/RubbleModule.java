package TheHair.Rubble;

import battlecode.common.*;

public class RubbleModule {

    public Direction getOptimalRubbleClearanceDirection(final RobotController robotController) throws GameActionException {

        final MapLocation currentLocation = robotController.getLocation();

        MapLocation bestRubbleLocation = null;
        double bestRubbleDifference = Double.MAX_VALUE;

        MapLocation lowestRubbleLocation = null;
        double lowestRubbleTotal = Double.MAX_VALUE;

        for (int i = -1; i <= 1; i ++) {

            for (int j = -1; j <= 1; j ++) {

                final MapLocation checkLocation = new MapLocation(currentLocation.x + i, currentLocation.y + j);
                if (!robotController.onTheMap(checkLocation)) {

                    continue;

                }
                final double checkLocationRubble = robotController.senseRubble(checkLocation);
                if (checkLocationRubble < lowestRubbleTotal) {

                    lowestRubbleLocation = checkLocation;
                    lowestRubbleTotal = checkLocationRubble;

                }
                final double checkLocationRubbleDifference = checkLocationRubble - 50;
                if (checkLocationRubbleDifference < bestRubbleDifference && checkLocationRubbleDifference >= 0) {

                    bestRubbleLocation = checkLocation;
                    bestRubbleDifference = checkLocationRubbleDifference;

                }

            }

        }
        if (bestRubbleLocation == null) {

            bestRubbleLocation = lowestRubbleLocation;

        }
        if (bestRubbleLocation == null) {

            return null;

        }
        return currentLocation.directionTo(bestRubbleLocation);

    }

    public Direction getAnyRubbleClearanceDirectionFromDirection(final Direction direction, final RobotController robotController) throws GameActionException {

        boolean found = false;
        final MapLocation mapLocation = robotController.getLocation();
        Direction rubbleClearanceDirection = direction;
        for (int i = 0; i < 8; i++) {

            final MapLocation rubbleLocation = mapLocation.add(rubbleClearanceDirection);
            if (robotController.onTheMap(rubbleLocation) && robotController.senseRubble(rubbleLocation) > 0) {

                found = true;
                break;

            }
            rubbleClearanceDirection = robotController.getID() % 2 == 0 ? rubbleClearanceDirection.rotateLeft() : rubbleClearanceDirection.rotateRight();

        }
        if (!found) {

            return null;

        }
        return rubbleClearanceDirection;

    }

    public Direction getRubbleClearanceDirectionFromTargetDirection(final Direction direction, final RobotController robotController) throws GameActionException {

        if (direction == Direction.OMNI) {

            return null;

        }

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
