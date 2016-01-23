package AaronBot3.Information;

import java.util.*;

public class ImmutableInformationCollection <T> {

    private Hashtable<Integer, T> hashtable = new Hashtable<Integer, T>();
    private Object[] array = new Object[1024];
    private int size = 0;

    public void add(final T item, final int identifier) {

        if (this.hashtable.containsKey(identifier)) {

            return;

        }
        this.hashtable.put(identifier, item);
        this.array[this.size] = item;
        this.size ++;

    }

    public boolean contains(final int identifier) {

        return this.hashtable.containsKey(identifier);

    }

    public T get(final int i) {

        return (T)this.array[i];

    }

    public int size() {

        return this.size;

    }

}
