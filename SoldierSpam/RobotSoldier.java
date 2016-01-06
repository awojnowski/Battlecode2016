package SoldierSpam;

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
            Signal[] signals = robotController.emptySignalQueue();

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
                        if (bestEnemy != null && bestEnemy.type == RobotType.ZOMBIEDEN) {
                            if (robot.health < bestEnemy.health) {
                                bestEnemy = robot;
                            }
                        } else {
                            bestEnemy = robot;
                        }
                        continue;
                    }
                    if (robot.health < bestEnemy.health) {
                        bestEnemy = robot;
                    }

                }

                if (bestEnemy != null && robotController.canAttackLocation(bestEnemy.location)) {

                    //if (lastAttackedEnemy == null) {
                    //}

                    robotController.attackLocation(bestEnemy.location);
                    attackedEnemy = bestEnemy;
                    lastAttackedEnemy = attackedEnemy;
                    if (signals.length < 5) {

                        robotController.broadcastSignal(100);

                    }

                    status += "attacked ";

                } else {

                    lastAttackedEnemy = null;

                }

            }
            if (robotController.isCoreReady() && attackedEnemy == null) {

                if (lastAttackedEnemy != null || signals.length > 0) { // move towards the last attacked enemy or closest signal

                    Direction direction;

                    if (lastAttackedEnemy != null) {

                        direction = robotController.getLocation().directionTo(lastAttackedEnemy.location);

                    } else { // Find closest signal

                        int minDistance = Integer.MAX_VALUE;
                        MapLocation closestLocation = null;

                        for (int i = 0; i < signals.length; i++) {

                            int distance = robotController.getLocation().distanceSquaredTo(signals[i].getLocation());

                            if (distance < minDistance) {

                                minDistance = distance;
                                closestLocation = signals[i].getLocation();

                            }

                        }

                        direction = robotController.getLocation().directionTo(closestLocation);

                    }

                    //if (lastAttackedEnemy != null && lastAttackedEnemy.type == RobotType.ZOMBIEDEN && robotController.getLocation().distanceSquaredTo(lastAttackedEnemy.location) > 3) {
                    if (lastAttackedEnemy == null) { // For some reason this works way better???

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

                                status += "moved ";
                                break;

                            }

                        }

                    }

                } else { // Move randomly around team

                    Direction direction = directions[random.nextInt(directions.length)]; // Initialize to random dir
                    RobotInfo[] closeTeammates = robotController.senseNearbyRobots(3, team);

                    if (closeTeammates.length == 0) { // Move towards team if far away

                        RobotInfo[] nearbyTeammates = robotController.senseNearbyRobots(24, team);

                        if (nearbyTeammates.length > 0) {

                            RobotInfo teammateToMoveTowards = nearbyTeammates[0];

                            for (int i = 0; i < nearbyTeammates.length; i++) { // Move towards archon if there is one

                                if (nearbyTeammates[i].type == RobotType.ARCHON) {

                                    teammateToMoveTowards = nearbyTeammates[i];
                                    break;

                                }

                            }

                            direction = robotController.getLocation().directionTo(teammateToMoveTowards.location);

                        }

                    }

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