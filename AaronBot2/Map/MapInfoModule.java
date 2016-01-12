package AaronBot2.Map;

import battlecode.common.*;

public class MapInfoModule {

    public static int UnknownValue = Integer.MAX_VALUE;
    public static int ThrowGameIndicatorIndex = Math.max(0, GameConstants.TEAM_MEMORY_LENGTH - 4);
    public static int ThrowGameIndicatorValue = 48678332;
    public static int NotThrowGameIndicatorValue = 15687865;

    public int northBoundaryValue = MapInfoModule.UnknownValue;
    public int eastBoundaryValue = MapInfoModule.UnknownValue;
    public int southBoundaryValue = MapInfoModule.UnknownValue;
    public int westBoundaryValue = MapInfoModule.UnknownValue;

    public boolean shouldThrowGame = false;

    public void detectWhetherToThrowGame(final RobotController robotController) {

        final long[] teamMemory = robotController.getTeamMemory();
        if (teamMemory[MapInfoModule.ThrowGameIndicatorIndex] == MapInfoModule.ThrowGameIndicatorValue) {

            this.throwGame(robotController);
            return;

        } else if (teamMemory[MapInfoModule.ThrowGameIndicatorIndex] == MapInfoModule.NotThrowGameIndicatorValue) {

            this.doNotThrowGame(robotController);
            return;

        }

        final MapLocation currentLocation = robotController.getLocation();
        final RobotInfo[] nearbyRobots = robotController.senseNearbyRobots(-1, robotController.getTeam());

        int closestArchonDistance = Integer.MAX_VALUE;
        int totalArchons = 0;

        for (int i = 0; i < nearbyRobots.length; i++) {

            final RobotInfo robot = nearbyRobots[i];
            if (robot.type != RobotType.ARCHON) {

                continue;

            }
            closestArchonDistance = Math.min(closestArchonDistance, robot.location.distanceSquaredTo(currentLocation));
            totalArchons ++;

        }

        if (closestArchonDistance == 2 && totalArchons == 2) {

            this.throwGame(robotController);

        } else {

            this.doNotThrowGame(robotController);

        }

    }

    private void throwGame(final RobotController robotController) {

        robotController.setTeamMemory(MapInfoModule.ThrowGameIndicatorIndex, MapInfoModule.ThrowGameIndicatorValue);
        this.shouldThrowGame = true;

    }

    private void doNotThrowGame(final RobotController robotController) {

        robotController.setTeamMemory(MapInfoModule.ThrowGameIndicatorIndex, MapInfoModule.NotThrowGameIndicatorValue);
        this.shouldThrowGame = false;

    }

}
