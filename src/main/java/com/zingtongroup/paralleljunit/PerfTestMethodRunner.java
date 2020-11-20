package com.zingtongroup.paralleljunit;

import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.TestTimedOutException;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

class PerfTestMethodRunner extends CustomTestMethodRunner {

    int threadCount;
    ExecutorService testThreadPool;
    int maxMilliseconds;
    List<ParallelTestRunnable> testMethods;
    List<Object> testClassObjects;
    long startTime;

    PerfTestMethodRunner(RunNotifier notifier, Class testClass, Method method) throws Exception {
        super(notifier, testClass, method);
        notifier.fireTestStarted(Description
                .createTestDescription(testClass, method.getName()));
        ParallelTest parallelTest = method.getAnnotation(ParallelTest.class);
        if(parallelTest == null) throw new Exception("Test method annotation is not @Perf.");

        this.threadCount = parallelTest.threadCount();
        this.maxMilliseconds = parallelTest.timeout();

        String timeoutMessage = "";
        if(maxMilliseconds != 0)
            timeoutMessage = " and a maximum expected test duration of " + maxMilliseconds + " ms";
        System.out.println("Running test method " + method.getName() + " in " + threadCount + " parallel threads" + timeoutMessage + ".");

        testClassObjects = new ArrayList<Object>();
        testThreadPool = Executors.newFixedThreadPool(threadCount);
        testMethods = new ArrayList<ParallelTestRunnable>();
    }

    @Override
    void run(){
        instantiateTestClass();
        executeTest();
        testExecutionCleanup();
    }

    void instantiateTestClass() {
        for(int i = 0; i < threadCount; i++){
            testClassObjects.add(createTestClassInstance());
        }
    }

    void executeTest() {
        for(int i=0; i<threadCount; i++)
            testMethods.add(new ParallelTestRunnable(testClassObjects.get(i), method));

        startTime = System.currentTimeMillis();

        for (ParallelTestRunnable testMethod : testMethods) {
            try {
                testThreadPool.execute(testMethod);
            } catch (Exception e) {
                Class<? extends Throwable> expectedException = null;
                for (Annotation a : method.getAnnotations()) {
                    Object annotationObject = method.getAnnotation(a.getClass());
                    try {
                        Method m = annotationObject.getClass().getMethod("expected", annotationObject.getClass());
                        expectedException = (Class<? extends Throwable>) m.invoke(annotationObject, new Object[]{null});
                    } catch (NoSuchMethodException noSuchMethodException) {
                        //Ignored
                    } catch (IllegalAccessException illegalAccessException) {
                        //Ignored
                    } catch (InvocationTargetException invocationTargetException) {
                        //Ignored
                    }
                }
                if (!e.getClass().equals(expectedException))
                    innerExceptions.add(new TestMethodExecutionException(e));
            }
        }

        testThreadPool.shutdown();
        threadsTimeoutCheck();
    }

    void threadsTimeoutCheck(){
        try {
            if(!testThreadPool.awaitTermination(1, TimeUnit.MINUTES)){
                notifier.fireTestFailure(new Failure(
                        Description.createTestDescription(testClass, method.getName()),
                        new TestTimedOutException(System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS))
                );
            }
        } catch (InterruptedException e) {
            innerExceptions.add(e);
        }
    }

    void testExecutionCleanup() {
        if(maxMilliseconds != 0)
            testDurationCheck(maxMilliseconds, System.currentTimeMillis() - startTime);
        innerExceptionCheck();
        notifier.fireTestFinished(Description
                .createTestDescription(testClass, method.getName()));

    }
}
