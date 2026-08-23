package com.orapay.common.exception;

public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String exceptionMessageText) {
        super(exceptionMessageText);
    }

    public BusinessRuleException(String exceptionMessageText, Throwable causeExceptionInstance) {
        super(exceptionMessageText, causeExceptionInstance);
    }
}
