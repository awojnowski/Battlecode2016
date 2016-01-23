package AaronBot3.Information;

public class ImmutableInformationCollection <T> {

    private Object[] hashtable = new Object[8192];
    private Object[] array = new Object[1024];
    private int size = 0;

    public void add(final T item, final int identifier) {

        if (this.hashtable[identifier] != null) {

            return;

        }
        this.hashtable[identifier] = item;
        this.array[this.size] = item;
        this.size ++;

    }

    public boolean contains(final int identifier) {

        return this.hashtable[identifier] != null;

    }

    public T get(final int i) {

        return (T)this.array[i];

    }

    public int size() {

        return this.size;

    }

}
