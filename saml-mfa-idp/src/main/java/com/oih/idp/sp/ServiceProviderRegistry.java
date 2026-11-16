package com.oih.idp.sp;

import com.oih.idp.keys.KeyLifecycleService;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServiceProviderRegistry {

    private final ServiceProviderRepository repository;

    public ServiceProviderRegistry(ServiceProviderRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Optional<ServiceProvider> findEnabled(String entityId) {
        return repository.findByEntityId(entityId).filter(ServiceProvider::isEnabled);
    }

    public List<ServiceProvider> list() {
        return repository.findAll();
    }

    @Transactional
    public ServiceProvider register(
            String entityId,
            String displayName,
            String acsPost,
            String sloPost,
            boolean wantSigned,
            boolean encrypt,
            X509Certificate signingCert,
            X509Certificate encryptionCert) {
        if (repository.findByEntityId(entityId).isPresent()) {
            return repository.findByEntityId(entityId).orElseThrow();
        }
        ServiceProvider sp = ServiceProvider.create(entityId, displayName);
        sp.setWantAuthnRequestsSigned(wantSigned);
        sp.setEncryptAssertions(encrypt);
        sp.getAcs().add(new SpAcs(sp, SpAcs.POST, acsPost, true));
        if (sloPost != null) {
            sp.getSlo().add(new SpSlo(sp, SpAcs.POST, sloPost));
        }
        if (signingCert != null) {
            sp.getCertificates()
                    .add(new SpCertificate(sp, SpCertificate.SIGNING, signingCert, KeyLifecycleService.fingerprint(signingCert)));
        }
        if (encryptionCert != null) {
            sp.getCertificates()
                    .add(new SpCertificate(
                            sp, SpCertificate.ENCRYPTION, encryptionCert, KeyLifecycleService.fingerprint(encryptionCert)));
        }
        sp.getMappings().add(new AttributeMapping(sp, "email", "email"));
        sp.getMappings().add(new AttributeMapping(sp, "uid", "username"));
        return repository.save(sp);
    }

    public Optional<X509Certificate> signingCert(ServiceProvider sp) {
        return sp.getCertificates().stream()
                .filter(cert -> SpCertificate.SIGNING.equals(cert.getUsage()))
                .map(SpCertificate::toCertificate)
                .findFirst();
    }

    public Optional<X509Certificate> encryptionCert(ServiceProvider sp) {
        return sp.getCertificates().stream()
                .filter(cert -> SpCertificate.ENCRYPTION.equals(cert.getUsage()))
                .map(SpCertificate::toCertificate)
                .findFirst();
    }
}
