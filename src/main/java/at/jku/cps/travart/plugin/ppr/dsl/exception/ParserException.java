/*******************************************************************************
 * TODO: explanation what the class does
 *  
 *  @author Kevin Feichtinger
 *  
 * Copyright 2023 Johannes Kepler University Linz
 * LIT Cyber-Physical Systems Lab
 * All rights reserved
 *******************************************************************************/
package at.jku.cps.travart.plugin.ppr.dsl.exception;

public class ParserException extends RuntimeException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new parser exception with the given message.
     *
     * @param message a string.
     */
    public ParserException(final String message) {
        super(message);
    }

    /**
     * Creates a new parser exception with the given sub exception.
     *
     * @param e an exception.
     */
    public ParserException(final Exception e) {
        super(e);
    }
}
