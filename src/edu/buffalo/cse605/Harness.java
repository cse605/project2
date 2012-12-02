package edu.buffalo.cse605;

import static java.lang.System.out;

import java.util.concurrent.CyclicBarrier;

import edu.buffalo.cse605.workloads.Test;

public class Harness {
  
  public enum WorkloadType {W1, W2, W3, W4, W5};
  
  public static final int ITERATIONS = 100 * 1000 * 1000; // 1 BIL
  public static final int WARMUP_ITERATIONS = 1 * 1000 * 1000; //1 MIL
  private static WorkloadType workloadType;
  private static int numThreads;
  private static int warmUp;
  
  /**
   * @param args
   */
  public static void main(String[] args) throws Exception {
	  workloadType = WorkloadType.valueOf(args[0]);
	  numThreads = Integer.parseInt(args[1]);
	  warmUp = Integer.parseInt(args[2]);
	  
	  if ( warmUp == 1 ) {
		  out.printf("warmup => ");
		  Test.setupList(WARMUP_ITERATIONS);
		  for ( int i = 0; i < 10; i++) {
			  runTest(numThreads, WARMUP_ITERATIONS);
	      }
	  }
	  
	  Test.setupList(ITERATIONS);
	  final long start = System.nanoTime();
	  runTest(numThreads, ITERATIONS);
	  final long duration = System.nanoTime() - start;
	  out.printf("%d threads, duration %,d (ns)\n", numThreads, duration);
	  out.printf("%,d ns/op\n", duration / ITERATIONS);
	  out.printf("%,d ops/s\n", (ITERATIONS * 1000000000L) / duration);
	  
	  switch ( workloadType ) {
	      case W2:
	      case W5:
	    	  Test.checkList(ITERATIONS);
	    	  break;
	  }
  }
  
  private static void runTest(final int numThreads, final long iterationLimit) throws Exception {
    CyclicBarrier barrier = new CyclicBarrier(numThreads);
    Thread[] threads = new Thread[numThreads];
    for (int i = 0; i < numThreads; i++) {
        threads[i] = new Thread(new Test(i, barrier, iterationLimit, numThreads, workloadType));
    }

    for (Thread t : threads) {
        t.start();
    }

    for (Thread t : threads) {
        t.join();
    }
  }
}
