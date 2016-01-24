package team059.Information;

import battlecode.common.*;

public class EnemyInfo {

    public MapLocation location = null;
    public RobotType type = null;
    public int identifier = 0;
    public int health = 0;

    public EnemyInfo() {

        ;

    }

    public EnemyInfo(final InformationSignal signal) {

        this.location = signal.location;
        this.type = RobotType.values()[((signal.data & 0x0000000f) >> 0)]; // 4 bits
        this.health = ((signal.data & 0x00003ff0) >> 4);                   // 10 bits
        this.identifier = ((signal.data & 0x07ffc000) >> 14);              // 13 bits

    }

    public void fillSignalWithEnemyInfo(final InformationSignal signal) {

        signal.location = this.location;

        int data = 0;
        data += this.identifier;
        data <<= 10;
        data += this.health;
        data <<= 4;
        data += this.type.ordinal();
        signal.data = data;

    }

}
