package org.languagetool.gector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.translate.NoBatchifyTranslator;
import ai.djl.translate.TranslatorContext;

public class ProjectorTranslator implements NoBatchifyTranslator<NDList, List>{

	@Override
	public NDList processInput(TranslatorContext ctx, NDList input) throws Exception {
		return input;
	}

	@Override
	public List processOutput(TranslatorContext ctx, NDList list) throws Exception {
		List<Number[]> output = new ArrayList<Number[]>(3);
		for (NDArray arr : list) {
			output.add(arr.toArray());
		}
		return output;
	}

}
