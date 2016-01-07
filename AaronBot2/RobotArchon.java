package AaronBot2;

import AaronBot2.Signals.*;
import battlecode.common.*;
import java.util.*;

public class RobotArchon implements Robot {

    public void run(final RobotController robotController) throws GameActionException {

        final CommunicationModule communicationModule = new CommunicationModule();

        {
            final CommunicationModuleSignal signal = new CommunicationModuleSignal();
            signal.location = robotController.getLocation();
            signal.robotIdentifier = robotController.getID();
            communicationModule.broadcastSignal(signal, robotController, CommunicationModule.MaximumBroadcastRange);
        }

        while (true) {

            communicationModule.processIncomingSignals(robotController);
            final Enumeration<CommunicationModuleSignal> signals = communicationModule.communications.elements();

            System.out.println("We have recorded the signals (" + communicationModule.communications.size() + "):");
            while (signals.hasMoreElements()) {

                final CommunicationModuleSignal signal = signals.nextElement();
                System.out.println("Location: " + signal.location + " Robot ID: " + signal.robotIdentifier);

            }
            System.out.println("(end recording signals)");

            Clock.yield();

        }

    }

}
