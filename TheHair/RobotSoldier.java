package TheHair;

import TheHair.Combat.*;
import TheHair.Map.MapInfoModule;
import TheHair.Movement.*;
import TheHair.Signals.*;
import TheHair.Rubble.*;
import TheHair.Turtle.TurtleInfo;
import battlecode.common.*;

import java.util.*;

public class RobotSoldier implements Robot {

    enum State {
        UNKNOWN,
        SKIRMISH,
        TURTLE_CLEARING,
        TURTLE_STAGING,
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

        final MapLocation archonRendezvousLocation = movementModule.getArchonRendezvousLocation(robotController);
        final RobotType currentType = robotController.getType();

        int consecutiveInvalidMovementTurns = 0;

        // GLOBAL FLAGS

        State currentState = State.UNKNOWN;

        while (true) {

            // ROUND FLAGS

            RobotInfo desiredAttackUnit = null;

            MapLocation currentLocation = robotController.getLocation();
            Direction desiredMovementDirection = null;

            boolean clearRubbleIfPossible = false;
            boolean clearRubbleInAnyDirection = false;
            Direction desiredRubbleClearanceDirection = null;

            // ROUND CONSTANTS

            final RobotInfo[] enemies = robotController.senseHostileRobots(currentLocation, currentType.sensorRadiusSquared);
            final RobotInfo[] enemyRobots = robotController.senseNearbyRobots(currentType.sensorRadiusSquared, robotController.getTeam().opponent());

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
                    if (distance < closestObjectiveLocationDistance && movementModule.isLocationOnOurSide(robotController, signal.location)) {

                        objectiveSignal = signal;
                        closestObjectiveLocationDistance = distance;

                    }

                }

                final Enumeration<CommunicationModuleSignal> enemyArchonCommunicationModuleSignals = communicationModule.enemyArchons.elements();
                while (enemyArchonCommunicationModuleSignals.hasMoreElements()) {

                    final CommunicationModuleSignal signal = enemyArchonCommunicationModuleSignals.nextElement();
                    final int distance = signal.location.distanceSquaredTo(currentLocation);
                    if (distance < closestObjectiveLocationDistance && movementModule.isLocationOnOurSide(robotController, signal.location)) {

                        objectiveSignal = signal;
                        closestObjectiveLocationDistance = distance;

                    }

                }

                if (objectiveSignal != null) {

                    desiredMovementDirection = currentLocation.directionTo(objectiveSignal.location);

                } else {

                    MapLocation signalLocation = communicationModule.closestNotificationLocation(robotController);

                    if (signalLocation != null) {

                        desiredMovementDirection = currentLocation.directionTo(signalLocation);

                    } else if (currentLocation.distanceSquaredTo(archonRendezvousLocation) < 64) {

                        desiredMovementDirection = directionModule.randomDirection();

                    } else {

                        desiredMovementDirection = currentLocation.directionTo(archonRendezvousLocation);

                    }

                }

                // see if we can kite or attack anything

                final RobotInfo[] immediateEnemies = robotController.senseHostileRobots(currentLocation, 3);
                final RobotInfo[] immediateKitableZombies = combatModule.robotsOfTypesFromRobots(immediateEnemies, new RobotType[]{RobotType.STANDARDZOMBIE, RobotType.BIGZOMBIE});

                if (immediateKitableZombies.length > 0) {

                    desiredAttackUnit = combatModule.lowestHealthEnemyFromEnemies(immediateKitableZombies);

                } else {

                    final RobotInfo[] attackableEnemies = robotController.senseHostileRobots(currentLocation, currentType.attackRadiusSquared);
                    desiredAttackUnit = combatModule.lowestHealthEnemyFromEnemies(attackableEnemies);

                }

                // check if we should kite

                if (robotController.getType() == RobotType.SOLDIER && desiredAttackUnit != null && (desiredAttackUnit.type == RobotType.STANDARDZOMBIE || desiredAttackUnit.type == RobotType.BIGZOMBIE) && currentLocation.distanceSquaredTo(desiredAttackUnit.location) <= desiredAttackUnit.type.attackRadiusSquared) {

                    desiredMovementDirection = currentLocation.directionTo(desiredAttackUnit.location).opposite();
                    desiredAttackUnit = null;

                }

                // run away from enemies if there are lots

                if (enemyRobots.length > 3) {

                    desiredAttackUnit = null;
                    desiredMovementDirection = directionModule.averageDirectionTowardRobots(robotController, enemyRobots).opposite();

                }

                // broadcast if we're attacking

                if (desiredAttackUnit != null) {

                    communicationModule.broadcastSignal(robotController, CommunicationModule.maximumFreeBroadcastRangeForRobotType(robotController.getType()));

                }

            } else if (currentState == State.TURTLE_CLEARING) {

                // see if we can attack anything

                final RobotInfo[] attackableEnemies = robotController.senseHostileRobots(currentLocation, currentType.attackRadiusSquared);
                desiredAttackUnit = combatModule.lowestHealthEnemyFromEnemies(attackableEnemies);

                // try to go to the turtle location

                desiredMovementDirection = currentLocation.directionTo(communicationModule.turtleInfo.location);

                // try to clear rubble

                clearRubbleIfPossible = true;

            } else if (currentState == State.TURTLE_STAGING) {

                // see if we can attack anything

                final RobotInfo[] attackableEnemies = robotController.senseHostileRobots(currentLocation, currentType.attackRadiusSquared);
                desiredAttackUnit = combatModule.lowestHealthEnemyFromEnemies(attackableEnemies);

                // give the turtle area some space

                final Direction direction = currentLocation.directionTo(communicationModule.turtleInfo.location);
                if (currentLocation.distanceSquaredTo(communicationModule.turtleInfo.location) < 64) {

                    desiredMovementDirection = direction.opposite();

                } else {

                    desiredMovementDirection = directionModule.randomDirection();

                }

                // try to clear rubble

                clearRubbleIfPossible = true;

            } else if (currentState == State.TURTLE) {

                // see if we can attack anything

                final RobotInfo[] attackableEnemies = robotController.senseHostileRobots(currentLocation, currentType.attackRadiusSquared);
                desiredAttackUnit = combatModule.lowestHealthEnemyFromEnemies(attackableEnemies);

                // try to go to the turtle location

                final int turtleDistance = communicationModule.turtleInfo.distance;
                final int bufferDistance = (int)Math.pow(Math.floor(Math.sqrt(turtleDistance)) + 2, 2);
                if (currentLocation.distanceSquaredTo(communicationModule.turtleInfo.location) > bufferDistance) {

                    desiredMovementDirection = currentLocation.directionTo(communicationModule.turtleInfo.location);

                } else {

                    desiredMovementDirection = currentLocation.directionTo(communicationModule.turtleInfo.location).opposite();

                }

            }

            // process flags

            // attempt to attack the enemy

            if (robotController.isWeaponReady() && desiredAttackUnit != null) {

                robotController.attackLocation(desiredAttackUnit.location);

            }

            // attempt to move to the desired movement location

            if (robotController.isCoreReady() && communicationModule.initialInformationReceived && desiredMovementDirection != null) {

                final Direction movementDirection = directionModule.recommendedMovementDirectionForDirection(desiredMovementDirection, robotController, false);
                if (movementDirection != null && !movementModule.isMovementLocationRepetitive(currentLocation.add(movementDirection), robotController)) {

                    robotController.move(movementDirection);
                    currentLocation = robotController.getLocation();
                    movementModule.addMovementLocation(currentLocation, robotController);

                    consecutiveInvalidMovementTurns = 0;

                } else {

                    desiredRubbleClearanceDirection = desiredMovementDirection;
                    if (movementDirection != null) {

                        movementModule.extendLocationInvalidationTurn(robotController);

                    }

                    consecutiveInvalidMovementTurns ++;

                }

            }

            // check if we should clear rubble

            if (robotController.isCoreReady()) {

                Direction rubbleClearanceDirection = null;
                if (desiredRubbleClearanceDirection == null) {

                    if (clearRubbleIfPossible) {

                        rubbleClearanceDirection = rubbleModule.getOptimalRubbleClearanceDirection(robotController);

                    }

                } else {

                    if (clearRubbleInAnyDirection) {

                        rubbleClearanceDirection = rubbleModule.getAnyRubbleClearanceDirectionFromDirection(desiredRubbleClearanceDirection, robotController);

                    } else {

                        rubbleClearanceDirection = rubbleModule.getRubbleClearanceDirectionFromTargetDirection(desiredRubbleClearanceDirection, robotController);

                    }

                }
                if (rubbleClearanceDirection != null) {

                    robotController.clearRubble(rubbleClearanceDirection);

                }

            }

            // confirm states

            if (currentState == State.SKIRMISH) {

                if (communicationModule.turtleInfo.status == TurtleInfo.StatusSiteClearance) {

                    currentState = State.TURTLE_CLEARING;

                } else if (communicationModule.turtleInfo.status == TurtleInfo.StatusSiteStaging) {

                    currentState = State.TURTLE_STAGING;

                } else if (communicationModule.turtleInfo.status == TurtleInfo.StatusSiteEstablished) {

                    currentState = State.TURTLE;

                }

            } else if (currentState == State.TURTLE_CLEARING) {

                if (communicationModule.turtleInfo.status == TurtleInfo.StatusSiteStaging) {

                    currentState = State.TURTLE_STAGING;

                } else if (communicationModule.turtleInfo.status == TurtleInfo.StatusSiteEstablished) {

                    currentState = State.TURTLE;

                }

            } else if (currentState == State.TURTLE_STAGING) {

                if (communicationModule.turtleInfo.status == TurtleInfo.StatusSiteEstablished) {

                    currentState = State.TURTLE;

                }

            } else if (currentState == State.TURTLE) {

                ;

            }

            robotController.setIndicatorString(0, "State: " + currentState.name());

            Clock.yield();

        }

    }

}
