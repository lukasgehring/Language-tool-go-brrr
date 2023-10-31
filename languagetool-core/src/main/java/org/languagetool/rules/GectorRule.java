/* LanguageTool, a natural language style checker 
 * Copyright (C) 2015 Daniel Naber (http://www.danielnaber.de)
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301
 * USA
 */
package org.languagetool.rules;

import org.languagetool.AnalyzedSentence;
import org.languagetool.AnalyzedToken;
import org.languagetool.AnalyzedTokenReadings;
// import org.languagetool.gector.GectorInterface;
import org.languagetool.gector.GectorInterface;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

/**
 * A simple demo rule as an example for how to implement your own Java-based
 * rule in LanguageTool. Simple walks over the text and prints the words
 * and their analysis.
 * 
 * <p>To activate this rule, add it to {@code getRelevantRules()} in e.g. {@code English.java}.
 * 
 * <p>This rule works on sentences, extend {@link TextLevelRule} instead to work 
 * on the complete text.
 */
public class GectorRule extends Rule {
	
  private String description = "";
  
  @Override
  public String getId() {
    return "GectorRule";  // a unique id that doesn't change over time
  }

  @Override
  public String getDescription() {
    return description;  // shown in the configuration dialog
  }

  // This is the method with the error detection logic that you need to implement:
  @Override
  public RuleMatch[] match(AnalyzedSentence sentence) throws IOException {
	  
	// System.out.println("Check sentence with Gector2");
	// System.out.println("Sentence: " + sentence.getText());
	
	GectorInterface s = new GectorInterface();
	List<List<String>> result = s.correct(sentence.getText());
	List<RuleMatch> ruleMatches = new ArrayList<>();
	
	if (result != null) {
		
		// token
		List<String> gector_token = result.get(0);
		
		// explanations
		List<String> gector_hint = result.get(1);
		
		// correct sentence
		List<String> gector_correct = result.get(2);
		
		
		ListIterator<String> it = gector_hint.listIterator();
		
		// start idx of current token
		int idx_count = 0;
		
		while (it.hasNext()) {
			int idx = it.nextIndex();
			String hint = it.next();
			
			if (idx == 0) {
				continue;
			} else if (hint == null || hint.isBlank()) {
				idx_count += gector_token.get(idx-1).length();
				continue;
			} else {
				
				description = gector_hint.get(idx);
				
				RuleMatch ruleMatch = new RuleMatch(this, sentence, idx_count, idx_count + gector_token.get(idx-1).length(), gector_hint.get(idx));
				ruleMatch.setSuggestedReplacement(gector_correct.get(idx-1));  // the user will see this as a suggested correction
				ruleMatches.add(ruleMatch);
				
				idx_count += gector_token.get(idx-1).length();
			}
		}
	}

    return toRuleMatchArray(ruleMatches);
  }

}
