package team059;

import team059.Combat.CombatModule;
import team059.Map.MapInfoModule;
import team059.Movement.DirectionModule;
import team059.Movement.MovementModule;
import team059.Rubble.RubbleModule;
import team059.Signals.CommunicationModule;
import team059.Signals.CommunicationModuleSignal;
import team059.Signals.CommunicationModuleSignalCollection;
import battlecode.common.*;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Random;

public class RobotTurret implements Robot {

    public void run(final RobotController robotController) throws GameActionException {

        final MapInfoModule mapInfoModule = new MapInfoModule();

        final CombatModule combatModule = new CombatModule();
        final CommunicationModule communicationModule = new CommunicationModule(mapInfoModule);
        final DirectionModule directionModule = new DirectionModule(robotController.getID());
        final MovementModule movementModule = new MovementModule();
        final RubbleModule rubbleModule = new RubbleModule();

        final Team currentTeam = robotController.getTeam();
        int turnsStuck = 0;

        while (true) {

            RobotType type = robotController.getType();
            MapLocation currentLocation = robotController.getLocation();

            // update communication

            communicationModule.processIncomingSignals(robotController);

            // let's verify existing information

            communicationModule.verifyCommunicationsInformation(robotController, null, false);

            if (type == RobotType.TTM) {

                // MOVE

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

                final Enumeration<CommunicationModuleSignal> enemyTurretCommunicationModuleSignals = communicationModule.enemyTurrets.elements();
                while (enemyTurretCommunicationModuleSignals.hasMoreElements()) {

                    final CommunicationModuleSignal signal = enemyTurretCommunicationModuleSignals.nextElement();
                    final int distance = signal.location.distanceSquaredTo(currentLocation) * 20;
                    if (distance < closestObjectiveLocationDistance) {

                        objectiveSignal = signal;
                        closestObjectiveLocationDistance = distance;

                    }

                }

                boolean shouldMove = true;
                Direction desiredMovementDirection = null;
                Direction targetRubbleClearanceDirection = null;

                // check to make sure we are safe

                final RobotInfo[] enemies = robotController.senseHostileRobots(currentLocation, robotController.getType().sensorRadiusSquared);

                if (robotController.isCoreReady() && enemies.length > 0) {

                    final Direction fleeDirection = directionModule.averageDirectionTowardDangerousRobotsAndOuterBounds(robotController, enemies);
                    if (fleeDirection != null) {

                        final Direction fleeMovementDirection = directionModule.recommendedMovementDirectionForDirection(fleeDirection.opposite(), robotController, false);
                        if (fleeMovementDirection != null) {

                            robotController.move(fleeMovementDirection);
                            currentLocation = robotController.getLocation();
                            robotController.setIndicatorString(1, fleeDirection.name() + " " + fleeMovementDirection.name());

                        }

                    }

                }

                // check if there are nearby signals

                if (desiredMovementDirection == null) {

                    int closestSignalDistance = Integer.MAX_VALUE;
                    MapLocation closestSignalLocation = null;

                    final ArrayList<Signal> notifications = communicationModule.notifications;
                    for (int i = 0; i < notifications.size(); i++) {

                        final Signal signal = notifications.get(i);
                        final int distance = currentLocation.distanceSquaredTo(signal.getLocation());
                        if (distance < closestSignalDistance) {

                            closestSignalDistance = distance;
                            closestSignalLocation = signal.getLocation();

                        }

                    }
                    if (closestSignalLocation != null) {

                        desiredMovementDirection = currentLocation.directionTo(closestSignalLocation);

                    }

                }

                // now let's try move toward an assignment

                if (robotController.isCoreReady() && communicationModule.initialInformationReceived && shouldMove) {

                    // check if we have an objective

                    if (desiredMovementDirection == null) {

                        if (objectiveSignal != null) {

                            final MapLocation objectiveLocation = objectiveSignal.location;
                            if (objectiveLocation.distanceSquaredTo(currentLocation) >= 8) {

                                desiredMovementDirection = currentLocation.directionTo(objectiveLocation);

                            }

                        }

                    }

                    // try move towards archon starting positions

                    if (desiredMovementDirection == null) {

                        int closestArchonDistance = Integer.MAX_VALUE;
                        MapLocation closestArchonLocation = null;

                        final MapLocation[] archonLocations = robotController.getInitialArchonLocations(robotController.getTeam().opponent());
                        for (int i = 0; i < archonLocations.length; i++) {

                            final MapLocation location = archonLocations[i];
                            final int distance = currentLocation.distanceSquaredTo(location);
                            if (distance < closestArchonDistance) {

                                closestArchonDistance = distance;
                                closestArchonLocation = location;

                            }

                        }
                        if (closestArchonLocation != null) {

                            desiredMovementDirection = currentLocation.directionTo(closestArchonLocation);

                        }

                    }

                    // process movement

                    if (desiredMovementDirection != null) {

                        final Direction recommendedMovementDirection = directionModule.recommendedMovementDirectionForDirection(desiredMovementDirection, robotController, false);
                        final MapLocation recommendedMovementLocation = recommendedMovementDirection != null ? currentLocation.add(recommendedMovementDirection) : null;
                        if (recommendedMovementDirection != null && !movementModule.isMovementLocationRepetitive(recommendedMovementLocation, robotController)) {

                            robotController.move(recommendedMovementDirection);
                            movementModule.addMovementLocation(recommendedMovementLocation, robotController);
                            currentLocation = robotController.getLocation();
                            turnsStuck = 0;

                        }

                    }

                }

                // unpack if we're safe

                RobotInfo[] nearbyTeammates = robotController.senseNearbyRobots(8, currentTeam);
                RobotInfo[] nearbySoldiers = combatModule.robotsOfTypesFromRobots(nearbyTeammates, new RobotType[]{RobotType.SOLDIER});

                if (nearbySoldiers.length > 2) {

                    robotController.unpack();

                }

            } else {

                // ATTACK

                final RobotInfo[] enemies = robotController.senseHostileRobots(currentLocation, robotController.getType().attackRadiusSquared);

                RobotInfo bestEnemy = this.getBestEnemyToAttackFromEnemies(robotController, enemies);

                // handle attacking

                if (bestEnemy != null) {

                    if (robotController.isWeaponReady()) {

                        // we can attack the enemy

                        robotController.attackLocation(bestEnemy.location);
                        if (bestEnemy.type != RobotType.ZOMBIEDEN) {

                            communicationModule.broadcastSignal(robotController, CommunicationModule.maximumFreeBroadcastRangeForRobotType(robotController.getType()));

                        }

                    }

                }

                // pack if we aren't near soldiers

                RobotInfo[] nearbyTeammates = robotController.senseNearbyRobots(type.sensorRadiusSquared, currentTeam);
                RobotInfo[] nearbySoldiers = combatModule.robotsOfTypesFromRobots(nearbyTeammates, new RobotType[]{RobotType.SOLDIER});

                if (nearbySoldiers.length < 3) {

                    robotController.pack();

                }

            }

            Clock.yield();

        }

    }

    /*
    COMBAT
     */

    private RobotInfo getBestEnemyToAttackFromEnemies(final RobotController robotController, final RobotInfo[] enemies) {

        MapLocation currentLocaiton = robotController.getLocation();
        RobotInfo bestEnemy = null;
        for (int i = 0; i < enemies.length; i++) {

            final RobotInfo enemy = enemies[i];
            if (currentLocaiton.distanceSquaredTo(enemy.location) < GameConstants.TURRET_MINIMUM_RANGE) {

                continue;

            }
            if (bestEnemy == null) {

                bestEnemy = enemy;
                continue;

            }
            if (enemy.type != RobotType.ZOMBIEDEN && bestEnemy.type == RobotType.ZOMBIEDEN) {

                bestEnemy = enemy;
                continue;

            }
            if (enemy.type == RobotType.ZOMBIEDEN && bestEnemy.type != RobotType.ZOMBIEDEN) {

                continue;

            }
            if (enemy.health < bestEnemy.health) {

                bestEnemy = enemy;
                continue;

            }

        }
        return bestEnemy;

    }

}
