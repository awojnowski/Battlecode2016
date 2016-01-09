package AaronBot2;

import AaronBot2.Combat.*;
import AaronBot2.Movement.*;
import AaronBot2.Signals.*;
import AaronBot2.Rubble.*;
import battlecode.common.*;

import java.util.Enumeration;
import java.util.Random;

public class RobotSoldier implements Robot {

    public void run(final RobotController robotController) throws GameActionException {

        final CombatModule combatModule = new CombatModule();
        final CommunicationModule communicationModule = new CommunicationModule();
        final DirectionModule directionModule = new DirectionModule(robotController.getID());
        final MovementModule movementModule = new MovementModule();
        final RubbleModule rubbleModule = new RubbleModule();

        while (true) {

            communicationModule.processIncomingSignals(robotController);

            // let's get the best assignment

            CommunicationModuleSignal objectiveSignal = null;
            final MapLocation currentLocation = robotController.getLocation();
            int closestLocationDistance = Integer.MAX_VALUE;

            final Enumeration<CommunicationModuleSignal> communicationModuleSignals = communicationModule.zombieDens.elements();
            while (communicationModuleSignals.hasMoreElements()) {

                final CommunicationModuleSignal signal = communicationModuleSignals.nextElement();
                final int distance = signal.location.distanceSquaredTo(currentLocation);
                if (distance < closestLocationDistance) {

                    objectiveSignal = signal;
                    closestLocationDistance = distance;

                }

            }

            // now let's verify existing information

            communicationModule.verifyCommunicationsInformation(robotController, false);

            // now let's see if we can attack anything

            boolean attacked = false;
            final RobotInfo[] enemies = robotController.senseHostileRobots(currentLocation, robotController.getType().attackRadiusSquared);
            final RobotInfo bestEnemy = combatModule.bestEnemyToAttackFromEnemies(enemies);

            if (robotController.isWeaponReady()) {

                if (bestEnemy != null) {

                    robotController.attackLocation(bestEnemy.location);
                    attacked = true;

                }

            }

            // now let's try move toward an assignment
            
            Direction targetRubbleClearanceDirection = null;
            if (robotController.isCoreReady() && (bestEnemy == null || bestEnemy.team != robotController.getTeam().opponent())) {

                if (objectiveSignal != null) {

                    final MapLocation objectiveLocation = objectiveSignal.location;
                    if (objectiveLocation.distanceSquaredTo(currentLocation) >= 8) {

                        final Direction objectiveDirection = currentLocation.directionTo(objectiveLocation);
                        final Direction objectiveMovementDirection = directionModule.recommendedMovementDirectionForDirection(objectiveDirection, robotController, false);
                        if (objectiveMovementDirection != null) {

                            robotController.move(objectiveMovementDirection);

                        } else {

                            targetRubbleClearanceDirection = objectiveDirection;

                        }

                    }

                } else {

                    final Direction randomDirection = directionModule.randomDirection();
                    final Direction randomMovementDirection = directionModule.recommendedMovementDirectionForDirection(randomDirection, robotController, false);
                    if (randomMovementDirection != null) {

                        robotController.move(randomMovementDirection);

                    } else {

                        targetRubbleClearanceDirection = randomDirection;

                    }

                }

            }

            // we can try clear rubble if we didn't move

            if (robotController.isCoreReady()) {

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

            Clock.yield();

        }

    }

}
