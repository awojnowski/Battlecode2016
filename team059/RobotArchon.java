package team059;

import battlecode.common.*;
import java.util.ArrayList;

public class RobotArchon implements Robot {

    public void run(final RobotController robotController) throws GameActionException {

        // variables

        final Direction[] directions = { Direction.EAST, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.NORTH_EAST, Direction.SOUTH_WEST, Direction.NORTH_WEST, Direction.SOUTH_EAST };
        ArchonSignal goalArchonSignal = null;
        final int robotID = robotController.getID();

        // make contact with other archons

        this.sendArchonSignal(robotController);

        // run the main loop

        while (true) {

            final int roundNumber = robotController.getRoundNum();
            if (roundNumber == 1) {

                // on the first round, we want to rally with other archons

                final ArchonSignal localArchonSignal = new ArchonSignal();
                localArchonSignal.robotID = robotID;

                final ArchonSignal[] archons = this.collectArchonsFromSignals(robotController.emptySignalQueue(), robotController.getTeam());
                ArchonSignal bestArchonSignal = localArchonSignal;
                for (int i = 0; i < archons.length; i++) {

                    final ArchonSignal archonSignal = archons[i];
                    if (archonSignal.robotID < bestArchonSignal.robotID) {

                        bestArchonSignal = archonSignal;

                    }

                }
                if (bestArchonSignal.robotID != robotController.getID()) {

                    goalArchonSignal = bestArchonSignal;

                } else {

                    goalArchonSignal = null;

                }

            }

            // handle movement

            if (robotController.isCoreReady()) {

                if (goalArchonSignal != null) {

                    // try to rally with other archons

                    boolean didMove = false;
                    final MapLocation location = robotController.getLocation();
                    final Direction directionTowardsArchonSignal = location.directionTo(goalArchonSignal.location);
                    for (int i = 0; i < 3; i++) {

                        Direction movementDirection = directionTowardsArchonSignal;
                        if (i == 1) {

                            movementDirection = robotID % 2 == 0 ? movementDirection.rotateLeft() : movementDirection.rotateRight();

                        } else if (i == 2) {

                            movementDirection = robotID % 2 == 0 ? movementDirection.rotateRight() : movementDirection.rotateLeft();

                        }
                        if (robotController.canMove(movementDirection)) {

                            robotController.move(movementDirection);
                            didMove = true;
                            break;

                        }

                    }

                    final int distance = location.distanceSquaredTo(goalArchonSignal.location);
                    if (!didMove || distance < 35) {

                        goalArchonSignal = null;

                    }

                } else {

                    // let's just chill out and build units

                    boolean builtUnit = false;

                    if (robotController.getTeamParts() >= RobotType.TURRET.partCost) {

                        for (int i = 0; i < directions.length; i ++) {

                            final Direction direction = directions[i];
                            if (robotController.canBuild(direction, RobotType.TURRET)) {

                                robotController.build(direction, RobotType.TURRET);
                                builtUnit = true;
                                break;

                            }

                        }

                    }

                    if (!builtUnit) {

                        // try to clear rubble

                        final double rubbleThreshold = 49;
                        double bestRubbleDifference = Integer.MAX_VALUE;
                        Direction bestRubbleDirection = null;
                        for (int i = 0; i < directions.length; i ++) {

                            final Direction direction = directions[i];
                            final MapLocation rubbleLocation = robotController.getLocation().add(direction);
                            final double rubbleTotal = robotController.senseRubble(rubbleLocation);
                            final double rubbleDifference = rubbleTotal - rubbleThreshold;
                            if (rubbleDifference > 0 && rubbleDifference < bestRubbleDifference) {

                                bestRubbleDifference = rubbleDifference;
                                bestRubbleDirection = direction;

                            }

                        }
                        if (bestRubbleDirection != null) {

                            robotController.clearRubble(bestRubbleDirection);

                        }

                    }

                }

            }

            // handle healing

            RobotInfo injuredRobot = null;
            final RobotInfo[] friendlyRobots = robotController.senseNearbyRobots(robotController.getType().attackRadiusSquared, robotController.getTeam());
            for (int i = 0; i < friendlyRobots.length; i++) {

                final RobotInfo robot = friendlyRobots[i];
                if (robot.health < robot.maxHealth) {

                    if (injuredRobot == null || robot.health < injuredRobot.health) {

                        injuredRobot = robot;

                    }

                }

            }
            if (injuredRobot != null) {

                robotController.repair(injuredRobot.location);

            }

            Clock.yield();

        }

    }

    /**
     * Archon Connection
     */

    private class ArchonSignal {

        public MapLocation location;
        public int robotID;

    }

    private ArchonSignal[] collectArchonsFromSignals(final Signal[] signals, final Team team) {

        final ArrayList<ArchonSignal> list = new ArrayList<ArchonSignal>();
        for (int i = 0; i < signals.length; i++) {

            final Signal signal = signals[i];
            if (signal.getTeam() != team) {

                continue;

            }

            final int[] message = signal.getMessage();
            if (message != null && message.length == 2 && message[0] == 69 && message[1] == 69) {

                final ArchonSignal archonSignal = new ArchonSignal();
                archonSignal.location = signal.getLocation();
                archonSignal.robotID = signal.getRobotID();
                list.add(archonSignal);

            }

        }

        final ArchonSignal[] archons = new ArchonSignal[list.size()];
        for (int i = 0; i < archons.length; i++) {

            archons[i] = list.get(i);

        }
        return archons;

    }

    private void sendArchonSignal(final RobotController robotController) throws GameActionException {

        robotController.broadcastMessageSignal(69, 69, 3000);

    }

}
