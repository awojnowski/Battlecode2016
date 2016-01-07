package AaronBot2.Signals;

import battlecode.common.*;

import java.util.*;

public class CommunicationModule {

    public static final int MaximumBroadcastRange = 20000;

    // these are the communications received
    // the hashtable is indexed by an Integer which represents the location
    public final Hashtable<Integer, CommunicationModuleSignal> communications = new Hashtable<Integer, CommunicationModuleSignal>();

    // contains signals without a message associated with them, received last time the queue was cleared
    public final ArrayList<Signal> notifications = new ArrayList<Signal>();

    public interface Delegate {

        public boolean shouldProcessSignalType(final int signalType);


    }
    public CommunicationModule.Delegate delegate = null;

    public CommunicationModule() {

        ;

    }

    /*
    BROADCASTING
     */

    public void broadcastSignal(final CommunicationModuleSignal communicationModuleSignal, final RobotController robotController, final int broadcastRange) throws GameActionException {

        final int[] message = communicationModuleSignal.serialize();
        robotController.broadcastMessageSignal(message[0], message[1], broadcastRange);
        this.writeSignal(communicationModuleSignal);

    }

    public void broadcastSignal(final RobotController robotController, final int broadcastRange) throws GameActionException {

        robotController.broadcastSignal(broadcastRange);

    }

    /*
    COMMUNICATIONS INDEXING
     */

    public static int communicationsIndexFromLocation(final MapLocation mapLocation) {

        return CommunicationModuleSignal.serializeMapLocation(mapLocation);

    }

    /*
    RANGES
     */

    public static int maximumFreeBroadcastRangeForRobotType(final RobotType type) {

        return type.sensorRadiusSquared * 2;

    }

    /*
    RECEIVING
     */

    public void processIncomingSignals(final RobotController robotController) {

        this.notifications.clear();

        final Signal[] signals = robotController.emptySignalQueue();
        if (signals.length == 0) {

            return;

        }

        final Team currentTeam = robotController.getTeam();

        for (int i = 0; i < signals.length; i++) {

            final Signal signal = signals[i];
            if (signal.getTeam() != currentTeam) {

                continue;

            }

            final int[] message = signal.getMessage();
            if (message.length < 2) {

                this.notifications.add(signal);
                continue;

            }

            final CommunicationModuleSignal communicationModuleSignal = new CommunicationModuleSignal(message);
            if (this.delegate != null) {

                if (!this.delegate.shouldProcessSignalType(communicationModuleSignal.type)) {

                    continue;

                }

            }
            if (communicationModuleSignal.action == CommunicationModuleSignal.ACTION_DELETE) {

                this.clearSignal(communicationModuleSignal);

            } else {

                this.writeSignal(communicationModuleSignal);

            }

        }

    }

    public void writeSignal(final CommunicationModuleSignal communicationModuleSignal) {

        this.communications.put(communicationModuleSignal.serializedLocation(), communicationModuleSignal);

    }

    public void clearSignal(final CommunicationModuleSignal communicationModuleSignal) {

        this.communications.remove(communicationModuleSignal.serializedLocation());

    }

}
