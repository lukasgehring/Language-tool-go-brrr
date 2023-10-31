package org.languagetool.gector;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import ai.djl.MalformedModelException;
import ai.djl.Model;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.djl.inference.Predictor;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.Translator;

public class Gector {
	
	private HuggingFaceTokenizer tokenizer;
	private Predictor<List, List> embedder;
	private EmbedderTranslator translator;
	
	private final float min_error_prob = .5f;
	private final long START_TOKEN = 50265;
	private final int MAX_LEN = 50;
	private String[] labels;
	private GectorHelper myHelper;
	
	public Gector() throws IOException, ModelNotFoundException, MalformedModelException {
		/* Load helper*/
		this.myHelper = new GectorHelper();
		/* Load label vocab */
		String line = null;
		List<String> vocab = new ArrayList<String>(5002);
        
        InputStream is = getClass().getClassLoader().getResourceAsStream("labels.txt");
		InputStreamReader isr = new InputStreamReader(is);
        BufferedReader bufferedReader = new BufferedReader(isr);

        while((line = bufferedReader.readLine()) != null) {
            vocab.add(line);//output is name of textarea
        }   
        bufferedReader.close();         
	    this.labels = new String[vocab.size()];
	    this.labels = vocab.toArray(this.labels);
		/* HuggingFace AutoTokenizer */
		this.tokenizer = HuggingFaceTokenizer.newInstance("roberta-base", Map.of("truncation", "true"));
		/* Load Embedder*/
		this.translator = new EmbedderTranslator();
		Translator<List, List> translator = this.translator;
		Criteria<List, List> criteria = Criteria.builder()
  		      .setTypes(List.class, List.class)// I / O types of the processing pipeline
  		      //.optModelPath(Paths.get("/home/lgehring/Documents/GitHub/Language-tool-go-brrr/languagetool-gector/target/classes/"))
  		      .optModelUrls(getClass().getClassLoader().getResource("roberta_embedder.pt").toString())
  		      .optModelName("roberta_embedder.pt")
  		      .optTranslator(translator)
  		      .build();
  	
	  	      Model model = criteria.loadModel();
	  	      this.embedder = model.newPredictor(translator);
	}
	  
	public List<List<String>> correct(String input) throws Exception {
		/*
		 * This method starts the inferencing pipeline based on gector.gec_model.py handle_batch().
		 * In this implementation, there is no batching, so we only correct one sentence at a time.
		 * There is also only a single run of corrections performed, not multiple like in the original implementation.
		 */
		  String[] splitted_sent = input.split(" ");
		  List<Long> offsets = new ArrayList<Long>();
		  List<Long> token_ids = new ArrayList<Long>();
		  List<Long> input_mask = new ArrayList<Long>();
		  token_ids.add(this.START_TOKEN);
		  offsets.add(0l);
		  input_mask.add(1l);
		  long running_offset = 1l;
		  // We create the token offsets (as in gector.tokenizer get_offsets_and_reduce_input_ids()) manually.
		  for(String word : splitted_sent) {
			  long[] ids = this.tokenizer.encode(" " + word, false).getIds();
			  long len = (long) ids.length;
			  if (running_offset + len <= this.MAX_LEN) {
				  offsets.add(running_offset);
				  running_offset += len;
				  for (long id : ids) {
					  token_ids.add(id);
					  input_mask.add(1l);
			      }
			  }
		  }
		  
		  // Because the embedder is traced, we truncate / pad to input length 50
		  long[] tokenIDs = Arrays.copyOfRange(token_ids.stream().mapToLong(Long::longValue).toArray(), 0, this.MAX_LEN);
		  long[] inputMask = Arrays.copyOfRange(input_mask.stream().mapToLong(Long::longValue).toArray(), 0, this.MAX_LEN);
		  
		  // The projection layers are handled inside of the embedder translator, so we need to pass the offset indices
		  this.translator.setIndices(offsets.stream().mapToLong(Long::longValue).toArray());
		  
		  //Output of this is a list containing the label_probs, label_ids and sentence error-prob
		  List<Number[]> outputList = embedder.predict(List.of(tokenIDs, inputMask));
		  
		  Number[] probsNumber = outputList.get(0);
		  float[] probs = new float[probsNumber.length];
		  for (int i = 0; i < probs.length; i++) {
			  probs[i] = (float) probsNumber[i];
		  }
		  Number[] idxsNumber = outputList.get(1);
		  long[] idxs = new long[idxsNumber.length];
		  for (int i = 0; i < probs.length; i++) {
			  idxs[i] = (long) idxsNumber[i];
		  }
		  float errorProb = (float) outputList.get(2)[0];

		  return postProcess(splitted_sent, probs, idxs, errorProb);
	  }
	
	private List<List<String>> postProcess(String[] tokens, float[] probs, long[] idxs, float incorrProb) throws Exception {
		/*
		 * Unbatched Java implementation of gector.gec_model postprocess_batch()
		 * We save an edit / action for all tokens, meaning that it's null for tokens not associated with any action
		 */
		long max = 0;
		for (long l : idxs)
			max = (l > max)?l:max;
		if (incorrProb < this.min_error_prob || max == 0) {
			return null;
		}
		
		List<List> edits = new ArrayList<List>(tokens.length + 1);
		for (int i = 0; i < tokens.length + 1; i++) {
			String token = null;
			if (i == 0)
				token = "$START";
			else
				token = tokens[i-1];
			if (idxs[i] == 0 || idxs[i] == 5000 || idxs[i] == 5001 || probs[i] < this.min_error_prob)
				edits.add(null);
			else {
				String suggestedLabel = this.labels[(int) idxs[i]];
				edits.add(this.getTokenAction(token, i, probs[i], suggestedLabel));
			}
		}		
		return myHelper.getTargetSentByEdits(tokens, edits);
	}
	
	private List getTokenAction(String token, int i, float prob, String suggestedLabel){
		/*
		 * Straight Java implementation of gector.gec_model get_token_action(). The list returns
		 * 	ret.get(0) start position as int.
		 * 	ret.get(1) end position as int.
		 * 	ret.get(2) suggested label action as String.
		 */
		int startPos = 0;
		int endPos = 0;
		String suggestedToken = null;
		if (suggestedLabel.startsWith("$REPLACE_") || suggestedLabel.startsWith("$TRANSFORM_") || suggestedLabel.equals("$DELETE")){
			startPos = i;
			endPos = i + 1;
		} else if (suggestedLabel.startsWith("$APPEND_") || suggestedLabel.startsWith("$MERGE_")) {
			startPos = i + 1;
			endPos = i + 1;
		}
		if (suggestedLabel.equals("$DELETE"))
			suggestedToken = "";
		else if (suggestedLabel.startsWith("$TRANSFORM_") || suggestedLabel.startsWith("$MERGE_"))
			suggestedToken = suggestedLabel;
		else
			suggestedToken = suggestedLabel.substring(suggestedLabel.indexOf("_") + 1);
		List ret = new ArrayList();
		ret.add(startPos - 1);
		ret.add(endPos - 1);
		ret.add(suggestedToken);
		return ret;
	}

}
