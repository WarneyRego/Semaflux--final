package org.semaflux.sim.core;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class ListaLigada<T> implements Iterable<T> {
    private int size;
    private No<T> last;
    private No<T> first;

    private static class No<T> { 
        No<T> next;
        T data;

        No(T data) {
            this.next = null;
            this.data = data;
        }
    }

    public ListaLigada() {
        this.size = 0;
        this.last = null;
        this.first = null;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public boolean contains(T item) {
        return indexOf(item) != -1;
    }

    public int indexOf(T item) {
        No<T> current = first;
        int index = 0;
        while (current != null) {
            if (item == null) {
                if (current.data == null) {
                    return index;
                }
            } else {
                if (item.equals(current.data)) {
                    return index;
                }
            }
            current = current.next;
            index++;
        }
        return -1;
    }

    public T getFirst() {
        if (isEmpty()) {
            throw new NoSuchElementException("A lista está vazia.");
        }
        return first.data;
    }

    public T getLast() {
        if (isEmpty()) {
            throw new NoSuchElementException("A lista está vazia.");
        }
        return last.data;
    }

    public T get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Índice inválido: " + index + " para tamanho " + size);
        }
        No<T> current = first;
        for (int i = 0; i < index; i++) {
            current = current.next;
        }
        return current.data;
    }

    public void add(T item) {
        No<T> newNode = new No<>(item);
        if (isEmpty()) {
            first = newNode;
            last = newNode;
        } else {
            last.next = newNode;
            last = newNode;
        }
        size++;
    }

    public void addFirst(T item) {
        No<T> newNode = new No<>(item);
        newNode.next = first;
        first = newNode;
        if (last == null) {
            last = newNode;
        }
        size++;
    }

    public T removeFirst() {
        if (isEmpty()) {
            throw new NoSuchElementException("Não é possível remover de uma lista vazia.");
        }
        T data = first.data;
        first = first.next;
        size--;
        if (isEmpty()) {
            last = null;
        }
        return data;
    }

    public boolean remove(T item) {
        if (isEmpty()) {
            return false;
        }

        if ((item == null && first.data == null) || (item != null && item.equals(first.data))) {
            removeFirst();
            return true;
        }

        No<T> current = first;
        while (current.next != null) {
            if ((item == null && current.next.data == null) || (item != null && item.equals(current.next.data))) {
                if (current.next == last) {
                    last = current;
                }
                current.next = current.next.next;
                size--;
                return true;
            }
            current = current.next;
        }
        return false;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private No<T> current = first;

            @Override
            public boolean hasNext() {
                return current != null;
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException("Não há mais elementos na lista.");
                }
                T data = current.data;
                current = current.next;
                return data;
            }
        };
    }

    @Override
    public String toString() {
        if (isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        No<T> current = first;
        while (current != null) {
            sb.append(current.data == null ? "null" : current.data.toString());
            if (current.next != null) {
                sb.append(" -> ");
            }
            current = current.next;
        }
        sb.append("]");
        return sb.toString();
    }
}