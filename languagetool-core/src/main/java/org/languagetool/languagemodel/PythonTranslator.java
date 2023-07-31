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
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.NoBatchifyTranslator;
import ai.djl.translate.TranslateException;
import ai.djl.translate.TranslatorContext;

import java.io.IOException;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PythonTranslator implements NoBatchifyTranslator<String, Classifications> {

  private ZooModel<Input, Output> model;
  private Predictor<Input, Output> predictor;

  @Override
  public NDList processInput(TranslatorContext ctx, String sentence)
    throws IOException, TranslateException {
    HuggingFaceTokenizer tokenizer = HuggingFaceTokenizer.newInstance("bert-base-cased"); //set max_tokens to 50 here?
    tokenizer.enableBatch(); // Sets padding and truncation to true TODO: figure out how to set to len 50?
    Encoding tokenized = tokenizer.encode(sentence); // includes tokens and attention mask, can be cast to NDList directly
    /*
    String[] tokens = tokenized.getTokens();
    long[] attention_mask = tokenized.getAttentionMask();
    int size = tokens.length;
    */
    NDList processed_input = tokenized.toNDList(ctx.getNDManager(), true);
    tokenizer.close();
    // TODO add sequence length as single value NDArray to NDList?
    return processed_input;
  }

  @Override
  public Classifications processOutput(TranslatorContext ctx, NDList list){
    //example code, TODO integrate GECTOR here
    /*
    we should be able to get the Label-probs, d-probs and incorrect-probs as arrays in the output NDList
    and can map them to our desired output/pass-along types here
     */
    NDArray probabilities = list.singletonOrThrow().softmax(0);
    List<String> classNames = IntStream.range(0, 10).mapToObj(String::valueOf).collect(Collectors.toList());
    return new Classifications(classNames, probabilities);

  }

}