package AaronBot2.Signals;

import battlecode.common.*;

import java.util.*;

public class CommunicationModule implements CommunicationModuleDelegate {

    public static final int MaximumBroadcastRange = 20000;

    // these are the hashtables managing the received communications
    // the hashtables are indexed by an Integer which represents the location
    public final Hashtable<Integer, CommunicationModuleSignal> enemyArchons = new Hashtable<Integer, CommunicationModuleSignal>();
    public final Hashtable<Integer, CommunicationModuleSignal> zombieDens = new Hashtable<Integer, CommunicationModuleSignal>();

    // contains signals without a message associated with them, received last time the queue was cleared
    public final ArrayList<Signal> notifications = new ArrayList<Signal>();

    public CommunicationModuleDelegate delegate = this;
    public boolean initialInformationReceived = false;

    public CommunicationModule() {

        ;

    }

    /*
    BROADCASTING
     */

    public void broadcastSignal(final CommunicationModuleSignal communicationModuleSignal, final RobotController robotController, final int broadcastRange) throws GameActionException {

        final int[] message = communicationModuleSignal.serialize();
        robotController.broadcastMessageSignal(message[0], message[1], broadcastRange);
        this.processSignal(communicationModuleSignal);

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
    DELEGATE
     */

    public boolean shouldProcessSignalType(final int signalType) {

        return true;

    }

    /*
    INFORMATION VERIFICATION
     */

    public void verifyCommunicationsInformation(final RobotController robotController, boolean broadcastInformation) throws GameActionException {

        final Team team = robotController.getTeam();

        // verify the zombie dens

        final Enumeration<CommunicationModuleSignal> zombieDens = this.zombieDens.elements();
        while (zombieDens.hasMoreElements()) {

            final CommunicationModuleSignal communicationModuleSignal = zombieDens.nextElement();
            if (!robotController.canSenseLocation(communicationModuleSignal.location)) {

                continue;

            }

            final RobotInfo robotInfo = robotController.senseRobotAtLocation(communicationModuleSignal.location);
            if (robotInfo == null || robotInfo.type != RobotType.ZOMBIEDEN) {

                communicationModuleSignal.action = CommunicationModuleSignal.ACTION_DELETE;
                if (broadcastInformation) {

                    this.broadcastSignal(communicationModuleSignal, robotController, CommunicationModule.MaximumBroadcastRange);

                } else {

                    this.clearSignal(communicationModuleSignal, this.zombieDens);

                }

            }

        }

        // verify the archons

        final Enumeration<CommunicationModuleSignal> enemyArchons = this.enemyArchons.elements();
        while (enemyArchons.hasMoreElements()) {

            final CommunicationModuleSignal communicationModuleSignal = enemyArchons.nextElement();
            if (!robotController.canSenseLocation(communicationModuleSignal.location)) {

                continue;

            }

            final RobotInfo robotInfo = robotController.senseRobotAtLocation(communicationModuleSignal.location);
            if (robotInfo == null || robotInfo.type != RobotType.ARCHON || robotInfo.team == team) {

                communicationModuleSignal.action = CommunicationModuleSignal.ACTION_DELETE;
                if (broadcastInformation) {

                    this.broadcastSignal(communicationModuleSignal, robotController, CommunicationModule.MaximumBroadcastRange);

                } else {

                    this.clearSignal(communicationModuleSignal, this.enemyArchons);

                }

            }

        }

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
            if (!this.delegate.shouldProcessSignalType(communicationModuleSignal.type)) {

                continue;

            }
            this.processSignal(communicationModuleSignal);

        }

    }

    public void processSignal(final CommunicationModuleSignal communicationModuleSignal) {

        if (communicationModuleSignal.action == CommunicationModuleSignal.ACTION_INITIAL_UPDATE_COMPLETE) {

            this.initialInformationReceived = true;

        }

        Hashtable<Integer, CommunicationModuleSignal> hashtable = this.getHashtableForSignalType(communicationModuleSignal.type);
        if (hashtable == null) {

            return;

        }

        if (communicationModuleSignal.action == CommunicationModuleSignal.ACTION_DELETE) {

            this.clearSignal(communicationModuleSignal, hashtable);

        } else {

            this.writeSignal(communicationModuleSignal, hashtable);

        }

    }

    private Hashtable<Integer, CommunicationModuleSignal> getHashtableForSignalType(int signalType) {

        if (signalType == CommunicationModuleSignal.TYPE_ENEMY_ARCHON) {

            return this.enemyArchons;

        }
        if (signalType == CommunicationModuleSignal.TYPE_ZOMBIEDEN) {

            return this.zombieDens;

        }
        return null;

    }

    private void writeSignal(final CommunicationModuleSignal communicationModuleSignal, final Hashtable<Integer, CommunicationModuleSignal> hashtable) {

        hashtable.put(communicationModuleSignal.serializedLocation(), communicationModuleSignal);

    }

    private void clearSignal(final CommunicationModuleSignal communicationModuleSignal, final Hashtable<Integer, CommunicationModuleSignal> hashtable) {

        hashtable.remove(communicationModuleSignal.serializedLocation());

    }

    /*
    SIGNALS
     */

    public CommunicationModuleSignalCollection allCommunicationModuleSignals() {

        final CommunicationModuleSignalCollection communicationModuleSignalCollection = new CommunicationModuleSignalCollection();
        if (this.delegate.shouldProcessSignalType(CommunicationModuleSignal.TYPE_ENEMY_ARCHON)) {

            communicationModuleSignalCollection.addEnumeration(this.enemyArchons.elements());

        }
        if (this.delegate.shouldProcessSignalType(CommunicationModuleSignal.TYPE_ZOMBIEDEN)) {

            communicationModuleSignalCollection.addEnumeration(this.zombieDens.elements());

        }
        return communicationModuleSignalCollection;

    }

}
