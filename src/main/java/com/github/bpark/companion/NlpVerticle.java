/*
 * Copyright 2017 Kurt Sparber
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.bpark.companion;

import io.vertx.core.Handler;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.eventbus.EventBus;
import io.vertx.rxjava.core.eventbus.Message;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.Span;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author ksr
 */
public class NlpVerticle extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(NlpVerticle.class);

    private static final String TOKEN_BINARY = "/nlp/en-token.bin";
    private static final String NER_PERSON_BINARY = "/nlp/en-ner-person.bin";
    private static final String POS_MAXENT_BINARY = "/nlp/en-pos-maxent.bin";
    private static final String SENT_BINARY = "/nlp/en-sent.bin";

    private Tokenizer tokenizer;
    private NameFinderME nameFinder;
    private POSTaggerME posTagger;
    private SentenceDetectorME sentenceDetectorME;

    @Override
    public void start() throws Exception {

        initTokenizer();
        initNameFinder();
        initPosTagger();
        initSentenceDetector();

        registerTokenizer();
        registerNameFinder();
        registerPosTagger();
        registerSentenceDetector();
    }

    private void initTokenizer() {
        try (InputStream modelInToken = NlpVerticle.class.getResourceAsStream(TOKEN_BINARY)) {
            TokenizerModel modelToken = new TokenizerModel(modelInToken);
            tokenizer = new TokenizerME(modelToken);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void initNameFinder() {
        try (InputStream modelIn = NlpVerticle.class.getResourceAsStream(NER_PERSON_BINARY)) {
            TokenNameFinderModel model = new TokenNameFinderModel(modelIn);
            nameFinder = new NameFinderME(model);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void initPosTagger() {
        try (InputStream modelIn = NlpVerticle.class.getResourceAsStream(POS_MAXENT_BINARY)) {
            POSModel model = new POSModel(modelIn);
            posTagger = new POSTaggerME(model);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void initSentenceDetector() {
        try (InputStream modelIn = NlpVerticle.class.getResourceAsStream(SENT_BINARY)) {
            SentenceModel model = new SentenceModel(modelIn);
            sentenceDetectorME = new SentenceDetectorME(model);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void registerTokenizer() {

        EventBus eventBus = vertx.eventBus();

        eventBus.consumer(NlpAddresses.TOKENS.getAddress(), (Handler<Message<String>>) message -> {
            String sentence = message.body();
            String[] tokens = tokenizer.tokenize(sentence);

            logger.info("evaluated tokens: {}", Arrays.asList(tokens));

            message.reply(tokens);
        });

    }

    private void registerNameFinder() {

        EventBus eventBus = vertx.eventBus();

        eventBus.consumer(NlpAddresses.PERSONNAME.getAddress(), (Handler<Message<String[]>>) message -> {
            String[] tokens = message.body();

            List<PersonName> names = new ArrayList<>();

            Span nameSpans[] = nameFinder.find(tokens);
            double[] spanProbs = nameFinder.probs(nameSpans);


            for (int i = 0; i < nameSpans.length; i++) {
                Span nameSpan = nameSpans[i];
                int start = nameSpan.getStart();
                int end = nameSpan.getEnd() - 1;
                String name;
                if (start == end) {
                    name = tokens[start];
                } else {
                    name = tokens[start] + " " + tokens[end];
                }
                double probability = spanProbs[i];
                String[] nameTokens = Arrays.copyOfRange(tokens, start, end + 1);

                names.add(new PersonName(name, nameTokens, probability));
            }

            message.reply(names);
        });

    }

    private void registerPosTagger() {

        EventBus eventBus = vertx.eventBus();

        eventBus.consumer(NlpAddresses.POSTAGGING.getAddress(), (Handler<Message<String[]>>) message -> {
            String[] tokens = message.body();
            String[] tags = posTagger.tag(tokens);

            logger.info("pos tagged: {}", Arrays.asList(tags));

            message.reply(tags);
        });

    }

    private void registerSentenceDetector() {

        EventBus eventBus = vertx.eventBus();

        eventBus.consumer(NlpAddresses.SENTENCES.getAddress(), (Handler<Message<String>>) message -> {
            String messageBody = message.body();
            String[] sentences = sentenceDetectorME.sentDetect(messageBody);

            logger.info("evaluated sentences: {}", Arrays.asList(sentences));

            message.reply(sentences);
        });

    }

}
