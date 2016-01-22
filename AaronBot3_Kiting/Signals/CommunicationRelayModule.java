package AaronBot3_Kiting.Signals;

import battlecode.common.RobotType;

public class CommunicationRelayModule {

    public static boolean shouldRelaySignalTypeToRobotType(int signalType, RobotType robotType) {

        if (signalType == CommunicationModuleSignal.TYPE_NEUTRAL_ROBOT) {

            if (robotType != RobotType.ARCHON && robotType != RobotType.SCOUT) {

                return false;

            }

        } else if (signalType == CommunicationModuleSignal.TYPE_SPARE_PARTS) {

            if (robotType != RobotType.ARCHON && robotType != RobotType.SCOUT) {

                return false;

            }

        }
        return true;

    }

}
