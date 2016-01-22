package AaronBot3.Combat;

import battlecode.common.*;

public class CombatModule {

    public RobotInfo lowestHealthEnemyFromEnemies(RobotInfo[] enemies) {

        RobotInfo bestEnemy = null;
        double lowestHealth = Integer.MAX_VALUE;

        for (int i = 0; i < enemies.length; i++) {

            final RobotInfo enemy = enemies[i];
            if (enemy.health < lowestHealth) {

                bestEnemy = enemy;
                lowestHealth = enemy.health;

            }

        }
        return bestEnemy;

    }

    public RobotInfo lowestInfectionEnemyFromEnemies(RobotInfo[] enemies) {

        RobotInfo bestEnemy = null;
        double lowestInfection = Integer.MAX_VALUE;

        for (int i = 0; i < enemies.length; i++) {

            final RobotInfo enemy = enemies[i];
            if (enemy.viperInfectedTurns < lowestInfection) {

                bestEnemy = enemy;
                lowestInfection = enemy.viperInfectedTurns;

            }

        }
        return bestEnemy;

    }

    public static RobotInfo[] robotsOfTypesFromRobots(RobotInfo[] robots, RobotType[] types) {

        int filteredRobotCount = 0;

        for (int i = 0; i < robots.length; i++) {

            for (int j = 0; j < types.length; j++) {

                if (robots[i].type == types[j]) {

                    filteredRobotCount++;

                }

            }

        }

        RobotInfo[] filteredRobots = new RobotInfo[filteredRobotCount];
        int robotIndex = 0;

        for (int i = 0; i < robots.length; i++) {

            for (int j = 0; j < types.length; j++) {

                if (robots[i].type == types[j]) {

                    filteredRobots[robotIndex++] = robots[i];

                }

            }

        }

        return filteredRobots;

    }

    public RobotInfo enemyToKiteFrom(RobotInfo[] enemies) {

        RobotInfo bestEnemy = null;
        double highestHealth = -1;

        for (int i = 0; i < enemies.length; i++) {

            final RobotInfo enemy = enemies[i];
            if ((enemy.type == RobotType.STANDARDZOMBIE || enemy.type == RobotType.BIGZOMBIE) && enemy.health > highestHealth) {

                bestEnemy = enemy;
                highestHealth = enemy.health;

            }

        }
        return bestEnemy;

    }

    /*
    LOCATIONS / SIDES
     */

    public boolean isLocationOnOurSide(final RobotController robotController, final MapLocation denLocation) {

        int totalDistanceToUs = 0;
        int totalDistanceToThem = 0;

        MapLocation[] friendlyArchonLocations = robotController.getInitialArchonLocations(robotController.getTeam());
        MapLocation[] enemyArchonLocations = robotController.getInitialArchonLocations(robotController.getTeam().opponent());

        for (int i = 0; i < friendlyArchonLocations.length; i++) {

            totalDistanceToUs += friendlyArchonLocations[i].distanceSquaredTo(denLocation);

        }

        for (int i = 0; i < enemyArchonLocations.length; i++) {

            totalDistanceToThem += enemyArchonLocations[i].distanceSquaredTo(denLocation);

        }

        return totalDistanceToUs < totalDistanceToThem;

    }

}
