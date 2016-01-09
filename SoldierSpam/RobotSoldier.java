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

            MapLocation currentLocation = robotController.getLocation();
            String status = "";
            Signal[] signals = robotController.emptySignalQueue();

            if (lastAttackedEnemy != null && lastAttackedEnemy.health <= 0) {
                lastAttackedEnemy = null;
            }

            RobotInfo attackedEnemy = null;

            RobotInfo[] robots = robotController.senseNearbyRobots();
            if (robotController.isWeaponReady()) {

                RobotInfo bestEnemy = null;
                final MapLocation location = currentLocation;

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

                // Move in if they're attacking den, have space in front, and soldier behind
                boolean movedIn = false;

                if (lastAttackedEnemy != null && lastAttackedEnemy.type == RobotType.ZOMBIEDEN) {

                    Direction directionToEnemy = currentLocation.directionTo(lastAttackedEnemy.location);

                    // If there is no robot in front
                    if (robotController.senseRobotAtLocation(currentLocation.add(directionToEnemy)) == null) {

                        if (currentLocation.distanceSquaredTo(lastAttackedEnemy.location) > 8) {

                            RobotInfo[] adjacentTeammates = robotController.senseNearbyRobots(3, team);

                            if (adjacentTeammates.length > 3 && robotController.isCoreReady() && robotController.canMove(directionToEnemy)) {

                                robotController.move(directionToEnemy);
                                movedIn = true;

                            }

                        }

                    }

                }

                // Attack if they have a target

                if (!movedIn && bestEnemy != null && robotController.canAttackLocation(bestEnemy.location)) {

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

            // MOVEMENT

            if (robotController.isCoreReady() && attackedEnemy == null) {

                if (lastAttackedEnemy != null || signals.length > 0) { // move towards the last attacked enemy or closest signal

                    Direction direction;

                    if (lastAttackedEnemy != null) {

                        direction = currentLocation.directionTo(lastAttackedEnemy.location);

                    } else { // Find closest signal

                        int minDistance = Integer.MAX_VALUE;
                        MapLocation closestLocation = null;

                        for (int i = 0; i < signals.length; i++) {

                            int distance = currentLocation.distanceSquaredTo(signals[i].getLocation());

                            if (distance < minDistance) {

                                minDistance = distance;
                                closestLocation = signals[i].getLocation();

                            }

                        }

                        direction = currentLocation.directionTo(closestLocation);

                    }

                    //if (lastAttackedEnemy != null && lastAttackedEnemy.type == RobotType.ZOMBIEDEN && currentLocation.distanceSquaredTo(lastAttackedEnemy.location) > 3) {
                    if (lastAttackedEnemy == null) { // For some reason this works way better???

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
                            if (robotController.canMove(moveDirection)) {

                                robotController.move(moveDirection);
                                status += "moved ";
                                break;

                            } else if (i == 2) { // If can't go straight, left, or right

                                double rubble = robotController.senseRubble(currentLocation.add(moveDirection));

                                if (rubble > 50 && rubble <= 5000) { // TODO: Find optimal move vs clear rubble algorithm

                                    robotController.clearRubble(moveDirection);
                                    status += "cleared rubble ";
                                    break;

                                }

                            }

                        }

                    }

                } else { // Move randomly around team

                    Direction direction = directions[random.nextInt(directions.length)]; // Initialize to random dir
                    RobotInfo[] closeTeammates = robotController.senseNearbyRobots(15, team); // How close they stay to their team, lower means they'll stay closer

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

                            direction = currentLocation.directionTo(teammateToMoveTowards.location);

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