package com.zingtongroup.paralleljunit;

import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("rawtypes")
abstract class CustomTestMethodRunner {

    RunNotifier notifier;
    Class<?> testClass;
    Method method;
    List<Exception> innerExceptions;
    Class expectedException;

    CustomTestMethodRunner(RunNotifier notifier, Class<?> testClass, Method method){
        this.notifier = notifier;
        this.testClass = testClass;
        this.method = method;
        innerExceptions = new ArrayList<>();
        expectedException = null;
        for(Annotation a : method.getAnnotations()){
            Object annotationObject = method.getAnnotation(a.getClass());
            if(annotationObject == null)continue;
            for(Method m1: annotationObject.getClass().getMethods()){
                System.out.println(m1.getName());
            }
            try {
                Method m = annotationObject.getClass().getMethod("expected", annotationObject.getClass());
                expectedException = (Class)m.invoke(annotationObject, new Object[]{null});
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    abstract void run();

    Object createTestClassInstance(){
        try{
            return testClass.getDeclaredConstructor().newInstance();
        } catch (Exception e){
            if(!e.getClass().equals(expectedException))
                innerExceptions.add(new TestClassInstantiationException(e));
        }
        return null;
    }

    void testDurationCheck(long maxDuration, long actualDuration){
        if(expectedException != null)
            System.out.println(expectedException.getClass().getName());

        if(maxDuration < actualDuration)
            notifier.fireTestFailure(
                    new Failure(
                            Description.createTestDescription(testClass, method.getName()),
                            new TestMethodExecutionDurationCheckFailedException("The test took " + actualDuration + " ms while the expected max duration was " + maxDuration + " ms.")
                    )
            );
    }

    void innerExceptionCheck(){
        for(Exception e : innerExceptions){
            if(!e.getClass().equals(expectedException))
                notifier.fireTestFailure(new Failure(Description.createTestDescription(testClass, method.getName()), e));
        }
    }

}
