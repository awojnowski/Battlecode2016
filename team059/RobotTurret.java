package team059;

import battlecode.common.*;
import java.util.Random;

public class RobotTurret implements Robot {

    public void run(final RobotController robotController) throws GameActionException {

        final Direction[] directions = { Direction.EAST, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.NORTH_EAST, Direction.SOUTH_WEST, Direction.NORTH_WEST, Direction.SOUTH_EAST };
        final int attackRadiusSquared = robotController.getType().attackRadiusSquared;
        final Random random = new Random(robotController.getID());
        final Team team = robotController.getTeam();

        int lastTransformationRoundNumber = robotController.getRoundNum();
        int ttmMovements = 0;

        while (true) {

            final MapLocation location = robotController.getLocation();
            final RobotInfo[] robots = robotController.senseNearbyRobots();

            if (robotController.getType() == RobotType.TURRET) {

                if (robotController.isWeaponReady()) {

                    RobotInfo bestRobot = null;
                    for (int i = 0; i < robots.length; i++) {

                        final RobotInfo robot = robots[i];
                        if (robot.team == team || robot.team == Team.NEUTRAL) {

                            continue;

                        }

                        final int distance = location.distanceSquaredTo(robot.location);
                        if (distance < 5 || distance > attackRadiusSquared) {

                            continue;

                        }

                        if (bestRobot == null || robot.health < bestRobot.health) {

                            bestRobot = robot;

                        }

                    }

                    if (bestRobot != null) {

                        if (robotController.canAttackLocation(bestRobot.location)) {

                            robotController.attackLocation(bestRobot.location);

                        }

                    }

                }

                if (robotController.isCoreReady()) {

                    if (robotController.getRoundNum() - lastTransformationRoundNumber > 100) {

                        int totalEnemies = 0;
                        for (int i = 0; i < robots.length; i ++) {

                            final RobotInfo robot = robots[i];
                            if (robot.team == team || robot.team == Team.NEUTRAL) {

                                continue;

                            }
                            totalEnemies += 1;

                        }
                        if (totalEnemies == 0) {

                            // it is safe to move now
                            robotController.pack();

                        }

                    }

                }

            } else if (robotController.getType() == RobotType.TTM) {

                if (robotController.isCoreReady()) {

                    if (ttmMovements < 2) {

                        int increments = 0;
                        for (int i = random.nextInt(directions.length); increments < directions.length; i++, increments ++) {

                            if (i >= directions.length) {

                                i = 0;

                            }
                            final Direction direction = directions[i];
                            final MapLocation newLocation = location.add(direction);
                            boolean isMovementEligible = false;
                            for (int j = 0; j < robots.length; j++) {

                                final RobotInfo robot = robots[j];
                                if (robot.team != team) {

                                    continue;

                                }
                                if (robot.location.distanceSquaredTo(newLocation) < 8) {

                                    isMovementEligible = true;
                                    break;

                                }

                            }
                            if (!isMovementEligible) {

                                continue;

                            }
                            if (robotController.canMove(direction)) {

                                robotController.move(direction);
                                ttmMovements ++;
                                break;

                            }

                        }

                    } else {

                        // all done!

                        lastTransformationRoundNumber = robotController.getRoundNum();
                        ttmMovements = 0;

                        robotController.unpack();

                    }

                }

            }

            Clock.yield();

        }

    }

}
