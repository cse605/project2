// Program

package edu.buffalo.cse605;

import static java.lang.System.out;
public class OSR {

	private static final int[] array = new int[10 * 1000];
	private static final long ITERATIONS = 1000 * 10000;
	static {
	    for (int i = 0; i < array.length; i++) {
	        array[i] = i;
	    }
	}

	public static void main(String[] args) {
		int osr = Integer.parseInt(args[0]); 
	    long t1 = System.nanoTime();
	    long result = 0;
	   
	    if ( osr == 1)  {
		    for (int i = 0; i < 1000 * 1000; i++) {    // outer loop
		        for (int j = 0; j < array.length; j++) {    // inner loop 1
		            result += array[j];
		        }
		        for (int j = 0; j < array.length; j++) {    // inner loop 2
		            result ^= array[j];
		        }
		    }
	    } else {
		    for (int i = 0; i < ITERATIONS; i++) {    // sole loop
		        result = add(result);
		        result = xor(result);
		    }
		 }


	    long t2 = System.nanoTime();
	    System.out.println("Execution time: " + ((t2 - t1) * 1e-9) +
	        " seconds to compute result = " + result);
		out.printf("%,d ns/op\n", (t2 - t1) / (ITERATIONS * array.length));
		out.printf("%,d ops/s\n", (ITERATIONS * array.length * 1000000000L) / (t2 - t1));
	}
	
	private static long add(long result) {    // method extraction of inner loop 1
	    for (int j = 0; j < array.length; j++) {
	        result += array[j];
	    }
	    return result;
	}
	
	private static long xor(long result) {    // method extraction of inner loop 2
	    for (int j = 0; j < array.length; j++) {
	        result ^= array[j];
	    }
	    return result;
	}
}