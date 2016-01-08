package AaronBot2;

import AaronBot2.Movement.*;
import AaronBot2.Signals.*;
import battlecode.common.*;
import java.util.*;

public class RobotScout implements Robot {

    public void run(final RobotController robotController) throws GameActionException {

        final CommunicationModule communicationModule = new CommunicationModule();
        final DirectionModule directionModule = new DirectionModule(robotController.getID());
        final MovementModule movementModule = new MovementModule();

        final Random random = new Random(robotController.getID());
        final Team team = robotController.getTeam();

        Direction movementDirection = null;

        while (true) {

            communicationModule.processIncomingSignals(robotController);

            // let's try to make sure we're safe and run from enemies

            final MapLocation currentLocation = robotController.getLocation();
            final RobotInfo[] enemies = robotController.senseHostileRobots(currentLocation, robotController.getType().sensorRadiusSquared);

            if (robotController.isCoreReady()) {

                final RobotInfo dangerousEnemy = directionModule.getNearestEnemyInRangeOfMapLocation(currentLocation, enemies);
                if (dangerousEnemy != null) {

                    final Direction fleeDirection = currentLocation.directionTo(dangerousEnemy.location).opposite();
                    final Direction fleeMovementDirection = directionModule.recommendedSafeMovementDirectionForDirection(fleeDirection, robotController, enemies);
                    if (fleeMovementDirection != null) {

                        robotController.move(fleeMovementDirection);

                    }

                }

            }

            // let's check up on existing communications to verify the information, if we can

            final Enumeration<CommunicationModuleSignal> communicationModuleSignals = communicationModule.communications.elements();
            while (communicationModuleSignals.hasMoreElements()) {

                final CommunicationModuleSignal communicationModuleSignal = communicationModuleSignals.nextElement();
                if (robotController.canSenseLocation(communicationModuleSignal.location)) {

                    if (communicationModuleSignal.type == CommunicationModuleSignal.TYPE_ZOMBIEDEN) {

                        final RobotInfo robotInfo = robotController.senseRobotAtLocation(communicationModuleSignal.location);
                        if (robotInfo == null || robotInfo.type != RobotType.ZOMBIEDEN) {

                            communicationModuleSignal.action = CommunicationModuleSignal.ACTION_DELETE;
                            communicationModule.broadcastSignal(communicationModuleSignal, robotController, CommunicationModule.MaximumBroadcastRange);

                        }

                    }

                }

            }

            // let's try identify what we can see

            for (int i = 0; i < enemies.length; i++) {

                final RobotInfo enemy = enemies[i];
                if (enemy.type == RobotType.ZOMBIEDEN) {

                    final CommunicationModuleSignal existingSignal = communicationModule.communications.get(CommunicationModule.communicationsIndexFromLocation(enemy.location));
                    if (existingSignal != null && existingSignal.type == CommunicationModuleSignal.TYPE_ZOMBIEDEN) {

                        continue; // a signal already exists for this den

                    }

                    final CommunicationModuleSignal signal = new CommunicationModuleSignal();
                    signal.action = CommunicationModuleSignal.ACTION_SEEN;
                    signal.location = enemy.location;
                    signal.robotIdentifier = enemy.ID;
                    signal.type = CommunicationModuleSignal.TYPE_ZOMBIEDEN;
                    communicationModule.broadcastSignal(signal, robotController, CommunicationModule.MaximumBroadcastRange);

                }

            }

            // now let's try move to see more

            if (robotController.isCoreReady()) {

                // let's see if we have a movement direction before moving (if not, create one)
                if (movementDirection == null || !robotController.onTheMap(currentLocation.add(movementDirection))) {

                    movementDirection = directionModule.randomDirection();

                }

                final Direction actualMovementDirection = directionModule.recommendedSafeMovementDirectionForDirection(movementDirection, robotController, enemies);
                if (actualMovementDirection != null) {

                    robotController.move(actualMovementDirection);

                } else {

                    movementDirection = null;

                }

            }

            robotController.setIndicatorString(0, "I know of " + communicationModule.communications.size() + " communications.");
            robotController.setIndicatorString(1, "I know of " + enemies.length + " enemies nearby.");

            Clock.yield();

        }

    }

}
