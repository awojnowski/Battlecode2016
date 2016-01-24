package AaronBot3;

import AaronBot3.Combat.*;
import AaronBot3.Information.*;
import AaronBot3.Movement.*;
import AaronBot3.Rubble.*;
import battlecode.common.*;
import jdk.nashorn.internal.ir.annotations.Immutable;

import java.util.*;

public class RobotSoldier implements Robot {

    public void run(final RobotController robotController) throws GameActionException {

        robotController.emptySignalQueue();

        final CombatModule combatModule = new CombatModule();
        final MovementModule movementModule = new MovementModule();
        final PoliticalAgenda politicalAgenda = new PoliticalAgenda();
        final Random random = new Random(robotController.getID());
        final RubbleModule rubbleModule = new RubbleModule();

        final Team currentTeam = robotController.getTeam();
        final RobotType type = robotController.getType();

        boolean requiresHealing = false;
        int aggressiveLock = 0;

        while (true) {

            // prep to run this turn

            robotController.setIndicatorString(0, "");
            robotController.setIndicatorString(1, "");

            if (aggressiveLock > 0) {

                aggressiveLock --;

            }

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

                if (!robotController.isCoreReady() || !robotController.isWeaponReady()) {

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
                final RobotInfo bestAttackableEnemy = this.getBestEnemyToAttackFromEnemies(attackableEnemies);
                final RobotInfo bestFoundEnemy = this.getBestEnemyToAttackFromEnemies(enemies);

                // attack the enemy if it is not a zombie den (those are handled later and prioritized lower)

                if (robotController.isWeaponReady()) {

                    if (bestAttackableEnemy != null && bestAttackableEnemy.type != RobotType.ZOMBIEDEN) {

                        robotController.attackLocation(bestAttackableEnemy.location);
                        break;

                    }

                }

                // move forward or kite back

                if (robotController.isCoreReady()) {

                    // check whether we should move forward to engage or kite back

                    boolean shouldBePassive = true;
                    if (aggressiveLock == 0) {

                        if (friendlies.length > enemies.length * 3) {

                            shouldBePassive = false;

                        }

                    }

                    if (shouldBePassive) {

                        final Direction kiteDirection = directionController.getAverageDirectionTowardsEnemies(enemies, true);
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


                            final Direction pushDirection = currentLocation.directionTo(bestFoundEnemy.location);

                            directionController.shouldAvoidEnemies = false;
                            final DirectionController.Result kiteDirectionResult = directionController.getDirectionResultFromDirection(pushDirection, DirectionController.ADJUSTMENT_THRESHOLD_LOW);
                            directionController.shouldAvoidEnemies = true;

                            if (kiteDirectionResult.direction != null) {

                                robotController.move(kiteDirectionResult.direction);
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

                    

                }

                break;

            }



            // the while block allows us to just break out of the actions when one is completed
            /*while (true) {

                if (!robotController.isCoreReady() && !robotController.isWeaponReady()) {

                    break;

                }

                // create the direction controller

                final DirectionController directionController = new DirectionController(robotController);
                directionController.currentLocation = currentLocation;
                directionController.nearbyEnemies = enemies;
                directionController.random = random;
                directionController.shouldAvoidEnemies = true;
                directionController.enemyBufferDistance = 1;

                if (requiresHealing) {

                    directionController.enemyBufferDistance = 2;

                }

                // firstly let's find the closest archon, for reference

                int nearestArchonDistance = Integer.MAX_VALUE;
                MapLocation nearestArchonLocation = null;

                for (int i = 0; i < friendlies.length; i++) {

                    final RobotInfo robot = friendlies[i];
                    if (robot.type != RobotType.ARCHON) {

                        continue;

                    }
                    final int distance = currentLocation.distanceSquaredTo(robot.location);
                    if (distance < nearestArchonDistance) {

                        nearestArchonDistance = distance;
                        nearestArchonLocation = robot.location;

                    }

                }
                if (nearestArchonLocation == null) {

                    for (int i = 0; i < politicalAgenda.archonLocations.size(); i++) {

                        final MapLocation archonLocation = politicalAgenda.archonLocations.get(i);
                        final int distance = currentLocation.distanceSquaredTo(archonLocation);
                        if (distance < nearestArchonDistance) {

                            nearestArchonDistance = distance;
                            nearestArchonLocation = archonLocation;

                        }

                    }

                }

                // information

                final RobotInfo[] attackableEnemies = robotController.senseHostileRobots(currentLocation, robotController.getType().attackRadiusSquared);
                final RobotInfo bestAttackableEnemy = this.getBestEnemyToAttackFromEnemies(attackableEnemies);

                // flags

                if (robotController.isWeaponReady()) {

                    if (bestAttackableEnemy != null) {

                        robotController.attackLocation(bestAttackableEnemy.location);
                        if (bestAttackableEnemy.type != RobotType.ZOMBIEDEN) {

                            politicalAgenda.broadcastSignal(robotController, politicalAgenda.maximumFreeBroadcastRangeForType(robotController.getType()));

                        }
                        break;

                    }

                }

                // logic

                // see if we should kite or move forward to an enemy

                if (enemies.length > 0 && friendlies.length > enemies.length * 4) {

                    robotController.setIndicatorString(1, "Aggressive");

                    if (robotController.isCoreReady()) {

                        if (bestAttackableEnemy != null) {

                            final Direction bestAttackableEnemyDirection = currentLocation.directionTo(bestAttackableEnemy.location);

                            directionController.shouldAvoidEnemies = false;
                            final DirectionController.Result bestAttackableEnemyDirectionResult = directionController.getDirectionResultFromDirection(bestAttackableEnemyDirection, DirectionController.ADJUSTMENT_THRESHOLD_LOW);
                            directionController.shouldAvoidEnemies = true;

                            if (bestAttackableEnemyDirectionResult.direction != null && !movementModule.isMovementLocationRepetitive(currentLocation.add(bestAttackableEnemyDirectionResult.direction), robotController)) {

                                robotController.move(bestAttackableEnemyDirectionResult.direction);
                                currentLocation = robotController.getLocation();
                                movementModule.addMovementLocation(currentLocation, robotController);

                                robotController.setIndicatorString(0, "I am moving to an enemy " + bestAttackableEnemy.location);
                                break;

                            }

                        }

                    }

                } else {

                    robotController.setIndicatorString(1, "Passive");

                    if (robotController.isCoreReady()) {

                        final Direction kiteDirection = directionController.getAverageDirectionTowardsEnemies(enemies, true);
                        if (kiteDirection != null) {

                            directionController.shouldAvoidEnemies = false;
                            final DirectionController.Result kiteDirectionResult = directionController.getDirectionResultFromDirection(kiteDirection.opposite(), DirectionController.ADJUSTMENT_THRESHOLD_MEDIUM);
                            directionController.shouldAvoidEnemies = true;

                            if (kiteDirectionResult.direction != null) {

                                robotController.move(kiteDirectionResult.direction);
                                currentLocation = robotController.getLocation();

                                robotController.setIndicatorString(0, "I am trying to kite from enemies " + kiteDirectionResult.direction);
                                break;

                            }

                        }

                    }

                }

                // respond to nearby unit pings

                if (robotController.isCoreReady() && !requiresHealing) {

                    int closestSignalDistance = Integer.MAX_VALUE;
                    MapLocation closestSignalLocation = null;

                    final ArrayList<Signal> notifications = politicalAgenda.notifications;
                    for (int i = 0; i < notifications.size(); i++) {

                        final Signal signal = notifications.get(i);
                        final int distance = currentLocation.distanceSquaredTo(signal.getLocation());
                        if (distance < closestSignalDistance) {

                            closestSignalDistance = distance;
                            closestSignalLocation = signal.getLocation();

                        }

                    }
                    if (closestSignalLocation != null) {

                        final Direction closestSignalDirection = currentLocation.directionTo(closestSignalLocation);
                        final DirectionController.Result closestSignalResult = directionController.getDirectionResultFromDirection(closestSignalDirection, DirectionController.ADJUSTMENT_THRESHOLD_LOW);
                        if (closestSignalResult.direction != null && !movementModule.isMovementLocationRepetitive(currentLocation.add(closestSignalResult.direction), robotController)) {

                            robotController.move(closestSignalResult.direction);
                            currentLocation = robotController.getLocation();
                            movementModule.addMovementLocation(currentLocation, robotController);

                            robotController.setIndicatorString(0, "I am moving to a signal at " + closestSignalLocation);
                            break;

                        } else if (closestSignalResult.error == DirectionController.ErrorType.BLOCKED_FRIENDLIES) {

                            final DirectionController.Result closestSignalResultExtended = directionController.getDirectionResultFromDirection(closestSignalDirection, DirectionController.ADJUSTMENT_THRESHOLD_MEDIUM);
                            if (closestSignalResultExtended.direction != null && !movementModule.isMovementLocationRepetitive(currentLocation.add(closestSignalResultExtended.direction), robotController)) {

                                robotController.move(closestSignalResultExtended.direction);
                                currentLocation = robotController.getLocation();
                                movementModule.addMovementLocation(currentLocation, robotController);

                                robotController.setIndicatorString(0, "I am moving to a signal at " + closestSignalLocation);
                                break;

                            }

                        }

                    }

                }

                // try to move to an objective

                if (robotController.isCoreReady() && objectiveSignal != null) {

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
                                robotController.setIndicatorString(0, "I am moving to an objective at " + objectiveSignal.location);
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
                                    robotController.setIndicatorString(0, "I am moving to an objective at " + objectiveSignal.location + " (extended)");
                                    break;

                                }

                            }

                        }

                        if (clearRubble) {

                            final Direction rubbleClearanceDirection = rubbleModule.getRubbleClearanceDirectionFromDirection(signalDirection, robotController, RubbleModule.ADJUSTMENT_THRESHOLD_HIGH);
                            if (rubbleClearanceDirection != null) {

                                robotController.clearRubble(rubbleClearanceDirection);
                                movementModule.extendLocationInvalidationTurn(robotController);
                                robotController.setIndicatorString(0, "I cleared rubble " + rubbleClearanceDirection);
                                break;

                            }

                        }

                    }

                }

                break;

            }*/

            /*// see if we can attack anything this turn

            final RobotInfo[] attackableEnemies = robotController.senseHostileRobots(currentLocation, robotController.getType().attackRadiusSquared);
            final RobotInfo bestEnemy = this.getBestEnemyToAttackFromEnemies(attackableEnemies);

            if (robotController.isWeaponReady()) {

                if (bestEnemy != null && (!this.shouldKiteEnemy(bestEnemy) || currentLocation.distanceSquaredTo(bestEnemy.location) >= bestEnemy.type.attackRadiusSquared)) {

                    robotController.attackLocation(bestEnemy.location);
                    robotController.setIndicatorString(0, "I attacked enemy at " + bestEnemy.location);

                    if (bestEnemy.type != RobotType.ZOMBIEDEN) {

                        politicalAgenda.broadcastSignal(robotController, politicalAgenda.maximumFreeBroadcastRangeForType(robotController.getType()));

                    }

                }

            }

            // handle movement

            Direction desiredRubbleClearanceDirection = null;

            final DirectionController directionController = new DirectionController(robotController);
            directionController.currentLocation = currentLocation;
            directionController.nearbyEnemies = enemies;
            directionController.random = random;
            directionController.shouldAvoidEnemies = true;
            directionController.enemyBufferDistance = 1;

            if (robotController.getHealth() < 30.0) {

                directionController.enemyBufferDistance = 2;
                requiresHealing = true;

            } else if (robotController.getHealth() / robotController.getType().maxHealth > 0.90) {

                requiresHealing = false;

            }

            // kite from nearby enemies

            if (robotController.isCoreReady()) {

                final Direction kiteDirection = directionController.getAverageDirectionTowardsEnemies(enemies, true);
                if (kiteDirection != null) {

                    directionController.shouldAvoidEnemies = false;
                    final DirectionController.Result kiteDirectionResult = directionController.getDirectionResultFromDirection(kiteDirection.opposite(), DirectionController.ADJUSTMENT_THRESHOLD_MEDIUM);
                    directionController.shouldAvoidEnemies = true;

                    if (kiteDirectionResult.direction != null) {

                        robotController.move(kiteDirectionResult.direction);
                        currentLocation = robotController.getLocation();

                        robotController.setIndicatorString(0, "I am trying to kite from enemies " + kiteDirectionResult.direction);

                    }

                }

                if (desiredRubbleClearanceDirection != null) {

                    desiredRubbleClearanceDirection = kiteDirection;

                }

            }

            if (robotController.isCoreReady()) {

                if (bestEnemy == null) {

                    // try find an archon to heal up

                    if (robotController.isCoreReady() && requiresHealing) {

                        RobotInfo nearestArchon = null;
                        int nearestArchonDistance = Integer.MAX_VALUE;

                        for (int i = 0; i < friendlies.length; i++) {

                            final RobotInfo robot = friendlies[i];
                            if (robot.type != RobotType.ARCHON) {

                                continue;

                            }
                            final int distance = currentLocation.distanceSquaredTo(robot.location);
                            if (distance < nearestArchonDistance) {

                                nearestArchon = robot;
                                nearestArchonDistance = distance;

                            }

                        }

                        if (nearestArchon != null) {

                            final Direction nearestArchonDirection = currentLocation.directionTo(nearestArchon.location);

                            directionController.shouldAvoidEnemies = false;
                            final DirectionController.Result nearestArchonResult = directionController.getDirectionResultFromDirection(nearestArchonDirection, DirectionController.ADJUSTMENT_THRESHOLD_MEDIUM);
                            directionController.shouldAvoidEnemies = true;

                            if (nearestArchonResult.direction != null) {

                                robotController.move(nearestArchonResult.direction);
                                currentLocation = robotController.getLocation();

                                robotController.setIndicatorString(0, "I am moving to an archon at " + nearestArchon.location);

                            }

                            if (desiredRubbleClearanceDirection != null) {

                                desiredRubbleClearanceDirection = nearestArchonDirection;

                            }

                        }

                    }

                    // respond to nearby unit pings

                    if (robotController.isCoreReady() && !requiresHealing) {

                        int closestSignalDistance = Integer.MAX_VALUE;
                        MapLocation closestSignalLocation = null;

                        final ArrayList<Signal> notifications = politicalAgenda.notifications;
                        for (int i = 0; i < notifications.size(); i++) {

                            final Signal signal = notifications.get(i);
                            final int distance = currentLocation.distanceSquaredTo(signal.getLocation());
                            if (distance < closestSignalDistance) {

                                closestSignalDistance = distance;
                                closestSignalLocation = signal.getLocation();

                            }

                        }
                        if (closestSignalLocation != null) {

                            final Direction closestSignalDirection = currentLocation.directionTo(closestSignalLocation);
                            final DirectionController.Result closestSignalResult = directionController.getDirectionResultFromDirection(closestSignalDirection, DirectionController.ADJUSTMENT_THRESHOLD_LOW);
                            if (closestSignalResult.direction != null && !movementModule.isMovementLocationRepetitive(currentLocation.add(closestSignalResult.direction), robotController)) {

                                robotController.move(closestSignalResult.direction);
                                currentLocation = robotController.getLocation();
                                movementModule.addMovementLocation(currentLocation, robotController);

                                robotController.setIndicatorString(0, "I am moving to a signal at " + closestSignalLocation);

                            } else if (closestSignalResult.error == DirectionController.ErrorType.BLOCKED_FRIENDLIES) {

                                final DirectionController.Result closestSignalResultExtended = directionController.getDirectionResultFromDirection(closestSignalDirection, DirectionController.ADJUSTMENT_THRESHOLD_MEDIUM);
                                if (closestSignalResultExtended.direction != null && !movementModule.isMovementLocationRepetitive(currentLocation.add(closestSignalResultExtended.direction), robotController)) {

                                    robotController.move(closestSignalResultExtended.direction);
                                    currentLocation = robotController.getLocation();
                                    movementModule.addMovementLocation(currentLocation, robotController);

                                    robotController.setIndicatorString(0, "I am moving to a signal at " + closestSignalLocation);

                                }

                            }

                            if (desiredRubbleClearanceDirection != null) {

                                desiredRubbleClearanceDirection = closestSignalDirection;

                            }

                        }

                    }

                    // move to an objective

                    if (robotController.isCoreReady() && objectiveSignal != null) {

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
                            if (signalResult.direction != null && !movementModule.isMovementLocationRepetitive(currentLocation.add(signalResult.direction), robotController)) {

                                robotController.move(signalResult.direction);
                                currentLocation = robotController.getLocation();
                                movementModule.addMovementLocation(currentLocation, robotController);

                                robotController.setIndicatorString(0, "I am moving to an objective at " + objectiveSignal.location);

                            }

                            if (desiredRubbleClearanceDirection != null) {

                                desiredRubbleClearanceDirection = signalDirection;

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

                                robotController.setIndicatorString(0, "I am moving to an enemy " + closestLocation);

                            }

                            if (desiredRubbleClearanceDirection != null) {

                                desiredRubbleClearanceDirection = closestSignalDirection;

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

                                robotController.setIndicatorString(0, "I am moving to an enemy clump at " + closestLocation);

                            }

                            if (desiredRubbleClearanceDirection != null) {

                                desiredRubbleClearanceDirection = closestSignalDirection;

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

                                robotController.setIndicatorString(0, "I am moving to a friendly clump at " + closestLocation);

                            }

                            if (desiredRubbleClearanceDirection != null) {

                                desiredRubbleClearanceDirection = closestSignalDirection;

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

                                robotController.setIndicatorString(0, "I am moving to a friendly archon at " + closestLocation);

                            }

                            if (desiredRubbleClearanceDirection != null) {

                                desiredRubbleClearanceDirection = closestSignalDirection;

                            }

                        }

                    }

                } else if (bestEnemy.type == RobotType.ZOMBIEDEN) {

                    final int distance = currentLocation.distanceSquaredTo(bestEnemy.location);
                    if (distance > 5) {

                        final Direction closestSignalDirection = currentLocation.directionTo(bestEnemy.location);
                        final DirectionController.Result closestSignalResult = directionController.getDirectionResultFromDirection(closestSignalDirection, DirectionController.ADJUSTMENT_THRESHOLD_LOW);
                        if (closestSignalResult.direction != null && !movementModule.isMovementLocationRepetitive(currentLocation.add(closestSignalResult.direction), robotController)) {

                            robotController.move(closestSignalResult.direction);
                            currentLocation = robotController.getLocation();
                            movementModule.addMovementLocation(currentLocation, robotController);

                            robotController.setIndicatorString(0, "I am moving to a zombie den.");

                        }

                        if (desiredRubbleClearanceDirection != null) {

                            desiredRubbleClearanceDirection = closestSignalDirection;

                        }

                    }

                }

            }

            // try clear rubble

            if (robotController.isCoreReady()) {

                int rubbleAdjustmentThreshold = RubbleModule.ADJUSTMENT_THRESHOLD_MEDIUM;
                if (desiredRubbleClearanceDirection == null) {

                    desiredRubbleClearanceDirection = directionController.getRandomDirection();
                    rubbleAdjustmentThreshold = RubbleModule.ADJUSTMENT_THRESHOLD_ALL;

                }

                final Direction rubbleClearanceDirection = rubbleModule.getRubbleClearanceDirectionFromDirection(desiredRubbleClearanceDirection, robotController, rubbleAdjustmentThreshold);
                if (rubbleClearanceDirection != null) {

                    robotController.clearRubble(rubbleClearanceDirection);

                    robotController.setIndicatorString(0, "I cleared rubble " + rubbleClearanceDirection);

                }

            }*/

            if (robotController.getID() == 1607) {

                System.out.println("Bytecode check D: " + Clock.getBytecodeNum());

            }

            // finish up

            for (int i = 0; i < politicalAgenda.archonLocations.size(); i++) {

                final MapLocation archonLocation = politicalAgenda.archonLocations.get(i);
                robotController.setIndicatorLine(currentLocation, archonLocation, 136, 125, 255);

            }

            for (int i = 0; i < politicalAgenda.enemies.size(); i++) {

                final EnemyInfo enemy = politicalAgenda.enemies.get(i);
                robotController.setIndicatorLine(currentLocation, enemy.location, 255, 0, 208);

            }

            for (int i = 0; i < politicalAgenda.enemyArchons.size(); i++) {

                final InformationSignal signal = politicalAgenda.enemyArchons.get(i);
                robotController.setIndicatorLine(currentLocation, signal.location, 174, 0, 255);

            }

            for (int i = 0; i < politicalAgenda.zombieDens.size(); i++) {

                final InformationSignal signal = politicalAgenda.zombieDens.get(i);
                robotController.setIndicatorLine(currentLocation, signal.location, 0, 255, 0);

            }

            for (int i = 0; i < politicalAgenda.enemyClumps.size(); i++) {

                final ClumpInfo clumpInfo = politicalAgenda.enemyClumps.get(i);
                robotController.setIndicatorLine(currentLocation, clumpInfo.location, 255, 186, 186);

            }

            for (int i = 0; i < politicalAgenda.friendlyClumps.size(); i++) {

                final ClumpInfo clumpInfo = politicalAgenda.friendlyClumps.get(i);
                robotController.setIndicatorLine(currentLocation, clumpInfo.location, 186, 207, 255);

            }

            if (objectiveSignal != null) {

                robotController.setIndicatorLine(objectiveSignal.location, robotController.getLocation(), 255, 0, 0);

            }

            Clock.yield();

        }

    }

    /*
    COMBAT
     */

    private RobotInfo getBestEnemyToAttackFromEnemies(final RobotInfo[] enemies) {

        RobotInfo bestEnemy = null;
        for (int i = 0; i < enemies.length; i++) {

            final RobotInfo enemy = enemies[i];
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

    private boolean shouldKiteEnemy(final RobotInfo enemy) {

        return enemy.type == RobotType.BIGZOMBIE || enemy.type == RobotType.STANDARDZOMBIE;

    }

    private boolean shouldMoveTowardsEnemy(final RobotInfo enemy, final MapLocation currentLocation) {

        if (enemy.type == RobotType.TURRET || enemy.type == RobotType.ARCHON || enemy.type == RobotType.SCOUT) {

            return true;

        }
        if (enemy.type == RobotType.ZOMBIEDEN) {

            final int distance = currentLocation.distanceSquaredTo(enemy.location);
            if (distance > 8) {

                return true;

            }

        }
        return false;

    }

}
