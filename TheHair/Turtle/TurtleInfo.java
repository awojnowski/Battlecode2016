package TheHair.Turtle;

import battlecode.common.*;

public class TurtleInfo {

    public static int DistanceTurnChangeLockTurnsCount = 15;

    public static int StatusNone = 0;
    public static int StatusSiteClearance = 1;
    public static int StatusSiteStaging = 2;
    public static int StatusSiteEstablished = 3;

    public int distance = 1;
    public int distanceTurnLock = TurtleInfo.DistanceTurnChangeLockTurnsCount;
    public MapLocation location = null;
    public boolean hasLocation = false;
    public int status = TurtleInfo.StatusNone;

    /*
    SERIALIZATION
     */

    public int serialize() {

        int result = 0;
        result += this.status; // 3 bits
        result <<= 1;
        result += this.hasLocation ? 1 : 0; // 1 bit
        result <<= 8;
        result += this.distance > 255 ? 255 : this.distance; // 8 bits

        return result;

    }

    public void fillFromSerializedData(final int serializedData) {

        final int distanceBefore = this.distance;

        this.distance = ((serializedData & 0x000000ff) >> 0); // [7-0]
        this.hasLocation = ((serializedData & 0x00000100) >> 8) == 1; // [8-8]
        this.status = ((serializedData & 0x00000E00) >> 9); // [11-9]

        if (this.distance != distanceBefore) {

            this.lockDistanceTurns();

        }

    }

    public void lockDistanceTurns() {

        this.distanceTurnLock = TurtleInfo.DistanceTurnChangeLockTurnsCount;

    }

}
