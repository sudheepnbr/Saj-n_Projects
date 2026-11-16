package com.oih.idp.crypto;

import java.security.PrivateKey;

/**
 * In-memory token handle. {@code getEncoded()} is always null so tests can assert
 * the same fail-closed extractability rule used for PKCS#11 keys.
 */
public final class NonExtractablePrivateKey implements PrivateKey {

    private final PrivateKey delegate;

    public NonExtractablePrivateKey(PrivateKey delegate) {
        this.delegate = delegate;
    }

    PrivateKey unwrap() {
        return delegate;
    }

    @Override
    public String getAlgorithm() {
        return delegate.getAlgorithm();
    }

    @Override
    public String getFormat() {
        return null;
    }

    @Override
    public byte[] getEncoded() {
        return null;
    }
}
