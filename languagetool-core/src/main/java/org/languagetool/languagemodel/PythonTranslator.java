package org.languagetool.languagemodel;

/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance
 * with the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.djl.inference.Predictor;
import ai.djl.modality.Classifications;
import ai.djl.modality.Input;
import ai.djl.modality.Output;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.NoBatchifyTranslator;
import ai.djl.translate.TranslateException;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;

import java.io.IOException;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PythonTranslator implements Translator<String, String> {

  @Override
  public NDList processInput(TranslatorContext ctx, String sentence)
    throws IOException, TranslateException {
    HuggingFaceTokenizer tokenizer = HuggingFaceTokenizer.newInstance("bert-base-cased"); //set max_tokens to 50 here?
    tokenizer.enableBatch(); // Sets padding and truncation to true TODO: figure out how to set to len 50? manual padding?
    Encoding tokenizedInput = tokenizer.encode(sentence); // includes tokens and attention mask, can be cast to NDList directly

    NDManager manager = ctx.getNDManager();

    String[] tokens = tokenizedInput.getTokens();
    System.out.println(tokens);
    NDArray tokenArray = manager.create(tokens);

    long[] attentionMask = tokenizedInput.getAttentionMask();
    System.out.println(attentionMask);
    NDArray attentionMaskArray = manager.create(attentionMask);

    int numTokens = tokens.length; // TODO padding handling?
    System.out.println("num_tokens: "+numTokens);
    NDArray tokenNumArray = manager.create(numTokens);

    // NDList processed_input = tokenizedInput.toNDList(ctx.getNDManager(), true);
    tokenizer.close();

    return new NDList(tokenArray, attentionMaskArray, tokenNumArray);
  }

  @Override
  public String processOutput(TranslatorContext ctx, NDList list){
    NDArray labelProbs = list.get(0);
    NDArray dProbs = list.get(1);
    NDArray incorrectProbs = list.get(2);

    /*
    we should be able to get the Label-probs, d-probs and incorrect-probs as arrays in the output NDList
    and can either map them to our desired output here or just pass them along
    */

    String output = "label-probs: "+ labelProbs + "\nd-probs:" + dProbs + "\nincorr-probs:" + incorrectProbs;

    return output;
  }

}