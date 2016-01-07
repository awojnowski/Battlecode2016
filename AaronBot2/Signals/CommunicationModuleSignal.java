package AaronBot2.Signals;

import battlecode.common.*;

public class CommunicationModuleSignal {

    public static final int LocationNormalizationIncrement = 16000; // locations are [-16000, 16000] so we must normalize to [0, 32000]

    public static final int ACTION_NONE = 0;
    public static final int ACTION_SEEN = 1;
    public static final int ACTION_DELETE = 2;

    public static final int TYPE_NONE = 0;
    public static final int TYPE_ZOMBIEDEN = 1;
    public static final int TYPE_ORE = 2;
    public static final int TYPE_ENEMY_ARCHON = 3;
    public static final int TYPE_MAP_CORNER = 4;

    public int action = CommunicationModuleSignal.ACTION_NONE;
    public MapLocation location;
    public int robotIdentifier;
    public int type = CommunicationModuleSignal.TYPE_NONE;

    public CommunicationModuleSignal() {

        ;

    }

    public CommunicationModuleSignal(final int[] serializedSignal) {

        this.location = new MapLocation(
                ((serializedSignal[0] & 0xffff0000) >> 16) - CommunicationModuleSignal.LocationNormalizationIncrement,
                ((serializedSignal[0] & 0x0000ffff) >> 0) - CommunicationModuleSignal.LocationNormalizationIncrement
        ); // ([31-16], [15-0])

        this.action = ((serializedSignal[1] & 0x000000f0) >> 4); // [7-4]
        this.robotIdentifier = ((serializedSignal[1] & 0x007fff00) >> 8); // [22-8]
        this.type = ((serializedSignal[1] & 0x0000000f) >> 0); // [3-0]

    }

    public int serializedLocation() {

        return CommunicationModuleSignal.serializeMapLocation(this.location);

    }

    public int serializedData() {

        Integer result = 0;
        result += this.robotIdentifier; // 15 bits
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

        int result = 0;
        result += mapLocation.x + CommunicationModuleSignal.LocationNormalizationIncrement; // 16 bits
        result <<= 16;
        result += mapLocation.y + CommunicationModuleSignal.LocationNormalizationIncrement; // 16 bits
        return result;

    }

}