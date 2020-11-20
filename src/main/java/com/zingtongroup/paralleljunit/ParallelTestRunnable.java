package com.zingtongroup.paralleljunit;

import java.lang.reflect.Method;

class ParallelTestRunnable implements Runnable{

    Object testClassObject;
    Method method;
    Exception innerException;
    Object testMethodReturnObject;

    @Override
    public void run() {
        try {
            testMethodReturnObject = method.invoke(testClassObject);
        } catch (Exception e) {
            innerException = e;
        }
    }

    ParallelTestRunnable(Object testClassObject, Method testMethod){
        this.testClassObject = testClassObject;
        this.method = testMethod;
    }
}
