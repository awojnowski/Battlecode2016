package AaronBot3.Information;

import battlecode.common.*;

public class InformationSignal {

    public int broadcastRange = 0;

    public int action = 0;
    public MapLocation location = null;
    public int data = 0;
    public int type = 0;

    public InformationSignal() {

        ;

    }

    public InformationSignal(final int[] serializedSignal) {

        this.location = new MapLocation(
                ((serializedSignal[0] & 0x00fff000) >> 12),
                ((serializedSignal[0] & 0x00000fff) >> 0)
        );
        this.type = ((serializedSignal[1] & 0x0000000f) >> 0);
        this.action = ((serializedSignal[1] & 0x00000010) >> 4);
        this.data = ((serializedSignal[1] & 0xffffffe0) >> 5);

    }

    public int serializedLocation() {

        return InformationSignal.serializeMapLocation(this.location);

    }

    public int serializedData() {

        Integer result = 0;
        result += this.data; // 27 bits
        result <<= 1;
        result += this.action; // 1 bit
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

    public static int serializeMapLocation(final MapLocation mapLocation) {

        if (mapLocation == null) {

            return 0;

        }

        int result = 0;
        result += mapLocation.x;
        result <<= 12;
        result += mapLocation.y;
        return result;

    }

}
