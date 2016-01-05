package SoldierSpam;

import battlecode.common.*;

public class RobotTurret implements Robot {

    public void run(final RobotController robotController) throws GameActionException {

        final Team team = robotController.getTeam();

        while (true) {

            if (robotController.isWeaponReady()) {

                RobotInfo[] robots = robotController.senseNearbyRobots();
                for (int i = 0; i < robots.length; i++) {

                    final RobotInfo robot = robots[i];
                    if (robot.team == team || robot.team == Team.NEUTRAL) {

                        continue;

                    }

                    if (robotController.canAttackLocation(robot.location)) {

                        robotController.attackLocation(robot.location);

                    }

                }

            }

            Clock.yield();

        }

    }

}