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

        MapLocation turtleLocation = null;

        while (true) {

            // ROUND FLAGS

            RobotInfo desiredAttackUnit = null;

            MapLocation currentLocation = robotController.getLocation();
            Direction desiredMovementDirection = null;

            Direction desiredRubbleClearanceDirection = null;

            // ROUND CONSTANTS

            final RobotInfo[] enemies = robotController.senseHostileRobots(currentLocation, currentType.sensorRadiusSquared);

            // begin...

            // we need to figure out the initial state

            if (currentState == State.UNKNOWN) {

                currentState = State.SKIRMISH;

            }

            // process communication

            communicationModule.processIncomingSignals(robotController);
            communicationModule.verifyCommunicationsInformation(robotController, enemies, false);

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
                    final int distance = signal.location.distanceSquaredTo(currentLocation);
                    if (distance < closestObjectiveLocationDistance) {

                        objectiveSignal = signal;
                        closestObjectiveLocationDistance = distance;

                    }

                }

                if (objectiveSignal != null) {

                    desiredMovementDirection = currentLocation.directionTo(objectiveSignal.location);

                } else if (currentLocation.distanceSquaredTo(archonRendezvousLocation) < 64) {

                    desiredMovementDirection = directionModule.randomDirection();

                } else {

                    desiredMovementDirection = currentLocation.directionTo(archonRendezvousLocation);

                }

                // see if we can attack anything

                final RobotInfo[] attackableEnemies = robotController.senseHostileRobots(currentLocation, currentType.attackRadiusSquared);
                desiredAttackUnit = combatModule.bestEnemyToAttackFromEnemies(attackableEnemies);

            } else if (currentState == State.TURTLE) {

                // see if we can attack anything

                final RobotInfo[] attackableEnemies = robotController.senseHostileRobots(currentLocation, currentType.attackRadiusSquared);
                desiredAttackUnit = combatModule.bestEnemyToAttackFromEnemies(attackableEnemies);

                // try to go to the turtle location

                if (currentLocation.distanceSquaredTo(turtleLocation) > 35) {

                    desiredMovementDirection = currentLocation.directionTo(turtleLocation);

                }

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

                Direction rubbleClearanceDirection = rubbleModule.getRubbleClearanceDirectionFromTargetDirection(desiredRubbleClearanceDirection, robotController);
                if (rubbleClearanceDirection != null) {

                    robotController.clearRubble(rubbleClearanceDirection);

                }

            }

            // confirm states

            if (currentState == State.SKIRMISH) {

                turtleLocation = communicationModule.turtleLocation;
                if (turtleLocation != null) {

                    turtleLocation = turtleLocation;
                    currentState = State.TURTLE;

                }

            } else if (currentState == State.TURTLE) {

                ;

            }

            Clock.yield();

        }

    }

}
