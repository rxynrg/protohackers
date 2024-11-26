import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.GregorianCalendar;

public class Demo {
  private static long counter;

  public static void main(String[] args) throws InterruptedException {
    // warm up
    // This is due to the JVM's JIT compiler optimising the code during runtime
    int[] display = {0, 1, 10, 100, 1_000, 10_000, 20_000, 100_001};
    for (int i = 0; i <= display[display.length - 1]; i++) {
      long start = System.nanoTime();
      doTask();
      long time = System.nanoTime() - start;
      if (Arrays.binarySearch(display, i) >= 0)
        System.out.printf("%,d: Took %,d us to serialize/deserialize GregorianCalendar%n", i, time / 1_000);
    }

    // cool down
    // Certain operations can degrade performance
    // by affecting the CPU caches and power settings.
    // Even a simple Thread.sleep() or blocking call
    // can slow down subsequent code execution.
    for (int i : new int[]{0, 1, 2, 5, 10, 20, 50, 100}) {
      int runs = i < 10 ? 1_000 : 100;
      long total = 0;
      for (int j = 0; j < runs; j++) {
        Thread.sleep(i);
        long start = System.nanoTime();
        doTask();
        long time = System.nanoTime() - start;
        total += time;
      }
      System.out.printf("After sleep %d ms: Took %,d us to serialize/deserialize GregorianCalendar%n", i, total / runs / 1_000);
    }

    // This occurs because
    // the CPU caches are cleared during sleep,
    // and power management reduces the CPU speed,
    // causing the subsequent operations to fetch data
    // from slower memory and code to execute slower.

    System.out.println(counter);
  }

  static void doTask() {
    try {
      var cal = new GregorianCalendar();
      var baos = new ByteArrayOutputStream();
      var oos = new ObjectOutputStream(baos);
      oos.writeObject(cal);
      oos.close();
      var ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
      var cal2 = (GregorianCalendar) ois.readObject();
      ois.close();
      counter += cal2.toString().length();
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }
}
