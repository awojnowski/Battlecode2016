package AaronBot3.Information;

public class ImmutableInformationCollection <T> {

    private int[] hashtable = new int[8192];
    private Object[] array = new Object[1024];
    private int size = 0;

    public void add(final T item, final int identifier) {

        if (this.hashtable[identifier] != 0) {

            return;

        }
        this.hashtable[identifier] = identifier;
        this.array[this.size] = item;
        this.size ++;

    }

    public boolean contains(final int identifier) {

        return this.hashtable[identifier] != 0;

    }

    public T get(final int i) {

        return (T)this.array[i];

    }

    public int size() {

        return this.size;

    }

}
