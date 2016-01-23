package AaronBot3.Map;

import AaronBot2.Robot;
import AaronBot3.Signals.CommunicationModuleSignal;
import TheHair.Signals.CommunicationModule;
import battlecode.common.*;

import java.util.Map;

public class MapInfoModule {

    // set this to determine whether to throw games or not (games throw on Desert first, currently)
    public static boolean CanThrowGame = false;

    public static int UnknownValue = Integer.MAX_VALUE;
    public static int ThrowGameIndicatorIndex = Math.max(0, GameConstants.TEAM_MEMORY_LENGTH - 4);
    public static int ThrowGameIndicatorValue = 48678332;
    public static int NotThrowGameIndicatorValue = 15687865;

    public int northBoundaryValue = MapInfoModule.UnknownValue;
    public int eastBoundaryValue = MapInfoModule.UnknownValue;
    public int southBoundaryValue = MapInfoModule.UnknownValue;
    public int westBoundaryValue = MapInfoModule.UnknownValue;

    public boolean shouldThrowGame = false;

    public double midX = UnknownValue;
    public double midY = UnknownValue;

    /*
    MAP DATA
     */

    public boolean hasAllBoundaries() {

        return this.northBoundaryValue != MapInfoModule.UnknownValue &&
                this.eastBoundaryValue != MapInfoModule.UnknownValue &&
                this.southBoundaryValue != MapInfoModule.UnknownValue &&
                this.westBoundaryValue != MapInfoModule.UnknownValue;

    }

    public void fillDataFromCommunicationModuleSignal(final CommunicationModuleSignal communicationModuleSignal) {

        if (communicationModuleSignal.type == CommunicationModuleSignal.TYPE_MAP_INFO) {

            final MapLocation topLeftCoordinate = communicationModuleSignal.location;
            this.northBoundaryValue = topLeftCoordinate.y;
            this.westBoundaryValue = topLeftCoordinate.x;

            final int data = communicationModuleSignal.data;
            final int mapWidth = (data & 0x000000FF);
            final int mapHeight = ((data >> 8) & 0x000000FF);
            this.eastBoundaryValue = this.westBoundaryValue + mapWidth + 1;
            this.southBoundaryValue = this.northBoundaryValue + mapHeight + 1;

        } else if (communicationModuleSignal.type == CommunicationModuleSignal.TYPE_MAP_WALL_EAST) {

            this.eastBoundaryValue = communicationModuleSignal.data;

        } else if (communicationModuleSignal.type == CommunicationModuleSignal.TYPE_MAP_WALL_NORTH) {

            this.northBoundaryValue = communicationModuleSignal.data;

        } else if (communicationModuleSignal.type == CommunicationModuleSignal.TYPE_MAP_WALL_WEST) {

            this.westBoundaryValue = communicationModuleSignal.data;

        } else if (communicationModuleSignal.type == CommunicationModuleSignal.TYPE_MAP_WALL_SOUTH) {

            this.southBoundaryValue = communicationModuleSignal.data;

        }

    }

    public void fillCommunicationModuleSignalWithMapSizeData(final CommunicationModuleSignal communicationModuleSignal) {

        communicationModuleSignal.type = CommunicationModuleSignal.TYPE_MAP_INFO;
        communicationModuleSignal.location = new MapLocation(this.westBoundaryValue, this.northBoundaryValue);

        int data = this.mapHeight();
        data <<= 8;
        data += this.mapWidth();
        communicationModuleSignal.data = data;

    }

    public boolean hasMapWidth() {

        return this.westBoundaryValue != MapInfoModule.UnknownValue && this.eastBoundaryValue != MapInfoModule.UnknownValue;

    }

    public int mapWidth() {

        return this.eastBoundaryValue - this.westBoundaryValue - 1;

    }

    public boolean hasMapHeight() {

        return this.northBoundaryValue != MapInfoModule.UnknownValue && this.southBoundaryValue != MapInfoModule.UnknownValue;

    }

    public int mapHeight() {

        return this.southBoundaryValue - this.northBoundaryValue - 1;

    }

    public void determineMapMirror(final RobotController robotController) {

        final MapLocation[] friendlyArchonLocations = robotController.getInitialArchonLocations(robotController.getTeam());
        final MapLocation[] enemyArchonLocations = robotController.getInitialArchonLocations(robotController.getTeam().opponent());

        int totalFriendlyX = 0;
        int totalFriendlyY = 0;
        int totalEnemyX = 0;
        int totalEnemyY = 0;

        for (int i = 0; i < friendlyArchonLocations.length; i++) {

            final MapLocation location = friendlyArchonLocations[i];
            totalFriendlyX += location.x;
            totalFriendlyY += location.y;

        }

        for (int i = 0; i < enemyArchonLocations.length; i++) {

            final MapLocation location = enemyArchonLocations[i];
            totalEnemyX += location.x;
            totalEnemyY += location.y;

        }

        double averageFriendlyX = (double)totalFriendlyX / friendlyArchonLocations.length;
        double averageFriendlyY = (double)totalFriendlyY / friendlyArchonLocations.length;
        double averageEnemyX = (double)totalEnemyX / enemyArchonLocations.length;
        double averageEnemyY = (double)totalEnemyY / enemyArchonLocations.length;

        double averageX = (averageFriendlyX + averageEnemyX) / 2;
        double averageY = (averageFriendlyY + averageEnemyY) / 2;
        double differenceX = averageFriendlyX - averageEnemyX;
        double differenceY = averageFriendlyY - averageEnemyY;

//        System.out.println("Average Friendly X: " + averageFriendlyX);
//        System.out.println("Average Friendly Y: " + averageFriendlyY);
//        System.out.println("Average Enemy X: " + averageEnemyX);
//        System.out.println("Average Enemy Y: " + averageEnemyY);
//        System.out.println("Average X: " + averageX);
//        System.out.println("Average Y: " + averageY);
//        System.out.println("Difference X: " + differenceX);
//        System.out.println("Difference Y: " + differenceY);

        if (differenceX != 0) { // The map mirrors over the x-axis

            midX = averageX;

        }
        if (differenceY != 0) { // The map mirrors over the y-axis

            midY = averageY;

        }

    }

    public MapLocation mirroredLocation(final MapLocation location) {

        if (midX != UnknownValue && midY != UnknownValue) { // reflect over both axes

            int x = (int)(midX - (location.x - midX));
            int y = (int)(midY - (location.y - midY));
            return new MapLocation(x, y);

        } else if (midX != UnknownValue) { // reflect over x-axis

            int x = (int)(midX - (location.x - midX));
            int y = location.y;
            return new MapLocation(x, y);

        } else if (midY != UnknownValue) { // reflect over y-axis

            int x = location.x;
            int y = (int)(midY - (location.y - midY));
            return new MapLocation(x, y);

        }
        // Should always return a location since maps are always symmetrical
        return null;

    }

    public CommunicationModuleSignal mirroredBoundarySignal(final CommunicationModuleSignal signal) {

        if ((signal.type == CommunicationModuleSignal.TYPE_MAP_WALL_EAST || signal.type == CommunicationModuleSignal.TYPE_MAP_WALL_WEST) && midX != UnknownValue) { // reflect over x-axis

            int mirrorX = (int)(midX - (signal.data - midX));

            final CommunicationModuleSignal newSignal = new CommunicationModuleSignal();
            newSignal.action = CommunicationModuleSignal.ACTION_SEEN;
            newSignal.type = signal.type == CommunicationModuleSignal.TYPE_MAP_WALL_EAST ? CommunicationModuleSignal.TYPE_MAP_WALL_WEST : CommunicationModuleSignal.TYPE_MAP_WALL_EAST;
            newSignal.data = mirrorX;
            return newSignal;

        } else if ((signal.type == CommunicationModuleSignal.TYPE_MAP_WALL_NORTH || signal.type == CommunicationModuleSignal.TYPE_MAP_WALL_SOUTH) && midY != UnknownValue) { // reflect over y-axis

            int mirrorY = (int)(midY - (signal.data - midY));

            final CommunicationModuleSignal newSignal = new CommunicationModuleSignal();
            newSignal.action = CommunicationModuleSignal.ACTION_SEEN;
            newSignal.type = signal.type == CommunicationModuleSignal.TYPE_MAP_WALL_NORTH ? CommunicationModuleSignal.TYPE_MAP_WALL_SOUTH : CommunicationModuleSignal.TYPE_MAP_WALL_NORTH;
            newSignal.data = mirrorY;
            return newSignal;

        }
        return null;

    }

    /*
    THROWING
     */

    public void detectWhetherToThrowGame(final RobotController robotController) {

        if (!MapInfoModule.CanThrowGame) {

            this.doNotThrowGame(robotController);
            return;

        }

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
