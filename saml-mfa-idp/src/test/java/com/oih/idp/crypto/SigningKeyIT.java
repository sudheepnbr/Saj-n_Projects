package com.oih.idp.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import com.oih.idp.keys.KeyLifecycleService;
import com.oih.idp.keys.SigningKey;
import com.oih.idp.keys.SigningKey.Status;
import com.oih.idp.support.PostgresIntegrationTest;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SigningKeyIT extends PostgresIntegrationTest {

    @Autowired
    private SigningService signing;

    @Autowired
    private KeyLifecycleService keys;

    @Autowired
    private CryptoToken token;

    @Test
    void generatedKeyIsNotExtractableAndSelfTestPasses() {
        SigningKey active = keys.requireActive();
        assertThat(token.isExtractable(token.privateKey(active.getCkaId()))).isFalse();
        byte[] payload = "nonce".getBytes(StandardCharsets.UTF_8);
        byte[] signature = signing.sign(payload);
        assertThat(signing.verify(payload, signature, signing.activeCertificate())).isTrue();
    }

    @Test
    void rotationPublishesTwoCertsAndSignsWithActiveOnly() {
        SigningKey active = keys.requireActive();
        SigningKey next = keys.generateNext();
        assertThat(signing.metadataKeys()).extracting(SigningKey::getStatus)
                .containsExactlyInAnyOrder(Status.ACTIVE, Status.NEXT);
        byte[] payload = "rotate".getBytes(StandardCharsets.UTF_8);
        byte[] signature = signing.sign(payload);
        assertThat(signing.verify(payload, signature, signing.certificateOf(active))).isTrue();
        assertThat(signing.verify(payload, signature, signing.certificateOf(next))).isFalse();
        keys.activateNext();
        byte[] after = signing.sign(payload);
        assertThat(signing.verify(payload, after, signing.certificateOf(next))).isTrue();
    }
}
