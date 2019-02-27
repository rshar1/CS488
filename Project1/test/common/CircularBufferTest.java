package common;



import static org.junit.Assert.*;

public class CircularBufferTest {

  CircularBuffer<Integer> buffer1;
  CircularBuffer<Integer> buffer5;
  CircularBuffer<Integer> buffer6;

  @org.junit.Before
  public void setUp() throws Exception {

    buffer1 = new CircularBuffer<>(1);
    buffer5 = new CircularBuffer<>(5);
    buffer6 = new CircularBuffer<>(6);

  }

  @org.junit.After
  public void tearDown() throws Exception {
  }

  @org.junit.Test
  public void add() {

    buffer1.add(5);
    try {
      buffer1.add(5);
      fail();
    } catch (IllegalStateException exc) {

    }

    buffer5.add(5);
    buffer5.add(6);
    buffer5.add(7);
    buffer5.add(8);
    buffer5.add(9);
    try {
      buffer5.add(10);
      fail();
    } catch (IllegalStateException exc) {

    }

    buffer6.add(11);
    buffer6.add(12);
    buffer6.add(13);
    buffer6.add(14);
    buffer6.add(15);
    buffer6.add(16);
    try {
      buffer6.add(17);
      fail();
    } catch (IllegalStateException exc) {

    }

  }

  @org.junit.Test
  public void remove() {

    buffer1.add(5);
    try {
      buffer1.add(5);
      fail();
    } catch (IllegalStateException exc) {

    }

    buffer5.add(5);
    buffer5.add(6);
    buffer5.add(7);
    buffer5.add(8);
    buffer5.add(9);
    try {
      buffer5.add(10);
      fail();
    } catch (IllegalStateException exc) {

    }

    buffer6.add(11);
    buffer6.add(12);
    buffer6.add(13);
    buffer6.add(14);
    buffer6.add(15);
    buffer6.add(16);
    try {
      buffer6.add(17);
      fail();
    } catch (IllegalStateException exc) {

    }

    assertEquals(buffer1.remove(), Integer.valueOf(5));

    assertEquals(buffer5.remove(), Integer.valueOf(5));
    assertEquals(buffer5.remove(), Integer.valueOf(6));
    assertEquals(buffer5.remove(), Integer.valueOf(7));
    assertEquals(buffer5.remove(), Integer.valueOf(8));
    assertEquals(buffer5.remove(), Integer.valueOf(9));

    assertEquals(buffer6.remove(), Integer.valueOf(11));
    assertEquals(buffer6.remove(), Integer.valueOf(12));
    assertEquals(buffer6.remove(), Integer.valueOf(13));
    assertEquals(buffer6.remove(), Integer.valueOf(14));
    assertEquals(buffer6.remove(), Integer.valueOf(15));
    assertEquals(buffer6.remove(), Integer.valueOf(16));

  }

  @org.junit.Test
  public void size() {

    assertEquals(buffer1.size(), 0);
    buffer1.add(5);
    assertEquals(buffer1.size(), 1);
    try {
      buffer1.add(5);
      fail();
    } catch (IllegalStateException exc) {

    }

    assertEquals(buffer1.size(), 1);
    assertEquals(buffer5.size(), 0);
    buffer5.add(5);
    assertEquals(buffer5.size(), 1);
    buffer5.add(6);
    assertEquals(buffer5.size(), 2);
    buffer5.add(7);
    assertEquals(buffer5.size(), 3);
    buffer5.add(8);
    assertEquals(buffer5.size(), 4);
    buffer5.add(9);
    assertEquals(buffer5.size(), 5);


  }
}
