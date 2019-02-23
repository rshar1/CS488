package common;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;

public class CircularBuffer<E> implements Queue<E> {

  // The window size
  private Object[] buf;
  private int head;
  private int size;


  public CircularBuffer(int size) {
    this.buf = new Object[size];
    this.head = 0;
    this.size = 0;
  }

  @Override
  public boolean add(E obj) {

    if (obj == null) {
      throw new NullPointerException();
    }

    if (this.size == this.buf.length) {
      throw new IllegalStateException();
    }

    int tail = (this.head + size) % buf.length;
    buf[tail] = obj;
    this.size++;
    return true;

  }

  @Override
  public E poll() {

    if (this.size == 0) {
      return null;
    }

    Object ret = buf[head];
    buf[head++] = null;
    head %= buf.length;
    this.size--;

    return (E) ret;

  }

  @Override
  public int size() {
    return this.size;
  }

  public E get(int index) {
    if (index < 0 || index >= buf.length) {
      throw new IndexOutOfBoundsException("Index is out of bounds for this buffer");
    }
    return (E) buf[(head + index) % buf.length];
  }


  @Override
  public boolean offer(E o) {
    try {
      this.add(o);
    } catch (IllegalStateException exc) {
      return false;
    }

    return true;
  }

  @Override
  public E remove() {

    if (this.size == 0) {
      throw new NoSuchElementException();
    }

    return this.poll();
  }

  @Override
  public E element() {

    if (this.size == 0) {
      throw new NoSuchElementException();
    }

    return (E) buf[head];

  }

  @Override
  public E peek() {

    if (this.size == 0) {
      return null;
    }

    return (E) buf[head];

  }

  @Override
  public boolean isEmpty() {
    return this.size == 0;
  }

  @Override
  public boolean contains(Object o) {

    if (o == null) {
      throw new NullPointerException();
    }

    for (int n = 0; n < this.size; n++) {
      int i = (head + n) % buf.length;
      Object curr = buf[i];
      if (curr == o) {
        return true;
      }

    }

    return false;
  }

  @Override
  public Iterator<E> iterator() {

    return new Iterator<E>() {

      private int currIndex = head;
      private int currCount = 0;

      @Override
      public boolean hasNext() {
        return currCount < size;
      }

      @Override
      public E next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        E res = (E) buf[currIndex];
        currIndex = (currIndex + 1) % buf.length;
        currCount++;
        return res;
      }
    };

  }

  @Override
  public Object[] toArray() {

    Object[] res = new Object[this.size];
    int i = 0;

    for (E obj : this) {
      res[i++] = obj;
    }

    return res;
  }

  @Override
  public Object[] toArray(Object[] a) {
    // todo implement
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public boolean remove(Object o) {
    throw new UnsupportedOperationException("Can only remove the head from this collection");
  }

  @Override
  public boolean containsAll(Collection c) {
    // todo implement
    return false;
  }

  @Override
  public boolean addAll(Collection c) {

    for (Object o: c) {
      if (!contains(o)) {
        return false;
      }
    }

    return true;
  }

  @Override
  public boolean removeAll(Collection c) {
    throw new UnsupportedOperationException("Cannot remove arbitrary objects");
  }

  @Override
  public boolean retainAll(Collection c) {
    throw new UnsupportedOperationException("Cannot remove arbitrary objects");
  }

  @Override
  public void clear() {
    while (this.size > 0) {
      this.remove();
    }
  }
}
