package AaronBot2Old.Map;

import AaronBot2Old.Signals.CommunicationModuleSignal;
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

    public int mapWidth() {

        return this.eastBoundaryValue - this.westBoundaryValue - 1;

    }

    public int mapHeight() {

        return this.southBoundaryValue - this.northBoundaryValue - 1;

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
