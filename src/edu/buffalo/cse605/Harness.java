package edu.buffalo.cse605;

import static java.lang.System.out;

import java.util.concurrent.CyclicBarrier;

import edu.buffalo.cse605.workloads.Default;
import edu.buffalo.cse605.workloads.JUC;
import edu.buffalo.cse605.workloads.JUCReadWrite;
import edu.buffalo.cse605.workloads.JVM;

public class Harness {
  public enum TestType {DEFAULT, JVM, JUC, JUCRW};
  public enum WorkloadType {W1, W2, W3, W4, W5};
  
  public static final int ITERATIONS = 100 * 1000 * 1000; // 1 BIL
  public static final int WARMUP_ITERATIONS = 1000 * 1000; //1 MIL
  private static WorkloadType workloadType;
  private static TestType testType;
  private static int numThreads;
  private static int warmUp;
  
  /**
   * @param args
   */
  public static void main(String[] args) throws Exception {
	  testType = TestType.valueOf(args[0]);
	  workloadType = WorkloadType.valueOf(args[1]);
	  numThreads = Integer.parseInt(args[2]);
	  warmUp = Integer.parseInt(args[3]);
	  
	  if ( warmUp == 1 ) {
		  out.printf("warmup\n");
		  switch (testType) {
			case DEFAULT:
				Default.setupList(WARMUP_ITERATIONS);
				break;
			case JVM:
				JVM.setupList(WARMUP_ITERATIONS);
				break;
			case JUC:
				JUC.setupList(WARMUP_ITERATIONS);
				break;
			case JUCRW:
				JUCReadWrite.setupList(WARMUP_ITERATIONS);
				break;
		  }
		  for ( int i = 0; i < 10; i++) {
			  runTest(numThreads, WARMUP_ITERATIONS);
	      }
	  }
	  
	  switch (testType) {
		case DEFAULT:
			Default.setupList(ITERATIONS);
			break;
		case JVM:
			JVM.setupList(ITERATIONS);
			break;
		case JUC:
			JUC.setupList(ITERATIONS);
			break;
		case JUCRW:
			JUCReadWrite.setupList(ITERATIONS);
			break;
	  }
	  
	  final long start = System.nanoTime();
	  runTest(numThreads, ITERATIONS);
	  final long duration = System.nanoTime() - start;
	  out.printf("%d threads, duration %,d (ns)\n", numThreads, duration);
	  out.printf("%,d ns/op\n", duration / ITERATIONS);
	  out.printf("%,d ops/s\n", (ITERATIONS * 1000000000L) / duration);
	  
	  switch ( workloadType ) {
	      case W1:
	    	  break;
	      default:
	    	  switch (testType) {
		  		case DEFAULT:
		  			Default.checkList(ITERATIONS);
		  			break;
		  		case JVM:
		  			JVM.checkList(ITERATIONS);
		  			break;
		  		case JUC:
		  			JUC.checkList(ITERATIONS);
		  			break;
		  		case JUCRW:
					JUCReadWrite.setupList(ITERATIONS);
					break;
		  	  }
	    	  break;
	  }
  }
  
  private static void runTest(final int numThreads, final long iterationLimit) throws Exception {
    CyclicBarrier barrier = new CyclicBarrier(numThreads);
    Thread[] threads = new Thread[numThreads];
    switch (testType) {
		case DEFAULT:
			for (int i = 0; i < numThreads; i++) {
				threads[i] = new Thread(new Default(i, barrier, iterationLimit, numThreads, workloadType));
			}
		break;
		case JVM:
			for (int i = 0; i < numThreads; i++) {
				threads[i] = new Thread(new JVM(i, barrier, iterationLimit, numThreads, workloadType));
			}
		break;
		case JUC:
			for (int i = 0; i < numThreads; i++) {
				threads[i] = new Thread(new JUC(i, barrier, iterationLimit, numThreads, workloadType));
			}
		break;
		case JUCRW:
			for (int i = 0; i < numThreads; i++) {
				threads[i] = new Thread(new JUCReadWrite(i, barrier, iterationLimit, numThreads, workloadType));
			}
			break;
    }

    for (Thread t : threads) {
        t.start();
    }

    for (Thread t : threads) {
        t.join();
    }
  }
}
