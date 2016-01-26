package team059;

import team059.Combat.*;
import team059.Information.*;
import team059.Movement.*;
import team059.Rubble.*;
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

        boolean requiresHealing = false;
        boolean isAggressive = false;
        int aggressiveLock = 0;

        final RobotType type = robotController.getType();

        while (true) {

            robotController.setIndicatorString(0, "When the zombie dens send us their people, they aren't sending the best. Let me tell you.");

            // update communication

            // begin

            MapLocation currentLocation = robotController.getLocation();

            // update communication

            politicalAgenda.processIncomingSignalsFromRobotController(robotController);
            if (!politicalAgenda.isInformationSynced) {

                Clock.yield();
                continue;

            }

            final RobotInfo[] enemies = robotController.senseHostileRobots(currentLocation, -1);
            final RobotInfo[] friendlies = robotController.senseNearbyRobots(-1, robotController.getTeam());
            politicalAgenda.verifyAllEnemyArchonSignals(robotController, enemies);

            while (true) {

                if (!robotController.isCoreReady() && !robotController.isWeaponReady()) {

                    break; // can't make any moves

                }

                // initialize some shared variables

                if (robotController.getHealth() < 30.0) {

                    requiresHealing = true;

                } else if (robotController.getHealth() / robotController.getType().maxHealth > 0.9) {

                    requiresHealing = false;

                }

                final DirectionController directionController = new DirectionController(robotController);
                directionController.currentLocation = currentLocation;
                directionController.nearbyEnemies = enemies;
                directionController.random = random;
                directionController.shouldAvoidEnemies = true;
                directionController.enemyBufferDistance = 1;

                final RobotInfo[] attackableEnemies = robotController.senseHostileRobots(currentLocation, robotController.getType().attackRadiusSquared);
                final RobotInfo bestAttackableEnemy = this.getBestEnemyToAttackFromEnemies(attackableEnemies, robotController, isAggressive);
                final RobotInfo bestFoundEnemy = this.getBestEnemyToAttackFromEnemies(enemies, robotController, isAggressive);

                if (robotController.isCoreReady()) {

                    if (bestAttackableEnemy != null) {

                        if (bestAttackableEnemy.type == RobotType.STANDARDZOMBIE || bestAttackableEnemy.type == RobotType.BIGZOMBIE) {

                            final int distance = currentLocation.distanceSquaredTo(bestAttackableEnemy.location);
                            if (distance < 9) {

                                final Direction kiteDirection = currentLocation.directionTo(bestAttackableEnemy.location);
                                if (kiteDirection != null) {

                                    directionController.shouldAvoidEnemies = false;
                                    final DirectionController.Result kiteDirectionResult = directionController.getDirectionResultFromDirection(kiteDirection.opposite(), DirectionController.ADJUSTMENT_THRESHOLD_MEDIUM);
                                    directionController.shouldAvoidEnemies = true;

                                    if (kiteDirectionResult.direction != null) {

                                        robotController.move(kiteDirectionResult.direction);
                                        currentLocation = robotController.getLocation();
                                        break;

                                    }

                                }

                            }

                        }

                    }

                }

                if (robotController.isWeaponReady()) {

                    if (bestAttackableEnemy != null) {

                        robotController.attackLocation(bestAttackableEnemy.location);
                        break;

                    }

                }

                // find an archon to heal at

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

                            } else if (nearestArchonResult.error == DirectionController.ErrorType.BLOCKED_RUBBLE) {

                                final Direction rubbleClearanceDirection = rubbleModule.getRubbleClearanceDirectionFromDirection(nearestArchonDirection, robotController, RubbleModule.ADJUSTMENT_THRESHOLD_MEDIUM);
                                if (rubbleClearanceDirection != null) {

                                    robotController.clearRubble(rubbleClearanceDirection);
                                    break;

                                }

                            }

                        }

                    }

                }

                // move forward or kite back

                if (robotController.isCoreReady()) {

                    // check whether we should move forward to engage or kite back

                    if (friendlies.length > enemies.length * 3) {

                        if (aggressiveLock == 0) {

                            isAggressive = true;

                        }

                    } else {

                        isAggressive = false;
                        aggressiveLock = 10;

                    }

                    if (bestAttackableEnemy != null && (bestAttackableEnemy.type == RobotType.BIGZOMBIE || bestAttackableEnemy.type == RobotType.STANDARDZOMBIE)) {

                        isAggressive = false;

                    }

                    if (!isAggressive) {

                        final Direction kiteDirection = directionController.getAverageDirectionTowardsEnemies(enemies, true, true, false);
                        if (kiteDirection != null) {

                            directionController.shouldAvoidEnemies = false;
                            final DirectionController.Result kiteDirectionResult = directionController.getDirectionResultFromDirection(kiteDirection.opposite(), DirectionController.ADJUSTMENT_THRESHOLD_LOW);
                            directionController.shouldAvoidEnemies = true;

                            if (kiteDirectionResult.direction != null) {

                                robotController.move(kiteDirectionResult.direction);
                                currentLocation = robotController.getLocation();
                                break;

                            }

                        }

                    } else {

                        if (bestFoundEnemy != null) {

                            final int distance = currentLocation.distanceSquaredTo(bestFoundEnemy.location);
                            if (distance > 8) {

                                final Direction pushDirection = currentLocation.directionTo(bestFoundEnemy.location);

                                directionController.shouldAvoidEnemies = false;
                                final DirectionController.Result pushDirectionResult = directionController.getDirectionResultFromDirection(pushDirection, DirectionController.ADJUSTMENT_THRESHOLD_LOW);
                                directionController.shouldAvoidEnemies = true;

                                if (pushDirectionResult.direction != null) {

                                    robotController.move(pushDirectionResult.direction);
                                    currentLocation = robotController.getLocation();
                                    break;

                                }

                            }

                        }

                    }

                    // if we have an enemy we're pushing toward or falling back from, then we should do nothing now
                    if (bestFoundEnemy != null) {

                        break;

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

                                } else {

                                    clearRubble = true;

                                }

                            } else if (signalResult.error == DirectionController.ErrorType.BLOCKED_RUBBLE) {

                                clearRubble = true;

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

                            if (clearRubble) {

                                final Direction rubbleClearanceDirection = rubbleModule.getRubbleClearanceDirectionFromDirection(signalDirection, robotController, RubbleModule.ADJUSTMENT_THRESHOLD_HIGH);
                                if (rubbleClearanceDirection != null) {

                                    robotController.clearRubble(rubbleClearanceDirection);
                                    movementModule.extendLocationInvalidationTurn(robotController);
                                    break;

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

                        } else if (closestSignalResult.error == DirectionController.ErrorType.BLOCKED_RUBBLE) {

                            final Direction rubbleClearanceDirection = rubbleModule.getRubbleClearanceDirectionFromDirection(closestSignalDirection, robotController, RubbleModule.ADJUSTMENT_THRESHOLD_MEDIUM);
                            if (rubbleClearanceDirection != null) {

                                robotController.clearRubble(rubbleClearanceDirection);
                                break;

                            }

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

                        } else if (closestSignalResult.error == DirectionController.ErrorType.BLOCKED_RUBBLE) {

                            final Direction rubbleClearanceDirection = rubbleModule.getRubbleClearanceDirectionFromDirection(closestSignalDirection, robotController, RubbleModule.ADJUSTMENT_THRESHOLD_MEDIUM);
                            if (rubbleClearanceDirection != null) {

                                robotController.clearRubble(rubbleClearanceDirection);
                                break;

                            }

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

                        } else if (closestSignalResult.error == DirectionController.ErrorType.BLOCKED_RUBBLE) {

                            final Direction rubbleClearanceDirection = rubbleModule.getRubbleClearanceDirectionFromDirection(closestSignalDirection, robotController, RubbleModule.ADJUSTMENT_THRESHOLD_MEDIUM);
                            if (rubbleClearanceDirection != null) {

                                robotController.clearRubble(rubbleClearanceDirection);
                                break;

                            }

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

                        } else if (closestSignalResult.error == DirectionController.ErrorType.BLOCKED_RUBBLE) {

                            final Direction rubbleClearanceDirection = rubbleModule.getRubbleClearanceDirectionFromDirection(closestSignalDirection, robotController, RubbleModule.ADJUSTMENT_THRESHOLD_MEDIUM);
                            if (rubbleClearanceDirection != null) {

                                robotController.clearRubble(rubbleClearanceDirection);
                                break;

                            }

                        }

                    }

                }

                // try clear rubble

                if (robotController.isCoreReady()) {

                    final Direction rubbleClearanceDirection = rubbleModule.getRubbleClearanceDirectionFromDirection(directionController.getRandomDirection(), robotController, RubbleModule.ADJUSTMENT_THRESHOLD_ALL);
                    if (rubbleClearanceDirection != null) {

                        robotController.clearRubble(rubbleClearanceDirection);
                        break;

                    }

                }

                break;

            }

            Clock.yield();

        }

    }

    /*
    COMBAT
     */

    private RobotInfo getBestEnemyToAttackFromEnemies(final RobotInfo[] enemies, final RobotController robotController, boolean isAggressive) {

        RobotInfo bestEnemy = null;
        for (int i = 0; i < enemies.length; i++) {

            final RobotInfo enemy = enemies[i];
            if (robotController.getRoundNum() > 2500 && isAggressive) {

                // we don't want to attack others if we're aggressive and it's past a late round and we have a lot of friendlies around (too risky)
                if (!enemy.type.isZombie) {

                    continue;

                }

            }
            if (bestEnemy == null) {

                // no best enemy, so this is the best enemy

                bestEnemy = enemy;
                continue;

            }
            if (enemy.type == RobotType.ZOMBIEDEN) {

                if (bestEnemy.type != RobotType.ZOMBIEDEN) {

                    continue;

                }

            }
            if (enemy.type.isZombie) {

                if (!bestEnemy.type.isZombie) {

                    // we don't want to prioritize a zombie over the enemy team
                    continue;

                }

                if (enemy.health < bestEnemy.health) {

                    bestEnemy = enemy;
                    continue;

                }

            } else {

                if (enemy.viperInfectedTurns < bestEnemy.viperInfectedTurns) {

                    bestEnemy = enemy;
                    continue;

                } else if (enemy.viperInfectedTurns == bestEnemy.viperInfectedTurns) {

                    if (enemy.health < bestEnemy.health) {

                        bestEnemy = enemy;
                        continue;

                    }

                }

            }

        }
        return bestEnemy;

    }

}
