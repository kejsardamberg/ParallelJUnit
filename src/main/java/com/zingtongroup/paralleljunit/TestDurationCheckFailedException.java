package com.zingtongroup.paralleljunit;

/**
 * Exception thrown when a test takes longer than the given timeout.
 */
public class TestDurationCheckFailedException extends Exception {

    public TestDurationCheckFailedException(String message){
        super(message);
    }

}
