package AaronBot3.Rubble;

import battlecode.common.*;

public class RubbleModule {

    public static final int ADJUSTMENT_THRESHOLD_NONE = 0;
    public static final int ADJUSTMENT_THRESHOLD_LOW = 1;
    public static final int ADJUSTMENT_THRESHOLD_MEDIUM = 2;
    public static final int ADJUSTMENT_THRESHOLD_HIGH = 3;
    public static final int ADJUSTMENT_THRESHOLD_ALL = 4;

    public Direction getRubbleClearanceDirectionFromDirection(final Direction direction, final RobotController robotController, final int adjustmentThreshold) throws GameActionException {

        final MapLocation currentLocation = robotController.getLocation();

        Direction directionA = direction;
        Direction directionB = direction;

        Direction bestRubbleDirection = null;
        double bestRubbleTotal = Double.MAX_VALUE;

        for (int i = 0; i <= adjustmentThreshold; i++) {

            final MapLocation rubbleLocationA = currentLocation.add(directionA);
            if (robotController.onTheMap(rubbleLocationA)) {

                final double totalRubbleA = robotController.senseRubble(rubbleLocationA);
                if (totalRubbleA > GameConstants.RUBBLE_SLOW_THRESH && totalRubbleA < bestRubbleTotal) {

                    bestRubbleDirection = directionA;
                    bestRubbleTotal = totalRubbleA;

                }

            }

            if (i > 0 && i < 4) {

                final MapLocation rubbleLocationB = currentLocation.add(directionB);
                if (robotController.onTheMap(rubbleLocationB)) {

                    final double totalRubbleB = robotController.senseRubble(rubbleLocationB);
                    if (totalRubbleB > GameConstants.RUBBLE_SLOW_THRESH && totalRubbleB < bestRubbleTotal) {

                        bestRubbleDirection = directionB;
                        bestRubbleTotal = totalRubbleB;

                    }

                }

            }

            if (i < adjustmentThreshold) {

                directionA = directionA.rotateLeft();
                directionB = directionB.rotateRight();

            }

        }
        return bestRubbleDirection;

    }

}
