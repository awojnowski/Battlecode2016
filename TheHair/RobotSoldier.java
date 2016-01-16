package TheHair;

import TheHair.Combat.*;
import TheHair.Map.MapInfoModule;
import TheHair.Movement.*;
import TheHair.Signals.*;
import TheHair.Rubble.*;
import battlecode.common.*;

import java.util.*;

public class RobotSoldier implements Robot {

    enum State {
        UNKNOWN,
        SKIRMISH,
        TURTLE
    }

    public void run(final RobotController robotController) throws GameActionException {

        final MapInfoModule mapInfoModule = new MapInfoModule();
        final CombatModule combatModule = new CombatModule();
        final CommunicationModule communicationModule = new CommunicationModule(mapInfoModule);
        final DirectionModule directionModule = new DirectionModule(robotController.getID());
        final MovementModule movementModule = new MovementModule();
        final RubbleModule rubbleModule = new RubbleModule();

        // GLOBAL CONSTANTS

        final MapLocation archonRendezvousLocation = movementModule.getArchonRendezvousLocation(robotController.getLocation(), robotController.getInitialArchonLocations(robotController.getTeam()));
        final RobotType currentType = robotController.getType();

        int consecutiveInvalidMovementTurns = 0;

        // GLOBAL FLAGS

        State currentState = State.UNKNOWN;

        Direction infoGatherDirection = null;
        boolean returnToRendezvous = false;

        while (true) {

            // ROUND FLAGS

            RobotInfo desiredAttackUnit = null;

            MapLocation currentLocation = robotController.getLocation();
            Direction desiredMovementDirection = null;

            Direction desiredRubbleClearanceDirection = null;
            boolean canClearAnyRubbleDirection = false;

            // ROUND CONSTANTS

            final RobotInfo[] enemies = robotController.senseHostileRobots(currentLocation, currentType.sensorRadiusSquared);

            // begin...

            // we need to figure out the initial state

            if (currentState == State.UNKNOWN) {

                currentState = State.SKIRMISH;

            }

            // process communication

            communicationModule.processIncomingSignals(robotController);

            // handle states

            if (currentState == State.SKIRMISH) {

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

                if (objectiveSignal != null) {

                    desiredMovementDirection = currentLocation.directionTo(objectiveSignal.location);

                } else {

                    desiredMovementDirection = directionModule.randomDirection();

                }

                // now let's try see if we can attack anything

                final RobotInfo[] attackableEnemies = robotController.senseHostileRobots(currentLocation, currentType.attackRadiusSquared);
                desiredAttackUnit = combatModule.bestEnemyToAttackFromEnemies(attackableEnemies);

            } else if (currentState == State.TURTLE) {

                ;

            }

            // process flags

            // attempt to attack the enemy

            if (robotController.isWeaponReady() && desiredAttackUnit != null) {

                robotController.attackLocation(desiredAttackUnit.location);

            }

            // attempt to move to the desired movement location

            if (robotController.isCoreReady() && communicationModule.initialInformationReceived && desiredMovementDirection != null) {

                Direction movementDirection = directionModule.recommendedMovementDirectionForDirection(desiredMovementDirection, robotController, false);
                if (movementDirection != null) {

                    robotController.move(movementDirection);
                    currentLocation = robotController.getLocation();

                    consecutiveInvalidMovementTurns = 0;

                } else {

                    desiredRubbleClearanceDirection = desiredMovementDirection;

                    consecutiveInvalidMovementTurns ++;

                }

            }

            // check if we should clear rubble

            if (robotController.isCoreReady() && desiredRubbleClearanceDirection != null) {

                Direction rubbleClearanceDirection = null;
                if (canClearAnyRubbleDirection) {

                    rubbleClearanceDirection = rubbleModule.getAnyRubbleClearanceDirectionFromDirection(desiredRubbleClearanceDirection, robotController);

                } else {

                    rubbleClearanceDirection = rubbleModule.getRubbleClearanceDirectionFromTargetDirection(desiredRubbleClearanceDirection, robotController);

                }
                if (rubbleClearanceDirection != null) {

                    robotController.clearRubble(rubbleClearanceDirection);

                }

            }

            // confirm states

            if (currentState == State.SKIRMISH) {

                ;

            } else if (currentState == State.TURTLE) {

                ;

            }

            Clock.yield();

        }

    }

}
