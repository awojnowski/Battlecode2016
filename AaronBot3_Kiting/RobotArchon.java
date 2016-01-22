package AaronBot3_Kiting;

import AaronBot3_Kiting.Combat.CombatModule;
import AaronBot3_Kiting.Map.*;
import AaronBot3_Kiting.Movement.*;
import AaronBot3_Kiting.Rubble.RubbleModule;
import AaronBot3_Kiting.Signals.*;
import AaronBot3_Kiting.ZombieSpawns.ZombieSpawnsModule;
import battlecode.common.*;
import java.util.*;

public class RobotArchon implements Robot {

    private static int InitialMessageUpdateLength = 2;

    public void run(final RobotController robotController) throws GameActionException {

        // modules

        final MapInfoModule mapInfoModule = new MapInfoModule();
        mapInfoModule.detectWhetherToThrowGame(robotController);

        final CommunicationModule communicationModule = new CommunicationModule(mapInfoModule);
        final MovementModule movementModule = new MovementModule();
        final Random random = new Random(robotController.getID());
        final RubbleModule rubbleModule = new RubbleModule();

        // unit building

        int scoutsBuilt = 0;
        int soldiersBuilt = 0;
        RobotType buildingUnitType = null;
        CommunicationModuleSignalCollection buildingUpdateSignalCollection = null;

        // general

        final Team currentTeam = robotController.getTeam();

        ZombieSpawnsModule.setSpawnScheduleIfNeeded(robotController);

        // loop

        while (true) {

            communicationModule.processIncomingSignals(robotController);

            // firstly check if we are throwing this game or not

            if (mapInfoModule.shouldThrowGame) {

                final int throwRound = robotController.getID() % 200;
                if (robotController.getRoundNum() == throwRound) {

                    robotController.senseRobotAtLocation(robotController.getLocation().add(Direction.NORTH, 10));

                }

            }

            // we should try activate robots

            if (robotController.isCoreReady()) {

                final RobotInfo[] neutrals = robotController.senseNearbyRobots(GameConstants.ARCHON_ACTIVATION_RANGE, Team.NEUTRAL);
                for (int i = 0; i < neutrals.length; i++) {

                    robotController.activate(neutrals[i].location);
                    buildingUpdateSignalCollection = communicationModule.allCommunicationModuleSignals();
                    break;

                }

            }

            // check if we are done building a unit

            if (robotController.isCoreReady()) {

                if (buildingUnitType != null) {

                    buildingUpdateSignalCollection = communicationModule.allCommunicationModuleSignals();

                }
                buildingUnitType = null;

            }

            // let's broadcast any remaining information that we have to the nearby, recently created units

            if (buildingUpdateSignalCollection != null) {

                boolean signalsSendingDone = true;
                int totalSignalsSent = 0;
                while (buildingUpdateSignalCollection.hasMoreElements()) {

                    if (totalSignalsSent >= GameConstants.MESSAGE_SIGNALS_PER_TURN - 1) {

                        signalsSendingDone = false;
                        break;

                    }

                    final CommunicationModuleSignal communicationModuleSignal = buildingUpdateSignalCollection.nextElement();
                    if (!CommunicationRelayModule.shouldRelaySignalTypeToRobotType(communicationModuleSignal.type, buildingUnitType)) {

                        continue;

                    }

                    communicationModule.broadcastSignal(communicationModuleSignal, robotController, RobotArchon.InitialMessageUpdateLength);
                    totalSignalsSent ++;

                }
                if (signalsSendingDone) {

                    final CommunicationModuleSignal signal = new CommunicationModuleSignal();
                    signal.action = CommunicationModuleSignal.ACTION_INITIAL_UPDATE_COMPLETE;
                    signal.location = robotController.getLocation();
                    signal.data = robotController.getID();
                    signal.type = CommunicationModuleSignal.TYPE_NONE;
                    communicationModule.broadcastSignal(signal, robotController, RobotArchon.InitialMessageUpdateLength);

                    buildingUpdateSignalCollection = null;

                }

            }

            // check to make sure we are safe

            MapLocation currentLocation = robotController.getLocation();
            final RobotInfo[] enemies = robotController.senseHostileRobots(currentLocation, robotController.getType().sensorRadiusSquared);

            final DirectionController directionController = new DirectionController(robotController);
            directionController.currentLocation = currentLocation;
            directionController.enemyBufferDistance = 2;
            directionController.nearbyEnemies = enemies;
            directionController.random = random;
            directionController.shouldAvoidEnemies = true;

            if (robotController.isCoreReady() && enemies.length > 0) {

                final Direction enemiesDirection = directionController.getAverageDirectionTowardsEnemies(enemies, true);
                if (enemiesDirection != null) {

                    directionController.shouldAvoidEnemies = false;
                    final DirectionController.Result enemiesMovementResult = directionController.getDirectionResultFromDirection(enemiesDirection.opposite(), DirectionController.ADJUSTMENT_THRESHOLD_MEDIUM);
                    directionController.shouldAvoidEnemies = true;

                    if (enemiesMovementResult.direction != null) {

                        robotController.move(enemiesMovementResult.direction);
                        currentLocation = robotController.getLocation();

                    }

                }

            }

            // let's check up on existing communications to verify the information, if we can

            final ArrayList<CommunicationModuleSignal> verifiedSignals = communicationModule.verifyCommunicationsInformation(robotController, enemies, true);
            for (int i = 0; i < verifiedSignals.size(); i++) {

                communicationModule.enqueueSignalForBroadcast(verifiedSignals.get(i));

            }

            if (communicationModule.hasEnqueuedSignalsForBroadcast()) {

                communicationModule.broadcastEnqueuedSignals(robotController, CommunicationModule.maximumBroadcastRange(mapInfoModule, currentLocation));

            }

            // attempt to build new units

            if (robotController.isCoreReady()) {

                RobotType typeToBuild = RobotType.SOLDIER;
                if (scoutsBuilt == 0 || (int)Math.pow(scoutsBuilt + 1, 3) < soldiersBuilt) {

                    typeToBuild = RobotType.SCOUT;

                }
                if (robotController.getTeamParts() >= typeToBuild.partCost) {

                    for (int i = 0; i < DirectionController.DIRECTIONS.length; i++) {

                        if (robotController.canBuild(DirectionController.DIRECTIONS[i], typeToBuild)) {

                            buildingUnitType = typeToBuild;
                            if (typeToBuild == RobotType.SCOUT) {

                                scoutsBuilt ++;

                            }
                            if (typeToBuild == RobotType.SOLDIER) {

                                soldiersBuilt ++;

                            }
                            robotController.build(DirectionController.DIRECTIONS[i], typeToBuild);
                            break;

                        }

                    }

                }

            }

            // try to move toward neutral robots

            Direction targetRubbleClearanceDirection = null;
            if (robotController.isCoreReady()) {

                MapLocation nearestNeutralLocation = null;

                final RobotInfo[] neutrals = robotController.senseNearbyRobots(robotController.getType().sensorRadiusSquared, Team.NEUTRAL);
                if (neutrals.length > 0) {

                    double nearestNeutralRanking = 0;

                    for (int i = 0; i < neutrals.length; i++) {

                        final RobotInfo neutralRobot = neutrals[i];
                        final MapLocation neutralLocation = neutralRobot.location;
                        final double neutralValue = neutralRobot.maxHealth * neutralRobot.type.attackRadiusSquared;
                        final int distance = neutralLocation.distanceSquaredTo(currentLocation);

                        final double ranking = neutralValue / distance;
                        if (ranking > nearestNeutralRanking) {

                            nearestNeutralLocation = neutralLocation;
                            nearestNeutralRanking = ranking;

                        }

                    }

                }
                if (nearestNeutralLocation != null) {

                    final Direction nearestNeutralDirection = currentLocation.directionTo(nearestNeutralLocation);
                    final DirectionController.Result nearestNeutralMovementResult = directionController.getDirectionResultFromDirection(nearestNeutralDirection, DirectionController.ADJUSTMENT_THRESHOLD_MEDIUM);
                    if (nearestNeutralMovementResult.direction != null && !movementModule.isMovementLocationRepetitive(currentLocation.add(nearestNeutralMovementResult.direction), robotController)) {

                        robotController.move(nearestNeutralMovementResult.direction);
                        currentLocation = robotController.getLocation();
                        movementModule.addMovementLocation(currentLocation, robotController);

                    } else {

                        targetRubbleClearanceDirection = nearestNeutralDirection;

                    }

                }

            }

            // try to move toward some spare parts

            if (robotController.isCoreReady()) {

                MapLocation nearestPartsLocation = null;

                final MapLocation[] partsLocations = robotController.sensePartLocations(-1);
                if (partsLocations.length > 0) {

                    double nearestPartsRanking = 0;

                    for (int i = 0; i < partsLocations.length; i++) {

                        final MapLocation partsLocation = partsLocations[i];
                        final double partsTotal = robotController.senseParts(partsLocation);
                        final double rubbleTotal = Math.max(1, robotController.senseRubble(partsLocation));
                        final int distance = partsLocation.distanceSquaredTo(currentLocation);

                        final double ranking = partsTotal / Math.sqrt(rubbleTotal) / distance;
                        if (ranking > nearestPartsRanking) {

                            nearestPartsLocation = partsLocation;
                            nearestPartsRanking = ranking;

                        }

                    }

                }
                if (nearestPartsLocation != null) {

                    final Direction nearestPartsDirection = currentLocation.directionTo(nearestPartsLocation);
                    final DirectionController.Result nearestPartsMovementResult = directionController.getDirectionResultFromDirection(nearestPartsDirection, DirectionController.ADJUSTMENT_THRESHOLD_MEDIUM);
                    if (nearestPartsMovementResult.direction != null && !movementModule.isMovementLocationRepetitive(currentLocation.add(nearestPartsMovementResult.direction), robotController)) {

                        robotController.move(nearestPartsMovementResult.direction);
                        currentLocation = robotController.getLocation();
                        movementModule.addMovementLocation(currentLocation, robotController);

                    } else {

                        targetRubbleClearanceDirection = nearestPartsDirection;

                    }

                }

            }

            // we can try clear rubble if we didn't move

            if (robotController.isCoreReady()) {

                if (targetRubbleClearanceDirection != null) {

                    final Direction rubbleClearanceDirection = rubbleModule.getRubbleClearanceDirectionFromDirection(targetRubbleClearanceDirection, robotController, RubbleModule.ADJUSTMENT_THRESHOLD_LOW);
                    if (rubbleClearanceDirection != null) {

                        robotController.clearRubble(rubbleClearanceDirection);
                        movementModule.extendLocationInvalidationTurn(robotController);

                    }

                }

            }

            // otherwise, try to follow the soldier clump

            if (robotController.isCoreReady()) {

                Direction nearestFriendliesDirection = null;

                RobotInfo[] closeAllies = robotController.senseNearbyRobots(3, currentTeam); // How close they stay to their team, lower means they'll stay closer
                RobotInfo[] closeSoldiers = CombatModule.robotsOfTypesFromRobots(closeAllies, new RobotType[]{RobotType.SOLDIER, RobotType.GUARD});

                if (closeSoldiers.length < 2) { // Move towards team if far away

                    final RobotInfo[] nearbyFriendlies = robotController.senseNearbyRobots(-1, currentTeam);
                    if (nearbyFriendlies.length > 0) {

                        nearestFriendliesDirection = directionController.getAverageDirectionTowardFriendlies(nearbyFriendlies, false);

                    }

                }

                if (nearestFriendliesDirection != null) {

                    final DirectionController.Result nearestFriendliesMovementResult = directionController.getDirectionResultFromDirection(nearestFriendliesDirection, DirectionController.ADJUSTMENT_THRESHOLD_MEDIUM);
                    if (nearestFriendliesMovementResult.direction != null && !movementModule.isMovementLocationRepetitive(currentLocation.add(nearestFriendliesMovementResult.direction), robotController)) {

                        robotController.move(nearestFriendliesMovementResult.direction);
                        currentLocation = robotController.getLocation();
                        movementModule.addMovementLocation(currentLocation, robotController);

                    } else {

                        targetRubbleClearanceDirection = nearestFriendliesDirection;

                    }

                }

            }

            // try to repair any units nearby

            RobotInfo injuredUnit = null;
            double injuredUnitHealth = Integer.MAX_VALUE;
            final RobotInfo[] friendlyRepairableUnits = robotController.senseNearbyRobots(robotController.getType().attackRadiusSquared, currentTeam);
            for (int i = 0; i < friendlyRepairableUnits.length; i++) {

                final RobotInfo friendly = friendlyRepairableUnits[i];
                if (friendly.type == RobotType.ARCHON) {

                    continue;

                }
                if (friendly.health < injuredUnitHealth && friendly.health < friendly.maxHealth) {

                    injuredUnit = friendly;
                    injuredUnitHealth = friendly.health;

                }

            }
            if (injuredUnit != null) {

                robotController.repair(injuredUnit.location);

            }

            // show what we know

            final CommunicationModuleSignalCollection communicationModuleSignalCollection = communicationModule.allCommunicationModuleSignals();
            final MapLocation location = robotController.getLocation();
            while (communicationModuleSignalCollection.hasMoreElements()) {

                final CommunicationModuleSignal communicationModuleSignal = communicationModuleSignalCollection.nextElement();
                int[] color = new int[]{255, 255, 255};
                if (communicationModuleSignal.type == CommunicationModuleSignal.TYPE_ZOMBIEDEN) {

                    color = new int[]{50, 255, 50};

                } else if (communicationModuleSignal.type == CommunicationModuleSignal.TYPE_ENEMY_ARCHON) {

                    color = new int[]{255, 0, 0};

                } else if (communicationModuleSignal.type == CommunicationModuleSignal.TYPE_ENEMY_TURRET) {

                    color = new int[]{255, 50, 100};

                } else {

                    continue;

                }
                robotController.setIndicatorLine(location, communicationModuleSignal.location, color[0], color[1], color[2]);

            }

            Clock.yield();

        }

    }

}
