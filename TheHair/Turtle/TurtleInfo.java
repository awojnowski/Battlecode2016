package TheHair.Turtle;

import battlecode.common.*;

public class TurtleInfo {

    public int distance = 1;
    public MapLocation location = null;
    public boolean hasLocation = false;

    /*
    SERIALIZATION
     */

    public int serialize() {

        int result = 0;
        result += this.hasLocation ? 1 : 0; // 1 bit
        result <<= 8;
        result += this.distance; // 8 bits
        return result;

    }

    public void fillFromSerializedData(final int serializedData) {

        this.distance = ((serializedData & 0x000000ff) >> 0); // [7-0]
        this.hasLocation = ((serializedData & 0x00000100) >> 8) == 1; // [8-8]

    }

}
