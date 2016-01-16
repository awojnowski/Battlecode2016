package TheHair;

import TheHair.Combat.*;
import TheHair.Map.MapInfoModule;
import TheHair.Movement.*;
import TheHair.Signals.*;
import TheHair.Rubble.*;
import TheHair.Turtle.TurtleInfo;
import TheHair.Turtle.TurtlePlacementModule;
import battlecode.common.*;
import java.util.Random;

public class RobotTurret implements Robot {

    enum State {
        UNKNOWN,
        TURTLE
    }

    public void run(final RobotController robotController) throws GameActionException {

        final MapInfoModule mapInfoModule = new MapInfoModule();
        final CombatModule combatModule = new CombatModule();
        final CommunicationModule communicationModule = new CommunicationModule(mapInfoModule);
        final DirectionModule directionModule = new DirectionModule(robotController.getID());
        final MovementModule movementModule = new MovementModule();
        final TurtlePlacementModule turtlePlacementModule = new TurtlePlacementModule();

        // GLOBAL FLAGS

        State currentState = State.UNKNOWN;

        while (true) {

            robotController.setIndicatorString(1, "");

            // ROUND FLAGS

            RobotInfo desiredAttackUnit = null;

            MapLocation currentLocation = robotController.getLocation();
            Direction desiredMovementDirection = null;

            // ROUND CONSTANTS

            final RobotType currentType = robotController.getType();
            final RobotInfo[] enemies = robotController.senseHostileRobots(currentLocation, currentType.sensorRadiusSquared);

            // begin...

            // we need to figure out the initial state

            if (currentState == State.UNKNOWN) {

                currentState = State.TURTLE;

            }

            // process communication

            communicationModule.processIncomingSignals(robotController);
            communicationModule.verifyCommunicationsInformation(robotController, enemies, false);

            // handle states

            if (currentState == State.TURTLE) {

                if (currentType == RobotType.TURRET) {

                    // see if we can attack anything

                    final RobotInfo[] attackableEnemies = robotController.senseHostileRobots(currentLocation, currentType.attackRadiusSquared);
                    desiredAttackUnit = this.bestEnemyToAttackFromEnemies(robotController, currentLocation, attackableEnemies);

                    // see if we need to expand to a further location

                    if (communicationModule.initialInformationReceived) {

                        if (desiredAttackUnit == null) {

                            final MapLocation bestTurretLocation = turtlePlacementModule.fetchBestTurretLocation(currentLocation, robotController, communicationModule.turtleInfo.location, communicationModule.turtleInfo, 1);
                            if (bestTurretLocation != null) {

                                robotController.pack();

                            }

                        }

                    }

                } else if (currentType == RobotType.TTM) {

                    if (communicationModule.initialInformationReceived) {

                        final MapLocation bestTurretLocation = turtlePlacementModule.fetchBestTurretLocation(currentLocation, robotController, communicationModule.turtleInfo.location, communicationModule.turtleInfo, 1);
                        if (bestTurretLocation == null) {

                            robotController.unpack();

                        } else {

                            desiredMovementDirection = currentLocation.directionTo(bestTurretLocation);

                        }

                    }

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
                    movementModule.addMovementLocation(currentLocation);

                }

            }

            // confirm states

            if (currentState == State.TURTLE) {

                ;

            }

            Clock.yield();

        }

    }

    private RobotInfo bestEnemyToAttackFromEnemies(final RobotController robotController, final MapLocation currentLocation, final RobotInfo[] enemies) {

        RobotInfo bestEnemy = null;
        double lowestHealth = Integer.MAX_VALUE;

        for (int i = 0; i < enemies.length; i++) {

            final RobotInfo enemy = enemies[i];
            final int distance = enemy.location.distanceSquaredTo(currentLocation);
            if (distance < GameConstants.TURRET_MINIMUM_RANGE) {

                continue;

            }
            if (bestEnemy == null) {

                bestEnemy = enemy;
                lowestHealth = enemy.health;
                continue;

            }
            if (enemy.type != RobotType.BIGZOMBIE && bestEnemy.type == RobotType.BIGZOMBIE) {

                continue;

            }
            if (enemy.type == RobotType.BIGZOMBIE && bestEnemy.type != RobotType.BIGZOMBIE) {

                bestEnemy = enemy;
                lowestHealth = bestEnemy.health;
                continue;

            }
            if (enemy.health < lowestHealth) {

                bestEnemy = enemy;
                lowestHealth = enemy.health;

            }

        }
        return bestEnemy;

    }

}
