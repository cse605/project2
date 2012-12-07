# Measuring (How to measure) impact of sychronization primitives in concurrent systems

Data synchronization is the core essential for writing concurrent programs. Ideally, a synchronization technique should be able to fully exploit the available cores, leading to improved performance. This study investigates aspects of synchronization and co-ordination for large concurrent systems. But before diving into that, this study also validates and aggregates some of the best methodologies to perform micro benchmarking in the JVM as found from various journals and studies.

## Complexitities involving Concurrency

Concurrency means when two or more tasks happen in parallel, it also means that they contend on access to resources. The contended resource may be a database, file, socket or even a location in memory. Concurrent execution of code is all about, 

1. Mutual exclusion
2. Visibility of change

**Mutual exclusion** is about protecting shared resources from being arbitrarily accessed by managing contended updates it.

**Visibility of change** is about controlling when changes made to a shared resource are visible to other threads. 

It is possible to avoid the need for mutual exclusion if the need for contended updates is eliminated. If an algorithm can guarantee that any given resource is modified by only one thread, then mutual exclusion is unnecessary. Read and write operations require that all changes are made visible to other threads. However only contended write operations require the mutual exclusion of the changes. 

The most costly operation in any concurrent environment is a **contended write access**.  To have multiple threads write to the same resource requires complex and expensive coordination.  Typically this is achieved by employing a synchronization primitive.

### Types of Synchronization Primitives

1. Lock
2. Semaphore
3. RWLock - Read/Write Lock
4. Condition Variables
5. Monitors

### Cost of Locks

While locks provide a way of keeping threads out of each other's way, they really don't provide a mechanism for them to cooperate (synchronize). Locks provide mutual exclusion and ensure that the visibility of change occurs in an ordered manner. 

Locks are very expensive because they require arbitration when contended. This arbitration is achieved by a context switch to the operating system kernel which will suspend threads waiting on a lock until it is released. During such a context switch, as well as releasing control to the operating system which may decide to do other tasks while it has control, execution context can lose previously cached data and instructions. 

The other reason for the expense is the effect inter-process communication has on the memory system. After a lock has been taken, the program is very likely to access memory that may have been recently modified by another thread. If this thread ran on another processor, it is necessary to ensure that all pending writes on all other processors have been flushed so that the current thread sees the updates (visibility of change). The cost of doing this largely depends on how the memory system works and how many writes need to be flushed. This cost can be pretty high in the worst case and thus have a serious performance impact on modern processors. 

In addition to the raw overhead of entering and leaving locks, as the number of processors in the system grows, locks become the main impediment in using all the processors efficiently. If there are too few locks in the program, it is impossible to keep all the processors busy since they are waiting on memory that is locked by another processor. On the other hand, if there are many locks in the program, it is easy to have a "hot" lock that is being entered and exited frequently by many processors. This causes the memory-flushing overhead to be very high, and throughput again does not scale linearly with the number of processors. The only design that scales well is one in which worker threads can do significant work without interacting with shared data.

Fast user mode locks can be employed but these are only of any real benefit when not contended. The actual processors provide several instructions that simplify greatly the implementation of these non-blocking algorithms, the most-used operation today is the compare-and-swap operation (CAS). This operation takes three parameters, the memory address, the expected current value and the new value. It atomically update the value at the given memory address if the current value is the expected, otherwise it do nothing. In both cases, the operation return the value at the address after the operation execution. So when several threads try to execute the CAS operation, one thread wins and the others do nothing. So the caller can choose to retry or to do something else. 

According to a white paper<sup>[1]</sup> presented by the folks at LMAX who developed Disrupter - A high performance alternative to bounded queue for exchanging data.

<table>
<tr>
<th>Method</th><th>Time msec</th>
</tr>
<tr>
<td>Single thread</td><td>300</td>
</tr>
<tr>
<td>Single thread with lock</td><td>10,000</td>
</tr>
<tr>
<td>Two threads with lock</td><td>224,000</td>
</tr>
<tr>
<td>Single thread with CAS</td><td>5,700</td>
</tr>
<tr>
<td>Two threads with CAS</td><td>30,000</td>
</tr>
<tr>
<td>Single thread with volatile write</td><td>4,700</td>
</tr>
</table>

<!--
b) motivating scenario, what deployment and/or software system would benefit from the project
c) implementation details -- structure of code, constraints, limitations
d) evaluation methodology -- what are your measurement techniques, what benchmarks -- how does this compare to a and b, what metrics are you using (performance, memory, predictability, etc.) 
e) results -- what are the salient characteristics of your system (ie your measurements of d)
f) future work -- where would you take this project (ie how would you address the limitations outlined in c)
g) related work -- how does your project compare to other approaches
-->


## Benchmark

We wish to understand the intrinsic performance properties of a specific lock implementation in different scenarios.

### Enter (Micro) benchmark

>  **microbenchmark** attempts to measure the performance of a "small" bit of code. These tests are typically in the sub-millisecond range. The code being tested usually performs no I/O, or else is a test of some single, specific I/O task.

Writing a micro-benchmark to test a particular idiom is very difficult. Most of the times, one writes a benchmark to measure some aspect of a system but ends up measuring either nothing or something else. The JVM is highly unpredictable and it is very important to understand some important facets about the runtime before writing a single line of benchmarking code.
Micro benchmarking is also very different than profiling! When profiling, one works with an entire application, either in production or in an environment which resembles production as much as possible. 

We wanted a "good" micro-benchmarking solution for our use case. After extensive study on previous research material from various authors, we aggregated and validated some of the most important factors which influence a good measurement study.

##### 1. Dead Code Elimination

"Dead code" is part of source code that compiler infers that its result is not used and does 
not affect output of program. Optimizing compilers are adept at spotting dead code. Normally, benchmark programs often don't produce any output, which means some, or all, of the code can be optimized away without one realizing it, at which point one is measuring less execution than actually there is.

Consider Listing 1<sup>[2]</sup>, where `doSomeStuff()` runs nested `for` loops to calculate the sum of the iterations. 
The `doSomeStuff()` method is supposed to give the threads something to do, so we can infer something about the scheduling overhead of multiple threads from the run time of `StupidThreadBenchmark`. However, the compiler can determine that all the code in `doSomeStuff` is dead, and optimize it all away because `uselessSum` is never used. Once the code inside the loop goes away, the loops can go away, too, leaving `doSomeStuff()` entirely empty !

````
// Listing 1: Shows how the compiler optimizes dead code
// StupidThreadTest.java

package edu.buffalo.cse605;

public class StupidThreadTest {
    private static int nThreads;
    
    public static double doSomeStuff() {
        double uselessSum = 0;
        for (int i=0; i<1000; i++) {
            for (int j=0;j<1000; j++) {
                uselessSum += (double) i + (double) j;
            }
        }
        
        return 0;
//      return uselessSum; -> This line makes all the difference
    }

    public static void main(String[] args) throws InterruptedException {
      doSomeStuff();
        
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
    }
}]

````

````
// Output with 'return 0;' enabled
$ java -server -XX:+PrintCompilation edu.buffalo.cse605.StupidThreadTest 1000
    107   1       java.lang.String::hashCode (64 bytes)
    160   2       sun.nio.cs.UTF_8$Decoder::decodeArrayLoop (553 bytes)
    173   3       java.math.BigInteger::mulAdd (81 bytes)
    178   4       java.math.BigInteger::multiplyToLen (219 bytes)
    184   5       java.math.BigInteger::addOne (77 bytes)
    188   6       java.math.BigInteger::squareToLen (172 bytes)
    190   7       java.math.BigInteger::primitiveLeftShift (79 bytes)
    196   8       java.math.BigInteger::montReduce (99 bytes)
    219   9       sun.security.provider.SHA::implCompress (491 bytes)
    236  10       java.lang.String::charAt (33 bytes)
    237   1%      edu.buffalo.cse605.StupidThreadTest::doSomeStuff @ 12 (42 bytes)
    242  11       java.lang.Object::<init> (1 bytes)
    249  12       java.lang.ThreadLocal$ThreadLocalMap::<init> (148 bytes)
    256  13       edu.buffalo.cse605.StupidThreadTest::doSomeStuff (42 bytes)
Time: 110ms
````

````
// Output with 'return uselessSum;' enabled
$ java -server -XX:+PrintCompilation edu.buffalo.cse605.StupidThreadTest 1000
    106   1       java.lang.String::hashCode (64 bytes)
    160   2       sun.nio.cs.UTF_8$Decoder::decodeArrayLoop (553 bytes)
    171   3       java.math.BigInteger::mulAdd (81 bytes)
    175   4       java.math.BigInteger::multiplyToLen (219 bytes)
    182   5       java.math.BigInteger::addOne (77 bytes)
    186   6       java.math.BigInteger::squareToLen (172 bytes)
    187   7       java.math.BigInteger::primitiveLeftShift (79 bytes)
    191   1%      java.math.BigInteger::multiplyToLen @ 138 (219 bytes)
    209   8       java.math.BigInteger::montReduce (99 bytes)
    219   9       sun.security.provider.SHA::implCompress (491 bytes)
    248  10       java.lang.String::charAt (33 bytes)
    249   2%      edu.buffalo.cse605.StupidThreadTest::doSomeStuff @ 12 (42 bytes)
    263  11       java.lang.Object::<init> (1 bytes)
    267  12       java.lang.ThreadLocal$ThreadLocalMap::<init> (148 bytes)
    275  13       edu.buffalo.cse605.StupidThreadTest::doSomeStuff (42 bytes)
Time: 978ms
````
**Conclusion : ** As from the above example, JVM decides to do optimization by eliminating dead code and thus there is a 10x performance difference. Remember, that in this case, the purpose of the task was defeated because of JVM's aggressive optimization.

##### 2. JVM Modes (-server and -client)

There are two types of the HotSpot JVM, namely `-server` and `-client`. The server VM uses a larger default size for the heap, a parallel garbage collector, and optimizes code more aggressively at run time. The client VM is more conservative, resulting in shorter startup time and lower memory footprint.

In particular, many micro benchmarks perform much "better" when run with `-server` than with `-client`, not because the server compiler is faster (though it often is) but because the server compiler is more adept at optimizing away blocks of dead code. (More Explaination about `-server` and `-client` in the next section).

##### 3. Warmup

JVM will compile code to achieve greater performance based on runtime profiling.  Some VMs run an interpreter for the majority of code and replace hot areas with compiled code following the 80/20 rule (Hotspot). Other VMs compile all code simply at first then replace the simple code with more optimised code based on profiling (Azul VM).  

Hotspot JVM will count invocations of a method return plus branch backs for loops in that method, and if this exceeds 10K in server mode the method will be compiled. The compiled code on normal JIT'ing can be used when the method is next called.

What this means is that, one is likely to get better optimized code by doing a small number of shorter warm ups than a single large one.

> NOTE: When using thin locks (Atomic), it is important to create new primitive object after every warmup run. Although warm up aids in compiling "hot code" paths, it may force the JVM to convert the thin locks into fat locks leading to performance drops.

````
// With Warmup
$ java -server -XX:+PrintCompilation -verbose:gc edu.buffalo.cse605.TestLocks ATO 2 1
Warmup
    131    1             java.util.concurrent.atomic.AtomicLong::getAndIncrement (23 bytes)
    131    2     n       sun.misc.Unsafe::compareAndSwapLong (0 bytes)   
    131    3     n       sun.misc.Unsafe::compareAndSwapLong (0 bytes)   
    132    4             java.util.concurrent.atomic.AtomicLong::compareAndSet (13 bytes)
    137    5             java.util.concurrent.atomic.AtomicLong::get (5 bytes)
    140    1 %           edu.buffalo.cse605.TestLocks::atoInc @ 15 (34 bytes)
    151    6             edu.buffalo.cse605.TestLocks::atoInc (34 bytes)
2 threads, duration 73,556,175,000 (ns)
147 ns/op
6,797,525 ops/s
counter = 500000000
````


````
// Without Warmup
$ java -server -XX:+PrintCompilation -verbose:gc edu.buffalo.cse605.TestLocks ATO 2 0
    126    1             java.util.concurrent.atomic.AtomicLong::getAndIncrement (23 bytes)
    126    2     n       sun.misc.Unsafe::compareAndSwapLong (0 bytes)   
    127    3     n       sun.misc.Unsafe::compareAndSwapLong (0 bytes)   
    127    4             java.util.concurrent.atomic.AtomicLong::compareAndSet (13 bytes)
    133    5             java.util.concurrent.atomic.AtomicLong::get (5 bytes)
    136    1 %           edu.buffalo.cse605.TestLocks::atoInc @ 15 (34 bytes)
2 threads, duration 48,962,594,000 (ns)
97 ns/op
10,211,877 ops/s
counter = 500000000
````

````
// after ensuring that AtomicLong was created after every warmup run
$ java -server -XX:+PrintCompilation -verbose:gc edu.buffalo.cse605.TestLocks ATO 2 1
Warmup
   124    1             java.util.concurrent.atomic.AtomicLong::getAndIncrement (23 bytes)
   124    2     n       sun.misc.Unsafe::compareAndSwapLong (0 bytes)   
   124    3     n       sun.misc.Unsafe::compareAndSwapLong (0 bytes)   
   125    4             java.util.concurrent.atomic.AtomicLong::compareAndSet (13 bytes)
   131    5             java.util.concurrent.atomic.AtomicLong::get (5 bytes)
   133    1 %           edu.buffalo.cse605.TestLocks::atoInc @ 15 (34 bytes)
   145    6             edu.buffalo.cse605.TestLocks::atoInc (34 bytes)
2 threads, duration 53,821,730,000 (ns)
107 ns/op
9,289,928 ops/s
counter = 500000000
````
##### 4. On-Stack Replacement [OSR]

With the context of "Warmup" i.e compiling hot code instead of JITing, if a loop is still iterating it may make sense to replace the method before the loop completes, especially if it has many iterations to go.  On Stack Replacement [OSR] is the means by which a method gets replaced with a compiled version part way through iterating a loop.

The effects are illustrated in Listing 2 <sup>[3]</sup>

````
// Listing 2: Test the effects of OSR

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
}]
````

````
// OSR
$ java -server -XX:+PrintCompilation -verbose:gc edu.buffalo.cse605.OSR 1
    131    1 %           edu.buffalo.cse605.OSR::main @ 18 (196 bytes)
 142326    1 %           edu.buffalo.cse605.OSR::main @ -2 (196 bytes)   made not entrant
Execution time: 142.19685800000002 seconds to compute result = 499950000000000
1 ns/op
54,616,394 ops/s
````


````
// NO OSR
$ java -server -XX:+PrintCompilation -verbose:gc edu.buffalo.cse605.OSR 0
    130    1             edu.buffalo.cse605.NOOSR::add (27 bytes)
    130    2             edu.buffalo.cse605.NOOSR::xor (27 bytes)
    139    1 %           edu.buffalo.cse605.NOOSR::xor @ 5 (27 bytes)
    380    2 %           edu.buffalo.cse605.NOOSR::main @ 12 (150 bytes)
 147839    2 %           edu.buffalo.cse605.NOOSR::main @ -2 (150 bytes)   made not entrant
Execution time: 147.71015500000001 seconds to compute result = 499950000000000
1 ns/op
52,577,831 ops/s
````

##### 5. Deoptimization

The JVM can stop using a compiled method and return to interpreting it for a while before recompiling it. This can happen when assumptions made by an optimizing dynamic compiler have become outdated. One example is class loading that invalidates monomorphic call transformations (Converting a virtual method call to a direct method call is called monomorphic call transformation). 

Another example is uncommon traps: when a code block is initially compiled, only the most likely code path is compiled, while atypical branches (such as exception paths) are left interpreted. But if the uncommon traps turn out to be commonly executed, then they become hotspot paths that trigger recompilation.

````
// Listing 3: How inlining can lead to better dead-code optimization
public class Inline {
  public final void inner(String s) {
    if (s == null)
      return;
    else {
      // do something really complicated
    }
  }

  public void outer() {
    String s=null; 
    inner(s);
  }
}
````

Listing 3 <sup></sup> shows an example of the type of optimization that is enabled through inlining. The outer() method calls inner() with an argument of null, which will result in inner() doing nothing. But by inlining the call to inner(), the compiler can see that the else branch of inner() is dead code, and can optimize the test and the else branch away, at which point it can optimize away the entirety of the call to inner(). Had inner() not been inlined, this optimization would not have been possible.

##### 6. JVM options

Some relevant JVM options are:

1. Type of JVM: server (-server) versus client (-client).
2. Ensuring sufficient memory is available (-Xmx).
3. Type of garbage collector used (advanced JVMs offer many tuning options, but be careful).
4. Whether or not class garbage collection is allowed (-Xnoclassgc). The default is that class GC occurs; it has been argued that using -Xnoclassgc is a bad idea.
5. Whether or not escape analysis is being performed (-XX:+DoEscapeAnalysis).
6. Whether or not large page heaps are supported (-XX:+UseLargePages).
7. If thread stack size has been changed (for example, -Xss128k).
8. Whether or not JIT compiling is always used (-Xcomp), never used (-Xint), or only done on hotspots (-Xmixed; this is the default, and highest performance option).
9. The amount of profiling that is accumulated before JIT compilation occurs (-XX:CompileThreshold), and/or background JIT compilation (-Xbatch), and/or tiered JIT compilation (-XX:+TieredCompilation).
10. Whether or not biased locking is being performed (-XX:+UseBiasedLocking); note that JDK 1.6+ automatically does this.
11. Whether or not the latest experimental performance tweaks have been activated (-XX:+AggressiveOpts).
12. Enabling or disabling assertions (-enableassertions and -enablesystemassertions).
13. Enabling or disabling strict native call checking (-Xcheck:jni).
14. Enabling memory location optimizations for NUMA multi-CPU systems (-XX:+UseNUMA).


## Tests

### 1. Lock Evaluation

We start out with evaluating the locking schemes available in the JDK. 
> CAS instructions tend to be the most expensive type of CPU instructions (architecture dependent). Locks are often un-contended which gives rise to a possible optimization whereby a lock can be biased to the un-contended thread using techniques to avoid the use of atomic instructions. This biasing allows a lock in theory to be quickly reacquired by the same thread. If the lock turns out to be contended by multiple threads the algorithm with revert from being biased and fall back to the standard approach using atomic instructions.

#### Test 0: Increment shared counter

In this test, we increment a counter within a lock, and increase the number of contending threads on the lock.  This test is repeated for the 3 major lock implementations available to Java:

1. Atomic locking on Java language monitors
2. Biased locking on Java language monitors
3. ReentrantLock introduced with the java.util.concurrent package.

##### Setup

1. **Iterations:** 1 Billion
2. **Data:** Counter
3. **Threads:** 1,2,4,8,16,32,64

##### Code

````
// [TestLocks.java]
````

##### Results
<img src='http://dl.dropbox.com/u/32194349/Graph%200.png' />

##### Conclusion

From the graph, we can see that when we have more threads, due to the contention, the performance does not increase. Actually, we can see that the performance with one thread is the best.

1. We can see that when using locks, the performance drops dramatically. Among the four lock schemes that we use, we can find that the biased lock works best.


#### Test 1: Single Thread Performance for various locking schemes.

In this test, we determine the single thread performance of various locking schemes viz.

1. Volatile
2. AtomicLong (CAS)
3. JVM Locks (Effect of Biased/Unbiased Locking)
4. JUC Locks (Reentrant lock)

##### Setup

1. **Iterations:** 1 Billion
2. **Data:** Counter
3. **Threads:** 1

##### Code
````
[TestLocks.java]
````

##### Results

<table>
<tr>
<th>Method</th>
<th>Method ops/sec (Without Warmup)</th>
<th>Method ops/sec (With Warmup)</th>
<th>% compared to normal case (With Warmup)</th>
</tr>
<tr>
<td>Normal</td>
<td>516,866,383</td>
<td>580,879,311</td>
<td>n/a</td>
</tr>
<tr>
<td>Volatile</td>
<td>36,364,547</td>
<td>32,720,340</td>
<td>18X drop</td>
</tr>
<tr>
<td>ATO</td>
<td>38,507,808</td>
<td>38,314,857</td>
<td>15X drop</td>
</tr>
<tr>
<td>JVM</td>
<td>18,256,106</td>
<td>17,337,213</td>
<td>34X drop</td>
</tr>
<tr>
<td>JVM (+Biased)</td>
<td>167,927,071</td>
<td>177,511,904</td>
<td>3X drop</td>
</tr>
<tr>
<td>JUC</td>
<td>18,019,544</td>
<td>18,058,368</td>
<td>32X drop</td>
</tr>
</table>

##### Graph

<img src='http://dl.dropbox.com/u/32194349/Graph%201.png' />
<img src='http://dl.dropbox.com/u/32194349/Graph%202.png' />

##### Conclusion

1. Compares the results of methods without warm up and the methods with warm up. When having warm up, we should expect that the performance should be better. But from this graph, we can see that for volatile and atomic, it is not the case. So in benchmark when using thin locks (Atomic), it is important to create new primitive object after every warm up run. Although warm up aids in compiling "hot code" paths, it may force the JVM to convert the thin locks into fat locks leading to performance drops.

#### Test 3: Performance for various locking schemes under contention

Similar to the previous test, in this test, we determine the  performance of various locking schemes under contention viz.

1. Volatile
2. AtomicLong (CAS)
3. JVM Locks (Effect of Biased/Unbiased Locking)
4. JUC Locks (Reentrant lock)

##### Setup

1. **Iterations:** 1 Billion
2. **Data:** Counter
3. **Threads:** 2

##### Code
````
[TestLocks.java]
````

##### Results

<table>
<tr>
<th>Method</th>
<th>Method (Without Warmup)</th>
<th>Method (With Warmup)</th>
<th>% drop when compared with its single thread implementation (With Warmup)</th>
</tr>
<tr>
<td>Normal</td>
<td>516,866,383</td>
<td>580,879,311</td>
<td>n/a</td>
</tr>
<tr>
<td>ATO</td>
<td>10,554,537</td>
<td>8,728,628</td>
<td>4X drop</td>
</tr>
<tr>
<td>JVM</td>
<td>4,172,108</td>
<td>4,234,268</td>
<td>4X drop</td>
</tr>
<tr>
<td>JVM (+Biased)</td>
<td>6,598,297</td>
<td>3,187,797</td>
<td>56X drop</td>
</tr>
<tr>
<td>JUC*</td>
<td>2,865,952</td>
<td>2,633,649</td>
<td>7X drop</td>
</tr>
</table>

> * The JUC and JUC (warmup) benchmark under contention exhibited GC behavior (17 (+0.082493 sec) and 25 (+0.116112 sec) times respectively).

##### Graph

<img src='http://dl.dropbox.com/u/32194349/Graph%203.png' />
<img src='http://dl.dropbox.com/u/32194349/Graph%204.png' />

### Conclusion

1. This test yielded a very interesting result, When done on an Intel machine (1.7Ghz Intel Core i5 Sandybridge), the result is better than the normal case which is what we expected. But when we use the same benchmark on the '64 Core - AMD Opteron Machine' machine, the performance is worse than the normal case. We speculate this bizarre result maybe due to the difference in architecture of the machines and the overhead with CAS operations.


### 2. Our Benchmark Lock Tests

Now that we have a fair understanding of some important factors to consider when writing benchmarks, we would like to present our findings on the different locking schemes based on a number of workload scenarios.

#### Benchmark Harness Architecture

<img src='http://dl.dropbox.com/u/32194349/architecture.png' />

<img src='http://dl.dropbox.com/u/32194349/array.png' />

**Data Strucuture**: Array, 1 billion Elements

##### Features

1. Uses `System.nanoTime` instead of `System.currentTimeMillis` for higher resolution
2. Uses `CyclicBarrier` to ensure threads start the workloads at the same time.
3. Code Warmup.  In general, the initial performance is usually relatively slow, and then it greatly improves for a while (usually in discrete leaps) until it reaches a steady state. 
4. Dead Code Elimination
5. Ensures all Class Loading is done before the actuall test (atleast most of it)
6. Use of JVM flags such as -XX:+PrintCompilation, -verbose:gc to understand method compilation and GC stats.

##### What does the benchmark aim to do ?

We evaluate every individual lock by running the benchmark under different levels of contention in the following workloads 

**Workloads**

1. W1 - 100% Read - Reads iteratively/randomly from the list
2. W2 - 100% Write - Writes iteratively/randomly to the list
3. W3 - 80% Read, 20% Write
4. W4 - 20% Read, 80% Write
5. W5 - 50% Read, 50% Write

**Using the Harness**

````
$ java -server edu.buffalo.cse605.Harness <TESTTYPE> <WORKLOADTYPE> <NUMTHREADS> <WARMUP>

Parameters

TESTTYPE = {DEFAULT, JVM, JUC, JUCRW}

1. DEFAULT - No locking strategy
2. JVM - Synchronized method
3. JUC - java.util.concurrent.ReentrantLock
4. JUCRW - java.util.concurrent.ReentrantReadWriteLock

WORKLOADTYPE = {W1, W2, W3, W4, W5}

NUMTHREADS = INTEGER (1,2,4,8,16,32,64)
WARMUP = 0 - No warmup; 1 - Warmup
````

````
 # run.sh
threads=(1 2 4 8 16 32 64)
workloads=(W1 W2 W3 W4 W5)

 # warmup
for i in "${workloads[@]}"; 
do
  for j in "${threads[@]}"; 
  do 
      java -server edu.buffalo.cse605.Harness $i $e $j 1
  done
done

 # No warmup
for i in "${workloads[@]}"; 
do
  for j in "${threads[@]}"; 
  do 
      java -server edu.buffalo.cse605.Harness $i $e $j 0
  done
done
````

#### Test 1: Workload Performance without any synchronization primitives

<img src='http://dl.dropbox.com/u/32194349/Graph%205.png' />


#### Test 2a: Workload Performance with JVM (Sync) Locking

<img src='http://dl.dropbox.com/u/32194349/Graph%206.png' />

<img src='http://dl.dropbox.com/u/32194349/Graph%207.png' />

#### Test 2b: Workload Performance with Biased JVM (Sync) Locking

<img src='http://dl.dropbox.com/u/32194349/Graph%208.png' />

<img src='http://dl.dropbox.com/u/32194349/Graph%209.png' />

#### Test 3: Workload Performance with JUC (Reentrant) Locking

<img src='http://dl.dropbox.com/u/32194349/Graph%2010.png' />

<img src='http://dl.dropbox.com/u/32194349/Graph%2011.png' />

#### Test 3: Workload Performance with JUC (Reentrant) Locking

<img src='http://dl.dropbox.com/u/32194349/Graph%2010.png' />

<img src='http://dl.dropbox.com/u/32194349/Graph%2011.png' />

> We saw a lot of GC activity when running these benchmarks, ~ +1 sec added to the execution time.

#### Test 4: Workload Performance with JUC (ReadWrite) Locking

<img src='http://dl.dropbox.com/u/32194349/Graph%2012.png' />

<img src='http://dl.dropbox.com/u/32194349/Graph%2013.png' />

> We saw a lot of GC activity when running these benchmarks, ~ +2 sec added to the execution time.

## Conclusion

We started this study in a bid to learn the overhead of locking primitives in JAVA, but we went onto understand the various facets involved when benchmarking JAVA programs.
It is very important to answer the ""what,why and how to measure your system" before actually moving forward to write a benchmark to do it.

1. Locking is expensive, remember there is 27x overhead (single thread) and over 175x overhead (contention) when compared to normal case.
2. CAS is relatively cheap on the newer machines and highly architecture dependent. It does not scale well.
3. Biased Locking is highly recommended when there is no or very little contention.
4. JVM is free to optimize your code how ever it feels. While benchmarking make sure to address "Dead code elimination", "De-optimization", "OSR", "GC Kickins".

## References

[1] [http://lmax-exchange.github.com/disruptor/files/Disruptor-1.pdf](http://lmax-exchange.github.com/disruptor/files/Disruptor-1.pdf)

[2] [http://www.ibm.com/developerworks/java/library/j-jt1221/](http://www.ibm.com/developerworks/java/library/j-jt1221/)

[3] [Robust Java benchmarking](http://www.ibm.com/developerworks/java/library/j-benchmark1/index.html)

[Mechanical Sympathy](http://mechanical-sympathy.blogspot.com/)

[Cliff Click's Blog](http://www.azulsystems.com/blog/cliff)

[Art of Benchmarking by Cliff Click](http://www.azulsystems.com/sites/www.azulsystems.com/Cliff_Click_Art_of_Java_Benchmarking.pdf)

[Bad Concurrency](http://bad-concurrency.blogspot.com/) by Micheal Barker

[Details about -XX:PrintCompilation](https://gist.github.com/1165804#Notes.md)

[Caliper - Microbenchmarking framework for Java](http://code.google.com/p/caliper/wiki/JavaMicrobenchmarks)

[1]: http://lmax-exchange.github.com/disruptor/files/Disruptor-1.0.pdf
[2]: http://www.ibm.com/developerworks/java/library/j-jtp12214/
[3]: http://www.ibm.com/developerworks/java/library/j-benchmark1/index.html