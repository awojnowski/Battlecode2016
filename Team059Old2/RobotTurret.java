package Team059Old2;

import Team059Old2.Movement.DirectionModule;
import battlecode.common.*;
import java.util.Random;

public class RobotTurret implements Robot {

    public void run(final RobotController robotController) throws GameActionException {

        final DirectionModule directionModule = new DirectionModule(robotController.getID());

        while (true) {

            if (robotController.getType() != RobotType.TTM) {

                if (robotController.isCoreReady()) {

                    robotController.pack();

                }

            } else {

                if (robotController.isCoreReady()) {

                    final Direction desiredMovementDirection = directionModule.randomDirection();
                    if (desiredMovementDirection != null) {

                        final Direction recommendedMovementDirection = directionModule.recommendedMovementDirectionForDirection(desiredMovementDirection, robotController, false);
                        if (recommendedMovementDirection != null) {

                            robotController.move(recommendedMovementDirection);

                        }

                    }

                }

            }

            Clock.yield();

        }

    }

}
