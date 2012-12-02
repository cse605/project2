package edu.buffalo.cse605.workloads;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import edu.buffalo.cse605.Harness.WorkloadType;

public class Test implements Runnable {
	
	private final CyclicBarrier barrier;
	private final long iterationLimit;
	private final int numThreads;
	private final int threadId;
	private final WorkloadType workloadType;
	
	private static Long[] list;
	
	public static final Object jvmLock = new Object();
	public static final Lock jucLock = new ReentrantLock();
	
	public static AtomicLong atoCounter = new AtomicLong();
	public static volatile long volCounter = 0L;
	
	public Test(Long[] paramlist, final int threadId, final CyclicBarrier barrier, final long iterationLimit, final int numThreads, final WorkloadType workloadType) {
		this.barrier = barrier;
		this.iterationLimit = iterationLimit;
		this.numThreads = numThreads;
		this.threadId = threadId; 
		this.workloadType = workloadType;
		list = paramlist;
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
	      case DWRIT: workload1(); break;
	      case DREAD: workload2(); break;
	      case CWRIT: workload3(); break;
	      case CREAD: workload4(); break;
	      case SDWRIT: workload5(); break;
	      case SDREAD: workload6(); break;
	      case SCWRIT: workload7(); break;
	      case SCREAD: workload8(); break;
	      case LDWRIT: workload9(); break;
	      case LDREAD: workload10(); break;
	      case LCWRIT: workload11(); break;
	      case LCREAD: workload12(); break;
	    }
	}
	
	// Distributed Write - No contention - DWRIT
	private void workload1() {
		int count = (int) (iterationLimit / numThreads);
		int start = count * threadId;
		for ( int i = start; i < start+count; i++ ) {
			++list[i];
		}
	}
	
	// Distributed Read - No contention - DREAD
	private void workload2() {
		int count = (int) (iterationLimit / numThreads);
		int start = count * threadId;
		long temp = 0L;
		for ( int i = start; i < start+count; i++ ) {
			temp = list[i];
		}
	}
	
	// Contended Random Write - Contention - CREAD
	private void workload3() {
		int count = (int) (iterationLimit / numThreads);
//		int start = (int)(Math.random() * ((iterationLimit - count) + 1));
		for ( int i = 0; i < count; i++ ) {
			++list[i];
		}
	}
	
	// Contended Random Read - Contention - CWRIT
	private void workload4() {
		int count = (int) (iterationLimit / numThreads);
		long temp = 0;
		for ( int i = 0; i < count; i++ ) {
			temp = list[i];
		}
	}
	
	// Synchronized Workload 1 - SDWRIT
	private void workload5() {
//		int count = (int) (iterationLimit / numThreads);
//		int start = count * threadId;
//		for ( int i = start; i < start+count; i++ ) {
//			synchronized (jvmLock) {
//				++list[i];
//			}
//		}
	}
	
	// Synchronized Workload 2 -SDREAD
	private void workload6() {
//		int count = (int) (iterationLimit / numThreads);
//		int start = count * threadId;
//		long temp = 0L;
//		for ( int i = start; i < start+count; i++ ) {
//			synchronized (jvmLock) {
//				temp = list[i];
//			}
//		}
	}
	
	// Synchronized Workload 3 - SCWRIT
	private void workload7() {
		int count = (int) (iterationLimit / numThreads);
//		int start = (int)(Math.random() * ((iterationLimit - count) + 1));
		for ( int i = 0; i < count; i++ ) {
			synchronized (jvmLock) {
				++list[i];
			}
		}
	}
	
	// Synchronized Workload 4 - SCREAD
	private void workload8() {
		int count = (int) (iterationLimit / numThreads);
		long temp = 0;
		for ( int i = 0; i < count; i++ ) {
			synchronized (jvmLock) {
				temp = list[i];
			}
		}
	}
	
	// Synchronized Workload 1 - SDWRIT
	private void workload9() {
//		int count = (int) (iterationLimit / numThreads);
//		int start = count * threadId;
//		for ( int i = start; i < start+count; i++ ) {
//			jucLock.lock();
//		    try {
//		    	++list[i];
//		    }
//		    finally {
//		    	jucLock.unlock();
//		    }
//		}
	}
	
	// Synchronized Workload 2 -SDREAD
	private void workload10() {
//		int count = (int) (iterationLimit / numThreads);
//		int start = count * threadId;
//		long temp = 0L;
//		for ( int i = start; i < start+count; i++ ) {
//			jucLock.lock();
//		    try {
//		    	temp = list[i];
//		    }
//		    finally {
//		    	jucLock.unlock();
//		    }
//		}
	}
	
	// Synchronized Workload 3 - SCWRIT
	private void workload11() {
		int count = (int) (iterationLimit / numThreads);
//			int start = (int)(Math.random() * ((iterationLimit - count) + 1));
		for ( int i = 0; i < count; i++ ) {
			jucLock.lock();
		    try {
		    	++list[i];
		    }
		    finally {
		    	jucLock.unlock();
		    }
		}
	}
	
	// Synchronized Workload 4 - SCREAD
	private void workload12() {
		int count = (int) (iterationLimit / numThreads);
		long temp = 0;
		for ( int i = 0; i < count; i++ ) {
			jucLock.lock();
		    try {
		    	temp = list[i];
		    }
		    finally {
		    	jucLock.unlock();
		    }
		}
	}
	
}
