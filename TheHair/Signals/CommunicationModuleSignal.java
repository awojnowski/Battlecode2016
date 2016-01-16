package TheHair.Signals;

import battlecode.common.*;

public class CommunicationModuleSignal {

    public static final int LocationNormalizationIncrement = 16000; // locations are [-16000, 16000] so we must normalize to [0, 32000]

    public static final int ACTION_NONE = 0;
    public static final int ACTION_SEEN = 1;
    public static final int ACTION_DELETE = 2;
    public static final int ACTION_INITIAL_UPDATE_COMPLETE = 3;

    public static final int TYPE_NONE = 0;
    public static final int TYPE_ZOMBIEDEN = 1;
    public static final int TYPE_ENEMY_ARCHON = 2;
    public static final int TYPE_MAP_INFO = 3;
    public static final int TYPE_MAP_WALL_EAST = 4;
    public static final int TYPE_MAP_WALL_NORTH = 5;
    public static final int TYPE_MAP_WALL_WEST = 6;
    public static final int TYPE_MAP_WALL_SOUTH = 7;
    public static final int TYPE_TURTLE_LOCATION = 8;

    public int action = CommunicationModuleSignal.ACTION_NONE;
    public MapLocation location;
    public int data;
    public int type = CommunicationModuleSignal.TYPE_NONE;

    public CommunicationModuleSignal() {

        ;

    }

    public CommunicationModuleSignal(final int[] serializedSignal) {

        this.location = new MapLocation(
                ((serializedSignal[0] & 0xffff0000) >> 16) - CommunicationModuleSignal.LocationNormalizationIncrement,
                ((serializedSignal[0] & 0x0000ffff) >> 0) - CommunicationModuleSignal.LocationNormalizationIncrement
        ); // ([31-16], [15-0])

        this.type = ((serializedSignal[1] & 0x0000000f) >> 0); // [3-0]
        this.action = ((serializedSignal[1] & 0x000000f0) >> 4); // [7-4]
        this.data = ((serializedSignal[1] & 0xffffff00) >> 8); // [31-8]

    }

    public int serializedLocation() {

        return CommunicationModuleSignal.serializeMapLocation(this.location);

    }

    public int serializedData() {

        Integer result = 0;
        result += this.data; // 24 bits
        result <<= 4;
        result += this.action; // 4 bits
        result <<= 4;
        result += this.type; // 4 bits
        return result;

    }

    public int[] serialize() {

        return new int[]{ this.serializedLocation(), this.serializedData() };

    }

    /*
    HELPER METHODS
     */

    public static int serializeMapLocation(MapLocation mapLocation) {

        if (mapLocation == null) {

            return 0;

        }

        int result = 0;
        result += mapLocation.x + CommunicationModuleSignal.LocationNormalizationIncrement; // 16 bits
        result <<= 16;
        result += mapLocation.y + CommunicationModuleSignal.LocationNormalizationIncrement; // 16 bits
        return result;

    }

}
