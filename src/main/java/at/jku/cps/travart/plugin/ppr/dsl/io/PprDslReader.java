/*******************************************************************************
 * TODO: explanation what the class does
 *
 *  @author Kevin Feichtinger
 *
 * Copyright 2023 Johannes Kepler University Linz
 * LIT Cyber-Physical Systems Lab
 * All rights reserved
 *******************************************************************************/
package at.jku.cps.travart.plugin.ppr.dsl.io;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import at.jku.cps.travart.core.common.IReader;
import at.jku.cps.travart.core.exception.NotSupportedVariabilityTypeException;
import at.sqi.ppr.dsl.reader.DslReader;
import at.sqi.ppr.dsl.reader.exceptions.DslParsingException;
import at.sqi.ppr.model.AssemblySequence;

public class PprDslReader implements IReader<AssemblySequence> {

	@Override
	public AssemblySequence read(final Path path) throws IOException, NotSupportedVariabilityTypeException {
		final DslReader dslReader = new DslReader();
		try {
			return dslReader.readDsl(path.toString());
		} catch (final DslParsingException e) {
			throw new NotSupportedVariabilityTypeException(e);
		}
	}

	@Override
	public Iterable<String> fileExtensions() {
		return Collections.unmodifiableList(List.of(".dsl"));
	}
}
