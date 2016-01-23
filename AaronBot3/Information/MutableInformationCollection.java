package AaronBot3.Information;

import java.util.*;

public class MutableInformationCollection<T> {

    private Object[] hashtable = new Object[8192];
    private ArrayList<T> array = new ArrayList<T>();

    public void add(final T item, final int identifier) {

        if (this.hashtable[identifier] != null) {

            return;

        }
        this.hashtable[identifier] = item;
        this.array.add(item);

    }

    public void remove(final int identifier) {

        final T item = (T)this.hashtable[identifier];
        if (item == null) {

            return;

        }
        this.hashtable[identifier] = null;
        this.array.remove(item);

    }

    public boolean contains(final int identifier) {

        return this.hashtable[identifier] != null;

    }

    public T get(final int i) {

        return this.array.get(i);

    }

    public int size() {

        return this.array.size();

    }

}
