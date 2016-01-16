package AaronBot;

import battlecode.common.*;

public class RobotScout implements Robot {

    public void run(final RobotController robotController) throws GameActionException {

        while (true) {

            int signals = 0;

            RobotInfo[] nearbyZombies = robotController.senseNearbyRobots(-1, Team.ZOMBIE);
            RobotInfo[] nearbyEnemies = robotController.senseNearbyRobots(-1, robotController.getTeam().opponent());

            for (int i = 0; i < nearbyZombies.length && signals < 20; i++) {

                final RobotInfo zombie = nearbyZombies[i];
                robotController.broadcastMessageSignal(100, zombie.location.x, zombie.location.y);
                signals++;

            }

            for (int i = 0; i < nearbyEnemies.length && signals < 20; i++) {

                final RobotInfo enemy = nearbyEnemies[i];
                robotController.broadcastMessageSignal(enemy.location.x, enemy.location.y, 100);
                signals++;

            }

            RobotInfo[] nearbyAllies = robotController.senseNearbyRobots(8, robotController.getTeam());
            Direction awayFromArchon = null;
            int turretCount = 0;

            for (int i = 0; i < nearbyAllies.length; i++) {

                final RobotInfo ally = nearbyAllies[i];

                if (ally.type == RobotType.ARCHON) {

                    awayFromArchon = ally.location.directionTo(robotController.getLocation());

                } else if (ally.type == RobotType.TURRET) {

                    turretCount++;

                }

                if (robotController.isCoreReady() && turretCount > 7 && awayFromArchon != null) {

                    if (robotController.canMove(awayFromArchon)) {

                        robotController.move(awayFromArchon);

                    } else if (robotController.canMove(awayFromArchon.rotateLeft())) {

                        robotController.move(awayFromArchon.rotateLeft());

                    } else if (robotController.canMove(awayFromArchon.rotateRight())) {

                        robotController.move(awayFromArchon.rotateRight());

                    }

                }

            }

            Clock.yield();

        }

    }

}
