package org.languagetool.gector;

import java.nio.file.Paths;
import java.util.List;

import ai.djl.Model;
import ai.djl.inference.Predictor;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.Shape;
import ai.djl.repository.zoo.Criteria;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.NoBatchifyTranslator;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;

public class EmbedderTranslator implements NoBatchifyTranslator<List, List> {
	
	private Predictor<NDList, List> projector;
	private long[] offsets = null;
	
	public void setIndices(long[] indices) {
		this.offsets = indices;
	}
	
	public EmbedderTranslator() {
		Translator<NDList, List> translator = (Translator<NDList, List>) new ProjectorTranslator();
		Criteria<NDList, List> criteria = Criteria.builder()
  		      .setTypes(NDList.class, List.class)// I / O types of the processing pipeline
  		      .optModelUrls(getClass().getClassLoader().getResource("roberta_projector.pt").toString())
  		      .optModelName("roberta_projector.pt")
  		      .optTranslator(translator)
  		      .build();
  	
	  	try {
	  	      Model model = criteria.loadModel();
	  	      this.projector = model.newPredictor(translator);
	  	    } catch(Exception e) {
	  	      System.out.println(e.getMessage());
	  	      e.printStackTrace();
	  	    }
		}

	@Override
	public NDList processInput(TranslatorContext ctx, List input) throws Exception {
		NDManager manager = ctx.getNDManager();

	    NDArray tokenArray = manager.create((long[]) input.get(0), new Shape(50)).expandDims(0);
	    NDArray attentionMaskArray = manager.create((long[]) input.get(1), new Shape(50)).expandDims(0);

	    return new NDList(tokenArray, attentionMaskArray);
	}

	@Override
	public List processOutput(TranslatorContext ctx, NDList list) throws Exception {
		return projector.predict(new NDList(list.get(0), ctx.getNDManager().create(this.offsets)));
	}

}
