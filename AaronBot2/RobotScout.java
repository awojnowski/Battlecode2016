package AaronBot2;

import AaronBot2.Signals.*;
import battlecode.common.*;
import java.util.*;

public class RobotScout implements Robot {

    public void run(final RobotController robotController) throws GameActionException {

        final CommunicationModule communicationModule = new CommunicationModule();
        final Direction[] directions = { Direction.EAST, Direction.NORTH_EAST, Direction.NORTH, Direction.NORTH_WEST, Direction.WEST, Direction.SOUTH_WEST, Direction.SOUTH, Direction.SOUTH_EAST };
        final Random random = new Random(robotController.getID());
        final Team team = robotController.getTeam();

        Direction movementDirection = null;

        while (true) {

            communicationModule.processIncomingSignals(robotController);

            // let's try identify what we can see

            final RobotInfo[] zombies = robotController.senseNearbyRobots(robotController.getType().sensorRadiusSquared, Team.ZOMBIE);
            for (int i = 0; i < zombies.length; i++) {

                final RobotInfo zombie = zombies[i];
                if (zombie.type != RobotType.ZOMBIEDEN) {

                    continue;

                }

                final CommunicationModuleSignal existingSignal = communicationModule.communications.get(CommunicationModule.communicationsIndexFromLocation(zombie.location));
                if (existingSignal != null && existingSignal.type == CommunicationModuleSignal.TYPE_ZOMBIEDEN) {

                    continue; // a signal already exists for this den

                }

                final CommunicationModuleSignal signal = new CommunicationModuleSignal();
                signal.action = CommunicationModuleSignal.ACTION_SEEN;
                signal.location = zombie.location;
                signal.robotIdentifier = zombie.ID;
                signal.type = CommunicationModuleSignal.TYPE_ZOMBIEDEN;
                communicationModule.broadcastSignal(signal, robotController, CommunicationModule.MaximumBroadcastRange);

            }

            // now let's try move to see more

            if (robotController.isCoreReady()) {

                // let's see if we have a movement direction before moving (if not, create one)
                if (movementDirection == null) {

                    movementDirection = directions[random.nextInt(directions.length)];

                }

                if (robotController.canMove(movementDirection)) {

                    robotController.move(movementDirection);

                } else {

                    movementDirection = null;

                }

            }

            Clock.yield();

        }

    }

}
