package edu.buffalo.cse605.workloads;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import static java.lang.System.out;

import edu.buffalo.cse605.Harness.WorkloadType;

public class Test implements Runnable {
	
	private final CyclicBarrier barrier;
	private final long iterationLimit;
	private final int numThreads;
	private final int threadId;
	private final WorkloadType workloadType;
	
	public static Long[] list;
	
	private static int SEGMENT_SIZE = 100 * 1000;
	private static int NUMBER_OF_SEGMENTS;
	
	public static final Object jvmLock = new Object();
	public static final Lock jucLock = new ReentrantLock();
	
	public static AtomicInteger atoCounter = new AtomicInteger();
	public static volatile int volCounter = -1;
	
	public Test(final int threadId, final CyclicBarrier barrier, final long iterationLimit, final int numThreads, final WorkloadType workloadType) {
		this.barrier = barrier;
		this.iterationLimit = iterationLimit;
		this.numThreads = numThreads;
		this.threadId = threadId; 
		this.workloadType = workloadType;
	}
	
	public static void setupList(long iterations) {
		int count = (int) iterations;
		list = new Long[count];
		for ( int i = 0; i < count; i++) {
			list[i] = 0L;
		}
		NUMBER_OF_SEGMENTS = ((int) iterations / SEGMENT_SIZE)-1;
	}
	
	public static void checkList(long iterations) {
		int count = (int) iterations;
		int w = 0;
		for ( int i = 0; i < count; i++) {
			if (list[i] != 1) {
				w++;
//				out.printf("value => %d \n", list[i]);
//				throw new Error("can`t assert => " + i);
			}
		}
		out.printf("value => %d \n", w);
	}
	
	private static int getSegment() {
		int count = atoCounter.get();
		if (count < NUMBER_OF_SEGMENTS) {
			return atoCounter.incrementAndGet() -1 ;
		} else {
			return -1;
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
//		int start; 
//		while ((start = getSegment()) != -1) {
//			for ( int i = start; i < start+SEGMENT_SIZE; i++ ) {
//				++list[i];
//			}
//		}
//		int count = (int) (iterationLimit / numThreads);
//		int start = count * threadId;
//		for ( int i = start; i < start+count; i++ ) {
//			++list[i];
//		}
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
		int count = (int) (iterationLimit / numThreads);
//		int start = (int)(Math.random() * ((iterationLimit - count) + 1));
		for ( int i = 0; i < count; i++ ) {
			++list[i];
		}
	}
	
	// 20% Read, 80% Write
	private void workload4() {
		int count = (int) (iterationLimit / numThreads);
		long temp = 0;
		for ( int i = 0; i < count; i++ ) {
			temp = list[i];
		}
	}
	
	// 50% Read, 50% Write
	private void workload5() {
//		int count = (int) (iterationLimit / numThreads);
//		int start = count * threadId;
//		for ( int i = start; i < start+count; i++ ) {
//			synchronized (jvmLock) {
//				++list[i];
//			}
//		}
	}

}
