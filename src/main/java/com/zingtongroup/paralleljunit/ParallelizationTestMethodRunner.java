package com.zingtongroup.paralleljunit;

import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.TestTimedOutException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

class ParallelizationTestMethodRunner extends CustomTestMethodRunner {

    private final int multipleThreadsCount;
    private final double maxExecutionDurationMultipleForMultipleThreadsExecution;
    private final ExecutorService testThreadPool;
    private final List<ParallelTestRunnable> testMethods;
    private final List<Object> testClassObjects;
    long singleThreadDurationInMilliseconds;

    ParallelizationTestMethodRunner(RunNotifier notifier, Class<?> testClass, Method method) throws Exception {
        super(notifier, testClass, method);
        singleThreadDurationInMilliseconds = 0;
        ParallelizationTest para = method.getAnnotation(ParallelizationTest.class);
        if(para == null) throw new Exception("Test method annotation is not @ParallelizationTest.");

        multipleThreadsCount = para.multipleThreadsCount();
        maxExecutionDurationMultipleForMultipleThreadsExecution = para.maxExecutionDurationMultipleForMultipleThreadsExecution();

        System.out.println("Running test method " + method.getName() + " to check parallelization.");

        testClassObjects = new ArrayList<>();
        testThreadPool = Executors.newFixedThreadPool(multipleThreadsCount);
        testMethods = new ArrayList<>();
    }

    @Override
    void run() {
        notifier.fireTestStarted(Description
                .createTestDescription(testClass, method.getName()));

        executeSingleThreadRun(null); //Warm up test execution to avoid initiation differences.

        Object testClassObject = null;
        try {
            testClassObject = testClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            innerExceptions.add(new TestClassInstantiationException(e));
        }
        long startTime = System.currentTimeMillis();
        executeSingleThreadRun(testClassObject);
        this.singleThreadDurationInMilliseconds = System.currentTimeMillis() - startTime;
        instantiateTestClassObjects();
        long parallelExecutionStartTime = System.currentTimeMillis();
        executeTestInParallelThreads(testClassObjects);
        long parallelExecutionDurationInMilliseconds = System.currentTimeMillis() - parallelExecutionStartTime;
        if(parallelExecutionDurationInMilliseconds > singleThreadDurationInMilliseconds * maxExecutionDurationMultipleForMultipleThreadsExecution)
            notifier.fireTestFailure(
                    new Failure(
                            Description.createTestDescription(testClass, method.getName()),
                            new TestMethodExecutionDurationCheckFailedException("The single thread test execution took " + singleThreadDurationInMilliseconds + " ms while the " + multipleThreadsCount + " parallel threads execution took " + parallelExecutionDurationInMilliseconds + " ms.")
                    )
            );

        testExecutionExceptionCheck();
        notifier.fireTestFinished(Description.createTestDescription(testClass, method.getName()));
    }

    private void executeTestInParallelThreads(List<Object> testClassObjects) {
        for(int i=0; i<multipleThreadsCount; i++)
            testMethods.add(new ParallelTestRunnable(testClassObjects.get(i), method));

        for (ParallelTestRunnable testMethod : testMethods) {
            try {
                testThreadPool.execute(testMethod);
            } catch (Exception e) {
                innerExceptions.add(e);
            }
        }

        testThreadPool.shutdown();
        threadsTimeoutCheck();
    }

    private void threadsTimeoutCheck(){
        try {
            if(!testThreadPool.awaitTermination(1, TimeUnit.MINUTES)){
                notifier.fireTestFailure(new Failure(
                        Description.createTestDescription(testClass, method.getName()),
                        new TestTimedOutException(singleThreadDurationInMilliseconds, TimeUnit.MILLISECONDS))
                );
            }
        } catch (InterruptedException e) {
            innerExceptions.add(e);
        }
    }

    private void testExecutionExceptionCheck(){
        for (ParallelTestRunnable testMethod : testMethods) {
            if (testMethod.innerException != null) {
                notifier.fireTestFailure(new Failure(Description.createTestDescription(testClass, method.getName()), testMethod.innerException));
            }
        }
    }

    private void instantiateTestClassObjects(){
        for(int i = 0; i < multipleThreadsCount; i++){
            try {
                testClassObjects.add(testClass.getDeclaredConstructor().newInstance());
            } catch (InstantiationException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                innerExceptions.add(new TestClassInstantiationException(e));
            }
        }
    }

    private void executeSingleThreadRun(Object testClassObject) {
        if(testClassObject == null){
            try{
                testClassObject = testClass.getDeclaredConstructor().newInstance(); //Creation
            } catch (InstantiationException | InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
                innerExceptions.add(new TestClassInstantiationException(e));
            }
        }
        try {
            method.invoke(testClassObject);
        } catch (Exception e) {
            innerExceptions.add(new TestMethodExecutionException(e));
        }

    }
}
