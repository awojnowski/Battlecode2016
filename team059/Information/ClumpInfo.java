package team059.Information;

import battlecode.common.*;

public class ClumpInfo {

    public MapLocation location = null;
    public int turn = 0;

    public ClumpInfo() {

        ;

    }

    public ClumpInfo(final InformationSignal signal) {

        this.location = signal.location;

    }

}
