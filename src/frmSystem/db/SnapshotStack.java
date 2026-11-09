package frmSystem.db;

/** Hand-rolled LIFO stack (DSA concept #1) over dynamic-capacity arrays.
 *  Holds pending backup snapshot ids: the newest snapshot is always the top. */
public class SnapshotStack {
    private long[] elements = new long[16];
    private int top = -1;

    public void push(long id) {
        if (top == elements.length - 1) {
            grow();
        }
        elements[++top] = id;
    }

    public long pop() {
        if (isEmpty()) {
            throw new IllegalStateException("Snapshot stack is empty");
        }
        return elements[top--];
    }

    public long peek() {
        if (isEmpty()) {
            throw new IllegalStateException("Snapshot stack is empty");
        }
        return elements[top];
    }

    public boolean isEmpty() {
        return top < 0;
    }

    public int size() {
        return top + 1;
    }

    /** Double the capacity when full: amortized O(1) push. */
    private void grow() {
        long[] bigger = new long[elements.length * 2];
        System.arraycopy(elements, 0, bigger, 0, elements.length);
        elements = bigger;
    }
}
