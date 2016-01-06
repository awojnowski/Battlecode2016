package SoldierSpam;

import battlecode.common.*;
import java.util.Random;

public class RobotScout implements Robot {

    public void run(final RobotController robotController) throws GameActionException {

        Direction[] directions = { Direction.EAST, Direction.NORTH_EAST, Direction.NORTH, Direction.NORTH_WEST, Direction.WEST, Direction.SOUTH_WEST, Direction.SOUTH, Direction.SOUTH_EAST };
        final Random random = new Random(robotController.getID());
        final Team team = robotController.getTeam();
        Direction facingDirection = directions[random.nextInt(directions.length)];

        while (true) {

            if (robotController.isCoreReady()) {

                MapLocation moveLocation = null;
                RobotInfo[] nearbyRobots = robotController.senseNearbyRobots(53);
                MapLocation nearestDen = null;
                int nearestDenDistance = Integer.MAX_VALUE;

                for (int i = 0; i < nearbyRobots.length; i++) {

                    if (nearbyRobots[i].team == Team.ZOMBIE) {

                        if (nearbyRobots[i].type == RobotType.ZOMBIEDEN) { // Found a den

                            final int distance = robotController.getLocation().distanceSquaredTo(nearbyRobots[i].location);

                            if (distance < nearestDenDistance) {

                                nearestDen = nearbyRobots[i].location;
                                nearestDenDistance = distance;

                            }

                        } else { // Found zombies

                            if (robotController.getLocation().distanceSquaredTo(nearbyRobots[i].location) < nearbyRobots[i].type.attackRadiusSquared) {

                                moveLocation = robotController.getLocation().subtract(robotController.getLocation().directionTo(nearbyRobots[i].location));
                                break;

                            }

                        }

                    }

                }

                if (moveLocation == null && nearestDen != null) {

                    moveLocation = nearestDen;

                }

                if (moveLocation != null) { // Move towards den or send signal if near

                    if (nearestDenDistance < 4) {

                        robotController.broadcastSignal(1000);

                    } else {

                        final Direction direction = robotController.getLocation().directionTo(moveLocation);

                        final int robotID = robotController.getID();
                        for (int i = 0; i < 3; i++) {
                            Direction moveDirection = direction;
                            if (i == 1) {
                                moveDirection = robotID % 2 == 0 ? moveDirection.rotateLeft() : moveDirection.rotateRight();
                            } else if (i == 2) {
                                moveDirection = robotID % 2 == 0 ? moveDirection.rotateRight() : moveDirection.rotateLeft();
                            }
                            if (robotController.canMove(moveDirection)) {
                                robotController.move(moveDirection);
                                break;
                            }
                        }

                    }

                } else { // Scout out map

                    // TODO: Can get stuck
                    while (!robotController.canMove(facingDirection)) {

                        facingDirection = directions[random.nextInt(directions.length)];

                    }

                    robotController.move(facingDirection);

                }

            }

            Clock.yield();

        }

    }

}