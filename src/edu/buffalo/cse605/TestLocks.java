package edu.buffalo.cse605;


import static java.lang.System.out;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TestLocks implements Runnable {
  
  public enum ConcurrencyType {NOR, VOL, ATO, JVM, JUC};
  public static ConcurrencyType concurrencyType;

  public static final long WARMUP_ITERATIONS = 100L * 1000L;
  public static final long ITERATIONS = 500L * 1000L * 1000L;
  public static long counter = 0L;
  public static AtomicLong atoCounter = new AtomicLong();
  public static volatile long volCounter = 0L;
  
  private static int numThreads;
  
  private static int warmup;
  
  private final long iterationLimit;
  private final CyclicBarrier barrier;

  public static final Lock jucLock = new ReentrantLock();
  public static final Object jvmLock = new Object();
  
  public TestLocks(final CyclicBarrier barrier, final long iterationLimit) {
      this.barrier = barrier;
      this.iterationLimit = iterationLimit;
  }
    
  /**
   * @param args
   */
  public static void main(String[] args) throws Exception {
    concurrencyType = ConcurrencyType.valueOf(args[0]);
    numThreads = Integer.parseInt(args[1]);
    warmup = Integer.parseInt(args[2]);

    if ( warmup == 1 ) {
      out.println("Warmup");
      for (int i = 0; i < 10; i++) {
        runTest(numThreads, WARMUP_ITERATIONS);
        counter = 0L;
        volCounter = 0L;
        atoCounter = new AtomicLong();
      }
    }
    // TODO Auto-generated method stub
    final long start = System.nanoTime();
    runTest(numThreads, ITERATIONS);
    final long duration = System.nanoTime() - start;
    out.printf("%d threads, duration %,d (ns)\n", numThreads, duration);
    out.printf("%,d ns/op\n", duration / ITERATIONS);
    out.printf("%,d ops/s\n", (ITERATIONS * 1000000000L) / duration);
    
    switch ( concurrencyType ) {
      case VOL: out.println("counter = " + volCounter); break;
      case ATO: out.println("counter = " + atoCounter.get()); break;
      default: out.println("counter = " + counter); break;
    }
  }

  @Override
  public void run() {
    try {
        barrier.await();
    }
    catch (Exception e) {
        // don't care
    }
    switch ( concurrencyType ) {
      case NOR: norInc(); break;
      case VOL: volInc(); break;
      case ATO: atoInc(); break;
      case JVM: jvmLocInc(); break;
      case JUC: jucLockInc(); break;
    }
  }

  private long atoInc() {
    long count = iterationLimit / numThreads;
    long counter = 0;
    while (0 != count--) {
      counter = atoCounter.getAndIncrement();
    }
    return counter;
  }

  private void volInc() {
    long count = iterationLimit / numThreads;
    while (0 != count--) {
      ++volCounter;
    }
  }

  private void norInc() {
    long count = iterationLimit / numThreads;
    while (0 != count--) {
      ++counter;
    }
  }

  private void jvmLocInc() {
    long count = iterationLimit / numThreads;
    while (0 != count--) {
      synchronized(jvmLock) {
        ++counter;
      }
    }
  }
 
  private void jucLockInc() {
    long count = iterationLimit / numThreads;
    while (0 != count--) {
      jucLock.lock();
      try {
        ++counter;
      }
      finally {
        jucLock.unlock();
      }
    }
  }
  
  private static void runTest(final int numThreads, final long iterationLimit) throws Exception {
    CyclicBarrier barrier = new CyclicBarrier(numThreads);
    Thread[] threads = new Thread[numThreads];

    for (int i = 0; i < threads.length; i++) {
        threads[i] = new Thread(new TestLocks(barrier, iterationLimit));
    }

    for (Thread t : threads) {
        t.start();
    }

    for (Thread t : threads) {
        t.join();
    }
  }
}
