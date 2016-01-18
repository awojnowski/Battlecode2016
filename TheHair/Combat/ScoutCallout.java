package TheHair.Combat;

import battlecode.common.*;

public class ScoutCallout {

    public MapLocation location = null;
    public int remainingHealth = 0;
    public RobotType robotType = RobotType.ARCHON;

    public int serialize() {

        int result = 0;
        result += remainingHealth; // 10 bits
        result <<= 5;
        result += robotType.ordinal();
        return result;

    }

    public void fillFromSerializedData(final int serializedData) {

        this.robotType = RobotType.values()[((serializedData & 0x0000001f) >> 0)]; // [4-0]
        this.remainingHealth = ((serializedData & 0x00003FE0) >> 0); // [14-5]

    }

}
