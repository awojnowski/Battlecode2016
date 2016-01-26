package team059;

import team059.Cartography.*;
import team059.Combat.*;
import team059.Information.*;
import team059.Movement.*;
import battlecode.common.*;
import java.util.*;

public class RobotScout implements Robot {

    enum Mode {
        UKNNOWN,
        SCOUT,
        TURRET_ACQUIRE,
        TURRET_COMPANION
    }

    public void run(final RobotController robotController) throws GameActionException {

        robotController.emptySignalQueue();

        final CartographyModule cartographyModule = new CartographyModule();
        final CombatModule combatModule = new CombatModule();
        final PoliticalAgenda politicalAgenda = new PoliticalAgenda();
        final MovementModule movementModule = new MovementModule();
        final Random random = new Random(robotController.getID());

        final MapLocation spawnLocation = robotController.getLocation();
        final Team team = robotController.getTeam();

        Direction movementDirection = null;

        int currentCompanionIdentifier = PoliticalAgenda.UnknownValue;
        Mode currentMode = Mode.UKNNOWN;

        int turnsSinceLastEnemyClumpSignal = 0;
        int turnsSinceLastFriendlyClumpSignal = 0;

        politicalAgenda.determineMapMirroring(robotController);

        while (true) {

            // process incoming communications

            politicalAgenda.processIncomingSignalsFromRobotController(robotController);
            if (!politicalAgenda.isInformationSynced) {

                Clock.yield();
                continue;

            }
            if (currentMode == Mode.UKNNOWN) {

                if (politicalAgenda.startupData > 0) {

                    currentMode = Mode.TURRET_ACQUIRE;

                } else {

                    currentMode = Mode.SCOUT;

                }

            }

            // continue forward

            turnsSinceLastEnemyClumpSignal ++;
            turnsSinceLastFriendlyClumpSignal ++;

            MapLocation currentLocation = robotController.getLocation();
            final RobotInfo[] enemies = robotController.senseHostileRobots(currentLocation, robotController.getType().sensorRadiusSquared);
            final RobotInfo[] friendlies = robotController.senseNearbyRobots(-1, robotController.getTeam());

            boolean areFriendliesNearby = false;
            boolean foundTurretCompanion = false;
            for (int i = 0; i < friendlies.length; i++) {

                final RobotInfo friendly = friendlies[i];
                if (friendly.type != RobotType.SCOUT) {

                    areFriendliesNearby = true;

                }
                if (currentMode == Mode.TURRET_ACQUIRE) {

                    if (friendly.type == RobotType.TURRET || friendly.type == RobotType.TTM) {

                        if (!this.checkRobotForExistingCompanion(friendly, robotController)) {

                            currentCompanionIdentifier = friendly.ID;
                            currentMode = Mode.TURRET_COMPANION;
                            foundTurretCompanion = true;

                        }

                    }

                } else if (currentMode == Mode.TURRET_COMPANION) {

                    if (friendly.ID == currentCompanionIdentifier) {

                        foundTurretCompanion = true;

                    }

                }

            }
            if (currentMode == Mode.TURRET_COMPANION && !foundTurretCompanion) {

                currentCompanionIdentifier = PoliticalAgenda.UnknownValue;
                currentMode = Mode.TURRET_ACQUIRE;

            }

            robotController.setIndicatorString(0, "Map information: N: " + politicalAgenda.mapBoundaryNorth + " E: " + politicalAgenda.mapBoundaryEast + " S: " + politicalAgenda.mapBoundarySouth + " W: " + politicalAgenda.mapBoundaryWest + " SD: " + politicalAgenda.maximumBroadcastRangeForLocation(currentLocation));
            robotController.setIndicatorString(1, "Companion ID: " + currentCompanionIdentifier + " Mode: " + currentMode);

            // let's verify information

            for (int i = 0; i < politicalAgenda.enemyArchons.size(); i++) {

                final InformationSignal signal = politicalAgenda.enemyArchons.get(i);
                if (!politicalAgenda.verifyEnemyArchonSignal(signal, robotController, enemies)) {

                    i--;

                    signal.action = PoliticalAgenda.SignalActionErase;
                    signal.broadcastRange = politicalAgenda.maximumBroadcastRangeForLocation(currentLocation);
                    politicalAgenda.enqueueSignalForBroadcast(signal, robotController);

                }

            }

            int zombieDenCount = politicalAgenda.zombieDens.size();
            for (int i = 0; i < zombieDenCount; i++) {

                final InformationSignal signal = politicalAgenda.zombieDens.get(i);
                if (!politicalAgenda.verifyZombieDenSignal(signal, robotController)) {

                    signal.action = PoliticalAgenda.SignalActionErase;
                    signal.broadcastRange = politicalAgenda.maximumBroadcastRangeForLocation(currentLocation);
                    politicalAgenda.enqueueSignalForBroadcast(signal, robotController);

                    zombieDenCount --;
                    i--;

                }

            }

            // let's try identify what we can see

            final int maximumBroadcastRange = politicalAgenda.maximumBroadcastRangeForLocation(currentLocation);

            for (int i = 0; i < enemies.length; i++) {

                final RobotInfo enemy = enemies[i];
                if (enemy.type == RobotType.ZOMBIEDEN) {

                    if (politicalAgenda.zombieDens.contains(politicalAgenda.getIndexIdentifierForZombieDen(enemy.location))) {

                        continue;

                    }

                    final InformationSignal signal = politicalAgenda.generateZombieDenInformationSignal(enemy.location);
                    signal.broadcastRange = maximumBroadcastRange;
                    politicalAgenda.enqueueSignalForBroadcast(signal, robotController);

                    final InformationSignal mirroredSignal = politicalAgenda.generateZombieDenInformationSignal(politicalAgenda.getMirroredLocationFromLocation(enemy.location));
                    mirroredSignal.broadcastRange = maximumBroadcastRange;
                    politicalAgenda.enqueueSignalForBroadcast(mirroredSignal, robotController);

                } else {

                    if (currentMode == Mode.TURRET_COMPANION) {

                        // broadcast seen enemy information to friendlies

                        final InformationSignal signal = politicalAgenda.generateEnemyInformationSignal(enemy.location, enemy.type, (int)enemy.health, enemy.ID);
                        signal.broadcastRange = 3;
                        politicalAgenda.enqueueSignalForBroadcast(signal, robotController);

                    }

                    if (enemy.type == RobotType.ARCHON) {

                        final InformationSignal nearbySignal = politicalAgenda.generateEnemyArchonInformationSignal(enemy.location, enemy.ID);
                        nearbySignal.broadcastRange = 750;
                        politicalAgenda.enqueueSignalForBroadcast(nearbySignal, robotController);

                        if (politicalAgenda.enemyArchons.contains(enemy.ID)) {

                            continue;

                        }

                        final InformationSignal signal = politicalAgenda.generateEnemyArchonInformationSignal(enemy.location, enemy.ID);
                        signal.broadcastRange = politicalAgenda.maximumBroadcastRangeForLocation(currentLocation);
                        politicalAgenda.enqueueSignalForBroadcast(signal, robotController);

                    }

                }

            }

            if (turnsSinceLastEnemyClumpSignal > 20) {

                final int requiredEnemies = Math.max(1, 6 - (int)Math.floor(((turnsSinceLastEnemyClumpSignal - 20) / 30)));
                if (enemies.length > requiredEnemies) {

                    int totalX = 0;
                    int totalY = 0;
                    for (int i = 0; i < enemies.length; i++) {

                        totalX += enemies[i].location.x;
                        totalY += enemies[i].location.y;

                    }
                    int locationX = totalX / enemies.length;
                    int locationY = totalY / enemies.length;

                    final InformationSignal signal = politicalAgenda.generateEnemyClumpInformationSignal(new MapLocation(locationX, locationY));
                    signal.broadcastRange = maximumBroadcastRange;
                    politicalAgenda.enqueueSignalForBroadcast(signal, robotController);

                    turnsSinceLastEnemyClumpSignal = 0;

                }

            }

            if (turnsSinceLastFriendlyClumpSignal > 20) {

                if (friendlies.length > 15) {

                    int totalX = 0;
                    int totalY = 0;
                    for (int i = 0; i < friendlies.length; i++) {

                        totalX += friendlies[i].location.x;
                        totalY += friendlies[i].location.y;

                    }
                    int locationX = totalX / friendlies.length;
                    int locationY = totalY / friendlies.length;

                    final InformationSignal signal = politicalAgenda.generateFriendlyClumpInformationSignal(new MapLocation(locationX, locationY));
                    signal.broadcastRange = maximumBroadcastRange;
                    politicalAgenda.enqueueSignalForBroadcast(signal, robotController);

                    turnsSinceLastFriendlyClumpSignal = 0;

                }

            }

            if (!politicalAgenda.hasAllMapBoundaries()) {

                boolean hasEast = politicalAgenda.mapBoundaryEast != PoliticalAgenda.UnknownValue;
                boolean hasNorth = politicalAgenda.mapBoundaryNorth != PoliticalAgenda.UnknownValue;
                boolean hasWest = politicalAgenda.mapBoundaryWest != PoliticalAgenda.UnknownValue;
                boolean hasSouth = politicalAgenda.mapBoundarySouth != PoliticalAgenda.UnknownValue;

                cartographyModule.probeAndUpdatePoliticalAgenda(politicalAgenda, currentLocation, robotController);
                if (politicalAgenda.hasAllMapBoundaries()) {

                    final InformationSignal signal = politicalAgenda.generateMapInfoInformationSignal();
                    signal.broadcastRange = maximumBroadcastRange;
                    politicalAgenda.enqueueSignalForBroadcast(signal, robotController);

                } else {

                    if (!hasEast && politicalAgenda.mapBoundaryEast != PoliticalAgenda.UnknownValue) {

                        final InformationSignal signal = politicalAgenda.generateMapWallInformationSignal(PoliticalAgenda.SignalTypeMapWallEast, politicalAgenda.mapBoundaryEast);
                        signal.broadcastRange = maximumBroadcastRange;
                        politicalAgenda.enqueueSignalForBroadcast(signal, robotController);

                        if (politicalAgenda.mapBoundaryWest == PoliticalAgenda.UnknownValue) {

                            final InformationSignal mirroredSignal = politicalAgenda.getMirroredBoundarySignal(signal);
                            if (mirroredSignal != null) {

                                mirroredSignal.broadcastRange = maximumBroadcastRange;
                                politicalAgenda.mapBoundaryWest = mirroredSignal.data;
                                politicalAgenda.enqueueSignalForBroadcast(mirroredSignal, robotController);
                                hasWest = true;

                            }

                        }

                    }
                    if (!hasNorth && politicalAgenda.mapBoundaryNorth != PoliticalAgenda.UnknownValue) {

                        final InformationSignal signal = politicalAgenda.generateMapWallInformationSignal(PoliticalAgenda.SignalTypeMapWallNorth, politicalAgenda.mapBoundaryNorth);
                        signal.broadcastRange = maximumBroadcastRange;
                        politicalAgenda.enqueueSignalForBroadcast(signal, robotController);

                        if (politicalAgenda.mapBoundarySouth == PoliticalAgenda.UnknownValue) {

                            final InformationSignal mirroredSignal = politicalAgenda.getMirroredBoundarySignal(signal);
                            if (mirroredSignal != null) {

                                mirroredSignal.broadcastRange = maximumBroadcastRange;
                                politicalAgenda.mapBoundarySouth = mirroredSignal.data;
                                politicalAgenda.enqueueSignalForBroadcast(mirroredSignal, robotController);
                                hasSouth = true;

                            }

                        }

                    }
                    if (!hasWest && politicalAgenda.mapBoundaryWest != PoliticalAgenda.UnknownValue) {

                        final InformationSignal signal = politicalAgenda.generateMapWallInformationSignal(PoliticalAgenda.SignalTypeMapWallWest, politicalAgenda.mapBoundaryWest);
                        signal.broadcastRange = maximumBroadcastRange;
                        politicalAgenda.enqueueSignalForBroadcast(signal, robotController);

                        if (politicalAgenda.mapBoundaryEast == PoliticalAgenda.UnknownValue) {

                            final InformationSignal mirroredSignal = politicalAgenda.getMirroredBoundarySignal(signal);
                            if (mirroredSignal != null) {

                                mirroredSignal.broadcastRange = maximumBroadcastRange;
                                politicalAgenda.mapBoundaryEast = mirroredSignal.data;
                                politicalAgenda.enqueueSignalForBroadcast(mirroredSignal, robotController);
                                hasEast = true;

                            }

                        }

                    }
                    if (!hasSouth && politicalAgenda.mapBoundarySouth != PoliticalAgenda.UnknownValue) {

                        final InformationSignal signal = politicalAgenda.generateMapWallInformationSignal(PoliticalAgenda.SignalTypeMapWallSouth, politicalAgenda.mapBoundarySouth);
                        signal.broadcastRange = maximumBroadcastRange;
                        politicalAgenda.enqueueSignalForBroadcast(signal, robotController);

                        if (politicalAgenda.mapBoundaryNorth == PoliticalAgenda.UnknownValue) {

                            final InformationSignal mirroredSignal = politicalAgenda.getMirroredBoundarySignal(signal);
                            if (mirroredSignal != null) {

                                mirroredSignal.broadcastRange = maximumBroadcastRange;
                                politicalAgenda.mapBoundaryNorth = mirroredSignal.data;
                                politicalAgenda.enqueueSignalForBroadcast(mirroredSignal, robotController);
                                hasNorth = true;

                            }

                        }

                    }

                }

            }

            if (currentMode == Mode.TURRET_COMPANION) {

                if (robotController.isCoreReady()) {

                    final RobotInfo companion = robotController.senseRobot(currentCompanionIdentifier);
                    if (companion != null) {

                        final int distance = currentLocation.distanceSquaredTo(companion.location);
                        if (distance > 2) {

                            final DirectionController directionController = new DirectionController(robotController);
                            directionController.currentLocation = currentLocation;
                            directionController.random = random;

                            final Direction companionDirection = currentLocation.directionTo(companion.location);
                            if (companionDirection != null) {

                                final DirectionController.Result companionDirectionResult = directionController.getDirectionResultFromDirection(companionDirection, DirectionController.ADJUSTMENT_THRESHOLD_MEDIUM);
                                if (companionDirectionResult.direction != null) {

                                    robotController.move(companionDirectionResult.direction);

                                }

                            }

                        }

                    }

                }

            } else {

                // let's try to flee if we aren't safe

                final DirectionController directionController = new DirectionController(robotController);
                directionController.currentLocation = currentLocation;
                directionController.enemyBufferDistance = 2;
                directionController.nearbyEnemies = enemies;
                directionController.random = random;
                directionController.shouldAvoidEnemies = true;

                final Direction enemiesDirectionOutput = directionController.getAverageDirectionTowardsEnemies(enemies, true, false, false);
                if (enemiesDirectionOutput != null) {

                    robotController.setIndicatorLine(currentLocation, currentLocation.add(enemiesDirectionOutput, 1000), 50, 25, 25);

                }

                if (robotController.isCoreReady() && enemies.length > 0) {

                    final Direction enemiesDirection = directionController.getAverageDirectionTowardsEnemies(enemies, true, false, false);
                    if (enemiesDirection != null) {

                        directionController.shouldAvoidEnemies = false;
                        final DirectionController.Result enemiesMovementResult = directionController.getDirectionResultFromDirection(enemiesDirection.opposite(), DirectionController.ADJUSTMENT_THRESHOLD_MEDIUM);
                        directionController.shouldAvoidEnemies = true;

                        if (enemiesMovementResult.direction != null) {

                            robotController.move(enemiesMovementResult.direction);
                            currentLocation = robotController.getLocation();

                            if (movementDirection != null) {

                                movementDirection = RobotScout.rotateDirection(movementDirection, currentLocation, robotController);

                            }

                        }

                    }

                }

                // now let's try move to see more

                if (robotController.isCoreReady()) {

                    int nearestEnemyArchonDistance = 0;
                    MapLocation nearestEnemyArchonLocation = null;

                    for (int i = 0; i < enemies.length; i++) {

                        final RobotInfo enemy = enemies[i];
                        if (enemy.type != RobotType.ARCHON) {

                            continue;

                        }
                        final int distance = currentLocation.distanceSquaredTo(enemy.location);
                        if (distance < nearestEnemyArchonDistance) {

                            nearestEnemyArchonDistance = distance;
                            nearestEnemyArchonLocation = enemy.location;

                        }

                    }

                    if (nearestEnemyArchonLocation != null && robotController.getRoundNum() > 1000) {

                        final Direction nearestEnemyArchonDirection = currentLocation.directionTo(nearestEnemyArchonLocation);
                        final DirectionController.Result movementResult = directionController.getDirectionResultFromDirection(nearestEnemyArchonDirection, DirectionController.ADJUSTMENT_THRESHOLD_MEDIUM);
                        if (movementResult.direction != null) {

                            robotController.move(movementResult.direction);
                            currentLocation = robotController.getLocation();

                        }

                    } else {

                        if (movementDirection == null) {

                            movementDirection = directionController.getRandomDirection();

                        }

                        final MapLocation movementLocation = currentLocation.add(movementDirection);

                        // let's see if we have a movement direction before moving (if not, create one)
                        if (movementDirection == null) {

                            movementDirection = directionController.getRandomDirection();

                        } else if (!robotController.onTheMap(movementLocation)) {

                            movementDirection = RobotScout.rotateDirection(movementDirection, currentLocation, robotController);

                        } else if (robotController.getRoundNum() < 300 && combatModule.isLocationOnOurSide(robotController, currentLocation) && !combatModule.isLocationOnOurSide(robotController, movementLocation)) {

                            movementDirection = RobotScout.rotateDirection(movementDirection, currentLocation, robotController);

                        }

                        final DirectionController.Result movementResult = directionController.getDirectionResultFromDirection(movementDirection, DirectionController.ADJUSTMENT_THRESHOLD_MEDIUM);
                        if (movementResult.direction != null) {

                            robotController.move(movementResult.direction);
                            currentLocation = robotController.getLocation();

                        } else {

                            movementDirection = RobotScout.rotateDirection(movementDirection, currentLocation, robotController);

                        }

                    }

                }

                if (movementDirection != null) {

                    robotController.setIndicatorLine(currentLocation, currentLocation.add(movementDirection, 10000), 255, 255, 255);

                }

            }

            // now let's see if we should send our signals

            if (politicalAgenda.hasEnqueuedSignalsForBroadcast()) {

                politicalAgenda.broadcastEnqueuedSignals(robotController);

            }

            // show what we know

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

            Clock.yield();

        }

    }

    private static Direction rotateDirection(final Direction direction, final MapLocation location, final RobotController robotController) {

        if (robotController.getID() % 2 == 0) {

            return direction.rotateLeft().rotateLeft().rotateLeft();

        } else {

            return direction.rotateRight().rotateRight().rotateRight();

        }

    }

    /*
    COMPANION
     */

    private boolean checkRobotForExistingCompanion(final RobotInfo robot, final RobotController robotController) throws GameActionException {

        final MapLocation turretLocation = robot.location;
        for (int i = -1; i <= 1; i++) {

            for (int j = -1; j <= 1; j++) {

                if (i == 0 && j == 0) {

                    continue;

                }
                final MapLocation turretCheckLocation = new MapLocation(turretLocation.x + i, turretLocation.y + j);
                if (!robotController.canSenseLocation(turretCheckLocation)) {

                    continue;

                }
                if (!robotController.onTheMap(turretCheckLocation)) {

                    continue;

                }
                final RobotInfo possibleOtherTurretCompanion = robotController.senseRobotAtLocation(turretCheckLocation);
                if (possibleOtherTurretCompanion != null) {

                    if (possibleOtherTurretCompanion.type == RobotType.SCOUT) {

                        return true;

                    }

                }

            }

        }
        return false;

    }

}
