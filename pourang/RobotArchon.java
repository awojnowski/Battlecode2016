package pourang;

import battlecode.common.*;

public class RobotArchon implements Robot {

    public void run(final RobotController robotController) throws GameActionException {

        Direction[] directions = { Direction.EAST, Direction.NORTH_EAST, Direction.NORTH, Direction.NORTH_WEST, Direction.WEST, Direction.SOUTH_WEST, Direction.SOUTH, Direction.SOUTH_EAST };

        while (true) {

            if (robotController.isCoreReady()) {

                boolean builtRobot = false;

                final int soldierCost = RobotType.SOLDIER.partCost;
                if (robotController.getTeamParts() >= soldierCost) {

                    for (int i = 0; i < directions.length; i++) {

                        if (robotController.canBuild(directions[i], RobotType.SOLDIER)) {

                            robotController.build(directions[i], RobotType.SOLDIER);
                            builtRobot = true;
                            break;

                        }

                    }

                }

                if (!builtRobot) {
                    double bestRubbleDifference = Integer.MAX_VALUE;
                    Direction bestRubbleDirection = null;

                    final MapLocation location = robotController.getLocation();
                    for (int i = 0; i < directions.length; i++) {

                        final MapLocation senseLocation = location.add(directions[i]);
                        final double rubble = robotController.senseRubble(senseLocation);
                        if (rubble > 0 && rubble < bestRubbleDifference) {

                            bestRubbleDifference = rubble;
                            bestRubbleDirection = directions[i];

                        }

                    }
                    if (bestRubbleDirection != null) {

                        robotController.clearRubble(bestRubbleDirection);

                    }

                }

            }
            Clock.yield();

        }

    }

}
