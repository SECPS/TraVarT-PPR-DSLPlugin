/*******************************************************************************
 * TODO: explanation what the class does
 *  
 *  @author Kevin Feichtinger
 *  
 * Copyright 2023 Johannes Kepler University Linz
 * LIT Cyber-Physical Systems Lab
 * All rights reserved
 *******************************************************************************/
package at.jku.cps.travart.plugin.ppr.dsl.optimize;

import at.jku.cps.travart.core.common.IModelOptimizer;
import at.sqi.ppr.model.AssemblySequence;

public class PprDslOptimizer implements IModelOptimizer<AssemblySequence> {

	private static PprDslOptimizer instance;

	public static PprDslOptimizer getInstance() {
		if (instance == null) {
			instance = new PprDslOptimizer();
		}
		return instance;
	}

	private PprDslOptimizer() {

	}

	@Override
	public void optimize(final AssemblySequence model, final STRATEGY level) {
		// TODO Auto-generated method stub
	}

}
