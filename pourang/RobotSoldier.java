package pourang;

import battlecode.common.*;
import java.util.Random;

public class RobotSoldier implements Robot {

    public void run(final RobotController robotController) throws GameActionException {

        Direction[] directions = { Direction.EAST, Direction.NORTH_EAST, Direction.NORTH, Direction.NORTH_WEST, Direction.WEST, Direction.SOUTH_WEST, Direction.SOUTH, Direction.SOUTH_EAST };
        final Random random = new Random(robotController.getID());
        RobotInfo lastAttackedEnemy = null;
        final Team team = robotController.getTeam();

        while (true) {

            String status = "";

            if (lastAttackedEnemy != null && lastAttackedEnemy.health <= 0) {

                lastAttackedEnemy = null;

            }

            RobotInfo attackedEnemy = null;

            RobotInfo[] robots = robotController.senseNearbyRobots();
            if (robotController.isWeaponReady()) {

                RobotInfo bestEnemy = null;
                final MapLocation location = robotController.getLocation();

                for (int i = 0; i < robots.length; i++) {

                    final RobotInfo robot = robots[i];
                    if (robot.team == team || robot.team == Team.NEUTRAL) {

                        continue;

                    }

                    if (robotController.getType().attackRadiusSquared < location.distanceSquaredTo(robot.location)) {

                        continue;

                    }

                    if (bestEnemy == null) {

                        bestEnemy = robot;
                        continue;

                    }

                    if (robot == lastAttackedEnemy) {

                        bestEnemy = lastAttackedEnemy;
                        break;

                    }

                    if (robot.type == RobotType.ZOMBIEDEN) {

                        if (bestEnemy.type == RobotType.ZOMBIEDEN) {

                            if (robot.health < bestEnemy.health) {

                                bestEnemy = robot;

                            }

                        }
                        continue;

                    }

                    if (robot.health < bestEnemy.health) {

                        bestEnemy = robot;

                    }

                }

                if (bestEnemy != null && robotController.canAttackLocation(bestEnemy.location)) {

                    robotController.attackLocation(bestEnemy.location);
                    attackedEnemy = bestEnemy;
                    lastAttackedEnemy = attackedEnemy;

                    status += "attacked ";

                }

            }
            if (robotController.isCoreReady() && attackedEnemy == null) {

                if (lastAttackedEnemy != null) {

                    // move towards the den

                    final int robotID = robotController.getID();
                    final Direction direction = robotController.getLocation().directionTo(lastAttackedEnemy.location);
                    for (int i = 0; i < 3; i++) {

                        Direction moveDirection = direction;
                        if (i == 1) {

                            moveDirection = robotID % 2 == 0 ? moveDirection.rotateLeft() : moveDirection.rotateRight();

                        } else if (i == 2) {

                            moveDirection = robotID % 2 == 0 ? moveDirection.rotateRight() : moveDirection.rotateLeft();

                        }
                        if (robotController.canMove(moveDirection)) {

                            robotController.move(moveDirection);

                            status += "moved ";

                        }

                    }

                } else {

                    // move randomly

                    final Direction direction = directions[random.nextInt(directions.length)];
                    if (robotController.canMove(direction)) {

                        robotController.move(direction);

                        status += "moved ";

                    }

                }

            }
            robotController.setIndicatorString(0, status + robotController.getCoreDelay());
            Clock.yield();

        }

    }

}