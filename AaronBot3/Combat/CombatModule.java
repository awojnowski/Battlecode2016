package AaronBot3.Combat;

import battlecode.common.*;

public class CombatModule {

    public MapLocation averageFriendlyArchonLocation = null;
    public MapLocation averageEnemyArchonLocation = null;

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
                    break;

                }

            }

        }

        RobotInfo[] filteredRobots = new RobotInfo[filteredRobotCount];
        int robotIndex = 0;

        for (int i = 0; i < robots.length; i++) {

            for (int j = 0; j < types.length; j++) {

                if (robots[i].type == types[j]) {

                    filteredRobots[robotIndex++] = robots[i];
                    break;

                }

            }

        }

        return filteredRobots;

    }

    // Looks lengthy but a lot more efficient than using ArrayLists
    public static RobotInfo[] robotsExcludingTypesFromRobots(RobotInfo[] robots, RobotType[] types) {

        int filteredRobotCount = 0;

        for (int i = 0; i < robots.length; i++) {

            boolean isExcludedType = false;

            for (int j = 0; j < types.length && !isExcludedType; j++) {

                if (robots[i].type == types[j]) {

                    isExcludedType = true;

                }

            }
            if (!isExcludedType) {

                filteredRobotCount++;

            }

        }

        RobotInfo[] filteredRobots = new RobotInfo[filteredRobotCount];
        int robotIndex = 0;

        for (int i = 0; i < robots.length; i++) {

            boolean isExcludedType = false;

            for (int j = 0; j < types.length && !isExcludedType; j++) {

                if (robots[i].type == types[j]) {

                    isExcludedType = true;

                }

            }
            if (!isExcludedType) {

                filteredRobots[robotIndex++] = robots[i];

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

    private MapLocation getAverageFriendlyArchonLocation(final RobotController robotController) {

        if (this.averageFriendlyArchonLocation == null) {

            int totalX = 0;
            int totalY = 0;
            MapLocation[] friendlyArchonLocations = robotController.getInitialArchonLocations(robotController.getTeam());

            for (int i = 0; i < friendlyArchonLocations.length; i++) {

                totalX += friendlyArchonLocations[i].x;
                totalY += friendlyArchonLocations[i].y;

            }

            this.averageFriendlyArchonLocation = new MapLocation(totalX / friendlyArchonLocations.length, totalY / friendlyArchonLocations.length);

        }
        return this.averageFriendlyArchonLocation;

    }

    private MapLocation getAverageEnemyArchonLocation(final RobotController robotController) {

        if (this.averageEnemyArchonLocation == null) {

            MapLocation[] enemyArchonLocations = robotController.getInitialArchonLocations(robotController.getTeam().opponent());
            if (enemyArchonLocations.length == 0) {

                return null;

            }

            int totalX = 0;
            int totalY = 0;
            for (int i = 0; i < enemyArchonLocations.length; i++) {

                totalX += enemyArchonLocations[i].x;
                totalY += enemyArchonLocations[i].y;

            }

            this.averageEnemyArchonLocation = new MapLocation(totalX / enemyArchonLocations.length, totalY / enemyArchonLocations.length);

        }
        return this.averageEnemyArchonLocation;

    }

    public boolean isLocationOnOurSide(final RobotController robotController, final MapLocation location) {

        final MapLocation averageFriendlyArchon = getAverageFriendlyArchonLocation(robotController);
        final MapLocation averageEnemyArchon = getAverageEnemyArchonLocation(robotController);
        if (averageEnemyArchon == null) {

            return true;

        }
        final int distanceToUs = location.distanceSquaredTo(averageFriendlyArchon);
        final int distanceToThem = location.distanceSquaredTo(averageEnemyArchon);
        return distanceToUs < distanceToThem;

    }

}
