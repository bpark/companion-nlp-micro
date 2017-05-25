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

/**
 * @author ksr
 */
public class PersonName {

    private String name;

    private String[] tokens;

    private double probability;

    public PersonName(String name, String[] tokens, double probability) {
        this.name = name;
        this.tokens = tokens;
        this.probability = probability;
    }

    public String getName() {
        return name;
    }

    public String[] getTokens() {
        return tokens;
    }

    public double getProbability() {
        return probability;
    }
}
