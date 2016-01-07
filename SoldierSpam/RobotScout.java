package SoldierSpam;

import battlecode.common.*;
import java.util.Random;

public class RobotScout implements Robot {

    public void run(final RobotController robotController) throws GameActionException {

        Direction[] directions = { Direction.EAST, Direction.NORTH_EAST, Direction.NORTH, Direction.NORTH_WEST, Direction.WEST, Direction.SOUTH_WEST, Direction.SOUTH, Direction.SOUTH_EAST };
        final Random random = new Random(robotController.getID());
        final Team team = robotController.getTeam();
        Direction facingDirection = directions[random.nextInt(directions.length)];
        boolean kamikaze = false;

        while (true) {

            if (robotController.isCoreReady()) {
                MapLocation currentLocation = robotController.getLocation();
                MapLocation moveLocation = null;
                RobotInfo[] nearbyRobots = robotController.senseNearbyRobots(53);
                MapLocation nearestDen = null;
                int nearestDenDistance = Integer.MAX_VALUE;

                for (int i = 0; i < nearbyRobots.length; i++) {
                    
                    final RobotInfo robot = nearbyRobots[i];

                    if (robot.team == Team.ZOMBIE || robot.team == team.opponent()) {

                        if (robot.type == RobotType.ZOMBIEDEN) { // Found a den

                            boolean occupied = false;
                            
                            // Check if there is another scout near it already
                            for (int j = 0; j < directions.length; j++) {
                                final MapLocation searchLocation = robot.location.add(directions[j]);
                                if (robotController.canSenseLocation(searchLocation)) {
                                    final RobotInfo robotNearDen = robotController.senseRobotAtLocation(searchLocation);
                                    if (robotNearDen != null && robotNearDen.team == team && robotNearDen.type == RobotType.SCOUT && robotNearDen.ID != robotController.getID()) {
                                        occupied = true;
                                        break;
                                    }
                                }
                            }
                            if (occupied) {
                                continue;
                            }

                            final int distance = currentLocation.distanceSquaredTo(robot.location);

                            if (distance < nearestDenDistance) {

                                nearestDen = robot.location;
                                nearestDenDistance = distance;

                            }

                        } else { // Found zombies or enemies

                            if (currentLocation.distanceSquaredTo(robot.location) < robot.type.attackRadiusSquared * 2) {

                                if (robot.team == Team.ZOMBIE || robotController.getRoundNum() < robotController.getRoundLimit() * 0.6) {

                                    moveLocation = currentLocation.subtract(currentLocation.directionTo(robot.location));
                                    facingDirection = currentLocation.directionTo(moveLocation);

                                } else {

                                    kamikaze = true;

                                    if (robotController.isCoreReady()) {

                                        robotController.broadcastSignal(2000);

                                    }
                                }

                                break;

                            }

                        }

                    } else { // No nearby enemies or zombies

                        kamikaze = false;

                    }

                }

                if (moveLocation == null && nearestDen != null) {

                    moveLocation = nearestDen;

                }

                if (moveLocation != null && !kamikaze) { // Move towards den or send signal if near

                    if (nearestDenDistance < 4) {

                        robotController.broadcastSignal(1000);

                    } else {

                        final Direction direction = currentLocation.directionTo(moveLocation);

                        final int robotID = robotController.getID();
                        for (int i = 0; i < 5; i++) {
                            Direction moveDirection = direction;
                            if (i == 1) {
                                moveDirection = robotID % 2 == 0 ? moveDirection.rotateLeft() : moveDirection.rotateRight();
                            } else if (i == 2) {
                                moveDirection = robotID % 2 == 0 ? moveDirection.rotateRight() : moveDirection.rotateLeft();
                            } else if (i == 3) {
                                moveDirection = robotID % 2 == 0 ? moveDirection.rotateRight().rotateRight() : moveDirection.rotateLeft().rotateLeft();
                            } else if (i == 4) {
                                moveDirection = robotID % 2 == 0 ? moveDirection.rotateLeft().rotateLeft() : moveDirection.rotateRight().rotateRight();
                            }
                            if (robotController.isCoreReady() && robotController.canMove(moveDirection)) {
                                robotController.move(moveDirection);
                                break;
                            }
                        }

                    }

                } else if (robotController.isCoreReady() && !kamikaze) { // Scout out map

                    // TODO: Can be infinite loop if they're surrounded
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