package SimpleTurtle;

import battlecode.common.*;
import java.util.Random;

public class RobotTTM implements Robot {

    public void run(final RobotController robotController) throws GameActionException {

        Direction[] directions = { Direction.EAST, Direction.NORTH_EAST, Direction.NORTH, Direction.NORTH_WEST, Direction.WEST, Direction.SOUTH_WEST, Direction.SOUTH, Direction.SOUTH_EAST };
        final Random random = new Random(robotController.getID());
        boolean moved = false;

        while (true) {

            if (robotController.isCoreReady()) {

                if (!moved) {

                    // move randomly

                    final Direction direction = directions[random.nextInt(directions.length)];
                    if (robotController.canMove(direction)) {

                        robotController.move(direction);
                        moved = true;

                    }

                } else {

                    robotController.unpack();

                }

            }

            Clock.yield();

        }

    }

}