package AaronBot3.Information;

import AaronBot.Robot;
import battlecode.common.*;
import java.util.*;

public class PoliticalAgenda {

    public static int SignalActionWrite           = 0;
    public static int SignalActionErase           = 1;

    public static int SignalTypeZombieDen         = 0;
    public static int SignalTypeMapInfo           = 1;
    public static int SignalTypeMapWallNorth      = 2;
    public static int SignalTypeMapWallEast       = 3;
    public static int SignalTypeMapWallSouth      = 4;
    public static int SignalTypeMapWallWest       = 5;
    public static int SignalTypeEnemy             = 6;
    public static int SignalTypeInformationSynced = 7;

    public static int UnknownValue = -186784223;

    public boolean isInformationSynced = false;

    public int mapBoundaryNorth = PoliticalAgenda.UnknownValue;
    public int mapBoundaryEast = PoliticalAgenda.UnknownValue;
    public int mapBoundarySouth = PoliticalAgenda.UnknownValue;
    public int mapBoundaryWest = PoliticalAgenda.UnknownValue;

    public double mapMirrorX = PoliticalAgenda.UnknownValue;
    public double mapMirrorY = PoliticalAgenda.UnknownValue;
    public double mapSecondMirrorX = PoliticalAgenda.UnknownValue;
    public double mapSecondMirrorY = PoliticalAgenda.UnknownValue;

    public int companionIdentifier = PoliticalAgenda.UnknownValue;

    public ImmutableInformationCollection<EnemyInfo> enemies = null;
    public final ArrayList<Signal> notifications = new ArrayList<Signal>();
    public final MutableInformationCollection<InformationSignal> zombieDens = new MutableInformationCollection<InformationSignal>();

    public final ArrayList<InformationSignal> informationSignalQueue = new ArrayList<InformationSignal>();

    public PoliticalAgenda() {

        ;

    }

    /*
    BROADCASTING
     */

    public void broadcastSignal(final InformationSignal informationSignal, final RobotController robotController) throws GameActionException {

        this.broadcastSignal(informationSignal, robotController, true);

    }

    public void broadcastSignal(final InformationSignal informationSignal, final RobotController robotController, boolean processSignal) throws GameActionException {

        if (informationSignal.broadcastRange == 0) {

            System.out.println("WARNING: attempting to broadcast signal: " + informationSignal.type + " location: " + informationSignal.location + " with zero broadcast range.");
            return;

        }

        final int[] message = informationSignal.serialize();
        robotController.broadcastMessageSignal(message[0], message[1], informationSignal.broadcastRange);

        if (processSignal) {

            this.processSignal(informationSignal);

        }

    }

    public void broadcastSignal(final RobotController robotController, final int broadcastRange) throws GameActionException {

        robotController.broadcastSignal(broadcastRange);

    }

    public boolean hasEnqueuedSignalsForBroadcast() {

        return this.informationSignalQueue.size() > 0;

    }

    public void enqueueSignalForBroadcast(final InformationSignal informationSignal) {

        this.informationSignalQueue.add(informationSignal);
        this.processSignal(informationSignal);

    }

    public void broadcastEnqueuedSignals(final RobotController robotController) throws GameActionException {

        for (int i = 0; i < this.informationSignalQueue.size() && robotController.getMessageSignalCount() < GameConstants.MESSAGE_SIGNALS_PER_TURN; i++) {

            this.broadcastSignal(this.informationSignalQueue.get(i), robotController, false);

        }
        this.informationSignalQueue.clear();

    }

    /*
    MAP
     */

    public boolean hasAllMapBoundaries() {

        return this.mapBoundaryNorth != PoliticalAgenda.UnknownValue && this.mapBoundaryEast != PoliticalAgenda.UnknownValue && this.mapBoundarySouth != PoliticalAgenda.UnknownValue && this.mapBoundaryWest != PoliticalAgenda.UnknownValue;

    }

    public boolean hasMapWidth() {

        return this.mapBoundaryWest != PoliticalAgenda.UnknownValue && this.mapBoundaryEast != PoliticalAgenda.UnknownValue;

    }

    public int mapWidth() {

        return this.mapBoundaryEast - this.mapBoundaryWest - 1;

    }

    public boolean hasMapHeight() {

        return this.mapBoundaryNorth != PoliticalAgenda.UnknownValue && this.mapBoundarySouth != PoliticalAgenda.UnknownValue;

    }

    public int mapHeight() {

        return this.mapBoundarySouth - this.mapBoundaryNorth - 1;

    }

    /*
    MAP MIRRORING
     */

    public void determineMapMirroring(final RobotController robotController) {

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

        if (differenceX != 0) { // The map mirrors over the x-axis

            this.mapMirrorX = averageX;

        } else {

            this.mapSecondMirrorX = averageX;

        }
        if (differenceY != 0) { // The map mirrors over the y-axis

            this.mapMirrorY = averageY;

        } else {

            this.mapSecondMirrorY = averageY;

        }

        // Determine secondary mirror (in case of 4-way)

        if (friendlyArchonLocations.length > 1 && (this.mapSecondMirrorX != PoliticalAgenda.UnknownValue || this.mapSecondMirrorY != PoliticalAgenda.UnknownValue)) { // if we're only working with a one-axis mirror (could be 4-way)

            boolean isFourWayMirror = true;

            for (int i = 0; i < friendlyArchonLocations.length && isFourWayMirror; i++) {

                final MapLocation location = friendlyArchonLocations[i];
                boolean hasMirroredMatch = false;

                for (int j = 0; j < friendlyArchonLocations.length && !hasMirroredMatch; j++) {

                    final MapLocation location2 = friendlyArchonLocations[j];
                    final MapLocation mirroredLocation2 = getSecondaryMirroredLocationFromLocation(location2);

                    if (location.equals(mirroredLocation2)) {

                        hasMirroredMatch = true;

                    }

                }

                if (!hasMirroredMatch) {

                    isFourWayMirror = false;

                }

            }

            if (!isFourWayMirror) {

                this.mapSecondMirrorX = PoliticalAgenda.UnknownValue;
                this.mapSecondMirrorY = PoliticalAgenda.UnknownValue;

            }

        }


    }

    public MapLocation getMirroredLocationFromLocation(final MapLocation location) {

        if (this.mapMirrorX != UnknownValue && this.mapMirrorY != UnknownValue) { // reflect over both axes

            int x = (int)(this.mapMirrorX - (location.x - this.mapMirrorX));
            int y = (int)(this.mapMirrorY - (location.y - this.mapMirrorY));
            return new MapLocation(x, y);

        } else if (this.mapMirrorX != UnknownValue) { // reflect over x-axis

            int x = (int)(this.mapMirrorX - (location.x - this.mapMirrorX));
            int y = location.y;
            return new MapLocation(x, y);

        } else if (this.mapMirrorY != UnknownValue) { // reflect over y-axis

            int x = location.x;
            int y = (int)(this.mapMirrorY - (location.y - this.mapMirrorY));
            return new MapLocation(x, y);

        }
        return null; // should always return a location since maps are always symmetrical

    }

    public MapLocation getSecondaryMirroredLocationFromLocation(final MapLocation location) {

        if (this.mapSecondMirrorX != UnknownValue) { // reflect over x-axis

            int x = (int)(this.mapSecondMirrorX - (location.x - this.mapSecondMirrorX));
            int y = location.y;
            return new MapLocation(x, y);

        } else if (this.mapSecondMirrorY != UnknownValue) { // reflect over y-axis

            int x = location.x;
            int y = (int)(this.mapSecondMirrorY - (location.y - this.mapSecondMirrorY));
            return new MapLocation(x, y);

        }
        return null;

    }

    public InformationSignal getMirroredBoundarySignal(final InformationSignal signal) {

        if ((signal.type == PoliticalAgenda.SignalTypeMapWallEast || signal.type == PoliticalAgenda.SignalTypeMapWallWest) && (this.mapMirrorX != UnknownValue || this.mapSecondMirrorX != UnknownValue)) { // reflect over x-axis

            final double mapXMirror = this.mapMirrorX != UnknownValue ? this.mapMirrorX : this.mapSecondMirrorX;
            final int mirrorX = (int)(mapXMirror - (signal.data - mapXMirror));

            final InformationSignal newSignal = new InformationSignal();
            newSignal.action = PoliticalAgenda.SignalActionWrite;
            newSignal.data = mirrorX;
            newSignal.type = signal.type == PoliticalAgenda.SignalTypeMapWallEast ? PoliticalAgenda.SignalTypeMapWallWest : PoliticalAgenda.SignalTypeMapWallEast;
            return newSignal;

        } else if ((signal.type == PoliticalAgenda.SignalTypeMapWallNorth || signal.type == PoliticalAgenda.SignalTypeMapWallSouth) && (this.mapMirrorY != UnknownValue || this.mapSecondMirrorY != UnknownValue)) { // reflect over y-axis

            final double mapYMirror = this.mapMirrorY != UnknownValue ? this.mapMirrorY : this.mapSecondMirrorY;
            final int mirrorY = (int)(mapYMirror - (signal.data - mapYMirror));

            final InformationSignal newSignal = new InformationSignal();
            newSignal.action = PoliticalAgenda.SignalActionWrite;
            newSignal.data = mirrorY;
            newSignal.type = signal.type == PoliticalAgenda.SignalTypeMapWallNorth ? PoliticalAgenda.SignalTypeMapWallSouth : PoliticalAgenda.SignalTypeMapWallNorth;
            return newSignal;

        }
        return null;

    }

    /*
    SIGNALS
     */

    public void processIncomingSignalsFromRobotController(final RobotController robotController) {

        // clean up

        this.enemies = new ImmutableInformationCollection<EnemyInfo>();
        this.notifications.clear();

        // process the signals

        final Signal[] signals = robotController.emptySignalQueue();
        final RobotType robotType = robotController.getType();
        final Team team = robotController.getTeam();

        for (int i = 0; i < signals.length; i++) {

            final Signal signal = signals[i];
            if (signal.getTeam() != team) {

                continue;

            }

            final int[] message = signal.getMessage();
            if (message == null || message.length < 2) {

                this.notifications.add(signal);
                continue;

            }

            final InformationSignal informationSignal = new InformationSignal(message);
            if (!this.shouldRobotTypeProcessSignalType(robotType, informationSignal.type)) {

                continue;

            }
            this.processSignal(informationSignal);

        }

    }

    private void processSignal(final InformationSignal signal) {

        if (signal.type == PoliticalAgenda.SignalTypeZombieDen) {

            if (signal.action == PoliticalAgenda.SignalActionWrite) {

                this.zombieDens.add(signal, this.getIndexIdentifierForZombieDen(signal.location));

            } else if (signal.action == PoliticalAgenda.SignalActionErase) {

                this.zombieDens.remove(this.getIndexIdentifierForZombieDen(signal.location));

            }

        } else if (signal.type == PoliticalAgenda.SignalTypeMapInfo) {

            final MapLocation topLeftCoordinate = signal.location;
            this.mapBoundaryNorth = topLeftCoordinate.y;
            this.mapBoundaryWest = topLeftCoordinate.x;

            final int data = signal.data;
            final int mapWidth = (data & 0x000000FF);
            final int mapHeight = ((data >> 8) & 0x000000FF);
            this.mapBoundaryEast = this.mapBoundaryWest + mapWidth + 1;
            this.mapBoundarySouth = this.mapBoundaryNorth + mapHeight + 1;

        } else if (signal.type == PoliticalAgenda.SignalTypeMapWallNorth) {

            this.mapBoundaryNorth = signal.data;

        } else if (signal.type == PoliticalAgenda.SignalTypeMapWallEast) {

            this.mapBoundaryEast = signal.data;

        } else if (signal.type == PoliticalAgenda.SignalTypeMapWallSouth) {

            this.mapBoundarySouth = signal.data;

        } else if (signal.type == PoliticalAgenda.SignalTypeMapWallWest) {

            this.mapBoundaryWest = signal.data;

        } else if (signal.type == PoliticalAgenda.SignalTypeEnemy) {

            final EnemyInfo enemyInfo = new EnemyInfo(signal);
            this.enemies.add(enemyInfo, enemyInfo.identifier);

        } else if (signal.type == PoliticalAgenda.SignalTypeInformationSynced) {

            this.isInformationSynced = true;
            if (signal.data != 0) {

                this.companionIdentifier = signal.data;

            }

        }

    }

    public boolean shouldRobotTypeProcessSignalType(final RobotType robotType, final int signalType) {

        return true;

    }

    /*
    SIGNAL DISTANCE
     */

    public int broadcastRangeIncurringCooldownPenalty(final double cooldownPenalty, final RobotType type) {

        if (cooldownPenalty <= 0.05) {

            return this.maximumFreeBroadcastRangeForType(type);

        }
        final double x = (cooldownPenalty - 0.05) / 0.03;
        return (int)Math.floor(type.sensorRadiusSquared * (2 + x));

    }

    public int maximumFreeBroadcastRangeForType(final RobotType type) {

        return type.sensorRadiusSquared * 2;

    }

    public int maximumBroadcastRangeForLocation(final MapLocation location) {

        final int mapWidth = this.hasMapWidth() ? this.mapWidth() : 80;
        final int mapHeight = this.hasMapHeight() ? this.mapHeight() : 80;

        int broadcastWidth = 0;
        if (this.mapBoundaryWest != PoliticalAgenda.UnknownValue) {

            final int difference = location.x - this.mapBoundaryWest;
            broadcastWidth = Math.max(difference, mapWidth - difference);

        } else if (this.mapBoundaryEast != PoliticalAgenda.UnknownValue) {

            final int difference = this.mapBoundaryEast - location.x;
            broadcastWidth = Math.max(difference, mapWidth - difference);

        } else {

            broadcastWidth = mapWidth;

        }
        int broadcastHeight = 0;
        if (this.mapBoundaryNorth != PoliticalAgenda.UnknownValue) {

            final int difference = location.y - this.mapBoundaryNorth;
            broadcastHeight = Math.max(difference, mapHeight - difference);

        } else if (this.mapBoundarySouth != PoliticalAgenda.UnknownValue) {

            final int difference = this.mapBoundarySouth - location.y;
            broadcastHeight = Math.max(difference, mapHeight - difference);

        } else {

            broadcastHeight = mapHeight;

        }
        return Math.min(12800 /*failsafe*/, broadcastWidth * broadcastWidth + broadcastHeight * broadcastHeight);

    }

    /*
    SIGNAL GENERATION
     */

    public InformationSignal generateMapInfoInformationSignal() {

        int data = this.mapHeight();
        data <<= 8;
        data += this.mapWidth();

        final InformationSignal informationSignal = new InformationSignal();
        informationSignal.action = PoliticalAgenda.SignalActionWrite;
        informationSignal.data = data;
        informationSignal.location = new MapLocation(this.mapBoundaryWest, this.mapBoundaryNorth);
        informationSignal.type = PoliticalAgenda.SignalTypeMapInfo;
        return informationSignal;

    }

    public InformationSignal generateMapWallInformationSignal(final int wallType, final int value) {

        final InformationSignal informationSignal = new InformationSignal();
        informationSignal.action = PoliticalAgenda.SignalActionWrite;
        informationSignal.data = value;
        informationSignal.type = wallType;
        return informationSignal;

    }

    public InformationSignal generateZombieDenInformationSignal(final MapLocation location) {

        final InformationSignal informationSignal = new InformationSignal();
        informationSignal.action = PoliticalAgenda.SignalActionWrite;
        informationSignal.location = location;
        informationSignal.type = PoliticalAgenda.SignalTypeZombieDen;
        return informationSignal;

    }

    public InformationSignal generateEnemyInformationSignal(final MapLocation location, final RobotType robotType, final int health, final int identifier) {

        final EnemyInfo enemyInfo = new EnemyInfo();
        enemyInfo.location = location;
        enemyInfo.type = robotType;
        enemyInfo.health = health;
        enemyInfo.identifier = identifier;

        final InformationSignal informationSignal = new InformationSignal();
        informationSignal.action = PoliticalAgenda.SignalActionWrite;
        informationSignal.type = PoliticalAgenda.SignalTypeEnemy;
        enemyInfo.fillSignalWithEnemyInfo(informationSignal);
        return informationSignal;

    }

    /*
    SIGNAL INDEXING
     */

    public int getIndexIdentifierForZombieDen(final MapLocation location) {

        return InformationSignal.serializeMapLocation(location);

    }

    /*
    SIGNAL VERIFICATION
     */

    public boolean verifyZombieDenSignal(final InformationSignal signal, final RobotController robotController) throws GameActionException {

        if (!robotController.canSenseLocation(signal.location)) {

            return true;

        }
        final RobotInfo robotInfo = robotController.senseRobotAtLocation(signal.location);
        if (robotInfo != null && robotInfo.type == RobotType.ZOMBIEDEN) {

            return true;

        }
        this.zombieDens.remove(this.getIndexIdentifierForZombieDen(signal.location));
        return false;

    }

}
