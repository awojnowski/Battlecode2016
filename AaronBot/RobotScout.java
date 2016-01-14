package AaronBot;

import battlecode.common.*;

public class RobotScout implements Robot {

    public void run(final RobotController robotController) throws GameActionException {

        while (true) {

            RobotInfo[] nearbyZombies = robotController.senseNearbyRobots(-1, Team.ZOMBIE);
            RobotInfo[] nearbyEnemies = robotController.senseNearbyRobots(-1, robotController.getTeam().opponent());

            for (int i = 0; i < nearbyZombies.length; i++) {

                final RobotInfo zombie = nearbyEnemies[i];
                robotController.broadcastMessageSignal(100, zombie.location.x, zombie.location.y);

            }

            for (int i = 0; i < nearbyEnemies.length; i++) {

                final RobotInfo enemy = nearbyEnemies[i];
                robotController.broadcastMessageSignal(enemy.location.x, enemy.location.y, 100);

            }

            Clock.yield();

        }

    }

}
