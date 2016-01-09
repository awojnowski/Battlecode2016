package AaronBot2;

import AaronBot2.Combat.*;
import AaronBot2.Movement.*;
import AaronBot2.Signals.*;
import AaronBot2.Rubble.*;
import battlecode.common.*;

import java.util.*;

public class RobotSoldier implements Robot {

    public void run(final RobotController robotController) throws GameActionException {

        final CombatModule combatModule = new CombatModule();
        final CommunicationModule communicationModule = new CommunicationModule();
        final DirectionModule directionModule = new DirectionModule(robotController.getID());
        final MovementModule movementModule = new MovementModule();
        final RubbleModule rubbleModule = new RubbleModule();

        final RobotType type = robotController.getType();

        MapLocation robotHelpSignalLocation = null;

        while (true) {

            final MapLocation currentLocation = robotController.getLocation();

            // update communication

            communicationModule.processIncomingSignals(robotController);

            // let's check if we're done helping anything

            if (robotHelpSignalLocation != null) {

                final int distance = currentLocation.distanceSquaredTo(robotHelpSignalLocation);
                if (distance < type.sensorRadiusSquared) {

                    robotHelpSignalLocation = null;

                }

            }

            // let's get the best assignment

            CommunicationModuleSignal objectiveSignal = null;
            int closestObjectiveLocationDistance = Integer.MAX_VALUE;

            final Enumeration<CommunicationModuleSignal> communicationModuleSignals = communicationModule.zombieDens.elements();
            while (communicationModuleSignals.hasMoreElements()) {

                final CommunicationModuleSignal signal = communicationModuleSignals.nextElement();
                final int distance = signal.location.distanceSquaredTo(currentLocation);
                if (distance < closestObjectiveLocationDistance) {

                    objectiveSignal = signal;
                    closestObjectiveLocationDistance = distance;

                }

            }

            // now let's verify existing information

            communicationModule.verifyCommunicationsInformation(robotController, false);

            // now let's see if we can attack anything

            boolean attacked = false;
            final RobotInfo[] enemies = robotController.senseHostileRobots(currentLocation, type.attackRadiusSquared);
            final RobotInfo bestEnemy = combatModule.bestEnemyToAttackFromEnemies(enemies);

            if (robotController.isWeaponReady()) {

                if (bestEnemy != null) {

                    robotController.attackLocation(bestEnemy.location);
                    attacked = true;

                }

            }

            // check if we should broadcast danger

            if (bestEnemy != null && bestEnemy.team == robotController.getTeam().opponent()) {

                if (robotController.getRoundNum() == 0) {

                    //final int distance = CommunicationModule.maximumFreeBroadcastRangeForRobotType(type);
                    final int distance = 1000;
                    communicationModule.broadcastSignal(robotController, distance);

                }

            }

            // now let's try move toward an assignment

            boolean ableToMove = (bestEnemy == null || bestEnemy.type == RobotType.ZOMBIEDEN);

            Direction targetRubbleClearanceDirection = null;
            if (robotController.isCoreReady() && communicationModule.initialInformationReceived && ableToMove) {

                Direction desiredMovementDirection = null;

                // first check if we have signals from other robots

                if (desiredMovementDirection == null && ableToMove) {

                    if (robotHelpSignalLocation != null) {

                        desiredMovementDirection = currentLocation.directionTo(robotHelpSignalLocation);

                    }

                }

                if (desiredMovementDirection == null && ableToMove) {

                    int closestNotificationLocationDistance = Integer.MAX_VALUE;
                    MapLocation closestNotificationLocation = null;

                    final Iterator<Signal> notifications = communicationModule.notifications.iterator();
                    while (notifications.hasNext()) {

                        final Signal signal = notifications.next();
                        final int distance = signal.getLocation().distanceSquaredTo(currentLocation);
                        if (distance < closestNotificationLocationDistance) {

                            closestNotificationLocation = signal.getLocation();
                            closestNotificationLocationDistance = distance;

                        }

                    }

                    if (closestNotificationLocation != null) {

                        desiredMovementDirection = currentLocation.directionTo(closestNotificationLocation);
                        robotHelpSignalLocation = closestNotificationLocation;

                    }

                }

                // now check if we have an objective

                if (desiredMovementDirection == null && ableToMove) {

                    if (objectiveSignal != null) {

                        final MapLocation objectiveLocation = objectiveSignal.location;
                        if (objectiveLocation.distanceSquaredTo(currentLocation) >= 8) {

                            desiredMovementDirection = currentLocation.directionTo(objectiveLocation);

                        } else {

                            ableToMove = false;

                        }

                    }

                }

                // otherwise we can move randomly...

                if (desiredMovementDirection == null && ableToMove) {

                    desiredMovementDirection = directionModule.randomDirection();

                }

                // process movement

                if (desiredMovementDirection != null && ableToMove) {

                    final Direction randomMovementDirection = directionModule.recommendedMovementDirectionForDirection(desiredMovementDirection, robotController, false);
                    if (randomMovementDirection != null) {

                        robotController.move(randomMovementDirection);

                    } else {

                        targetRubbleClearanceDirection = desiredMovementDirection;

                    }

                }

            }

            // we can try clear rubble if we didn't move

            if (robotController.isCoreReady() && communicationModule.initialInformationReceived) {

                if (targetRubbleClearanceDirection != null) {

                    final Direction rubbleClearanceDirection = rubbleModule.rubbleClearanceDirectionFromTargetDirection(targetRubbleClearanceDirection, robotController);
                    if (rubbleClearanceDirection != null) {

                        robotController.clearRubble(rubbleClearanceDirection);

                    }

                }

            }

            // finish up

            final CommunicationModuleSignalCollection communicationModuleSignalCollection = communicationModule.allCommunicationModuleSignals();
            final MapLocation location = robotController.getLocation();
            while (communicationModuleSignalCollection.hasMoreElements()) {

                final CommunicationModuleSignal communicationModuleSignal = communicationModuleSignalCollection.nextElement();
                int[] color = new int[]{255, 255, 255};
                if (communicationModuleSignal.type == CommunicationModuleSignal.TYPE_ZOMBIEDEN) {

                    color = new int[]{50, 255, 50};

                } else if (communicationModuleSignal.type == CommunicationModuleSignal.TYPE_ENEMY_ARCHON) {

                    color = new int[]{255, 0, 0};

                } else if (communicationModuleSignal.type == CommunicationModuleSignal.TYPE_MAP_CORNER) {

                    color = new int[]{0, 0, 0};

                } else if (communicationModuleSignal.type == CommunicationModuleSignal.TYPE_MAP_CORNER) {

                    color = new int[]{0, 255, 0};

                }
                robotController.setIndicatorLine(location, communicationModuleSignal.location, color[0], color[1], color[2]);

            }

            if (objectiveSignal != null) {

                robotController.setIndicatorLine(objectiveSignal.location, robotController.getLocation(), 255, 0, 0);

            }

            if (robotHelpSignalLocation != null) {

                robotController.setIndicatorLine(robotHelpSignalLocation, robotController.getLocation(), 0, 0, 255);

            }

            Clock.yield();

        }

    }

}
