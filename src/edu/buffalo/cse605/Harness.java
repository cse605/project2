package edu.buffalo.cse605;

import static java.lang.System.out;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import edu.buffalo.cse605.workloads.Test;

public class Harness {
  
  public enum WorkloadType {W1, W2, W3, W4, W5};
  
  public static final int ITERATIONS = 1000 * 1000 * 1000; // 1 BIL
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
	  
	  Test.setupList(WARMUP_ITERATIONS);
	  if ( warmUp == 1 ) {
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
//	  Test.checkList(ITERATIONS);
	  
//	  switch ( workloadType ) {
//	      case SDWRIT:
//	    	  for ( int j = 0; j < ITERATIONS; j++) {
//	    		  if ( list[j] != 1 ) {
//	    			  out.printf("value => %d", list[j]);
//	    			  throw new Error("can`t assert => " + j);
//	    		  }
//	    	  }
//	    	  break;
//	      case SCWRIT:
//	    	  for ( int j = 0; j < ITERATIONS/numThreads; j++) {
//	    		  if ( list[j] != numThreads ) {
//	    			  out.printf("value => %d", list[j]);
//	    			  throw new Error("can`t assert => " + j);
//	    		  }
//	    	  }
//	    	  break;
//	    }
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
