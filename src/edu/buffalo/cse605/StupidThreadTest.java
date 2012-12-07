package edu.buffalo.cse605;

public class StupidThreadTest {
    private static int nThreads;
    
    public static double doSomeStuff() {
        double uselessSum = 0;
        for (int i=0; i<10000; i++) {
            for (int j=0;j<10000; j++) {
                uselessSum += (double) i + (double) j;
            }
        }
//        return 0;
        return uselessSum;
    }

    public static void main(String[] args) throws InterruptedException {
//        double uselessSum = doSomeStuff();
    	for (int j=0;j<10; j++) {
    		doSomeStuff();
    	}
        
        nThreads = Integer.parseInt(args[0]);
        
        Thread[] threads = new Thread[nThreads];
        
        for (int i=0; i<nThreads; i++) {
        	threads[i] = new Thread(new Runnable() {
        		public void run() { 
        			doSomeStuff(); 
        		}
        	});
        }
        
        final long start = System.currentTimeMillis();
        for (int i = 0; i < threads.length; i++)
            threads[i].start();
        for (int i = 0; i < threads.length; i++)
            threads[i].join();
        final long end = System.currentTimeMillis();
        System.out.println("Time: " + (end-start) + "ms");
//        System.out.println("counter =>" + uselessSum);
    }
}
