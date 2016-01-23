package AaronBot3.Information;

import java.util.*;

public class MutableInformationCollection<T> {

    private int[] hashtable = new int[8192];
    private ArrayList<T> array = new ArrayList<T>();

    public void add(final T item, final int identifier) {

        if (this.hashtable[identifier] != 0) {

            return;

        }
        this.hashtable[identifier] = identifier;
        this.array.add(item);

    }

    public void remove(final int i, final int identifier) {

        this.hashtable[identifier] = 0;
        this.array.remove(i);

    }

    public void remove(final T item, final int identifier) {

        this.hashtable[identifier] = 0;
        this.array.remove(item);

    }

    public boolean contains(final int identifier) {

        return this.hashtable[identifier] != 0;

    }

    public T get(final int i) {

        return this.array.get(i);

    }

    public int size() {

        return this.array.size();

    }

}
