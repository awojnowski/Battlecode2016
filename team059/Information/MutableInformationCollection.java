package team059.Information;

import java.util.*;

public class MutableInformationCollection<T> {

    private Hashtable<Integer, T> hashtable = new Hashtable<Integer, T>();
    private ArrayList<T> array = new ArrayList<T>();

    public void add(final T item, final int identifier) {

        if (this.hashtable.containsKey(identifier)) {

            return;

        }
        this.hashtable.put(identifier, item);
        this.array.add(item);

    }

    public void remove(final int identifier) {

        final T item = (T)this.hashtable.get(identifier);
        if (item == null) {

            return;

        }
        this.hashtable.remove(identifier);
        this.array.remove(item);

    }

    public boolean contains(final int identifier) {

        return this.hashtable.containsKey(identifier);

    }

    public T get(final int i) {

        return this.array.get(i);

    }

    public int size() {

        return this.array.size();

    }

}
