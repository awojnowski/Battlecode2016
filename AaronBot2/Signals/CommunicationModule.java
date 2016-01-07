package AaronBot2.Signals;

import battlecode.common.*;
import battlecode.world.signal.BroadcastSignal;

import java.util.ArrayList;
import java.util.Hashtable;

public class CommunicationModule {

    public static final int MaximumBroadcastRange = 20000;

    // these are the communications received
    // the hashtable is indexed by an Integer which represents the location
    public final Hashtable<Integer, CommunicationModuleSignal> communications = new Hashtable<Integer, CommunicationModuleSignal>();

    public CommunicationModule() {

        ;

    }

    /*
    BROADCASTING
     */

    public void broadcastSignal(final CommunicationModuleSignal signal, final RobotController robotController, final int broadcastRange) throws GameActionException {

        final int[] message = signal.serialize();
        robotController.broadcastMessageSignal(message[0], message[1], broadcastRange);

    }

    public void broadcastSignal(final RobotController robotController, final int broadcastRange) throws GameActionException {

        robotController.broadcastSignal(broadcastRange);

    }

    /*
    RANGES
     */

    public static int maximumFreeBroadcastRangeForRobotType(RobotType type) {

        return type.sensorRadiusSquared * 2;

    }

    /*
    RECEIVING
     */

    public void processIncomingSignals(final RobotController robotController) {

        final Signal[] signals = robotController.emptySignalQueue();
        final Team currentTeam = robotController.getTeam();

        for (int i = 0; i < signals.length; i++) {

            final Signal signal = signals[i];
            if (signal.getTeam() != currentTeam) {

                continue;

            }
            final int[] message = signal.getMessage();
            if (message.length < 2) {

                continue;

            }
            final CommunicationModuleSignal communicationModuleSignal = new CommunicationModuleSignal(message);
            this.communications.put(communicationModuleSignal.serializedLocation(), communicationModuleSignal);

        }

    }

}
