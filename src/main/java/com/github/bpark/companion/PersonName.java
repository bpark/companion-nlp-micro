package com.github.bpark.companion;

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
