package AaronBot2.Signals;

import java.util.*;

public class CommunicationModuleSignalCollection {

    private ArrayList<Enumeration<CommunicationModuleSignal>> enumerations;
    private int currentEnumerationIndex = 0;

    public CommunicationModuleSignalCollection() {

        this.enumerations = new ArrayList<Enumeration<CommunicationModuleSignal>>();

    }

    public void addEnumeration(final Enumeration<CommunicationModuleSignal> enumeration) {

        this.enumerations.add(enumeration);

    }

    public boolean hasMoreElements() {

        if (this.currentEnumerationIndex >= this.enumerations.size()) {

            return false;

        }
        return this.enumerations.get(this.currentEnumerationIndex).hasMoreElements();

    }

    public CommunicationModuleSignal nextElement() {

        if (this.currentEnumerationIndex >= this.enumerations.size()) {

            return null;

        }
        return this.enumerations.get(this.currentEnumerationIndex).nextElement();

    }

}
