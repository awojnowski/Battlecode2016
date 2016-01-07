package AaronBot.Controllers;

import battlecode.common.*;

public class RubbleHelper {

    public static final double ClearingThreshold = 49;

    public static Direction findOptimalRubbleInDirection(final RobotController robotController, final MapLocation currentLocation, final Direction[] directions) {

        return RubbleHelper.findOptimalRubbleDirection(robotController, directions, currentLocation, -1);

    }

    /*
    This method will find the best rubble pile to clear within an array of directions.

    It works by finding the rubble closest to the clearing threshold (to make rubble clearing aster) and if all of the
    rubble is below the clearing threshold, it will choose the rubble pile with the most rubble.

    A preferred direction index can be provided, which will bias rubble clearing in a certain direction over other directions.
     */
    public static Direction findOptimalRubbleDirection(final RobotController robotController, final Direction[] directions, final MapLocation currentLocation, final int preferredDirectionIndex) {

        double bestRubbleDifference = Integer.MAX_VALUE;
        Direction bestRubbleDirection = null;

        double mostRubble = 0;
        Direction mostRubbleDirection = null;

        for (int i = 0; i < directions.length; i ++) {

            final Direction direction = directions[i];
            final MapLocation rubbleLocation = currentLocation.add(direction);
            final double rubbleTotal = robotController.senseRubble(rubbleLocation);

            if (rubbleTotal > mostRubble) {

                mostRubble = rubbleTotal;
                mostRubbleDirection = direction;

            }

            final double rubbleDifference = rubbleTotal - RubbleHelper.ClearingThreshold;
            if (rubbleDifference > 0 && rubbleDifference < bestRubbleDifference) {

                bestRubbleDifference = rubbleDifference;
                bestRubbleDirection = direction;

                if (i == preferredDirectionIndex) {

                    break;

                }

            }

        }
        if (bestRubbleDirection == null) {

            bestRubbleDirection = mostRubbleDirection;

        }
        return bestRubbleDirection;

    }

}
