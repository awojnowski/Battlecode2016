package AaronBot3.ZombieSpawns;

import battlecode.common.*;

public class ZombieSpawnsModule {

    public static ZombieSpawnSchedule spawnSchedule = null;

    public static void setSpawnScheduleIfNeeded(final RobotController robotController) {

        if (spawnSchedule == null) {

            spawnSchedule = robotController.getZombieSpawnSchedule();

//            int[] rounds = spawnSchedule.getRounds();
//
//            for (int i = 0; i < rounds.length; i++) {
//
//                System.out.println("Round " + rounds[i] + "   Danger level: " + dangerLevelForSpawnAtRound(rounds[i], robotController));
//
//                ZombieCount[] counts = spawnSchedule.getScheduleForRound(rounds[i]);
//
//                for (int j = 0; j < counts.length; j++) {
//
//                    System.out.println(counts[j].getType() + ": " + counts[j].getCount());
//
//                }
//
//            }

        }

    }

    // MARK: Round Numbers

    // will return current round num if there's a spawn this round, returns -1 if there is no next spawn
    public static int nextZombieSpawn(final RobotController robotController) {

        final int currentRound = robotController.getRoundNum();

        if (spawnSchedule == null) {

            setSpawnScheduleIfNeeded(robotController);

        }

        final int[] spawnRounds = spawnSchedule.getRounds();

        for (int i = 0; i < spawnRounds.length; i++) {

            if (spawnRounds[i] >= currentRound) {

                return spawnRounds[i];

            }

        }

        return -1;

    }

    public static int roundsUntilNextZombieSpawn(final RobotController robotController) {

        final int currentRound = robotController.getRoundNum();
        final int nextSpawnRound = nextZombieSpawn(robotController);

        if (nextSpawnRound == -1) {

            return -1;

        } else {

            return nextSpawnRound - currentRound;

        }

    }

    // MARK: Danger Levels

    public static int dangerLevelForSpawnAtRound(final int spawnRound, final RobotController  robotController) {

        if (spawnSchedule == null) {

            setSpawnScheduleIfNeeded(robotController);

        }

        final ZombieCount[] zombieCounts = spawnSchedule.getScheduleForRound(spawnRound);
        int dangerLevel = 0;

        for (int i = 0; i < zombieCounts.length; i++) {

            final ZombieCount zombieCount = zombieCounts[i];

            dangerLevel += zombieCount.getCount() * zombieCount.getType().maxHealth(spawnRound);

        }

        return dangerLevel;

    }

    public static int dangerLevelForNextSpawn(final int spawnRound, final RobotController  robotController) {

        final int nextSpawnRound = nextZombieSpawn(robotController);

        if (nextSpawnRound == -1) {

            return -1;

        }

        return dangerLevelForSpawnAtRound(nextSpawnRound, robotController);

    }

    // MARK: Zombie Types

    public static boolean spawnAtRoundContainsType(final int spawnRound, final RobotType robotType, final RobotController robotController) {

        if (spawnSchedule == null) {

            setSpawnScheduleIfNeeded(robotController);

        }

        final ZombieCount[] zombieCounts = spawnSchedule.getScheduleForRound(spawnRound);

        for (int i = 0; i < zombieCounts.length; i++) {

            final ZombieCount zombieCount = zombieCounts[i];

            if (zombieCount.getType() == robotType) {

                return true;

            }

        }

        return false;

    }

}
