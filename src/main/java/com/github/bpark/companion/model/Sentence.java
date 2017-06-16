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
package com.github.bpark.companion.model;

import java.util.List;

public class Sentence {

    private String raw;

    private String[] tokens;

    private String[] posTags;

    private List<PersonName> personNames;


    public Sentence(String raw, String[] tokens, String[] posTags, List<PersonName> personNames) {
        this.raw = raw;
        this.tokens = tokens;
        this.posTags = posTags;
        this.personNames = personNames;
    }

    public String getRaw() {
        return raw;
    }

    public String[] getTokens() {
        return tokens;
    }

    public String[] getPosTags() {
        return posTags;
    }

    public List<PersonName> getPersonNames() {
        return personNames;
    }
}
