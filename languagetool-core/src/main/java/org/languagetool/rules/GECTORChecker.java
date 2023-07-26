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

import ai.djl.Model;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.djl.modality.Classifications;
import ai.djl.modality.nlp.generate.BatchTensorList;
import ai.djl.modality.nlp.translator.SimpleText2TextTranslator;
import ai.djl.ndarray.NDArray;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import org.languagetool.AnalyzedSentence;
import org.languagetool.AnalyzedToken;
import org.languagetool.AnalyzedTokenReadings;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static com.sun.tools.doclint.Entity.image;

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
public class GECTORChecker extends Rule {

  @Override
  public String getId() {
    return "GECTORChecker";  // a unique id that doesn't change over time
  }

  @Override
  public String getDescription() {
    return "Wrapper rule for the GECTOR LLM for error correction";  // shown in the configuration dialog
  }

  // This is the method with the error detection logic that you need to implement:
  @Override
  public RuleMatch[] match(AnalyzedSentence sentence) throws IOException {

    System.out.println("GECTOR check");
    List<RuleMatch> ruleMatches = new ArrayList<>();

    HuggingFaceTokenizer tokenizer = HuggingFaceTokenizer.newInstance("bert-base-cased");

    // load TorchScript model
    //TODO: figure out Translator - map tokenizer/encoder pipeline here? Ideally: feed sentence, get tokenized sentence as NDArray as model input
    // then map model output NDArray to whatever is needed
    // https://javadoc.io/doc/ai.djl/api/latest/ai/djl/translate/Translator.html
    // https://github.com/deepjavalibrary/djl-demo/tree/master/development/python
    /*
    * several possible output types ranging from low to high level
    * probabilities? incorrect token index?
    * corrected text? Custom RuleMatch?
    * Not sure which ones would work with the DJL Translator interface yet
    *
    * Is this strictly needed in the first place, or would it be easier to simply pre- and postprocess the model in/output in native java instead of using the Translator?
    * */
    SimpleText2TextTranslator translator;

    Criteria criteria = Criteria.builder() //TODO figure out correct I/O types and functional model path
      .setTypes(BatchTensorList.class, Classifications.class)
      .optModelPath(Paths.get("file://languagetool-core/src/main/resources/org/languagetool/resource/org/languagetool/models/traced_bert.pt"))
      //.optTranslator(translator)
      .optProgress(new ProgressBar()).build();

    // ZooModel model = criteria.loadModel();

    // #### dummy rule 42 as orientation ####

    // Let's get all the tokens (i.e. words) of this sentence, but not the spaces:
    AnalyzedTokenReadings[] tokens = sentence.getTokensWithoutWhitespace();

    // No let's iterate over those - note that the first token will
    // be a special token that indicates the start of a sentence:
    for (AnalyzedTokenReadings token : tokens) {

      try {
        // check if token is a number
        Double.parseDouble(token.getToken());

        // then
        RuleMatch ruleMatch = new RuleMatch(this, sentence, token.getStartPos(), token.getEndPos(), "You are not using the number 42 here.");
        ruleMatch.setSuggestedReplacement("42");  // the user will see this as a suggested correction
        ruleMatches.add(ruleMatch);

      } catch(NumberFormatException e){
        continue;
      }

    }

    // TODO: replace this for loop with GECTOR model application and return RuleMatches at the position of incorrect tokens
    // Make sure to consider differing tokenization between the transformer tokenizer and LT's tokenization
    // (which is standard word level tokens instead of subword token I assume) and match accordingly.

    //TODO: extract the nature of corrections from GECTOR output and pass feedback along accordingly
    // - either here in this class, modifying the RuleMatch object, or in some sort of wrapper that handles the RuleMatches generated by this class

    return toRuleMatchArray(ruleMatches);
  }

}
