# Measuring impact of sychronization primitives in highly concurrent systems

Data synchronization is the core essential for writing concurrent programs. Ideally, a synchronization technique should be able to fully exploit the available cores, leading to improved performance. This study investigates aspects of synchronization and co-ordination for large scale concurrent systems.

## Complexitities involving Concurrency

Concurrency means when two or more tasks happen in parallel, it also means that they contend on access to resources. The contended resource may be a database, file, socket or even a location in memory. Concurrent execution of code is all about, 

1. Mutual exclusion
2. Visibility of change

**Mutual exclusion** is about protecting shared resources from being arbitrarily acessed by managing contended updates it.

**Visibility of change** is about controlling when changes made to a shared resource are visible to other threads. 

It is possible to avoid the need for mutual exclusion if the need for contended updates is eliminated. If your algorithm can guarantee that any given resource is modified by only one thread, then mutual exclusion is unnecessary. Read and write operations require that all changes are made visible to other threads. However only contended write operations require the mutual exclusion of the changes. 

The most costly operation in any concurrent environment is a **contended write access**.  To have multiple threads write to the same resource requires complex and expensive coordination.  Typically this is achieved by employing a synchronization primitive.

### Types of Synchronization Primitives

1. Lock
2. Semaphore
3. RWLock - Read/Write Lock
4. Condition Variables
5. Monitors

[Do I need to explain ?]


### Cost of Locks

While locks provide a way of keeping threads out of each other's way, they really don't provide a mechanism for them to cooperate (synchronize). Locks provide mutual exclusion and ensure that the visibility of change occurs in an ordered manner. 

Locks are very expensive because they require arbitration when contended. This arbitration is achieved by a context switch to the operating system kernel which will suspend threads waiting on a lock until it is released. During such a context switch, as well as releasing control to the operating system which may decide to do other tasks while it has control, execution context can lose previously cached data and instructions. 

The other reason for the expense is the effect inter-process communication has on the memory system. After a lock has been taken, the program is very likely to access memory that may have been recently modified by another thread. If this thread ran on another processor, it is necessary to ensure that all pending writes on all other processors have been flushed so that the current thread sees the updates (visibility of change). The cost of doing this largely depends on how the memory system works and how many writes need to be flushed. This cost can be pretty high in the worst case and thus have a serious performance impact on modern processors. 

In addition to the raw overhead of entering and leaving locks, as the number of processors in the system grows, locks become the main impediment in using all the processors efficiently. If there are too few locks in the program, it is impossible to keep all the processors busy since they are waiting on memory that is locked by another processor. On the other hand, if there are many locks in the program, it is easy to have a "hot" lock that is being entered and exited frequently by many processors. This causes the memory-flushing overhead to be very high, and throughput again does not scale linearly with the number of processors. The only design that scales well is one in which worker threads can do significant work without interacting with shared data.

Fast user mode locks can be employed but these are only of any real benefit when not contended. The actual processors provide several instructions that simplify greatly the implementation of these non-blocking algorithms, the most-used operation today is the compare-and-swap operation (CAS). This operation takes three parameters, the memory address, the expected current value and the new value. It atomically update the value at the given memory address if the current value is the expected, otherwise it do nothing. In both cases, the operation return the value at the address after the operation execution. So when several threads try to execute the CAS operation, one thread wins and the others do nothing. So the caller can choose to retry or to do something else. 

As show below,

[Graph]

// Talk about AtomicInteger, BiasedLock, UnbiasedLock, Reentrant Lock
<!--
b) motivating scenario, what deployment and/or software system would benefit from the project
c) implementation details -- structure of code, constraints, limitations
d) evaluation methodology -- what are your measurement techniques, what benchmarks -- how does this compare to a and b, what metrics are you using (performance, memory, predictability, etc.) 
e) results -- what are the salient characteristics of your system (ie your measurements of d)
f) future work -- where would you take this project (ie how would you address the limitations outlined in c)
g) related work -- how does your project compare to other approaches
-->


## Benchmark

Our goal was to understand the performance of different synchronization primitives and observe how various parameters have an impact on the overall throughput of the system. Thus it was necessary to develop a micro-benchmarking harness which would test the same, simple to configure and have the ability to be extended to accomodate more tests as and when needed. 

### Benchmark Harness Design and Architecture

[Architecture Diagram]

##### 1. Micro Benchmark

> Why Micro-Benchmarks ?
> 
>1. Attempt to discover some narrow targeted fact
>2. Generally a timed tight loop around some “work”
>3. Report score as **iterations/sec** or **operations/sec** <br />
>		e.g. allocations/sec – object pooling vs GC

##### 2. Command Line Arguments
````
$ java -server edu.buffalo.cse605.Harness <TESTTYPE> <WORKLOADTYPE> <NUMTHREADS> <WARMUP>

TESTTYPE = {DEFAULT, JVM, JUC, JUCRW}

1. DEFAULT - No locking strategy
2. JVM - Synchronized method
3. JUC - java.util.concurrent.ReentrantLock
4. JUCRW - java.util.concurrent.ReentrantReadWriteLock


WORKLOADTYPE = {W1, W2, W3, W4, W5}

1. 100% Read (W1) - Reads iteratively/randomly from the list
2. 100% Write (W2) - Writes iteratively/randomly to the list
3. 80% Read, 20% Write (W3) 
4. 20% Read, 80% Write (W4)
5. 50% Read, 50% Write (W5)


NUMTHREADS = INTEGER (1,2,4,8,16,32,64)
WARMUP = 0 - No warmup; 1 - Warmup

````

##### 3. JVM Flag (-server)

> Tests were ran with `-server` flag set.

There are two types of the HotSpot JVM, namely `-server` and `-client`. The server VM uses a larger default size for the heap, a parallel garbage collector, and optimizes code more aggressively at run time. The client VM is more conservative, resulting in shorter startup time and lower memory footprint. Thanks to a concept called 'JVM ergonomics', the type of JVM is chosen automatically at JVM startup time based on certain criteria regarding the available hardware and operating system. The exact criteria can be found here. From the criteria table, we also see that the client VM is only available on 32-bit systems.

If we are not happy with the pre-selected JVM, we can use the flags -server and -client to prescribe the usage of the server and client VM, respectively. Even though the server VM was originally targeted at long-running server processes, nowadays it often shows superior performance than the client VM in many standalone applications as well.

[Graph] -server v/s -client

##### 4. Warmup [On Stack Replacement (OSR)]

JVM will compile code to achieve greater performance based on runtime profiling.  Some VMs run an interpreter for the majority of code and replace hot areas with compiled code following the 80/20 rule.  Other VMs compile all code simply at first then replace the simple code with more optimised code based on profiling.  Oracle Hotspot and Azul are examples of the first type and Oracle JRockit is an example of the second.

Hotspot JVM will count invocations of a method return plus branch backs for loops in that method, and if this exceeds 10K in server mode the method will be compiled.  The compiled code on normal JIT'ing can be used when the method is next called.  However if a loop is still iterating it may make sense to replace the method before the loop completes, especially if it has many iterations to go.  OSR is the means by which a method gets replaced with a compiled version part way through iterating a loop.

What this means is that you are likely to get better optimised code by doing a small number of shorter warm ups than a single large one.

[Graph] -with warmup and without warmup


### Lock Evaluation

We start out with evaluating the locking schemes available in the JDK. JDK locks come with two implementations. One uses atomic CAS style instructions to manage the claim process.  CAS instructions tend to be the most expensive type of CPU instructions. Often locks are un-contended which gives rise to a possible optimisation whereby a lock can be biased to the un-contended thread using techniques to avoid the use of atomic instructions.  This biasing allows a lock in theory to be quickly reacquired by the same thread.  If the lock turns out to be contended by multiple threads the algorithm with revert from being biased and fall back to the standard approach using atomic instructions.

#### Test 1

For the test I shall increment a counter within a lock, and increase the number of contending threads on the lock.  This test will be repeated for the 3 major lock implementations available to Java:
Atomic locking on Java language monitors
Biased locking on Java language monitors
ReentrantLock introduced with the java.util.concurrent package in Java 5.

This benchmark aims to provide a baseline and a general overview on systems performance varies based on various parameters and workloads as listed below. 

##### Parameters

1. Datastructure
2. Number of Elements
3. How many times the code is executed ( Hot Path)
4. Number of threads

> Note: This benchmark does not use any synchronization primitives.

##### Test
This test was performed on

1. **Elements:** 1 billion
2. **DataStructure:** Array
3. **Threads:** 1,2,4,8,16,32,64

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

##### Results
CREAD, CWRIT
DREAD, DWRIT
// Need to put graphs

##### Conclusion

Based on previous results, scaling is linear till a particular number of threads.
Performance is plateaued after a particular thread threshold is attained !


#### Test 2: Single Thread, multiple lock schemes.
 - Volatile
 - AtomicInteger
 - JVM Locks (Effect of Biased/Unbiased Locking)
 - JUC Locks (re entrant locks)



#### Test 3: Biased/Unbiased Locking JVM Warmup Test

Test Biased/Unbiased Locking Performance 

1. contention/no contention on resources
2. with warmup/non-warmup.

The testing framework included

1. 100, 1000 mil elements
2. synchronized method on Object (every iteration of the loop)
3. Biased Locking and Unbiased Locking


### Conclusion


