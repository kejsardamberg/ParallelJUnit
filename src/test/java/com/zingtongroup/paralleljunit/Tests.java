package com.zingtongroup.paralleljunit;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ParallelJUnit.class)
public class Tests {

    @Test
    public void passingRegularTestShouldPass(){
        Assert.assertTrue(true);
    }

    @ParallelTest(threadCount = 4, timeout = 200)
    public void parallelTestShouldPass() throws InterruptedException {
        Assert.assertTrue(true);
        Thread.sleep(100);
        Assert.assertTrue(true);
    }

    @ParallelTest(threadCount = 10, expected = TestMethodExecutionException.class)
    public void exceptionThrowingTestMethodShouldThrowException() throws Exception {
        Assert.assertTrue(true);
        if(Math.random() * 100 < 50){
            throw new Exception("Oups");
        }
        Assert.assertTrue(true);
    }

    @ParallelizationTest(multipleThreadsCount = 3, maxExecutionDurationMultipleForMultipleThreadsExecution = 1.5)
    public void parallelizationTest() throws InterruptedException {
        Thread.sleep(100);
    }

}
