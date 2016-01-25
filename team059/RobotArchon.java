package team059;

import team059.Combat.*;
import team059.Information.*;
import team059.Movement.*;
import team059.Rubble.*;
import team059.ZombieSpawns.*;
import battlecode.common.*;
import java.util.*;

public class RobotArchon implements Robot {

    private static int InitialMessageUpdateLength = 3;

    public void run(final RobotController robotController) throws GameActionException {

        // modules

        final MovementModule movementModule = new MovementModule();
        final PoliticalAgenda politicalAgenda = new PoliticalAgenda();
        final Random random = new Random(robotController.getID());
        final RubbleModule rubbleModule = new RubbleModule();

        // unit building

        int scoutsBuilt = 0;
        int soldiersBuilt = 0;
        int turretsBuilt = 0;
        int vipersBuilt = 0;
        RobotType buildingUnitType = null;
        RobotType lastBuiltUnitType = null;
        boolean isNextUnitACompanion = false;

        boolean relayInformation = false;
        boolean sendArchonUpdate = false;
        int relayInformationDelay = 0;
        ArrayList<InformationSignal> informationRelaySignals = null;

        RobotInfo lastRepairedRobot = null;

        // general

        final Team currentTeam = robotController.getTeam();

        ZombieSpawnsModule.setSpawnScheduleIfNeeded(robotController);

        // loop

        while (true) {

            robotController.setIndicatorString(0, "");
            robotController.setIndicatorString(1, "");

            // update our signals

            politicalAgenda.processIncomingSignalsFromRobotController(robotController);

            // check relay information

            if (relayInformation) {

                relayInformationDelay--;

            }
            if (!relayInformation) {

                // we should try activate robots

                if (robotController.isCoreReady()) {

                    final RobotInfo[] neutrals = robotController.senseNearbyRobots(GameConstants.ARCHON_ACTIVATION_RANGE, Team.NEUTRAL);
                    for (int i = 0; i < neutrals.length; i++) {

                        final RobotInfo neutral = neutrals[i];

                        lastBuiltUnitType = neutral.type;
                        robotController.activate(neutral.location);
                        relayInformation = true;
                        relayInformationDelay = 2;
                        break;

                    }

                }

                // check if we are done building a unit

                if (robotController.isCoreReady()) {

                    if (buildingUnitType != null) {

                        relayInformation = true;
                        relayInformationDelay = 2;

                    }
                    buildingUnitType = null;

                }

            }
            if (relayInformationDelay > 0) {

                Clock.yield();
                continue;

            }

            // let's broadcast any remaining information that we have to the nearby, recently created units

            if (relayInformation) {

                informationRelaySignals = this.generateSignalRelayList(politicalAgenda);
                relayInformation = false;
                relayInformationDelay = 0;

            }

            if (informationRelaySignals != null) {

                for (int i = 0; i < informationRelaySignals.size(); i++) {

                    if (robotController.getMessageSignalCount() >= GameConstants.MESSAGE_SIGNALS_PER_TURN - 1) {

                        Clock.yield();

                    }

                    final InformationSignal signal = informationRelaySignals.get(i);
                    if (!politicalAgenda.shouldRobotTypeProcessSignalType(buildingUnitType, signal.type)) {

                        continue;

                    }
                    signal.broadcastRange = RobotArchon.InitialMessageUpdateLength;
                    politicalAgenda.broadcastSignal(signal, robotController);

                }

                final InformationSignal signal = new InformationSignal();
                signal.action = PoliticalAgenda.SignalActionWrite;
                signal.broadcastRange = RobotArchon.InitialMessageUpdateLength;
                signal.type = PoliticalAgenda.SignalTypeInformationSynced;
                if (isNextUnitACompanion) {

                    signal.data = 420;

                }
                politicalAgenda.broadcastSignal(signal, robotController);

                informationRelaySignals = null;

            }

            // send an archon update if necessary

            if (robotController.getRoundNum() > 300 && robotController.getRoundNum() % PoliticalAgenda.ArchonUpdateModulus == 0) {

                sendArchonUpdate = true;

            }

            // setup action constants

            MapLocation currentLocation = robotController.getLocation();
            final RobotInfo[] enemies = robotController.senseHostileRobots(currentLocation, robotController.getType().sensorRadiusSquared);
            final RobotInfo[] friendlies = robotController.senseNearbyRobots(robotController.getType().sensorRadiusSquared, robotController.getTeam());
            final RobotInfo[] dangerousEnemies = CombatModule.robotsExcludingTypesFromRobots(enemies, new RobotType[] {RobotType.ZOMBIEDEN, RobotType.ARCHON, RobotType.SCOUT});

            final DirectionController directionController = new DirectionController(robotController);
            directionController.currentLocation = currentLocation;
            directionController.enemyBufferDistance = 2;
            directionController.nearbyEnemies = enemies;
            directionController.random = random;
            directionController.shouldAvoidEnemies = true;

            boolean inDanger = false;
            robotController.setIndicatorString(2, "Not in danger");
            if (dangerousEnemies.length > friendlies.length) {

                // either we are outnumbered

                inDanger = true;
                robotController.setIndicatorString(2, "In danger (outnumbered)");

            } else {

                // or we are in attack range

                for (int i = 0; i < dangerousEnemies.length; i++) {

                    final int attackRadiusSquared = directionController.attackRadiusSquaredWithBuffer(dangerousEnemies[i].type.attackRadiusSquared, 2);
                    if (attackRadiusSquared >= currentLocation.distanceSquaredTo(dangerousEnemies[i].location)) {

                        inDanger = true;
                        robotController.setIndicatorString(2, "In danger (attack range)");
                        break;

                    }

                }

            }

            // check if we need to send an archon update

            if (sendArchonUpdate && !inDanger) {

                robotController.setIndicatorString(1, "Sending archon update with enemies " + enemies.length);

                final InformationSignal signal = new InformationSignal();
                signal.action = PoliticalAgenda.SignalActionWrite;
                signal.broadcastRange = politicalAgenda.maximumBroadcastRangeForLocation(currentLocation);
                signal.location = currentLocation;
                signal.type = PoliticalAgenda.SignalTypeArchonUpdate;
                politicalAgenda.broadcastSignal(signal, robotController);

                sendArchonUpdate = false;

            }

            // perform action

            while (true) {

                if (!robotController.isCoreReady()) {

                    break;

                }

                // see if we are safe

                if (robotController.isCoreReady()) {

                    if (inDanger) {

                        final Direction enemiesDirection = directionController.getAverageDirectionTowardsEnemies(enemies, true, true);
                        if (enemiesDirection != null) {

                            directionController.shouldAvoidEnemies = false;
                            final DirectionController.Result enemiesMovementResult = directionController.getDirectionResultFromDirection(enemiesDirection.opposite(), DirectionController.ADJUSTMENT_THRESHOLD_MEDIUM);
                            directionController.shouldAvoidEnemies = true;

                            if (enemiesMovementResult.direction != null) {

                                robotController.move(enemiesMovementResult.direction);
                                currentLocation = robotController.getLocation();
                                robotController.setIndicatorString(0, "I'm fleeing " + enemiesMovementResult.direction + " to get away from enemies " + enemiesDirection);
                                break;

                            } else if (enemiesMovementResult.error == DirectionController.ErrorType.BLOCKED_RUBBLE) {

                                final Direction rubbleClearanceDirection = rubbleModule.getRubbleClearanceDirectionFromDirection(enemiesDirection.opposite(), robotController, RubbleModule.ADJUSTMENT_THRESHOLD_MEDIUM);
                                if (rubbleClearanceDirection != null) {

                                    final double rubble = robotController.senseRubble(currentLocation.add(rubbleClearanceDirection));
                                    if (rubble < 1000.0) {

                                        robotController.clearRubble(rubbleClearanceDirection);
                                        robotController.setIndicatorString(0, "I'm clearing rubble " + rubbleClearanceDirection + " to get away from enemies " + enemiesDirection);
                                        break;

                                    }

                                }

                            }

                        }

                    }

                }

                // try and build new units

                if (robotController.isCoreReady()) {

                    if (!inDanger) {

                        isNextUnitACompanion = false;
                        RobotType typeToBuild = null;
                        if (scoutsBuilt == 0 || scoutsBuilt * 15 < soldiersBuilt) {

                            typeToBuild = RobotType.SCOUT;

                        } else if (vipersBuilt * 20 < soldiersBuilt && soldiersBuilt > 5) {

                            typeToBuild = RobotType.VIPER;

                        } else if (!inDanger && turretsBuilt * 20 < soldiersBuilt && soldiersBuilt > 10) {

                            typeToBuild = RobotType.TURRET;

                        } else {

                            typeToBuild = RobotType.SOLDIER;

                        }
                        if (lastBuiltUnitType == RobotType.TURRET) {

                            typeToBuild = RobotType.SCOUT;
                            isNextUnitACompanion = true;

                        }
                        if (robotController.getTeamParts() >= typeToBuild.partCost) {

                            boolean built = false;
                            for (int i = 0; i < DirectionController.DIRECTIONS.length; i++) {

                                if (robotController.canBuild(DirectionController.DIRECTIONS[i], typeToBuild)) {

                                    buildingUnitType = typeToBuild;
                                    if (typeToBuild == RobotType.SCOUT && !isNextUnitACompanion) {

                                        scoutsBuilt ++;

                                    }
                                    if (typeToBuild == RobotType.SOLDIER) {

                                        soldiersBuilt ++;

                                    }
                                    if (typeToBuild == RobotType.VIPER) {

                                        vipersBuilt ++;

                                    }
                                    if (typeToBuild == RobotType.TURRET) {

                                        turretsBuilt ++;

                                    }
                                    robotController.build(DirectionController.DIRECTIONS[i], typeToBuild);
                                    robotController.setIndicatorString(0, "I started to build " + typeToBuild + " " + (isNextUnitACompanion ? " as a companion " : "") + "in direction " + DirectionController.DIRECTIONS[i]);
                                    built = true;
                                    lastBuiltUnitType = typeToBuild;
                                    break;

                                }

                            }
                            if (built) {

                                break;

                            }

                        }

                    }

                }

                // move toward visible neutral robots

                MapLocation lootLocation = null;

                if (!inDanger && robotController.isCoreReady()) {

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

                        lootLocation = nearestNeutralLocation;
                        final Direction nearestNeutralDirection = currentLocation.directionTo(nearestNeutralLocation);
                        final DirectionController.Result nearestNeutralMovementResult = directionController.getDirectionResultFromDirection(nearestNeutralDirection, DirectionController.ADJUSTMENT_THRESHOLD_MEDIUM);
                        if (nearestNeutralMovementResult.direction != null && !movementModule.isMovementLocationRepetitive(currentLocation.add(nearestNeutralMovementResult.direction), robotController)) {

                            robotController.move(nearestNeutralMovementResult.direction);
                            currentLocation = robotController.getLocation();
                            movementModule.addMovementLocation(currentLocation, robotController);
                            robotController.setIndicatorString(0, "I moved " + nearestNeutralMovementResult.direction + " to get to a neutral at " + nearestNeutralLocation);
                            break;

                        } else if (nearestNeutralMovementResult.error == DirectionController.ErrorType.BLOCKED_RUBBLE) {

                            final Direction rubbleClearanceDirection = rubbleModule.getRubbleClearanceDirectionFromDirection(nearestNeutralDirection, robotController, RubbleModule.ADJUSTMENT_THRESHOLD_MEDIUM);
                            if (rubbleClearanceDirection != null) {

                                robotController.clearRubble(rubbleClearanceDirection);
                                movementModule.extendLocationInvalidationTurn(robotController);
                                robotController.setIndicatorString(0, "I cleared rubble " + rubbleClearanceDirection + " to get to a neutral at " + nearestNeutralLocation);
                                break;

                            }

                        }

                    }

                }

                // move toward spare parts

                if (!inDanger && lootLocation == null && robotController.isCoreReady()) {

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

                        lootLocation = nearestPartsLocation;
                        final Direction nearestPartsDirection = currentLocation.directionTo(nearestPartsLocation);
                        final DirectionController.Result nearestPartsMovementResult = directionController.getDirectionResultFromDirection(nearestPartsDirection, DirectionController.ADJUSTMENT_THRESHOLD_MEDIUM);
                        if (nearestPartsMovementResult.direction != null && !movementModule.isMovementLocationRepetitive(currentLocation.add(nearestPartsMovementResult.direction), robotController)) {

                            robotController.move(nearestPartsMovementResult.direction);
                            currentLocation = robotController.getLocation();
                            movementModule.addMovementLocation(currentLocation, robotController);
                            robotController.setIndicatorString(0, "I moved " + nearestPartsMovementResult.direction + " to get to spare parts at " + nearestPartsLocation);
                            break;

                        } else if (nearestPartsMovementResult.error == DirectionController.ErrorType.BLOCKED_RUBBLE) {

                            final Direction rubbleClearanceDirection = rubbleModule.getRubbleClearanceDirectionFromDirection(nearestPartsDirection, robotController, RubbleModule.ADJUSTMENT_THRESHOLD_MEDIUM);
                            if (rubbleClearanceDirection != null) {

                                robotController.clearRubble(rubbleClearanceDirection);
                                movementModule.extendLocationInvalidationTurn(robotController);
                                robotController.setIndicatorString(0, "I cleared rubble " + rubbleClearanceDirection + " to get to spare parts at " + nearestPartsLocation);
                                break;

                            }

                        }

                    }

                }

                // get to our friendlies if need be

                if (robotController.isCoreReady()) {

                    if (lootLocation == null && friendlies.length < 8) {

                        robotController.setIndicatorString(1, "I need to try and find some friendlies...");

                        int closestFriendlyDistance = Integer.MAX_VALUE;
                        MapLocation closestLocation = null;

                        final ArrayList<ClumpInfo> friendlyClumps = politicalAgenda.friendlyClumps;
                        for (int i = 0; i < friendlyClumps.size(); i++) {

                            final ClumpInfo enemyInfo = friendlyClumps.get(i);
                            final int distance = currentLocation.distanceSquaredTo(enemyInfo.location);
                            if (distance < closestFriendlyDistance) {

                                closestFriendlyDistance = distance;
                                closestLocation = enemyInfo.location;

                            }

                        }
                        if (closestLocation == null) { // move towards nearby robots

                            RobotInfo[] friendlyFighters = CombatModule.robotsOfTypesFromRobots(friendlies, new RobotType[] {RobotType.SOLDIER, RobotType.GUARD, RobotType.TURRET, RobotType.VIPER});
                            Direction directionToFriendlies = directionController.getAverageDirectionTowardFriendlies(friendlyFighters, false, false);
                            if (directionToFriendlies != null) {

                                closestLocation = currentLocation.add(directionToFriendlies);

                            }

                        }
                        if (closestLocation != null) {

                            if (closestFriendlyDistance > 64) {

                                final Direction closestSignalDirection = currentLocation.directionTo(closestLocation);
                                final DirectionController.Result closestSignalResult = directionController.getDirectionResultFromDirection(closestSignalDirection, DirectionController.ADJUSTMENT_THRESHOLD_LOW);
                                if (closestSignalResult.direction != null && !movementModule.isMovementLocationRepetitive(currentLocation.add(closestSignalResult.direction), robotController)) {

                                    robotController.move(closestSignalResult.direction);
                                    currentLocation = robotController.getLocation();
                                    movementModule.addMovementLocation(currentLocation, robotController);
                                    robotController.setIndicatorString(0, "I am moving to a friendly clump at " + closestLocation);
                                    break;

                                } else if (closestSignalResult.error == DirectionController.ErrorType.BLOCKED_RUBBLE) {

                                    final Direction rubbleClearanceDirection = rubbleModule.getRubbleClearanceDirectionFromDirection(closestSignalDirection, robotController, RubbleModule.ADJUSTMENT_THRESHOLD_MEDIUM);
                                    if (rubbleClearanceDirection != null) {

                                        robotController.clearRubble(rubbleClearanceDirection);
                                        movementModule.extendLocationInvalidationTurn(robotController);
                                        robotController.setIndicatorString(0, "I cleared rubble to get to a friendly clump at " + closestLocation);
                                        break;

                                    }

                                }

                            }

                        }

                    }

                }

                // try clear rubble

                if (robotController.isCoreReady()) {

                    final Direction goalDirection = lootLocation != null ? currentLocation.directionTo(lootLocation) : directionController.getRandomDirection();
                    final Direction rubbleClearanceDirection = rubbleModule.getRubbleClearanceDirectionFromDirection(goalDirection, robotController, RubbleModule.ADJUSTMENT_THRESHOLD_ALL);
                    if (rubbleClearanceDirection != null) {

                        robotController.clearRubble(rubbleClearanceDirection);
                        robotController.setIndicatorString(0, "I cleared rubble " + rubbleClearanceDirection + " because I have nothing else to do.");
                        break;

                    }

                }

                break;

            }

            // try to repair any units nearby

            RobotInfo injuredUnit = null;
            double injuredUnitHealth = Integer.MAX_VALUE;
            final RobotInfo[] friendlyRepairableUnits = robotController.senseNearbyRobots(robotController.getType().attackRadiusSquared, currentTeam);
            for (int i = 0; i < friendlyRepairableUnits.length; i++) {

                final RobotInfo friendly = friendlyRepairableUnits[i];
                if (lastRepairedRobot != null && friendly.ID == lastRepairedRobot.ID && friendly.health < friendly.maxHealth) { // Prioritize last healed robot
                    
                    injuredUnit = friendly;
                    break;
                    
                }
                if (friendly.type == RobotType.ARCHON) {

                    continue;

                }
                if (friendly.health < injuredUnitHealth && friendly.health < friendly.maxHealth) {

                    injuredUnit = friendly;
                    injuredUnitHealth = friendly.health;

                }

            }
            if (injuredUnit != null) {
                
                if (lastRepairedRobot == null || injuredUnit.ID != lastRepairedRobot.ID) { // remember last healed robot
                    
                    lastRepairedRobot = injuredUnit;
                    
                }
                robotController.repair(injuredUnit.location);

            }

            // show what we know

            /*for (int i = 0; i < politicalAgenda.archonLocations.size(); i++) {

                final MapLocation archonLocation = politicalAgenda.archonLocations.get(i);
                robotController.setIndicatorLine(currentLocation, archonLocation, 136, 125, 255);

            }

            for (int i = 0; i < politicalAgenda.enemies.size(); i++) {

                final EnemyInfo enemy = politicalAgenda.enemies.get(i);
                robotController.setIndicatorLine(currentLocation, enemy.location, 255, 0, 208);

            }

            */for (int i = 0; i < politicalAgenda.enemyArchons.size(); i++) {

                final InformationSignal signal = politicalAgenda.enemyArchons.get(i);
                robotController.setIndicatorLine(currentLocation, signal.location, 174, 0, 255);

            }/*

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

            }*/

            Clock.yield();

        }

    }

    /*
    SIGNAL RELAY
     */

    public ArrayList<InformationSignal> generateSignalRelayList(final PoliticalAgenda politicalAgenda) {

        final ArrayList<InformationSignal> signalRelayList = new ArrayList<InformationSignal>();

        // map info

        if (politicalAgenda.hasAllMapBoundaries()) {

            final InformationSignal signal = politicalAgenda.generateMapInfoInformationSignal();
            signalRelayList.add(signal);

        } else {

            if (politicalAgenda.mapBoundaryNorth != PoliticalAgenda.UnknownValue) {

                final InformationSignal signal = politicalAgenda.generateMapWallInformationSignal(PoliticalAgenda.SignalTypeMapWallNorth, politicalAgenda.mapBoundaryNorth);
                signalRelayList.add(signal);

            }
            if (politicalAgenda.mapBoundaryEast != PoliticalAgenda.UnknownValue) {

                final InformationSignal signal = politicalAgenda.generateMapWallInformationSignal(PoliticalAgenda.SignalTypeMapWallEast, politicalAgenda.mapBoundaryEast);
                signalRelayList.add(signal);

            }
            if (politicalAgenda.mapBoundarySouth != PoliticalAgenda.UnknownValue) {

                final InformationSignal signal = politicalAgenda.generateMapWallInformationSignal(PoliticalAgenda.SignalTypeMapWallSouth, politicalAgenda.mapBoundarySouth);
                signalRelayList.add(signal);

            }
            if (politicalAgenda.mapBoundaryWest != PoliticalAgenda.UnknownValue) {

                final InformationSignal signal = politicalAgenda.generateMapWallInformationSignal(PoliticalAgenda.SignalTypeMapWallWest, politicalAgenda.mapBoundaryWest);
                signalRelayList.add(signal);

            }

        }

        // enemy archons

        final int enemyArchonCount = politicalAgenda.enemyArchons.size();
        for (int i = 0; i < enemyArchonCount; i++) {

            final InformationSignal signal = politicalAgenda.enemyArchons.get(i);
            signalRelayList.add(signal);

        }

        // zombie dens

        final int zombieDenCount = politicalAgenda.zombieDens.size();
        for (int i = 0; i < zombieDenCount; i++) {

            final InformationSignal signal = politicalAgenda.zombieDens.get(i);
            signalRelayList.add(signal);

        }

        return signalRelayList;

    }

}
