package com.oih.idp.saml;

public class SamlProtocolException extends RuntimeException {

    private final String category;

    public SamlProtocolException(String category, String message) {
        super(message);
        this.category = category;
    }

    public String category() {
        return category;
    }
}
