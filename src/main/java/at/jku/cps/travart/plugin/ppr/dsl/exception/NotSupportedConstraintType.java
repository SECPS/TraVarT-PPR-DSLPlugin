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

public class NotSupportedConstraintType extends Exception {

    private static final long serialVersionUID = 6564597146968910891L;

    public NotSupportedConstraintType(final String message) {
        super(message);
    }

    public NotSupportedConstraintType(final Exception e) {
        super(e);
    }

}
