package AaronBot2;

import AaronBot2.Cartography.*;
import AaronBot2.Map.*;
import AaronBot2.Movement.*;
import AaronBot2.Signals.*;
import battlecode.common.*;
import java.util.*;

public class RobotScout implements Robot {

    public void run(final RobotController robotController) throws GameActionException {

        final MapInfoModule mapInfoModule = new MapInfoModule();

        final CartographyModule cartographyModule = new CartographyModule();
        final CommunicationModule communicationModule = new CommunicationModule(mapInfoModule);
        final DirectionModule directionModule = new DirectionModule(robotController.getID());
        final MovementModule movementModule = new MovementModule();

        final Random random = new Random(robotController.getID());
        final Team team = robotController.getTeam();

        Direction movementDirection = null;

        while (true) {

            communicationModule.processIncomingSignals(robotController);

            // let's try to make sure we're safe and run from enemies

            MapLocation currentLocation = robotController.getLocation();
            final RobotInfo[] enemies = robotController.senseHostileRobots(currentLocation, robotController.getType().sensorRadiusSquared);

            if (robotController.isCoreReady() && communicationModule.initialInformationReceived && enemies.length > 0) {

                final Direction fleeDirection = directionModule.averageDirectionTowardRobots(robotController, enemies).opposite();
                final Direction fleeMovementDirection = directionModule.recommendedFleeDirectionForDirection(fleeDirection, robotController, false);
                if (fleeMovementDirection != null) {

                    robotController.move(fleeMovementDirection);
                    currentLocation = robotController.getLocation();
                    if (movementDirection != null) {

                        movementDirection = RobotScout.rotateDirection(movementDirection, currentLocation, robotController, mapInfoModule);

                    }

                }

            }

            // let's check up on existing communications to verify the information, if we can

            communicationModule.verifyCommunicationsInformation(robotController, enemies, true);

            // let's try identify what we can see

            for (int i = 0; i < enemies.length; i++) {

                final RobotInfo enemy = enemies[i];

                if (enemy.type == RobotType.ZOMBIEDEN) {

                    final CommunicationModuleSignal existingSignal = communicationModule.zombieDens.get(CommunicationModule.communicationsIndexFromLocation(enemy.location));
                    if (existingSignal != null) {

                        continue;

                    }

                    final CommunicationModuleSignal signal = new CommunicationModuleSignal();
                    signal.action = CommunicationModuleSignal.ACTION_SEEN;
                    signal.location = enemy.location;
                    signal.data = enemy.ID;
                    signal.type = CommunicationModuleSignal.TYPE_ZOMBIEDEN;
                    communicationModule.broadcastSignal(signal, robotController, CommunicationModule.maximumBroadcastRange(mapInfoModule));

                } else if (enemy.type == RobotType.ARCHON) {

                    final ArrayList<CommunicationModuleSignal> existingSignals = communicationModule.getCommunicationModuleSignalsNearbyLocation(communicationModule.enemyArchons, currentLocation, CommunicationModule.DefaultApproximateNearbyLocationRange);
                    if (existingSignals.size() > 0) {

                        continue;

                    }

                    final CommunicationModuleSignal signal = new CommunicationModuleSignal();
                    signal.action = CommunicationModuleSignal.ACTION_SEEN;
                    signal.location = enemy.location;
                    signal.data = enemy.ID;
                    signal.type = CommunicationModuleSignal.TYPE_ENEMY_ARCHON;
                    communicationModule.broadcastSignal(signal, robotController, CommunicationModule.maximumBroadcastRange(mapInfoModule));

                }

            }

            final MapLocation[] partsLocations = robotController.sensePartLocations(-1);
            for (int i = 0; i < partsLocations.length; i++) {

                final MapLocation partsLocation = partsLocations[i];
                final ArrayList<CommunicationModuleSignal> existingSignals = communicationModule.getCommunicationModuleSignalsNearbyLocation(communicationModule.spareParts, partsLocation, CommunicationModule.ApproximateNearbyPartsLocationRange);
                if (existingSignals.size() > 0) {

                    continue;

                }

                final CommunicationModuleSignal signal = new CommunicationModuleSignal();
                signal.action = CommunicationModuleSignal.ACTION_SEEN;
                signal.location = partsLocation;
                signal.data = 0;
                signal.type = CommunicationModuleSignal.TYPE_SPARE_PARTS;
                communicationModule.broadcastSignal(signal, robotController, CommunicationModule.maximumBroadcastRange(mapInfoModule));

            }

            if (!mapInfoModule.hasAllBoundaries()) {

                cartographyModule.probeAndUpdateMapInfoModule(mapInfoModule, currentLocation, robotController);
                if (mapInfoModule.hasAllBoundaries()) {

                    final CommunicationModuleSignal signal = new CommunicationModuleSignal();
                    signal.action = CommunicationModuleSignal.ACTION_SEEN;
                    mapInfoModule.fillCommunicationModuleSignalWithMapSizeData(signal);
                    communicationModule.broadcastSignal(signal, robotController, CommunicationModule.maximumBroadcastRange(mapInfoModule));

                }

            }

            // now let's try move to see more

            if (robotController.isCoreReady() && communicationModule.initialInformationReceived) {

                // let's see if we have a movement direction before moving (if not, create one)
                if (movementDirection == null || !robotController.onTheMap(currentLocation.add(movementDirection))) {

                    movementDirection = directionModule.randomDirection();

                }

                final Direction actualMovementDirection = directionModule.recommendedSafeMovementDirectionForDirection(movementDirection, robotController, enemies, 1, true);
                if (actualMovementDirection != null) {

                    robotController.move(actualMovementDirection);
                    currentLocation = robotController.getLocation();

                } else {

                    movementDirection = RobotScout.rotateDirection(movementDirection, currentLocation, robotController, mapInfoModule);

                }

            }

            if (movementDirection != null) {

                robotController.setIndicatorLine(currentLocation, currentLocation.add(movementDirection, 10000), 255, 255, 255);

            }

            Clock.yield();

        }

    }

    private static Direction rotateDirection(final Direction direction, final MapLocation location, final RobotController robotController, final MapInfoModule mapInfo) {

        if (robotController.getID() % 2 == 0) {

            return direction.rotateLeft();

        } else {

            return direction.rotateRight();

        }

    }

}
