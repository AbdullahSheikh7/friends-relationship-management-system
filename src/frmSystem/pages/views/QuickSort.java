package frmSystem.pages.views;

import java.util.Comparator;
import java.util.List;

/** Hand-written Quick Sort — DSA concept #5 (replaces Java's built-in TimSort
 *  for the frequency-ranked search results).
 *
 *  In-place Lomuto partition with median-of-three pivot selection, so already
 *  sorted input (friends come back ordered by name) cannot degrade it to O(n^2).
 *  Recursion always happens on the smaller side while the larger side loops,
 *  which keeps the call stack at O(log n). */
public final class QuickSort {

    private QuickSort() {}

    public static <T> void sort(List<T> list, Comparator<? super T> comparator) {
        sort(list, 0, list.size() - 1, comparator);
    }

    private static <T> void sort(List<T> list, int low, int high, Comparator<? super T> comparator) {
        while (low < high) {
            int pivot = partition(list, low, high, comparator);
            if (pivot - low < high - pivot) {
                sort(list, low, pivot - 1, comparator);
                low = pivot + 1;
            } else {
                sort(list, pivot + 1, high, comparator);
                high = pivot - 1;
            }
        }
    }

    private static <T> int partition(List<T> list, int low, int high, Comparator<? super T> comparator) {
        int middle = low + (high - low) / 2;
        if (comparator.compare(list.get(low), list.get(middle)) > 0) swap(list, low, middle);
        if (comparator.compare(list.get(low), list.get(high)) > 0) swap(list, low, high);
        if (comparator.compare(list.get(middle), list.get(high)) > 0) swap(list, middle, high);
        swap(list, middle, high);
        T pivot = list.get(high);
        int wall = low;
        for (int scan = low; scan < high; scan++) {
            if (comparator.compare(list.get(scan), pivot) <= 0) {
                swap(list, wall++, scan);
            }
        }
        swap(list, wall, high);
        return wall;
    }

    private static <T> void swap(List<T> list, int first, int second) {
        T temp = list.get(first);
        list.set(first, list.get(second));
        list.set(second, temp);
    }
}
