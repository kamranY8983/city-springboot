package com.sample.city.springboot.exception;

import org.slf4j.helpers.MessageFormatter;

/** Custom exception class for JEDI application */
public class CityException extends RuntimeException {

    /** Default constructor */
    public CityException() {
        super();
    }

    /**
     * Constructor with single string message
     *
     * @param message exception message
     */
    public CityException(String message) {
        super(message);
    }

    /**
     * Constructor with message pattern and arguments. Pattern is using slf4i format
     *
     * @param messagePattern message pattern Oparam args arguments to be used to render string from
     *     pattern
     */
    public CityException(String messagePattern, Object... args) {
        this(MessageFormatter.basicArrayFormat(messagePattern, args));
    }

    /**
     * Constructor with message and cause
     *
     * @param message exception message
     * @param cause exception cause
     */
    public CityException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor with cause
     *
     * @param cause exception cause
     */
    public CityException(Throwable cause) {
        super(cause);
    }
}
