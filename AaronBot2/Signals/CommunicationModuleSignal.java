package AaronBot2.Signals;

import battlecode.common.MapLocation;

public class CommunicationModuleSignal {

    public MapLocation location;
    public int robotIdentifier;

    public static final int LocationNormalizationIncrement = 16000; // locations are [-16000, 16000] so we must normalize to [0, 32000]

    public CommunicationModuleSignal() {

        ;

    }

    public CommunicationModuleSignal(final int[] serializedSignal) {

        this.location = new MapLocation(
                (serializedSignal[0] >> 16) - CommunicationModuleSignal.LocationNormalizationIncrement,
                (serializedSignal[0] & 0x0000ffff) - CommunicationModuleSignal.LocationNormalizationIncrement
        ); // ([31-16], [15-0])
        this.robotIdentifier = serializedSignal[1] & 0x00007fff; // [14-0]

    }

    public int serializedLocation() {

        final MapLocation location = this.location;

        Integer result = location.x + CommunicationModuleSignal.LocationNormalizationIncrement; // 16 bits
        result <<= 16;
        result += location.y + CommunicationModuleSignal.LocationNormalizationIncrement; // 16 bits
        return result;

    }

    public int serializedData() {

        Integer result = this.robotIdentifier; // 15 bits
        return result;

    }

    public int[] serialize() {

        return new int[]{ this.serializedLocation(), this.serializedData() };

    }

}