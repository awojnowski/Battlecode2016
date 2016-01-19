package AaronBot2Kiting.Combat;

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

}
