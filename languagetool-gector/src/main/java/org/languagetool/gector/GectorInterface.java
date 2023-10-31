package org.languagetool.gector;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import ai.djl.MalformedModelException;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.translate.TranslateException;

public class GectorInterface
{
	
	private Gector myGector;
	
	public GectorInterface() {
		try {
			myGector = new Gector();
		} catch (IOException e) {
			// TODO Happens when any file for the models is not found
			e.printStackTrace();
		} catch (ModelNotFoundException e) {
			// TODO If the PyTorch loading fails
			e.printStackTrace();
		} catch (MalformedModelException e) {
			// TODO That would be bad but shouldn't happen
			e.printStackTrace();
		}
	}
	
	public List<List<String>> correct(String sentence) {
		return correctSentenceAndPrint(myGector, sentence);
		
		//if (result != null) {
			//System.out.println(sentence);
			//System.out.println(result);
			//System.out.println(Arrays.toString(result.get(1).toArray()));
			//String[] sent = new String[result.get(2).size()];
			// System.out.println(String.join(" ", (String[]) result.get(2).toArray(sent)) + "\n");
		//}
	}
    
    private static List<List<String>> correctSentenceAndPrint(Gector gec, String sentence) {
    	try {
    		return gec.correct(sentence);
		} catch (TranslateException e) {
			// TODO This also shouldn't happen... If the model works correctly
			e.printStackTrace();
		} catch (Exception e) {
			// TODO This only happens if Transform is not found. Which should never be the case actually
			e.printStackTrace();
		}
    	return null;
    }
    
    
    
    
    
}
