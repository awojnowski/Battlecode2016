package TheHair.Signals;

import TheHair.Combat.*;
import TheHair.Map.*;
import TheHair.Turtle.TurtleInfo;
import battlecode.common.*;
import java.util.*;

public class CommunicationModule implements CommunicationModuleDelegate {

    public static final int DefaultApproximateNearbyLocationRange = 64;
    public static final int ApproximateNearbyPartsLocationRadius = 3;
    public static final int ApproximateNearbyPartsLocationRange = ApproximateNearbyPartsLocationRadius * ApproximateNearbyPartsLocationRadius;

    // these are the hashtables managing the received communications
    // the hashtables are indexed by an Integer which represents the location
    public final Hashtable<Integer, CommunicationModuleSignal> enemyArchons = new Hashtable<Integer, CommunicationModuleSignal>();
    public final Hashtable<Integer, CommunicationModuleSignal> zombieDens = new Hashtable<Integer, CommunicationModuleSignal>();
    public final Hashtable<Integer, ScoutCallout> scoutCallouts = new Hashtable<Integer, ScoutCallout>(); // reset every turn

    // contains signals without a message associated with them, received last time the queue was cleared
    public final ArrayList<Signal> notifications = new ArrayList<Signal>();

    // signal queue
    public final ArrayList<CommunicationModuleSignal> communicationModuleSignalQueue = new ArrayList<CommunicationModuleSignal>();

    public MapInfoModule mapInfoModule = null;
    public CommunicationModuleDelegate delegate = this;
    public boolean initialInformationReceived = false;
    public TurtleInfo turtleInfo = new TurtleInfo();

    public CommunicationModule(final MapInfoModule mapInfoModule) {

        this.mapInfoModule = mapInfoModule;

    }

    /*
    BROADCASTING
     */

    public void broadcastSignal(final CommunicationModuleSignal communicationModuleSignal, final RobotController robotController, final int broadcastRange) throws GameActionException {

        final int[] message = communicationModuleSignal.serialize();
        robotController.broadcastMessageSignal(message[0], message[1], broadcastRange);
        this.processSignal(communicationModuleSignal);

    }

    public void broadcastSignal(final RobotController robotController, final int broadcastRange) throws GameActionException {

        robotController.broadcastSignal(broadcastRange);

    }

    public boolean hasEnqueuedSignalsForBroadcast() {

        return this.communicationModuleSignalQueue.size() > 0;

    }

    public void enqueueSignalForBroadcast(final CommunicationModuleSignal communicationModuleSignal) {

        this.communicationModuleSignalQueue.add(communicationModuleSignal);

    }

    public void broadcastEnqueuedSignals(final RobotController robotController, final int broadcastRange) throws GameActionException {

        for (int i = 0; i < this.communicationModuleSignalQueue.size(); i++) {

            this.broadcastSignal(this.communicationModuleSignalQueue.get(i), robotController, broadcastRange);

        }
        this.communicationModuleSignalQueue.clear();

    }

    /*
    COMMUNICATIONS INDEXING
     */

    public static int communicationsIndexFromLocation(final MapLocation mapLocation) {

        return CommunicationModuleSignal.serializeMapLocation(mapLocation);

    }

    /*
    DELEGATE
     */

    public boolean shouldProcessSignalType(final int signalType) {

        return true;

    }

    /*
    INFORMATION VERIFICATION
     */

    public ArrayList<CommunicationModuleSignal> verifyCommunicationsInformation(final RobotController robotController, RobotInfo[] enemies, final boolean broadcastInformation) throws GameActionException {

        final ArrayList<CommunicationModuleSignal> signals = new ArrayList<CommunicationModuleSignal>();

        if (enemies == null) {

            enemies = robotController.senseHostileRobots(robotController.getLocation(), robotController.getType().sensorRadiusSquared);

        }

        // verify the archons

        final Enumeration<CommunicationModuleSignal> enemyArchons = this.enemyArchons.elements();
        while (enemyArchons.hasMoreElements()) {

            final CommunicationModuleSignal communicationModuleSignal = enemyArchons.nextElement();
            if (!this.verifyEnemyArchonCommunicationModuleSignal(communicationModuleSignal, robotController, enemies)) {

                if (broadcastInformation) {

                    communicationModuleSignal.action = CommunicationModuleSignal.ACTION_DELETE;
                    signals.add(communicationModuleSignal);

                }
                this.clearSignal(communicationModuleSignal, this.enemyArchons);

            }

        }

        // verify the zombie dens

        final Enumeration<CommunicationModuleSignal> zombieDens = this.zombieDens.elements();
        while (zombieDens.hasMoreElements()) {

            final CommunicationModuleSignal communicationModuleSignal = zombieDens.nextElement();
            if (!this.verifyZombieDenCommunicationModuleSignal(communicationModuleSignal, robotController)) {

                if (broadcastInformation) {

                    communicationModuleSignal.action = CommunicationModuleSignal.ACTION_DELETE;
                    signals.add(communicationModuleSignal);

                }
                this.clearSignal(communicationModuleSignal, this.zombieDens);

            }

        }

        return signals;

    }

    public boolean verifyZombieDenCommunicationModuleSignal(final CommunicationModuleSignal communicationModuleSignal, final RobotController robotController) throws GameActionException {

        if (!robotController.canSenseLocation(communicationModuleSignal.location)) {

            return true;

        }

        final RobotInfo robotInfo = robotController.senseRobotAtLocation(communicationModuleSignal.location);
        if (robotInfo == null || robotInfo.type != RobotType.ZOMBIEDEN) {

            return false;

        }
        return true;

    }

    public boolean verifySparePartsCommunicationModuleSignal(final CommunicationModuleSignal communicationModuleSignal, final RobotController robotController) throws GameActionException {

        if (!robotController.canSenseLocation(communicationModuleSignal.location)) {

            return true;

        }

        final int distance = robotController.getLocation().distanceSquaredTo(communicationModuleSignal.location);
        if (distance + CommunicationModule.ApproximateNearbyPartsLocationRange > robotController.getType().sensorRadiusSquared) {

            return true;

        }

        final MapLocation[] mapLocations = robotController.sensePartLocations(-1);
        if (mapLocations.length == 0) {

            return false;

        }
        return true;

    }

    public boolean verifyEnemyArchonCommunicationModuleSignal(final CommunicationModuleSignal communicationModuleSignal, final RobotController robotController, final RobotInfo[] enemies) throws GameActionException {

        if (robotController.getLocation().distanceSquaredTo(communicationModuleSignal.location) > robotController.getType().sensorRadiusSquared) {

            return true;

        }

        for (int i = 0; i < enemies.length; i++) {

            final RobotInfo enemy = enemies[i];
            if (enemy.ID == communicationModuleSignal.data) {

                if (communicationModuleSignal.location != enemy.location) {

                    this.clearSignal(communicationModuleSignal, this.enemyArchons);
                    communicationModuleSignal.location = enemy.location;
                    this.processSignal(communicationModuleSignal);

                }
                return true;

            }

        }

        final int distance = robotController.getLocation().distanceSquaredTo(communicationModuleSignal.location);
        if (distance + 16 > robotController.getType().sensorRadiusSquared) {

            return true;

        }

        return false;

    }

    /*
    RANGES
     */

    public static int maximumFreeBroadcastRangeForRobotType(final RobotType type) {

        return type.sensorRadiusSquared * 2;

    }

    public static int maximumBroadcastRange(final MapInfoModule mapInfoModule) {

        if (!mapInfoModule.hasAllBoundaries()) {

            return 12800;

        } else {

            final int mapWidth = mapInfoModule.mapWidth();
            final int mapHeight = mapInfoModule.mapHeight();
            return mapWidth * mapWidth + mapHeight * mapHeight;

        }

    }

    /*
    RECEIVING
     */

    public void processIncomingSignals(final RobotController robotController) {

        this.scoutCallouts.clear();
        this.notifications.clear();

        final Signal[] signals = robotController.emptySignalQueue();
        if (signals.length == 0) {

            return;

        }

        final Team currentTeam = robotController.getTeam();

        for (int i = 0; i < signals.length; i++) {

            final Signal signal = signals[i];
            if (signal.getTeam() != currentTeam) {

                continue;

            }

            final int[] message = signal.getMessage();
            if (message == null || message.length < 2) {

                this.notifications.add(signal);
                continue;

            }

            final CommunicationModuleSignal communicationModuleSignal = new CommunicationModuleSignal(message);
            if (!this.delegate.shouldProcessSignalType(communicationModuleSignal.type)) {

                continue;

            }
            this.processSignal(communicationModuleSignal);

        }

    }

    public void processSignal(final CommunicationModuleSignal communicationModuleSignal) {

        if (communicationModuleSignal.action == CommunicationModuleSignal.ACTION_INITIAL_UPDATE_COMPLETE) {

            this.initialInformationReceived = true;
            return;

        }
        if (communicationModuleSignal.type == CommunicationModuleSignal.TYPE_MAP_INFO ||
                communicationModuleSignal.type == CommunicationModuleSignal.TYPE_MAP_WALL_EAST ||
                communicationModuleSignal.type == CommunicationModuleSignal.TYPE_MAP_WALL_NORTH ||
                communicationModuleSignal.type == CommunicationModuleSignal.TYPE_MAP_WALL_WEST ||
                communicationModuleSignal.type == CommunicationModuleSignal.TYPE_MAP_WALL_SOUTH) {

            this.mapInfoModule.fillDataFromCommunicationModuleSignal(communicationModuleSignal);
            return;

        }
        if (communicationModuleSignal.type == CommunicationModuleSignal.TYPE_TURTLE_INFO) {

            this.turtleInfo.location = communicationModuleSignal.location;
            this.turtleInfo.fillFromSerializedData(communicationModuleSignal.data);
            return;

        }
        if (communicationModuleSignal.type == CommunicationModuleSignal.TYPE_SCOUT_CALLOUT) {

            final ScoutCallout scoutCallout = new ScoutCallout();
            scoutCallout.fillFromSerializedData(communicationModuleSignal.data);
            scoutCallout.location = communicationModuleSignal.location;
            this.scoutCallouts.put(communicationModuleSignal.serializedLocation(), scoutCallout);

            return;

        }

        Hashtable<Integer, CommunicationModuleSignal> hashtable = this.getHashtableForSignalType(communicationModuleSignal.type);
        if (hashtable == null) {

            return;

        }

        if (communicationModuleSignal.action == CommunicationModuleSignal.ACTION_DELETE) {

            this.clearSignal(communicationModuleSignal, hashtable);

        } else {

            this.writeSignal(communicationModuleSignal, hashtable);

        }

    }

    private Hashtable<Integer, CommunicationModuleSignal> getHashtableForSignalType(int signalType) {

        if (signalType == CommunicationModuleSignal.TYPE_ENEMY_ARCHON) {

            return this.enemyArchons;

        }
        if (signalType == CommunicationModuleSignal.TYPE_ZOMBIEDEN) {

            return this.zombieDens;

        }
        return null;

    }

    private void writeSignal(final CommunicationModuleSignal communicationModuleSignal, final Hashtable<Integer, CommunicationModuleSignal> hashtable) {

        hashtable.put(communicationModuleSignal.serializedLocation(), communicationModuleSignal);

    }

    public void clearSignal(final CommunicationModuleSignal communicationModuleSignal, final Hashtable<Integer, CommunicationModuleSignal> hashtable) {

        hashtable.remove(communicationModuleSignal.serializedLocation());
        for (int i = 0; i < this.communicationModuleSignalQueue.size(); i++) {

            final CommunicationModuleSignal signal = this.communicationModuleSignalQueue.get(i);
            if (signal.type == communicationModuleSignal.type && signal.location == communicationModuleSignal.location) {

                this.communicationModuleSignalQueue.remove(i);
                i--;

            }

        }

    }

    /*
    SIGNALS
     */

    public CommunicationModuleSignalCollection allCommunicationModuleSignals() {

        // archons

        final CommunicationModuleSignalCollection communicationModuleSignalCollection = new CommunicationModuleSignalCollection();
        if (this.delegate.shouldProcessSignalType(CommunicationModuleSignal.TYPE_ENEMY_ARCHON)) {

            communicationModuleSignalCollection.addEnumeration(this.enemyArchons.elements());

        }

        // zombies

        if (this.delegate.shouldProcessSignalType(CommunicationModuleSignal.TYPE_ZOMBIEDEN)) {

            communicationModuleSignalCollection.addEnumeration(this.zombieDens.elements());

        }

        // map info

        final ArrayList<CommunicationModuleSignal> mapInfoSignals = new ArrayList<CommunicationModuleSignal>();
        if (this.mapInfoModule.hasAllBoundaries()) {

            final CommunicationModuleSignal signal = new CommunicationModuleSignal();
            signal.action = CommunicationModuleSignal.ACTION_SEEN;
            this.mapInfoModule.fillCommunicationModuleSignalWithMapSizeData(signal);
            mapInfoSignals.add(signal);

        } else {

            if (this.mapInfoModule.eastBoundaryValue != MapInfoModule.UnknownValue) {

                final CommunicationModuleSignal signal = new CommunicationModuleSignal();
                signal.action = CommunicationModuleSignal.ACTION_SEEN;
                signal.type = CommunicationModuleSignal.TYPE_MAP_WALL_EAST;
                signal.data = this.mapInfoModule.eastBoundaryValue;
                mapInfoSignals.add(signal);

            }
            if (this.mapInfoModule.northBoundaryValue != MapInfoModule.UnknownValue) {

                final CommunicationModuleSignal signal = new CommunicationModuleSignal();
                signal.action = CommunicationModuleSignal.ACTION_SEEN;
                signal.type = CommunicationModuleSignal.TYPE_MAP_WALL_NORTH;
                signal.data = this.mapInfoModule.northBoundaryValue;
                mapInfoSignals.add(signal);

            }
            if (this.mapInfoModule.westBoundaryValue != MapInfoModule.UnknownValue) {

                final CommunicationModuleSignal signal = new CommunicationModuleSignal();
                signal.action = CommunicationModuleSignal.ACTION_SEEN;
                signal.type = CommunicationModuleSignal.TYPE_MAP_WALL_WEST;
                signal.data = this.mapInfoModule.westBoundaryValue;
                mapInfoSignals.add(signal);

            }
            if (this.mapInfoModule.southBoundaryValue != MapInfoModule.UnknownValue) {

                final CommunicationModuleSignal signal = new CommunicationModuleSignal();
                signal.action = CommunicationModuleSignal.ACTION_SEEN;
                signal.type = CommunicationModuleSignal.TYPE_MAP_WALL_SOUTH;
                signal.data = this.mapInfoModule.southBoundaryValue;
                mapInfoSignals.add(signal);

            }

        }
        communicationModuleSignalCollection.addEnumeration(Collections.enumeration(mapInfoSignals));

        // turtle info

        final ArrayList<CommunicationModuleSignal> turtleSignals = new ArrayList<CommunicationModuleSignal>();
        {
            final CommunicationModuleSignal signal = new CommunicationModuleSignal();
            signal.action = CommunicationModuleSignal.ACTION_SEEN;
            signal.type = CommunicationModuleSignal.TYPE_TURTLE_INFO;
            signal.data = this.turtleInfo.serialize();
            signal.location = this.turtleInfo.location;
            turtleSignals.add(signal);
        }
        communicationModuleSignalCollection.addEnumeration(Collections.enumeration(turtleSignals));

        // all done!

        return communicationModuleSignalCollection;

    }

    public ArrayList<CommunicationModuleSignal> getCommunicationModuleSignalsNearbyLocation(final Hashtable<Integer, CommunicationModuleSignal> hashtable, final MapLocation location, final int range) {

        final ArrayList<CommunicationModuleSignal> results = new ArrayList<CommunicationModuleSignal>();
        final Enumeration<CommunicationModuleSignal> communicationModuleSignals = hashtable.elements();
        while (communicationModuleSignals.hasMoreElements()) {

            final CommunicationModuleSignal signal = communicationModuleSignals.nextElement();
            final int distance = signal.location.distanceSquaredTo(location);
            if (distance < range) {

                results.add(signal);

            }

        }
        return results;

    }

    /*
    NOTIFICATIONS
     */

    public MapLocation closestNotificationLocation(final RobotController robotController) {

        int minDistance = Integer.MAX_VALUE;
        MapLocation closestLocation = null;
        MapLocation currentLocation = robotController.getLocation();

        for (int i = 0; i < notifications.size(); i++) {

            final Signal currentSignal = notifications.get(i);
            final int distance = currentLocation.distanceSquaredTo(currentSignal.getLocation());

            if (distance < minDistance) {

                minDistance = distance;
                closestLocation = currentSignal.getLocation();

            }

        }

        return closestLocation;

    }

}
