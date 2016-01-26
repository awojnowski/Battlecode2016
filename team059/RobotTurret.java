package team059;

import team059.Combat.*;
import team059.Information.*;
import team059.Movement.*;
import team059.Rubble.*;
import battlecode.common.*;
import java.util.*;

public class RobotTurret implements Robot {

    public void run(final RobotController robotController) throws GameActionException {

        robotController.emptySignalQueue();

        final CombatModule combatModule = new CombatModule();
        final MovementModule movementModule = new MovementModule();
        final PoliticalAgenda politicalAgenda = new PoliticalAgenda();
        final Random random = new Random(robotController.getID());

        final Team currentTeam = robotController.getTeam();

        boolean requiresHealing = false;
        int turnsWithoutEnemy = 0;

        while (true) {

            robotController.setIndicatorString(0, "I'm going to build a wall... of rubble.");

            // update communication

            politicalAgenda.processIncomingSignalsFromRobotController(robotController);
            if (!politicalAgenda.isInformationSynced) {

                Clock.yield();
                continue;

            }

            // begin

            MapLocation currentLocation = robotController.getLocation();
            final RobotInfo[] enemies = robotController.senseHostileRobots(currentLocation, robotController.getType().sensorRadiusSquared);
            final RobotInfo[] friendlies = robotController.senseNearbyRobots(robotController.getType().sensorRadiusSquared, robotController.getTeam());

            if (robotController.getHealth() < 30.0) {

                requiresHealing = true;

            } else if (robotController.getHealth() / robotController.getType().maxHealth > 0.9) {

                requiresHealing = false;

            }

            boolean isDoomed = false;
            if (robotController.getInfectedTurns() * 2 > robotController.getHealth()) {

                isDoomed = true;

            }

            if (robotController.getType() == RobotType.TTM) {

                if (enemies.length > 0 || this.isEnemySignalInTurretRange(politicalAgenda.enemies, currentLocation)) {

                    robotController.unpack();
                    Clock.yield();
                    continue;

                }

                while (true) {

                    final DirectionController directionController = new DirectionController(robotController);
                    directionController.currentLocation = currentLocation;
                    directionController.nearbyEnemies = enemies;
                    directionController.random = random;
                    directionController.shouldAvoidEnemies = true;
                    directionController.enemyBufferDistance = 1;

                    if (robotController.isCoreReady()) {

                        if (requiresHealing) {

                            MapLocation nearestArchonLocation = null;
                            int nearestArchonDistance = Integer.MAX_VALUE;

                            for (int i = 0; i < friendlies.length; i++) {

                                final RobotInfo robot = friendlies[i];
                                if (robot.type != RobotType.ARCHON) {

                                    continue;

                                }
                                final int distance = currentLocation.distanceSquaredTo(robot.location);
                                if (distance < nearestArchonDistance) {

                                    nearestArchonLocation = robot.location;
                                    nearestArchonDistance = distance;

                                }

                            }

                            if (nearestArchonLocation == null) {

                                for (int i = 0; i < politicalAgenda.archonLocations.size(); i++) {

                                    final MapLocation archonLocation = politicalAgenda.archonLocations.get(i);
                                    final int distance = currentLocation.distanceSquaredTo(archonLocation);
                                    if (distance < nearestArchonDistance) {

                                        nearestArchonLocation = archonLocation;
                                        nearestArchonDistance = distance;

                                    }

                                }

                            }

                            if (nearestArchonDistance > 12 && nearestArchonLocation != null) {

                                final Direction nearestArchonDirection = currentLocation.directionTo(nearestArchonLocation);

                                directionController.shouldAvoidEnemies = false;
                                final DirectionController.Result nearestArchonResult = directionController.getDirectionResultFromDirection(nearestArchonDirection, DirectionController.ADJUSTMENT_THRESHOLD_MEDIUM);
                                directionController.shouldAvoidEnemies = true;

                                if (nearestArchonResult.direction != null) {

                                    robotController.move(nearestArchonResult.direction);
                                    currentLocation = robotController.getLocation();
                                    break;

                                }

                            }

                        }

                    }

                    // move toward an objective

                    if (robotController.isCoreReady()) {

                        InformationSignal objectiveSignal = null;
                        int closestObjectiveLocationDistance = Integer.MAX_VALUE;

                        int enemyArchonCount = politicalAgenda.enemyArchons.size();
                        for (int i = 0; i < enemyArchonCount; i++) {

                            final InformationSignal signal = politicalAgenda.enemyArchons.get(i);
                            int distance = signal.location.distanceSquaredTo(currentLocation);
                            if (!combatModule.isLocationOnOurSide(robotController, signal.location)) {

                                distance = distance * 5;

                            }
                            if (distance < closestObjectiveLocationDistance) {

                                objectiveSignal = signal;
                                closestObjectiveLocationDistance = distance;

                            }

                        }

                        int zombieDenCount = politicalAgenda.zombieDens.size();
                        for (int i = 0; i < zombieDenCount; i++) {

                            final InformationSignal signal = politicalAgenda.zombieDens.get(i);
                            int distance = signal.location.distanceSquaredTo(currentLocation);
                            if (!combatModule.isLocationOnOurSide(robotController, signal.location)) {

                                distance = distance * 5;

                            }
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

                        if (objectiveSignal != null) {

                            boolean shouldMoveTowardsObjective = true;
                            if (objectiveSignal.type == PoliticalAgenda.SignalTypeZombieDen) {

                                final int distance = currentLocation.distanceSquaredTo(objectiveSignal.location);
                                if (distance < 9) {

                                    shouldMoveTowardsObjective = false;

                                }

                            }

                            if (shouldMoveTowardsObjective) {

                                final Direction signalDirection = currentLocation.directionTo(objectiveSignal.location);
                                final DirectionController.Result signalResult = directionController.getDirectionResultFromDirection(signalDirection, DirectionController.ADJUSTMENT_THRESHOLD_LOW);

                                boolean clearRubble = false;
                                if (signalResult.direction != null) {

                                    if (!movementModule.isMovementLocationRepetitive(currentLocation.add(signalResult.direction), robotController)) {

                                        robotController.move(signalResult.direction);
                                        currentLocation = robotController.getLocation();
                                        movementModule.addMovementLocation(currentLocation, robotController);
                                        break;

                                    }

                                } else if (signalResult.error == DirectionController.ErrorType.BLOCKED_FRIENDLIES) {

                                    final DirectionController.Result signalResultExtended = directionController.getDirectionResultFromDirection(signalDirection, DirectionController.ADJUSTMENT_THRESHOLD_LOW, DirectionController.ADJUSTMENT_THRESHOLD_MEDIUM);
                                    if (signalResultExtended.direction != null) {

                                        if (!movementModule.isMovementLocationRepetitive(currentLocation.add(signalResultExtended.direction), robotController)) {

                                            robotController.move(signalResultExtended.direction);
                                            currentLocation = robotController.getLocation();
                                            movementModule.addMovementLocation(currentLocation, robotController);
                                            break;

                                        }

                                    }

                                }

                            }

                        }

                    }

                    // move to an enemy

                    if (robotController.isCoreReady()) {

                        int closestSignalDistance = Integer.MAX_VALUE;
                        MapLocation closestLocation = null;

                        final ImmutableInformationCollection<EnemyInfo> enemySignals = politicalAgenda.enemies;
                        for (int i = 0; i < enemySignals.size(); i++) {

                            final EnemyInfo enemyInfo = enemySignals.get(i);
                            final int distance = currentLocation.distanceSquaredTo(enemyInfo.location);
                            if (distance < closestSignalDistance) {

                                closestSignalDistance = distance;
                                closestLocation = enemyInfo.location;

                            }

                        }
                        if (closestLocation != null) {

                            final Direction closestSignalDirection = currentLocation.directionTo(closestLocation);
                            final DirectionController.Result closestSignalResult = directionController.getDirectionResultFromDirection(closestSignalDirection, DirectionController.ADJUSTMENT_THRESHOLD_LOW);
                            if (closestSignalResult.direction != null && !movementModule.isMovementLocationRepetitive(currentLocation.add(closestSignalResult.direction), robotController)) {

                                robotController.move(closestSignalResult.direction);
                                currentLocation = robotController.getLocation();
                                movementModule.addMovementLocation(currentLocation, robotController);
                                break;

                            }

                        }

                    }

                    // move to an enemy clump

                    if (robotController.isCoreReady()) {

                        int closestEnemyClump = Integer.MAX_VALUE;
                        MapLocation closestLocation = null;

                        final ArrayList<ClumpInfo> enemyClumps = politicalAgenda.enemyClumps;
                        for (int i = 0; i < enemyClumps.size(); i++) {

                            final ClumpInfo enemyInfo = enemyClumps.get(i);
                            final int distance = currentLocation.distanceSquaredTo(enemyInfo.location);
                            if (distance < closestEnemyClump) {

                                closestEnemyClump = distance;
                                closestLocation = enemyInfo.location;

                            }

                        }
                        if (closestLocation != null) {

                            final Direction closestSignalDirection = currentLocation.directionTo(closestLocation);
                            final DirectionController.Result closestSignalResult = directionController.getDirectionResultFromDirection(closestSignalDirection, DirectionController.ADJUSTMENT_THRESHOLD_LOW);
                            if (closestSignalResult.direction != null && !movementModule.isMovementLocationRepetitive(currentLocation.add(closestSignalResult.direction), robotController)) {

                                robotController.move(closestSignalResult.direction);
                                currentLocation = robotController.getLocation();
                                movementModule.addMovementLocation(currentLocation, robotController);
                                break;

                            }

                        }

                    }

                    // move to a friendly clump

                    if (robotController.isCoreReady()) {

                        int closestFriendlyClump = Integer.MAX_VALUE;
                        MapLocation closestLocation = null;

                        final ArrayList<ClumpInfo> friendlyClumps = politicalAgenda.friendlyClumps;
                        for (int i = 0; i < friendlyClumps.size(); i++) {

                            final ClumpInfo enemyInfo = friendlyClumps.get(i);
                            final int distance = currentLocation.distanceSquaredTo(enemyInfo.location);
                            if (distance < closestFriendlyClump) {

                                closestFriendlyClump = distance;
                                closestLocation = enemyInfo.location;

                            }

                        }
                        if (closestLocation != null) {

                            final Direction closestSignalDirection = currentLocation.directionTo(closestLocation);
                            final DirectionController.Result closestSignalResult = directionController.getDirectionResultFromDirection(closestSignalDirection, DirectionController.ADJUSTMENT_THRESHOLD_LOW);
                            if (closestSignalResult.direction != null && !movementModule.isMovementLocationRepetitive(currentLocation.add(closestSignalResult.direction), robotController)) {

                                robotController.move(closestSignalResult.direction);
                                currentLocation = robotController.getLocation();
                                movementModule.addMovementLocation(currentLocation, robotController);
                                break;

                            }

                        }

                    }

                    // move to an archon

                    if (robotController.isCoreReady()) {

                        int closestSignalDistance = Integer.MAX_VALUE;
                        MapLocation closestLocation = null;

                        final ArrayList<MapLocation> archonLocations = politicalAgenda.archonLocations;
                        for (int i = 0; i < archonLocations.size(); i++) {

                            final MapLocation location = archonLocations.get(i);
                            final int distance = currentLocation.distanceSquaredTo(location);
                            if (distance < closestSignalDistance) {

                                closestSignalDistance = distance;
                                closestLocation = location;

                            }

                        }
                        if (closestLocation != null) {

                            final Direction closestSignalDirection = currentLocation.directionTo(closestLocation);
                            final DirectionController.Result closestSignalResult = directionController.getDirectionResultFromDirection(closestSignalDirection, DirectionController.ADJUSTMENT_THRESHOLD_LOW);
                            if (closestSignalResult.direction != null && !movementModule.isMovementLocationRepetitive(currentLocation.add(closestSignalResult.direction), robotController)) {

                                robotController.move(closestSignalResult.direction);
                                currentLocation = robotController.getLocation();
                                movementModule.addMovementLocation(currentLocation, robotController);
                                break;

                            }

                        }

                    }

                    break;

                }

            } else if (robotController.getType() == RobotType.TURRET) {

                final RobotInfo[] attackableEnemies = robotController.senseHostileRobots(currentLocation, robotController.getType().attackRadiusSquared);
                final MapLocation bestAttackableEnemyLocation = this.getBestEnemyLocationToAttackFromEnemies(attackableEnemies, politicalAgenda.enemies, currentLocation);
                if (bestAttackableEnemyLocation == null) {

                    turnsWithoutEnemy++;

                } else {

                    turnsWithoutEnemy = 0;

                }

                if (turnsWithoutEnemy >= 20) {

                    turnsWithoutEnemy = 0;

                    robotController.pack();
                    Clock.yield();
                    continue;

                }

                while (true) {

                    if (robotController.isWeaponReady()) {

                        if (bestAttackableEnemyLocation != null) {

                            robotController.attackLocation(bestAttackableEnemyLocation);
                            break;

                        }

                    }

                    break;

                }

            }

            // finish up

            Clock.yield();

        }

    }

    /*
    COMBAT
     */

    private boolean isEnemySignalInTurretRange(final ImmutableInformationCollection<EnemyInfo> enemySignals, final MapLocation currentLocation) {

        for (int i = 0; i < enemySignals.size(); i++) {

            final EnemyInfo enemy = enemySignals.get(i);
            final int distance = currentLocation.distanceSquaredTo(enemy.location);
            if (distance < GameConstants.TURRET_MINIMUM_RANGE || distance > RobotType.TURRET.attackRadiusSquared) {

                return true;

            }

        }
        return false;

    }

    private MapLocation getBestEnemyLocationToAttackFromEnemies(final RobotInfo[] enemies, final ImmutableInformationCollection<EnemyInfo> enemySignals, final MapLocation currentLocation) {

        double bestEnemyHealth = Double.MAX_VALUE;
        MapLocation bestEnemyLocation = null;
        RobotType bestEnemyType = null;
        for (int i = 0; i < enemies.length; i++) {

            final RobotInfo enemy = enemies[i];
            final int distance = currentLocation.distanceSquaredTo(enemy.location);
            if (distance < GameConstants.TURRET_MINIMUM_RANGE || distance > RobotType.TURRET.attackRadiusSquared) {

                continue;

            }
            if (bestEnemyLocation == null) {

                bestEnemyHealth = enemy.health;
                bestEnemyLocation = enemy.location;
                bestEnemyType = enemy.type;
                continue;

            }
            if (enemy.type != RobotType.ZOMBIEDEN && bestEnemyType == RobotType.ZOMBIEDEN) {

                bestEnemyHealth = enemy.health;
                bestEnemyLocation = enemy.location;
                bestEnemyType = enemy.type;
                continue;

            }
            if (enemy.type == RobotType.ZOMBIEDEN && bestEnemyType != RobotType.ZOMBIEDEN) {

                continue;

            }
            if (enemy.health < bestEnemyHealth) {

                bestEnemyHealth = enemy.health;
                bestEnemyLocation = enemy.location;
                bestEnemyType = enemy.type;
                continue;

            }

        }
        for (int i = 0; i < enemySignals.size(); i++) {

            final EnemyInfo enemy = enemySignals.get(i);
            final int distance = currentLocation.distanceSquaredTo(enemy.location);
            if (distance < GameConstants.TURRET_MINIMUM_RANGE || distance > RobotType.TURRET.attackRadiusSquared) {

                continue;

            }
            if (bestEnemyLocation == null) {

                bestEnemyHealth = enemy.health;
                bestEnemyLocation = enemy.location;
                bestEnemyType = enemy.type;
                continue;

            }
            if (enemy.type != RobotType.ZOMBIEDEN && bestEnemyType == RobotType.ZOMBIEDEN) {

                bestEnemyHealth = enemy.health;
                bestEnemyLocation = enemy.location;
                bestEnemyType = enemy.type;
                continue;

            }
            if (enemy.type == RobotType.ZOMBIEDEN && bestEnemyType != RobotType.ZOMBIEDEN) {

                continue;

            }
            if (enemy.health < bestEnemyHealth) {

                bestEnemyHealth = enemy.health;
                bestEnemyLocation = enemy.location;
                bestEnemyType = enemy.type;
                continue;

            }

        }
        return bestEnemyLocation;

    }

}
