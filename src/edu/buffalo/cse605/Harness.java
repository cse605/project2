package edu.buffalo.cse605;

import static java.lang.System.out;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import edu.buffalo.cse605.workloads.Test;

public class Harness {
  
  public enum WorkloadType {DWRIT, DREAD, CREAD, CWRIT, SDWRIT, SDREAD, SCREAD, SCWRIT, LDREAD, LCREAD ,LDWRIT,LCWRIT};
  private static WorkloadType workloadType;
  
  public static final int ITERATIONS = 1000 * 1000 * 1000; //100 MIL
  public static Long[] list = new Long[ITERATIONS];
  
  private static int numThreads;
  private static int warmUp;
  
  /**
   * @param args
   */
  public static void main(String[] args) throws Exception {
	  workloadType = WorkloadType.valueOf(args[0]);
	  numThreads = Integer.parseInt(args[1]);
	  warmUp = Integer.parseInt(args[2]);
	  if ( warmUp == 1) {
		  out.println("warmup");
		  for ( int i = 0; i < 5; i++) {
			  for ( int j = 0; j < ITERATIONS; j++) {
				  list[j] = 0L;
			  }
			  runTest(numThreads, ITERATIONS/1000);
	      }
	  }
	  for ( int i = 0; i < ITERATIONS; i++) {
		  list[i] = 0L;
	  }
	  final long start = System.nanoTime();
	  runTest(numThreads, ITERATIONS);
	  final long duration = System.nanoTime() - start;
	  out.printf("%d threads, duration %,d (ns)\n", numThreads, duration);
	  out.printf("%,d ns/op\n", duration / ITERATIONS);
	  out.printf("%,d ops/s\n", (ITERATIONS * 1000000000L) / duration);
	  
	  switch ( workloadType ) {
	      case SDWRIT:
	    	  for ( int j = 0; j < ITERATIONS; j++) {
	    		  if ( list[j] != 1 ) {
	    			  out.printf("value => %d", list[j]);
	    			  throw new Error("can`t assert => " + j);
	    		  }
	    	  }
	    	  break;
	      case SCWRIT:
	    	  for ( int j = 0; j < ITERATIONS/numThreads; j++) {
	    		  if ( list[j] != numThreads ) {
	    			  out.printf("value => %d", list[j]);
	    			  throw new Error("can`t assert => " + j);
	    		  }
	    	  }
	    	  break;
	    }
	  
	  
  }
  
  private static void runTest(final int numThreads, final long iterationLimit) throws Exception {
    CyclicBarrier barrier = new CyclicBarrier(numThreads);
    Thread[] threads = new Thread[numThreads];
    for (int i = 0; i < numThreads; i++) {
        threads[i] = new Thread(new Test(list, i, barrier, iterationLimit, numThreads, workloadType));
    }

    for (Thread t : threads) {
        t.start();
    }

    for (Thread t : threads) {
        t.join();
    }
  }
}
