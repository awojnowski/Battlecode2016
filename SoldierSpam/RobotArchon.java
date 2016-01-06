package SoldierSpam;

import battlecode.common.*;
import java.util.Random;

public class RobotArchon implements Robot {

    public void run(final RobotController robotController) throws GameActionException {

        Direction[] directions = { Direction.EAST, Direction.NORTH_EAST, Direction.NORTH, Direction.NORTH_WEST, Direction.WEST, Direction.SOUTH_WEST, Direction.SOUTH, Direction.SOUTH_EAST };
        final Random random = new Random(robotController.getID());
        final Team team = robotController.getTeam();

        while (true) {

            if (robotController.isCoreReady()) {

                boolean builtRobot = false;
                boolean activated = false;

                MapLocation bestLocation = null;
                RobotInfo[] nearbyRobots = robotController.senseNearbyRobots(35);

                for (int i = 0; i < nearbyRobots.length; i++) {

                    if ((nearbyRobots[i].team == Team.ZOMBIE && nearbyRobots[i].type != RobotType.ZOMBIEDEN) || nearbyRobots[i].team == team.opponent()) {

                        Direction directionToEnemy = robotController.getLocation().directionTo(nearbyRobots[i].location);
                        bestLocation = robotController.getLocation().subtract(directionToEnemy);
                        robotController.broadcastSignal(100);

                    }

                }

                RobotType typeToBuild = RobotType.SOLDIER;

                if (robotController.getRoundNum() == 0) { // Might not build if somehow near enemies at start
                    typeToBuild = RobotType.SCOUT;
                }

                if (bestLocation == null && robotController.getTeamParts() >= typeToBuild.partCost) {

                    for (int i = 0; i < directions.length; i++) {

                        if (robotController.canBuild(directions[i], typeToBuild)) {

                            robotController.build(directions[i], typeToBuild);
                            builtRobot = true;
                            break;

                        }

                    }

                }

                if (!builtRobot) {

                    if (robotController.isCoreReady()) {

                        RobotInfo[] nearbyNeutrals = robotController.senseNearbyRobots(35, Team.NEUTRAL);

                        if (bestLocation == null && nearbyNeutrals.length > 0) { // Look for neutrals

                            final MapLocation currentLocation = robotController.getLocation();
                            RobotInfo closestNeutral = nearbyNeutrals[0];
                            int closestNeutralDistance = currentLocation.distanceSquaredTo(closestNeutral.location);

                            for (int i = 0; i < nearbyNeutrals.length; i++) {

                                final int distance = currentLocation.distanceSquaredTo(nearbyNeutrals[i].location);

                                if (distance < closestNeutralDistance) {

                                    closestNeutral = nearbyNeutrals[i];
                                    closestNeutralDistance = distance;

                                }

                            }

                            if (currentLocation.isAdjacentTo(closestNeutral.location)) {

                                robotController.activate(closestNeutral.location);

                            } else {

                                bestLocation = closestNeutral.location;
                                activated = true;

                            }

                        } else if (bestLocation == null) { // Look for parts

                            // TODO: Super inefficient, should avoid checking squares multiple times (costs ~2500 bytecodes)
                            MapLocation[] surroundingLocations = MapLocation.getAllMapLocationsWithinRadiusSq(robotController.getLocation(), 35);
                            MapLocation mostPartsLocation = null;
                            double maxParts = 0;

                            for (int i = 0; i < surroundingLocations.length; i++) {

                                double parts = robotController.senseParts(surroundingLocations[i]);

                                if (parts > maxParts) {

                                    mostPartsLocation = surroundingLocations[i];
                                    maxParts = parts;

                                }

                            }

                            if (mostPartsLocation != null) {

                                bestLocation = mostPartsLocation;

                            }

                        }

                        if (bestLocation != null && !activated) {

                            // move towards the best location

                            final int robotID = robotController.getID();
                            final Direction direction = robotController.getLocation().directionTo(bestLocation);
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

                        } else { // Move randomly near teammates

                            Direction direction = directions[random.nextInt(directions.length)]; // Initialize to random dir
                            RobotInfo[] closeTeammates = robotController.senseNearbyRobots(15, team);
                            RobotInfo[] nearbyTeammates = robotController.senseNearbyRobots(35, team);

                            if (closeTeammates.length == 0 && nearbyTeammates.length > 0) { // Move towards team if far away

                                // TODO: Make this average location between teammates (instead of first one)
                                direction = robotController.getLocation().directionTo(nearbyTeammates[0].location);

                            }

                            if (robotController.canMove(direction)) {

                                robotController.move(direction);

                            }

                        }

                    }

                    if (robotController.isCoreReady()) {

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

            }
            Clock.yield();

        }

    }

}
