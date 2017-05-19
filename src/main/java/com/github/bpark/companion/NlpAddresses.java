package com.github.bpark.companion;

/**
 * @author ksr
 */
public enum NlpAddresses {

    TOKENS("nlp.tokens"),
    POSTAGGING("nlp.postagging"),
    SENTENCES("nlp.sentences"),
    PERSONNAME("nlp.personname");

    private String address;

    NlpAddresses(String address) {
        this.address = address;
    }

    public String getAddress() {
        return address;
    }
}
