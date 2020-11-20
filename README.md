# ParallelJUnit
Custom JUnit test runner that enables executing JUnit test methods in concurrent parallels threads by complimenting JUnit with the use of the *@Test* alternative annotations:

*@ParallelTest* 

*@ParallelizationTest*


## Examples
```java
    import org.junit.Assert;
    import org.junit.Test;
    import org.junit.runner.RunWith;    
    
    //Don't forget to use the adapted JUnit test runner
    @RunWith(JUnitWithPerf.class)
    public class Tests {
    
       //Regular JUnit test - passed on to default JUnit runner.
       @Test 
       public void passingRegularTestShouldPass(){
           Assert.assertTrue(true);
       }
       
       //Four parallel concurrent threads, passing timeout (ms)
       @ParallelTest(threadCount = 4, timeout = 200) 
       public void parallelTestShouldPass() throws InterruptedException {
           Assert.assertTrue(true);
           Thread.sleep(100);
           Assert.assertTrue(true);
       }

       //Bad test case since it's fuzzy. Ten parallel threads. Expecting some threads to throw exceptions.
       @ParallelTest(threadCount = 10, expected = TestMethodExecutionException.class)  
       public void exceptionThrowingTestMethodShouldThrowException() throws Exception {
           Assert.assertTrue(true);
           if(Math.random() * 100 < 50){
               throw new Exception("Oups");
           }
           Assert.assertTrue(true);
       }
       
       //Runs once single-threaded for warm-up, once single-threaded for benchmark, and once multi-threaded for comparison. 
       @ParallelizationTest(multipleThreadsCount = 3, maxExecutionDurationMultipleForMultipleThreadsExecution = 1.5)
       public void parallelizationTest() throws InterruptedException {
           Thread.sleep(100);
       }
    }
```

## ParallelTest annotation
The **@ParallelTest** is used instead of the **@Test** annotation of regular JUnit. When the **@ParallelTest** is used the test method is executed concurrently in multiple threads. The number of concurrent threads may be set by the **threadCount** argument. Default **threadCount** for ParallelTest is 2.

Regular JUnit **@Test** optional arguments **timeout** and **expected** applies for **@ParallelTest** too.

## ParallelizationTest
A variant of **ParallelTest** is testing if a tested method seem to be able to handle sequential execution or truly parallel execution. In order to do this in an easy fashion the **@ParallelizationTest** is used. It execute the test method in a single thread first (to avoid first execution initialization problems), then clocks how log the execution of the test method takes with execution with one thread. After this the same method is executed in multiple parallel threads to see if this takes significantly longer than the execution in a single thread. 

To summarize the execution sequence:
1. Executing once in one thread as a warm-up (populating caches, ready-compiling components and so forth)
1. Executing once more in a timed execution with one single thread.
1. Executing with multiple concurrent and parallel threads in a timed execution run.
1. Evaluating the success by assessing the ratio between the single thread execution duration with the multiple parallel execution duration variable (using **maxExecutionDurationMultipleForMultipleThreadsExecution** parameter). 

For this type of test the following parameters apply:
* Number of concurrent threads when executed in parallel: **multipleThreadsCount** (default = 3).
* Duration ration to assess test success towards **maxExecutionDurationMultipleForMultipleThreadsExecution** (default = 1.5)

## Technical notes
* Each test method thread execution is executed on its own test class instance.
* Throws **TestDurationCheckException** if test execution takes longer than the given timeout.
* Throws **TestClassInstantiationException** if test class cannot be instantiated with default parameter-less constructor.
* Throws **TestMethodExecutionException** if any of the executed threads throws any exception (collected as inner exceptions).
* Default **expected** (Expected thrown exceptions from test execution) = none.

## Testing notes
Testing with the same data over and over is a bit risky. In part because any cache in the system might respond after first request, giving un-normal response times, and partly because mant databases uses read-locks for requested records. Accessing the same record many times hence could create queues in the database. 

Testing in a dev environment is not in any way equal of testing in a more production like environment. An SQL triggering a full table scan is way different with limited data in the database, and an environment with indexes maintained and refreshed is way quicker than any environment that's remotely neglected.

Developer performance tests can only be used to make sure the code **CAN** get good capacity/performance. There are numerous more obstacles due to environmental factos, infrastructural factors, data-setup (data volume and distribution), other things happening in the system at the same time (batch jobs, other requests). But it is relevant making sure the system code **can** deliver. That helps narrowing down any performance related issues identified - and hopefully you yourself can come out without blame. 
