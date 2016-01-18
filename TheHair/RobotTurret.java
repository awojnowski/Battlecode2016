package TheHair;

import TheHair.Combat.*;
import TheHair.Map.MapInfoModule;
import TheHair.Movement.*;
import TheHair.Signals.*;
import TheHair.Rubble.*;
import TheHair.Turtle.TurtleInfo;
import TheHair.Turtle.TurtlePlacementModule;
import battlecode.common.*;
import java.util.*;

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

            MapLocation desiredAttackLocation = null;

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

                    desiredAttackLocation = this.findAttackableEnemyLocationNearby(communicationModule, robotController, currentLocation);

                    // see if we need to expand to a further location

                    if (communicationModule.initialInformationReceived) {

                        if (desiredAttackLocation == null) {

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

            if (robotController.isWeaponReady() && desiredAttackLocation != null) {

                robotController.attackLocation(desiredAttackLocation);

            }

            // attempt to move to the desired movement location

            if (robotController.isCoreReady() && communicationModule.initialInformationReceived && desiredMovementDirection != null) {

                Direction movementDirection = directionModule.recommendedMovementDirectionForDirection(desiredMovementDirection, robotController, false);
                if (movementDirection != null) {

                    robotController.move(movementDirection);
                    currentLocation = robotController.getLocation();
                    movementModule.addMovementLocation(currentLocation, robotController);

                }

            }

            // confirm states

            if (currentState == State.TURTLE) {

                ;

            }

            Clock.yield();

        }

    }

    private MapLocation findAttackableEnemyLocationNearby(final CommunicationModule communicationModule, final RobotController robotController, final MapLocation currentLocation) {

        final RobotInfo[] enemiesInSight = robotController.senseHostileRobots(currentLocation, robotController.getType().attackRadiusSquared);
        final Enumeration<ScoutCallout> scoutCallouts = communicationModule.scoutCallouts.elements();

        MapLocation bestEnemyLocation = null;
        double bestEnemyHealth = Double.MAX_VALUE;
        RobotType bestEnemyType = null;

        for (int i = 0; i < enemiesInSight.length; i++) {

            final RobotInfo enemy = enemiesInSight[i];
            if (!this.isEnemyAttackable(currentLocation, enemy.location)) {

                continue;

            }
            if (bestEnemyType == null || this.isEnemyABetterThanEnemyB(enemy.type, enemy.health, bestEnemyType, bestEnemyHealth)) {

                bestEnemyHealth = enemy.health;
                bestEnemyLocation = enemy.location;
                bestEnemyType = enemy.type;

            }

        }
        while (scoutCallouts.hasMoreElements()) {

            final ScoutCallout scoutCallout = scoutCallouts.nextElement();
            if (!this.isEnemyAttackable(currentLocation, scoutCallout.location)) {

                continue;

            }
            if (bestEnemyType == null || this.isEnemyABetterThanEnemyB(scoutCallout.robotType, scoutCallout.remainingHealth, bestEnemyType, bestEnemyHealth)) {

                bestEnemyHealth = scoutCallout.remainingHealth;
                bestEnemyLocation = scoutCallout.location;
                bestEnemyType = scoutCallout.robotType;

            }

        }

        return bestEnemyLocation;

    }

    private boolean isEnemyAttackable(final MapLocation currentLocation, final MapLocation location) {

        final int distance = currentLocation.distanceSquaredTo(location);
        if (distance > RobotType.TURRET.attackRadiusSquared) {

            return false;

        }
        if (distance < GameConstants.TURRET_MINIMUM_RANGE) {

            return false;

        }
        return true;

    }

    private boolean isEnemyABetterThanEnemyB(final RobotType typeA, final double healthA, final RobotType typeB, final double healthB) {

        final RobotType[] typeRankings = new RobotType[]{ RobotType.TURRET, RobotType.BIGZOMBIE, RobotType.SCOUT };

        // calculate the type rankings

        int typeRankingA = -1;
        int typeRankingB = -1;
        for (int i = 0; i < typeRankings.length; i++) {

            if (typeA.equals(typeRankings[i])) {

                typeRankingA = i;

            }
            if (typeB.equals(typeRankings[i])) {

                typeRankingB = i;

            }

        }

        // logic

        if (typeRankingA > typeRankingB) {

            return true;

        }
        if (typeRankingB > typeRankingA) {

            return false;

        }
        return healthA < healthB;

    }

}
