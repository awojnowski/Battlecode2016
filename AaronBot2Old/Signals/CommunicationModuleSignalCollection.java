package AaronBot2Old.Signals;

import java.util.*;

public class CommunicationModuleSignalCollection {

    private ArrayList<Enumeration<CommunicationModuleSignal>> enumerations = new ArrayList<Enumeration<CommunicationModuleSignal>>();
    private int currentEnumerationIndex = 0;

    public void addEnumeration(final Enumeration<CommunicationModuleSignal> enumeration) {

        this.enumerations.add(enumeration);

    }

    public boolean hasMoreElements() {

        while (true) {

            if (this.currentEnumerationIndex >= this.enumerations.size()) {

                return false;

            }
            if (!this.enumerations.get(this.currentEnumerationIndex).hasMoreElements()) {

                this.currentEnumerationIndex++;

            } else {

                return true;

            }

        }

    }

    public CommunicationModuleSignal nextElement() {

        if (this.currentEnumerationIndex >= this.enumerations.size()) {

            return null;

        }
        return this.enumerations.get(this.currentEnumerationIndex).nextElement();

    }

}
