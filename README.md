## Benchmark Harness

#### About

**Number of Elements:** 100 million, 1 billion

**Number of threads:** 1,2,4,8,16,32,64

**Warmup**
Runs the actual test on Number of Elements/1000, 5 times

## 1. Benchmark Scale based on Element Size

#### Hypothesis

Ops/sec increases as thread count increases, i.e completion of action will increase based on number of threads. Expection: Scaling is linear

#### About

**Workload methodologies**

1. 100% Distributed* Read
2. 100% Distributed* Write
3. 100% Contended** Read
4. 100% Contended** Write

> ### Note
> 
> Thread load distribution (count) = No.of Elements/No.of Threads
> 
> **READ** = Read to a variable <br />
> **WRITE** = Increment value by 1
> 
> **Distributed** - All threads perform action on the entire array of elements <br />
> **Contended** - All threads perform action on "count" elements only

#### Testing Framework
This test was performed on 1 billion elements with warmup enabled.

````
arr=(1 2 4 8 16 32 64)

for i in "${arr[@]}"; 
do 
  java -server edu.buffalo.cse605.Harness DREAD $i 1
done
````

#### Results
CREAD, CWRIT
DREAD, DWRIT
// Need to put graphs

#### Conclusion

Based on previous results, scaling is linear till a particular number of threads.
Performance is plateaued after a particular thread threshold is attained !


## 2. Single Thread, multiple lock schemes.
 - Volatile
 - AtomicInteger
 - JVM Locks (Effect of Biased/Unbiased Locking)
 - JUC Locks (re entrant locks)



## 3. Biased/Unbiased Locking JVM Warmup Test

#### About 

Test Biased/Unbiased Locking Performance 

1. contention/no contention on resources
2. with warmup/non-warmup.

The testing framework included

1. 100, 1000 mil elements
2. synchronized method on Object (every iteration of the loop)
3. Biased Locking and Unbiased Locking

#### Results

##### Single Thread

````
+Biased Locking, No BiasedLocking Startup delay
// java -server -XX:+UseBiasedLocking -XX:BiasedLockingStartupDelay=0 edu.buffalo.cse605.Harness SCWRIT 1 0

// 100 mil
// No Warmup
1 threads, duration 1,169,366,000 (ns)
11 ns/op
85,516,425 ops/s

// Warmup
1 threads, duration 5,959,311,000 (ns)
59 ns/op
16,780,463 ops/s

// 1 bil
// No Warmup
1 threads, duration 11,537,811,000 (ns)
11 ns/op
86,671,553 ops/s

// Warmup
1 threads, duration 59,775,891,000 (ns)
59 ns/op
16,729,152 ops/s
````

````
-Biased Locking
// java -server -XX:-UseBiasedLocking edu.buffalo.cse605.Harness SCWRIT 1 0

// 100 mil
// No Warmup
1 threads, duration 5,983,186,000 (ns)
59 ns/op
16,713,503 ops/s

// Warmup
1 threads, duration 5,964,517,000 (ns)
59 ns/op
16,765,816 ops/s

// 1 bil
// No Warmup
1 threads, duration 59,659,301,000 (ns)
59 ns/op
16,761,845 ops/s

// Warmup
1 threads, duration 59,573,744,000 (ns)
59 ns/op
16,785,918 ops/s

````

##### Multiple Threads

````
// Synchronized Contended Write
// java -server -XX:+UseBiasedLocking -XX:BiasedLockingStartupDelay=0 edu.buffalo.cse605.Harness SCWRIT 2 1

// 1 bil
// No Warmup
2 threads, duration 339,554,299,000 (ns)
339 ns/op
2,945,037 ops/s

// Warmup
2 threads, duration 346,796,349,000 (ns)
346 ns/op
2,883,536 ops/s

````

````
// Synchronized Contended Write
// java -server -XX:-UseBiasedLocking edu.buffalo.cse605.Harness SCWRIT 2 1

// 1 bil
// No Warmup
2 threads, duration 284,770,693,000 (ns)
284 ns/op
3,511,597 ops/s

// Warmup
2 threads, duration 182,545,871,000 (ns)
182 ns/op
5,478,075 ops/s

4 threads, duration 409,813,507,000 (ns)
409 ns/op
2,440,134 ops/s

8 threads, duration 625,499,292,000 (ns)
625 ns/op
1,598,722 ops/s

16 threads, duration 469,001,827,000 (ns)
469 ns/op
2,132,187 ops/s

32 threads, duration 491,399,991,000 (ns)
491 ns/op
2,035,002 ops/s

64 threads, duration 490,956,540,000 (ns)
490 ns/op
2,036,840 ops/s

````

#### Conclusion

