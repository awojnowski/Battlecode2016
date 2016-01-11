package AaronBot2.Parts;

import battlecode.common.*;
import java.util.ArrayList;

public class PartsModule {

    public class Result {
        public boolean allPositionsScanned = true;
        public ArrayList<MapLocation> locations = new ArrayList<MapLocation>();
    }
    public PartsModule.Result getPartsNearby(final MapLocation location, final RobotController robotController, final int partsScanRadius) throws GameActionException {

        final PartsModule.Result result = new PartsModule.Result();
        for (int i = -partsScanRadius; i <= partsScanRadius; i++) {

            for (int j = -partsScanRadius; j <= partsScanRadius; j++) {

                final MapLocation scanLocation = new MapLocation(location.x + i, location.y + j);
                if (!robotController.canSenseLocation(scanLocation)) {

                    result.allPositionsScanned = false;
                    continue;

                }
                if (!robotController.onTheMap(scanLocation)) {

                    continue;

                }
                final double totalParts = robotController.senseParts(scanLocation);
                if (totalParts > 0) {

                    result.locations.add(scanLocation);

                }

            }

        }
        return result;

    }

}
