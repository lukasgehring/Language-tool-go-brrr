package org.languagetool.gector;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GectorHelper {
	
	/*
	 * Java Reimplementation of Gector util.helpers.py with only the methods needed for inferencing.
	 */
	
	private Map<String, String> verbFormMap;
	
	public GectorHelper() throws IOException {
		this.verbFormMap = new HashMap<String, String>(168750);
		String line = "";
		
		InputStream is = getClass().getClassLoader().getResourceAsStream("verb-form-vocab.txt");
		InputStreamReader isr = new InputStreamReader(is);
        BufferedReader bufferedReader = new BufferedReader(isr);
        
        while((line = bufferedReader.readLine()) != null) {
            String[] wordsTags = line.split(":");
            String[] words = wordsTags[0].split("_");
            String[] tags = wordsTags[1].split("_");
            String decodeKey = words[0] + "_" + tags[0] + "_" + tags[1].strip();
            if (!verbFormMap.containsKey(decodeKey))
            	verbFormMap.put(decodeKey, words[1]);
        }   
        bufferedReader.close();
	}
	
	public List<List<String>> getTargetSentByEdits(String[] tokens, List<List> edits) throws Exception {
		/*
		 * In this method we also keep track of the token-level changes. Thus, we save a list of labels as well.
		 * This means that the return type has the following form:
		 * 	ret.get(0) gives the original token list.
		 * 	ret.get(1) gives the action for each token in the original list.
		 * 	ret.get(2) gives the new token list.
		 */
		List<String> targetTokens = new ArrayList<String>(Arrays.asList(tokens));
		List<String> tokenAnnotations = new ArrayList<String>(tokens.length);
		int shiftIdx = 0;
		boolean hasMerge = false;
		for (int i = 0; i < edits.size(); i++) {
			List edit = edits.get(i);
			if (edit == null) {
				tokenAnnotations.add("");
				continue;
			}
			int start = (int) edit.get(0);
			int end = (int) edit.get(1);
			String label = (String) edit.get(2);
			int targetPos = start + shiftIdx;
			String sourceToken = "";
			if (targetTokens.size() > targetPos && targetPos >= 0)
				sourceToken = targetTokens.get(targetPos);
			if (label.equals("")) {
				targetTokens.remove(targetPos);
				tokenAnnotations.add("Remove");
				shiftIdx -= 1;
			} else if (start == end) {
				String word = label.replace("$APPEND_", "");
				tokenAnnotations.add("Append " + word);
				targetTokens.add(targetPos, word);
				shiftIdx += 1;
			} else if (label.startsWith("$TRANSFORM_")) {
				String word = applyReverseTransformation(sourceToken, label);
				if (word == null)
					word = sourceToken;
				tokenAnnotations.add("Transform " + label.replace("$TRANSFORM_", "") + " " + word); 
				targetTokens.set(targetPos, word);
			} else if (start == end - 1) {
				String word = label.replace("$REPLACE_", "");
				tokenAnnotations.add("Replace " + word);
				targetTokens.set(targetPos, word);
			} else if (label.startsWith("$MERGE_")) {
				targetTokens.add(targetPos + 1, label);
				tokenAnnotations.add("Merge");
				shiftIdx += 1;
				hasMerge = true;
			}				
		}
		List<String> corrected = null;
		if (hasMerge)
			corrected = replaceMergeTransforms(targetTokens);
		else
			corrected = new ArrayList<String>(targetTokens);
		List<List<String>> ret = new ArrayList<List<String>>(3);
		ret.add(Arrays.asList(tokens));
		ret.add(tokenAnnotations);
		ret.add(corrected);
		return ret;
	}
	
	private List<String> replaceMergeTransforms(List<String> tokens){
		String sent = String.join(" ", (String[]) tokens.toArray());
		sent = sent.replace(" $MERGE_HYPHEN ", "-");
		sent = sent.replace(" $MERGE_SPACE ", "");
		return Arrays.asList(sent.split(" "));
	}
	
	private String applyReverseTransformation(String sourceToken, String label) throws Exception {
		if (label.startsWith("$TRANSFORM")){
			if (label.equals("$KEEP"))
				return sourceToken;
			else if (label.startsWith("$TRANSFORM_CASE")) 
				return this.convertUsingCase(sourceToken, label);
			else if (label.startsWith("$TRANSFORM_VERB"))
				return this.convertUsingVerb(sourceToken, label);
			else if (label.startsWith("$TRANSFORM_SPLIT"))
				return this.convertUsingSplit(sourceToken, label);
			else if (label.startsWith("$TRANSFORM_AGREEMENT"))
				return this.convertUsingPlural(sourceToken, label);
			else
				throw new Exception("Unknown action type " + label);
		} else 
			return sourceToken;
	}
	
	private String convertUsingCase(String sourceToken, String label) {
		if (label.endsWith("LOWER"))
			return sourceToken.toLowerCase();
		else if (label.endsWith("UPPER"))
			return sourceToken.toUpperCase();
		else if (label.endsWith("CAPITAL"))
			return sourceToken.substring(0, 1).toUpperCase() + sourceToken.substring(1);
		else if (label.endsWith("CAPITAL_1"))
			return sourceToken.substring(0, 1) + sourceToken.substring(1, 2).toUpperCase() + sourceToken.substring(2);
		else if (label.endsWith("UPPER_-1"))
			return sourceToken.substring(0, sourceToken.length() - 1).toUpperCase() + sourceToken.substring(sourceToken.length() - 1);
		else return sourceToken;
	}
	
	
	private String convertUsingVerb(String sourceToken, String label) {
		String encodingPart = sourceToken + "_" + label.substring(16);
		String decoded = this.verbFormMap.get(encodingPart);
		if (decoded == null)
			return sourceToken;
		else return decoded;
	}
	
	private String convertUsingSplit(String sourceToken, String label) {
		return String.join(" ", sourceToken.split("-"));
	}
	
	private String convertUsingPlural(String sourceToken, String label) throws Exception {
		if (label.endsWith("PLURAL"))
			return sourceToken + "s";
		else if (label.endsWith("SINGULAR"))
			return sourceToken.substring(0, sourceToken.length() - 1);
		else throw new Exception("Unknown action type " + label);
					
	}


}
