package com.oih.idp.saml.web;

import com.oih.idp.saml.MetadataFactory;
import com.oih.idp.saml.SamlSupport;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MetadataController {

    private final MetadataFactory metadataFactory;

    public MetadataController(MetadataFactory metadataFactory) {
        this.metadataFactory = metadataFactory;
    }

    @GetMapping(value = "/saml2/idp/metadata", produces = MediaType.APPLICATION_XML_VALUE)
    public byte[] metadata() {
        return SamlSupport.serialize(metadataFactory.entity());
    }
}
