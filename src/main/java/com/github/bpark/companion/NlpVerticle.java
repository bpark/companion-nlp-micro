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

import com.github.bpark.companion.model.AnalyzedText;
import com.github.bpark.companion.model.PersonName;
import com.github.bpark.companion.model.Sentence;
import io.vertx.core.json.Json;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.eventbus.EventBus;
import io.vertx.rxjava.core.eventbus.Message;
import io.vertx.rxjava.core.eventbus.MessageConsumer;
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
import rx.Observable;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Verticle to perform a full nlp analysis.
 *
 * The verticle initializes the openlnp trained models and listens on the eventbus for text to analyze.
 * The result contains a list of analyzed sentences.
 *
 * @author ksr
 */
public class NlpVerticle extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(NlpVerticle.class);

    private static final String MESSAGE_KEY = "message";
    private static final String NLP_KEY = "nlp";

    private static final String ADDRESS = "nlp.analyze";

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

        vertx.<Void>rxExecuteBlocking(future -> {

            try {

                initTokenizer();
                initNameFinder();
                initPosTagger();
                initSentenceDetector();

                future.complete();

            } catch (IOException e) {
                future.fail(e);
            }

        }).toObservable().subscribe(
                success -> registerAnalyzer(),
                error -> logger.error("faild to load nlp models!", error)
        );
    }

    private void initTokenizer() throws IOException {
        try (InputStream modelInToken = NlpVerticle.class.getResourceAsStream(TOKEN_BINARY)) {
            TokenizerModel modelToken = new TokenizerModel(modelInToken);
            tokenizer = new TokenizerME(modelToken);
        }
    }

    private void initNameFinder() throws IOException {
        try (InputStream modelIn = NlpVerticle.class.getResourceAsStream(NER_PERSON_BINARY)) {
            TokenNameFinderModel model = new TokenNameFinderModel(modelIn);
            nameFinder = new NameFinderME(model);
        }
    }

    private void initPosTagger() throws IOException {
        try (InputStream modelIn = NlpVerticle.class.getResourceAsStream(POS_MAXENT_BINARY)) {
            POSModel model = new POSModel(modelIn);
            posTagger = new POSTaggerME(model);
        }
    }

    private void initSentenceDetector() throws IOException {
        try (InputStream modelIn = NlpVerticle.class.getResourceAsStream(SENT_BINARY)) {
            SentenceModel model = new SentenceModel(modelIn);
            sentenceDetectorME = new SentenceDetectorME(model);
        }
    }

    private void registerAnalyzer() {

        EventBus eventBus = vertx.eventBus();

        MessageConsumer<String> consumer = eventBus.consumer(ADDRESS);
        Observable<Message<String>> observable = consumer.toObservable();

        observable.subscribe(message -> {

            String id = message.body();

            readMessage(id).flatMap(content -> {
                logger.info("text to analyze for sentences: {}", content);

                String[] sentences = sentenceDetectorME.sentDetect(content);

                List<Sentence> analyzedSentences = Stream.of(sentences).map(s -> {
                    String[] tokens = tokenizer.tokenize(s);
                    String[] posTags = posTagger.tag(tokens);
                    List<PersonName> names = findNames(tokens);

                    return new Sentence(s, tokens, posTags, names);
                }).collect(Collectors.toList());

                logger.info("evaluated sentences: {}", Arrays.asList(sentences));

                return Observable.just(new AnalyzedText(analyzedSentences));
            }).flatMap(analyzedText -> saveMessage(id, analyzedText)).subscribe(a -> message.reply(id));

        });

    }

    private List<PersonName> findNames(String[] tokens) {
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

        return names;
    }

    private Observable<String> readMessage(String id) {
        return vertx.sharedData().<String, String>rxGetClusterWideMap(id)
                .flatMap(map -> map.rxGet(MESSAGE_KEY))
                .toObservable();
    }

    private Observable<Void> saveMessage(String id, AnalyzedText analyzedText) {
        return vertx.sharedData().<String, String>rxGetClusterWideMap(id)
                .flatMap(map -> map.rxPut(NLP_KEY, Json.encode(analyzedText)))
                .toObservable();
    }

}
