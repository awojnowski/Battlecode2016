package AaronBot3;

import AaronBot3.Combat.*;
import AaronBot3.Information.*;
import AaronBot3.Movement.*;
import AaronBot3.Rubble.*;
import battlecode.common.*;
import java.util.*;

public class RobotViper implements Robot {

    public void run(final RobotController robotController) throws GameActionException {

        robotController.emptySignalQueue();

        final CombatModule combatModule = new CombatModule();
        final MovementModule movementModule = new MovementModule();
        final PoliticalAgenda politicalAgenda = new PoliticalAgenda();
        final Random random = new Random(robotController.getID());
        final RubbleModule rubbleModule = new RubbleModule();
        final Team currentTeam = robotController.getTeam();

        int turnsStuck = 0;
        double lastHealth = robotController.getHealth();
        Direction lastDirection = Direction.NONE;
        boolean stop = false;

        final RobotType type = robotController.getType();

        while (true) {

            // update communication

            politicalAgenda.processIncomingSignalsFromRobotController(robotController);
            if (!politicalAgenda.isInformationSynced) {

                Clock.yield();
                continue;

            }

            // begin

            boolean lostHealth = (lastHealth != robotController.getHealth());
            lastHealth = robotController.getHealth();

            MapLocation currentLocation = robotController.getLocation();

            // let's get the best assignment

            InformationSignal objectiveSignal = null;
            int closestObjectiveLocationDistance = Integer.MAX_VALUE;

            int zombieDenCount = politicalAgenda.zombieDens.size();
            for (int i = 0; i < zombieDenCount; i++) {

                final InformationSignal signal = politicalAgenda.zombieDens.get(i);
                final int distance = currentLocation.distanceSquaredTo(signal.location);
                if (distance < closestObjectiveLocationDistance) {

                    if (politicalAgenda.verifyZombieDenSignal(signal, robotController)) {

                        objectiveSignal = signal;
                        closestObjectiveLocationDistance = distance;

                    } else {

                        zombieDenCount --;
                        i--;

                    }

                }

            }

            // now let's see if we should kite or attack anything

            boolean attacked = false;
            final RobotInfo[] immediateEnemies = robotController.senseNearbyRobots(13, currentTeam.opponent());
            RobotInfo bestEnemy;

            if (immediateEnemies.length > 0) {

                bestEnemy = combatModule.lowestInfectionEnemyFromEnemies(immediateEnemies);

            } else {

                final RobotInfo[] enemies = robotController.senseNearbyRobots(type.attackRadiusSquared, currentTeam.opponent());
                bestEnemy = combatModule.lowestInfectionEnemyFromEnemies(enemies);

            }

            // movement variables

            boolean ableToMove = true;
            Direction targetRubbleClearanceDirection = null;
            Direction desiredMovementDirection = null;

            final DirectionController directionController = new DirectionController(robotController);
            directionController.currentLocation = currentLocation;
            directionController.random = random;
            directionController.shouldAvoidEnemies = true;

            final RobotInfo[] enemies = robotController.senseHostileRobots(currentLocation, type.sensorRadiusSquared);
            directionController.nearbyEnemies = enemies;

            if (bestEnemy != null && currentLocation.distanceSquaredTo(bestEnemy.location) <= bestEnemy.type.attackRadiusSquared) {

                // should kite

                final Direction directionTowardsEnemies = directionController.getAverageDirectionTowardsEnemies(immediateEnemies, false);
                if (directionTowardsEnemies != null) {

                    ableToMove = true;
                    desiredMovementDirection = directionTowardsEnemies.opposite();

                }

            } else if (robotController.isWeaponReady()) {

                if (bestEnemy != null) {

                    robotController.attackLocation(bestEnemy.location);
                    attacked = true;
                    politicalAgenda.broadcastSignal(robotController,politicalAgenda.maximumFreeBroadcastRangeForType(robotController.getType()));

                }

            }

            // now let's try move toward an assignment

            if (robotController.isCoreReady() && politicalAgenda.isInformationSynced && ableToMove) {

                final RobotInfo[] zombies = robotController.senseNearbyRobots(type.sensorRadiusSquared, Team.ZOMBIE);

                // run away from zombies

                if (ableToMove) {

                    if (zombies.length > 0) {

                        final Direction directionTowardsZombies = directionController.getAverageDirectionTowardsEnemies(zombies, false);
                        if (directionTowardsZombies != null) {

                            desiredMovementDirection = directionTowardsZombies.opposite();

                        }

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

                // try move towards archon starting positions

                if (desiredMovementDirection == null && ableToMove) {

                    MapLocation[] locations = robotController.getInitialArchonLocations(robotController.getTeam().opponent());

                    if (locations.length > 0) {

                        int minDistance = Integer.MAX_VALUE;
                        MapLocation closestLocation = null;

                        for (int i = 0; i < locations.length; i++) {

                            final MapLocation location = locations[i];
                            final int distance = currentLocation.distanceSquaredTo(location);

                            if (distance < minDistance) {

                                minDistance = distance;
                                closestLocation = location;

                            }

                        }

                        desiredMovementDirection = currentLocation.directionTo(closestLocation);

                    }

                }

                // process movement

                if (desiredMovementDirection != null) {

                    final DirectionController.Result desiredMovementResult = directionController.getDirectionResultFromDirection(desiredMovementDirection, DirectionController.ADJUSTMENT_THRESHOLD_MEDIUM);
                    if (desiredMovementResult.direction != null) {

                        robotController.move(desiredMovementResult.direction);
                        lastDirection = desiredMovementResult.direction;
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

            if (robotController.isCoreReady() && politicalAgenda.isInformationSynced) {

                if (targetRubbleClearanceDirection != null) {

                    final Direction rubbleClearanceDirection = rubbleModule.getRubbleClearanceDirectionFromDirection(targetRubbleClearanceDirection, robotController, RubbleModule.ADJUSTMENT_THRESHOLD_LOW);
                    if (rubbleClearanceDirection != null) {

                        robotController.clearRubble(rubbleClearanceDirection);

                        if (turnsStuck != 0) {

                            turnsStuck = 0;

                        }

                        // otherwise they didn't move or clear rubble, check if they're stuck

                    } else if (politicalAgenda.notifications.size() == 0 && objectiveSignal != null) {

                        turnsStuck++;

                        if (turnsStuck > 5) {

                            if (objectiveSignal.type == PoliticalAgenda.SignalTypeZombieDen) {

                                politicalAgenda.zombieDens.remove(politicalAgenda.getIndexIdentifierForZombieDen(objectiveSignal.location));

                            }
                            turnsStuck = 0;

                        }

                    } else if (turnsStuck != 0) {

                        turnsStuck = 0;

                    }

                }

            }

            // finish up

            for (int i = 0; i < politicalAgenda.archonLocations.size(); i++) {

                final MapLocation archonLocation = politicalAgenda.archonLocations.get(i);
                robotController.setIndicatorLine(currentLocation, archonLocation, 25, 25, 255);

            }

            for (int i = 0; i < politicalAgenda.enemies.size(); i++) {

                final EnemyInfo enemy = politicalAgenda.enemies.get(i);
                robotController.setIndicatorLine(currentLocation, enemy.location, 255, 0, 255);

            }

            for (int i = 0; i < politicalAgenda.zombieDens.size(); i++) {

                final InformationSignal signal = politicalAgenda.zombieDens.get(i);
                robotController.setIndicatorLine(currentLocation, signal.location, 0, 255, 0);

            }

            if (objectiveSignal != null) {

                robotController.setIndicatorLine(objectiveSignal.location, robotController.getLocation(), 125, 0, 0);

            }

            Clock.yield();

        }

    }

}
