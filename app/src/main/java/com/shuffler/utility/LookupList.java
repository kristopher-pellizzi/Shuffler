package com.shuffler.utility;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class LookupList<T> implements List<T> {

    private Map<T, Integer> indexes;
    private ArrayList<T> list;

    public LookupList(){
        indexes = new HashMap<>();
        list = new ArrayList<>();
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    public boolean contains(@androidx.annotation.Nullable Object o) {
        return indexes.containsKey(o);
    }

    @androidx.annotation.NonNull
    @Override
    public Iterator<T> iterator() {
        return list.iterator();
    }

    @androidx.annotation.Nullable
    @Override
    public Object[] toArray() {
        return list.toArray();
    }

    @Override
    public <T1> T1[] toArray(@androidx.annotation.Nullable T1[] a) {
        return list.toArray(a);
    }

    @Override
    public boolean add(T t) {
        if(indexes.containsKey(t))
            return false;
        list.add(t);
        indexes.put(t, list.size() - 1);
        return true;
    }

    @Override
    public boolean remove(@androidx.annotation.Nullable Object o) {
        Integer val = indexes.remove(o);
        if(val == null)
            return false;
        for(Map.Entry<T, Integer> entry : indexes.entrySet()){
            if(entry.getValue() > val)
                indexes.put(entry.getKey(), entry.getValue() - 1);
        }
        return list.remove(o);
    }

    @Override
    public boolean containsAll(@androidx.annotation.NonNull Collection<?> c) {
        Iterator<?> iterator = c.iterator();

        while(iterator.hasNext()){
            if(!indexes.containsKey(iterator.next()))
                return false;
        }
        return true;
    }

    @Override
    public boolean addAll(@androidx.annotation.NonNull Collection<? extends T> c) {
        boolean changed = false;
        Iterator<? extends T> iterator = c.iterator();

        while(iterator.hasNext()){
            changed = add(iterator.next()) || changed;
        }
        return changed;
    }

    @Override
    public boolean addAll(int index, @androidx.annotation.NonNull Collection<? extends T> c) {
        boolean changed = false;
        Iterator<? extends T> iterator = c.iterator();

        while(iterator.hasNext()){
            T item = iterator.next();

            changed = !indexes.containsKey(item) || changed;
            add(index, item);
        }
        return changed;
    }

    @Override
    public boolean removeAll(@androidx.annotation.NonNull Collection<?> c) {
        Iterator<?> iterator = c.iterator();
        boolean changed = false;

        while(iterator.hasNext()){
            Object item = iterator.next();

            changed = indexes.containsKey(item) || changed;
            remove(item);
        }
        return changed;
    }

    @Override
    public boolean retainAll(@androidx.annotation.NonNull Collection<?> c) {
        Iterator<?> iterator = c.iterator();
        boolean changed = false;
        Set<T> toRemove = new HashSet<>(this.list);

        toRemove.removeAll(c);
        return removeAll(toRemove);
    }

    @Override
    public void clear() {
        this.list = new ArrayList<>();
        this.indexes = new HashMap<>();
    }

    @Override
    public T get(int index) {
        return list.get(index);
    }

    @Override
    public T set(int index, T element) {
        T oldItem = list.remove(index);

        list.set(index, element);
        indexes.remove(oldItem);
        indexes.put(element, index);
        return oldItem;
    }

    @Override
    public void add(int index, T element) {
        list.add(index, element);
        indexes.put(element, index);
        for(Map.Entry<T, Integer> entry : indexes.entrySet()){
            if(entry.getValue() >= index)
                indexes.put(entry.getKey(), entry.getValue() + 1);
        }
    }

    @Override
    public T remove(int index) {
        T oldItem = list.remove(index);
        indexes.remove(oldItem);
        for(Map.Entry<T, Integer> entry : indexes.entrySet()){
            if(entry.getValue() >= index)
                indexes.put(entry.getKey(), entry.getValue() - 1);
        }
        return oldItem;
    }

    @Override
    public int indexOf(@androidx.annotation.Nullable Object o) {
        if(o == null)
            return -1;
        Integer index = indexes.get(o);
        return index != null ? index : -1;

    }

    // Note: this implementation of the List interface is thought to maintain a single copy of each item
    @Override
    public int lastIndexOf(@androidx.annotation.Nullable Object o) {
        return indexOf(o);
    }

    @androidx.annotation.NonNull
    @Override
    public ListIterator<T> listIterator() {
        return list.listIterator();
    }

    @androidx.annotation.NonNull
    @Override
    public ListIterator<T> listIterator(int index) {
        return list.listIterator(index);
    }

    @androidx.annotation.NonNull
    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        return list.subList(fromIndex, toIndex);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LookupList<?> that = (LookupList<?>) o;
        return Objects.equals(indexes, that.indexes) &&
                Objects.equals(list, that.list);
    }

    @Override
    public int hashCode() {
        return Objects.hash(indexes, list);
    }

    public T pop(){
        T elem;

        elem = list.remove(list.size() - 1);
        indexes.remove(elem);
        return elem;
    }
}
