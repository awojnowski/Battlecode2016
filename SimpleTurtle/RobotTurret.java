package SimpleTurtle;

import battlecode.common.*;

import java.util.Random;

public class RobotTurret implements Robot {

    public void run(final RobotController robotController) throws GameActionException {

        final Team team = robotController.getTeam();
        Direction[] directions = { Direction.EAST, Direction.NORTH_EAST, Direction.NORTH, Direction.NORTH_WEST, Direction.WEST, Direction.SOUTH_WEST, Direction.SOUTH, Direction.SOUTH_EAST };
        final Random random = new Random(robotController.getID());
        boolean moved = false;

        while (true) {

            if (robotController.getType() == RobotType.TTM) {

                if (robotController.isCoreReady()) {

                    if (!moved) {

                        // move randomly

                        final Direction direction = directions[random.nextInt(directions.length)];
                        if (robotController.canMove(direction)) {

                            robotController.move(direction);
                            moved = true;

                        }

                    } else {

                        robotController.unpack();
                        moved = false;

                    }

                }

            } else {

                if (robotController.getID() % 400 == robotController.getRoundNum() % 400) {
                    robotController.pack();
                }

                if (robotController.isWeaponReady()) {

                    RobotInfo[] robots = robotController.senseNearbyRobots();
                    for (int i = 0; i < robots.length; i++) {

                        final RobotInfo robot = robots[i];
                        if (robot.team == team || robot.team == Team.NEUTRAL) {

                            continue;

                        }

                        if (robotController.canAttackLocation(robot.location)) {

                            robotController.attackLocation(robot.location);
                            break;

                        }

                    }

                }

            }

            Clock.yield();

        }

    }

}