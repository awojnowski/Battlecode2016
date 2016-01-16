package AaronBot2;

import AaronBot2.Combat.*;
import AaronBot2.Map.MapInfoModule;
import AaronBot2.Movement.*;
import AaronBot2.Signals.*;
import AaronBot2.Rubble.*;
import battlecode.common.*;

import java.util.*;

public class RobotSoldier implements Robot, CommunicationModuleDelegate {

    public void run(final RobotController robotController) throws GameActionException {

        final MapInfoModule mapInfoModule = new MapInfoModule();

        final CombatModule combatModule = new CombatModule();
        final CommunicationModule communicationModule = new CommunicationModule(mapInfoModule);
        communicationModule.delegate = this;
        final DirectionModule directionModule = new DirectionModule(robotController.getID());
        final MovementModule movementModule = new MovementModule();
        final RubbleModule rubbleModule = new RubbleModule();
        final Team currentTeam = robotController.getTeam();
        int turnsStuck = 0;
        double lastHealth = robotController.getHealth();
        Direction lastDirection = Direction.NONE;
        boolean stop = false;

        final RobotType type = robotController.getType();

        while (true) {

            boolean lostHealth = (lastHealth != robotController.getHealth());
            lastHealth = robotController.getHealth();

            MapLocation currentLocation = robotController.getLocation();

            // update communication

            communicationModule.processIncomingSignals(robotController);

            // let's verify existing information

            communicationModule.verifyCommunicationsInformation(robotController, null, false);

            // let's get the best assignment

            CommunicationModuleSignal objectiveSignal = null;
            int closestObjectiveLocationDistance = Integer.MAX_VALUE;

            final Enumeration<CommunicationModuleSignal> zombieDenCommunicationModuleSignals = communicationModule.zombieDens.elements();
            while (zombieDenCommunicationModuleSignals.hasMoreElements()) {

                final CommunicationModuleSignal signal = zombieDenCommunicationModuleSignals.nextElement();
                final int distance = signal.location.distanceSquaredTo(currentLocation);
                if (distance < closestObjectiveLocationDistance) {

                    objectiveSignal = signal;
                    closestObjectiveLocationDistance = distance;

                }

            }

            final Enumeration<CommunicationModuleSignal> enemyArchonCommunicationModuleSignals = communicationModule.enemyArchons.elements();
            while (enemyArchonCommunicationModuleSignals.hasMoreElements()) {

                final CommunicationModuleSignal signal = enemyArchonCommunicationModuleSignals.nextElement();
                final int distance = signal.location.distanceSquaredTo(currentLocation) * 6; // multiplying by 6 to prioritize the dens
                if (distance < closestObjectiveLocationDistance) {

                    objectiveSignal = signal;
                    closestObjectiveLocationDistance = distance;

                }

            }

            // now let's see if we should kite or attack anything

            boolean attacked = false;
            final RobotInfo[] immediateEnemies = robotController.senseHostileRobots(currentLocation, 3);
            final RobotInfo[] immediateKitableZombies = CombatModule.robotsOfTypesFromRobots(immediateEnemies, new RobotType[]{RobotType.STANDARDZOMBIE, RobotType.BIGZOMBIE});
            RobotInfo bestEnemy;

            if (immediateKitableZombies.length > 0) {

                bestEnemy = combatModule.lowestHealthEnemyFromEnemies(immediateKitableZombies);

            } else {

                final RobotInfo[] enemies = robotController.senseHostileRobots(currentLocation, type.attackRadiusSquared);
                bestEnemy = combatModule.lowestHealthEnemyFromEnemies(enemies);

            }

            // movement variables

            boolean ableToMove = (bestEnemy == null || bestEnemy.type == RobotType.ZOMBIEDEN || bestEnemy.type == RobotType.TURRET);
            Direction targetRubbleClearanceDirection = null;
            Direction desiredMovementDirection = null;

            if (bestEnemy != null && (bestEnemy.type == RobotType.STANDARDZOMBIE || bestEnemy.type == RobotType.BIGZOMBIE) && currentLocation.distanceSquaredTo(bestEnemy.location) <= bestEnemy.type.attackRadiusSquared) {

                // should kite

                ableToMove = true;
                desiredMovementDirection = currentLocation.directionTo(bestEnemy.location).opposite();

            } else if (robotController.isWeaponReady()) {

                if (bestEnemy != null) {

                    robotController.attackLocation(bestEnemy.location);
                    attacked = true;
                    communicationModule.broadcastSignal(robotController, CommunicationModule.maximumFreeBroadcastRangeForRobotType(robotController.getType()));

                }

            }

            // check if they're getting hit by turrets

//            if (bestEnemy == null && lostHealth) {
//
//                desiredMovementDirection = lastDirection.opposite();
//                stop = true;
//
//            }
//
//            ableToMove = !stop;

            // now let's try move toward an assignment

            if (robotController.isCoreReady() && communicationModule.initialInformationReceived && ableToMove) {

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

                // otherwise check if there are nearby signals

                if (desiredMovementDirection == null && ableToMove) {

                    ArrayList<Signal> signals = communicationModule.notifications;

                    if (signals.size() > 0) {

                        int minDistance = Integer.MAX_VALUE;
                        MapLocation closestLocation = null;

                        for (int i = 0; i < signals.size(); i++) {

                            final Signal currentSignal = signals.get(i);
                            final int distance = currentLocation.distanceSquaredTo(currentSignal.getLocation());

                            if (distance < minDistance) {

                                minDistance = distance;
                                closestLocation = currentSignal.getLocation();

                            }

                        }

                        desiredMovementDirection = currentLocation.directionTo(closestLocation);

                    }

                }

                // if those fail we can move randomly near our teammates

                if (desiredMovementDirection == null && ableToMove) {

                    desiredMovementDirection = directionModule.randomDirection();

                    RobotInfo[] closeTeammates = robotController.senseNearbyRobots(3, currentTeam); // How close they stay to their team, lower means they'll stay closer

                    if (closeTeammates.length == 0) { // Move towards team if far away

                        RobotInfo[] nearbyTeammates = robotController.senseNearbyRobots(24, currentTeam);

                        if (nearbyTeammates.length > 0) {

                            desiredMovementDirection = directionModule.averageDirectionTowardRobots(robotController, nearbyTeammates);

                        }

                    }

                }

                // process movement

                if (desiredMovementDirection != null) {

                    final Direction recommendedMovementDirection = directionModule.recommendedMovementDirectionForDirection(desiredMovementDirection, robotController, false);
                    if (recommendedMovementDirection != null) {

                        robotController.move(recommendedMovementDirection);
                        lastDirection = recommendedMovementDirection;
                        currentLocation = robotController.getLocation();

                        if (turnsStuck != 0) {

                            turnsStuck = 0;

                        }

                    } else {

                        targetRubbleClearanceDirection = desiredMovementDirection;

                    }

                }

            }

            // we can try clear rubble if we didn't move

            if (robotController.isCoreReady() && communicationModule.initialInformationReceived) {

                if (targetRubbleClearanceDirection != null) {

                    final Direction rubbleClearanceDirection = rubbleModule.getRubbleClearanceDirectionFromTargetDirection(targetRubbleClearanceDirection, robotController);
                    if (rubbleClearanceDirection != null) {

                        robotController.clearRubble(rubbleClearanceDirection);

                        if (turnsStuck != 0) {

                            turnsStuck = 0;

                        }

                        // otherwise they didn't move or clear rubble, check if they're stuck

                    } else if (communicationModule.notifications.size() == 0 && objectiveSignal != null) {

                        turnsStuck++;

                        if (turnsStuck > 5) {

                            communicationModule.clearSignal(objectiveSignal, communicationModule.enemyArchons);
                            communicationModule.clearSignal(objectiveSignal, communicationModule.zombieDens);
                            turnsStuck = 0;

                        }

                    } else if (turnsStuck != 0) {

                        turnsStuck = 0;

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

                }
                robotController.setIndicatorLine(location, communicationModuleSignal.location, color[0], color[1], color[2]);

            }

            if (objectiveSignal != null) {

                robotController.setIndicatorLine(objectiveSignal.location, robotController.getLocation(), 255, 0, 0);

            }

            Clock.yield();

        }

    }

    /*
    COMMUNICATION MODULE DELEGATE
    */

    public boolean shouldProcessSignalType(final int signalType) {

        return true;

    }

}
