package edu.buffalo.cse605.workloads;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import static java.lang.System.out;

import edu.buffalo.cse605.Harness.WorkloadType;

public class Test implements Runnable {
	
	private static final int SEGMENT_SIZE = 100 * 1000;
	
	private final CyclicBarrier barrier;
	private final long iterationLimit;
	private final int numThreads;
	private final int threadId;
	private final WorkloadType workloadType;
	private final int numberOfSegments;
	
	public static Long[] list;
	
	private static final Object jvmLock = new Object();
	private static final Lock jucLock = new ReentrantLock();
	private static AtomicInteger atoCounter = new AtomicInteger(-1);
	private static volatile int volCounter = -1;
	
	public Test(final int threadId, final CyclicBarrier barrier, final long iterationLimit, final int numThreads, final WorkloadType workloadType) {
		this.barrier = barrier;
		this.iterationLimit = iterationLimit;
		this.numThreads = numThreads;
		this.threadId = threadId; 
		this.workloadType = workloadType;
		this.numberOfSegments = (int) (iterationLimit/SEGMENT_SIZE) - 1;
	}
	
	public static void setupList(long iterations) {
		int count = (int) iterations;
		list = new Long[count];
		for ( int i = 0; i < count; i++) {
			list[i] = 0L;
		}
	}
	
	public static void checkList(long iterations) {
		int count = (int) iterations;
		int w = 0;
		for ( int i = 0; i < count; i++) {
			if (list[i] != 1) {
				w++;
			}
		}
		out.printf("value => %d \n", w);
	}
	
	private int getSegment() {
//		if (count < NUMBER_OF_SEGMENTS) {
//			return ++count;
//		} else {
//			return -1;
//		}
		if ( atoCounter.get() < numberOfSegments ) {
			return atoCounter.incrementAndGet();
		} else {
			return -1;
		}
//		if ( volCounter < NUMBER_OF_SEGMENTS ) {
//			volCounter++;
//			return volCounter;
//		} else {
//			return -1;
//		}
	}

	@Override
	public void run() {
		try {
	        barrier.await();
	    }
	    catch (Exception e) {
	        // don't care
	    }
		switch ( workloadType ) {
	      case W1: workload1(); break;
	      case W2: workload2(); break;
	      case W3: workload3(); break;
	      case W4: workload4(); break;
	      case W5: workload5(); break;
	    }
	}
	
	// 100% READ
	private void workload1() {
		int start;
		long temp = 0;
		while ((start = getSegment()) != -1) {
			start = start * SEGMENT_SIZE;
			for ( int i = start; i < start+SEGMENT_SIZE; i++ ) {
				temp = list[i];
			}
		}
	}
	
	// 100% WRITE
	private void workload2() {
		int start;
		while ((start = getSegment()) != -1) {
			start = start * SEGMENT_SIZE;
			for ( int i = start; i < start+SEGMENT_SIZE; i++ ) {
				++list[i];
			}
		}
	}
	
	// 80% Read, 20% Write
	private void workload3() {
		
	}
	
	// 20% Read, 80% Write
	private void workload4() {
		
	}
	
	// 50% Read, 50% Write
	private void workload5() {

	}

}
