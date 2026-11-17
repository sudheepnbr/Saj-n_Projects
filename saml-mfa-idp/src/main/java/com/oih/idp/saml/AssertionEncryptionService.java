package com.oih.idp.saml;

import com.oih.idp.sp.ServiceProvider;
import com.oih.idp.sp.ServiceProviderRegistry;
import java.security.cert.X509Certificate;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import org.apache.xml.security.encryption.EncryptedData;
import org.apache.xml.security.encryption.EncryptedKey;
import org.apache.xml.security.encryption.XMLCipher;
import org.apache.xml.security.keys.KeyInfo;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@Component
public class AssertionEncryptionService {

    static {
        org.apache.xml.security.Init.init();
    }

    private final ServiceProviderRegistry registry;

    public AssertionEncryptionService(ServiceProviderRegistry registry) {
        this.registry = registry;
    }

    public Element encrypt(Document doc, Element assertion, ServiceProvider sp) {
        X509Certificate cert = registry.encryptionCert(sp)
                .orElseThrow(() -> new SamlProtocolException("SAML_ENCRYPT", "No SP encryption certificate"));
        try {
            KeyGenerator generator = KeyGenerator.getInstance("AES");
            generator.init(256);
            SecretKey dataKey = generator.generateKey();
            XMLCipher keyCipher = XMLCipher.getInstance(XMLCipher.RSA_OAEP);
            keyCipher.init(XMLCipher.WRAP_MODE, cert.getPublicKey());
            EncryptedKey encryptedKey = keyCipher.encryptKey(doc, dataKey);
            XMLCipher dataCipher = XMLCipher.getInstance(XMLCipher.AES_256_GCM);
            dataCipher.init(XMLCipher.ENCRYPT_MODE, dataKey);
            EncryptedData encryptedData = dataCipher.encryptData(doc, assertion);
            KeyInfo keyInfo = new KeyInfo(doc);
            keyInfo.add(encryptedKey);
            encryptedData.setKeyInfo(keyInfo);
            Element encryptedAssertion = SamlSupport.element(doc, SamlSupport.SAML, "EncryptedAssertion");
            encryptedAssertion.appendChild(dataCipher.martial(doc, encryptedData));
            return encryptedAssertion;
        } catch (SamlProtocolException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SamlProtocolException("SAML_ENCRYPT", "Assertion encryption failed");
        }
    }
}
