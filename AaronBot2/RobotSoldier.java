package AaronBot2;

import AaronBot2.Signals.CommunicationModule;
import AaronBot2.Signals.CommunicationModuleSignal;
import battlecode.common.*;

import java.util.Enumeration;
import java.util.Random;

public class RobotSoldier implements Robot {

    public void run(final RobotController robotController) throws GameActionException {

        final CommunicationModule communicationModule = new CommunicationModule();
        final Direction[] directions = { Direction.EAST, Direction.NORTH_EAST, Direction.NORTH, Direction.NORTH_WEST, Direction.WEST, Direction.SOUTH_WEST, Direction.SOUTH, Direction.SOUTH_EAST };
        final Random random = new Random(robotController.getID());

        CommunicationModuleSignal objectiveSignal = null;

        while (true) {

            communicationModule.processIncomingSignals(robotController);

            // let's get a new assignment if necessary

            if (robotController.getRoundNum() % 25 == 0 && objectiveSignal == null) {

                final MapLocation location = robotController.getLocation();
                int closestLocationDistance = Integer.MAX_VALUE;

                final Enumeration<CommunicationModuleSignal> communicationModuleSignals = communicationModule.zombieDens.elements();
                while (communicationModuleSignals.hasMoreElements()) {

                    final CommunicationModuleSignal signal = communicationModuleSignals.nextElement();
                    final int distance = signal.location.distanceSquaredTo(location);
                    if (distance < closestLocationDistance) {

                        objectiveSignal = signal;

                    }

                }

            }

            // now let's verify existing information

            communicationModule.verifyCommunicationsInformation(robotController, false);

            // now let's try move toward an assignment

            if (robotController.isWeaponReady()) {

                if (objectiveSignal != null) {

                    if (objectiveSignal.type == CommunicationModuleSignal.TYPE_ZOMBIEDEN) {

                        final MapLocation location = robotController.getLocation();
                        final RobotType ourType = robotController.getType();
                        final RobotInfo[] zombies = robotController.senseNearbyRobots(ourType.sensorRadiusSquared, Team.ZOMBIE);
                        for (int i = 0; i < zombies.length; i++) {

                            final RobotInfo robot = zombies[i];
                            if (robot.ID == objectiveSignal.robotIdentifier) {

                                if (location.distanceSquaredTo(robot.location) <= ourType.attackRadiusSquared) {

                                    if (robotController.canAttackLocation(robot.location)) {

                                        robotController.attackLocation(robot.location);
                                        break;

                                    }

                                }

                            }

                        }

                    }

                }

            }

            if (robotController.isCoreReady()) {

                if (objectiveSignal != null) {

                    final MapLocation location = robotController.getLocation();
                    final MapLocation objectiveLocation = objectiveSignal.location;
                    final Direction direction = location.directionTo(objectiveLocation);
                    if (robotController.canMove(direction)) {

                        robotController.move(direction);

                    }

                }

            }

            if (objectiveSignal != null) {

                robotController.setIndicatorLine(objectiveSignal.location, robotController.getLocation(), 0, 255, 0);
                robotController.setIndicatorString(1, "I have an objective.");

            } else {

                robotController.setIndicatorString(1, "I DO NOT have an objective.");

            }

            Clock.yield();

        }

    }

}
