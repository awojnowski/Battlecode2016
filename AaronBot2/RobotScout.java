package AaronBot2;

import AaronBot2.Movement.*;
import AaronBot2.Parts.PartsModule;
import AaronBot2.Signals.*;
import battlecode.common.*;
import java.util.*;

public class RobotScout implements Robot {

    public void run(final RobotController robotController) throws GameActionException {

        final CommunicationModule communicationModule = new CommunicationModule();
        final DirectionModule directionModule = new DirectionModule(robotController.getID());
        final MovementModule movementModule = new MovementModule();
        final PartsModule partsModule = new PartsModule();

        final Random random = new Random(robotController.getID());
        final Team team = robotController.getTeam();

        Direction movementDirection = null;

        while (true) {

            communicationModule.processIncomingSignals(robotController);

            // let's try to make sure we're safe and run from enemies

            final MapLocation currentLocation = robotController.getLocation();
            final RobotInfo[] enemies = robotController.senseHostileRobots(currentLocation, robotController.getType().sensorRadiusSquared);

            if (robotController.isCoreReady() && communicationModule.initialInformationReceived) {

                final RobotInfo dangerousEnemy = directionModule.getNearestEnemyInRangeOfMapLocation(currentLocation, enemies, 1);
                if (dangerousEnemy != null) {

                    final Direction fleeDirection = currentLocation.directionTo(dangerousEnemy.location).opposite();
                    Direction fleeMovementDirection = directionModule.recommendedSafeMovementDirectionForDirection(fleeDirection, robotController, enemies, 1, true);
                    if (fleeMovementDirection != null) {

                        robotController.move(fleeMovementDirection);
                        movementDirection = fleeMovementDirection;

                    } else {

                        fleeMovementDirection = directionModule.recommendedMovementDirectionForDirection(fleeDirection, robotController, true);
                        if (fleeMovementDirection != null) {

                            robotController.move(fleeMovementDirection);
                            movementDirection = fleeMovementDirection;

                        }

                    }

                }

            }

            // let's check up on existing communications to verify the information, if we can

            communicationModule.verifyCommunicationsInformation(robotController, enemies, partsModule, true);

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
                    signal.robotIdentifier = enemy.ID;
                    signal.type = CommunicationModuleSignal.TYPE_ZOMBIEDEN;
                    communicationModule.broadcastSignal(signal, robotController, CommunicationModule.MaximumBroadcastRange);

                } else if (enemy.type == RobotType.ARCHON) {

                    final ArrayList<CommunicationModuleSignal> existingSignals = communicationModule.getCommunicationModuleSignalsNearbyLocation(communicationModule.enemyArchons, currentLocation, CommunicationModule.DefaultApproximateNearbyLocationRange);
                    if (existingSignals.size() > 0) {

                        continue;

                    }

                    final CommunicationModuleSignal signal = new CommunicationModuleSignal();
                    signal.action = CommunicationModuleSignal.ACTION_SEEN;
                    signal.location = enemy.location;
                    signal.robotIdentifier = enemy.ID;
                    signal.type = CommunicationModuleSignal.TYPE_ENEMY_ARCHON;
                    communicationModule.broadcastSignal(signal, robotController, CommunicationModule.MaximumBroadcastRange);

                }

            }

            final PartsModule.Result partsScanResults = partsModule.getPartsNearby(currentLocation, robotController, CommunicationModule.ApproximateNearbyPartsLocationRadius);
            for (int i = 0; i < partsScanResults.locations.size(); i++) {

                final MapLocation partsLocation = partsScanResults.locations.get(i);
                final ArrayList<CommunicationModuleSignal> existingSignals = communicationModule.getCommunicationModuleSignalsNearbyLocation(communicationModule.spareParts, partsLocation, CommunicationModule.ApproximateNearbyPartsLocationRange);
                if (existingSignals.size() > 0) {

                    continue;

                }

                final CommunicationModuleSignal signal = new CommunicationModuleSignal();
                signal.action = CommunicationModuleSignal.ACTION_SEEN;
                signal.location = partsLocation;
                signal.robotIdentifier = 0;
                signal.type = CommunicationModuleSignal.TYPE_SPARE_PARTS;
                communicationModule.broadcastSignal(signal, robotController, CommunicationModule.MaximumBroadcastRange);

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

                } else {

                    movementDirection = null;

                }

            }

            if (movementDirection != null) {

                robotController.setIndicatorLine(currentLocation, currentLocation.add(movementDirection, 10000), 255, 255, 255);

            }

            Clock.yield();

        }

    }

}
