package AaronBot2;

import AaronBot2.Movement.DirectionModule;
import battlecode.common.*;

public class RobotViper implements Robot {

    public void run(final RobotController robotController) throws GameActionException {

        final DirectionModule directionModule = new DirectionModule(robotController.getID());

        while (true) {
            
            if (robotController.isCoreReady()) {

                final Direction desiredMovementDirection = directionModule.randomDirection();
                if (desiredMovementDirection != null) {

                    final Direction recommendedMovementDirection = directionModule.recommendedMovementDirectionForDirection(desiredMovementDirection, robotController, false);
                    if (recommendedMovementDirection != null) {

                        robotController.move(recommendedMovementDirection);

                    }

                }

            }

            Clock.yield();

        }

    }

}
