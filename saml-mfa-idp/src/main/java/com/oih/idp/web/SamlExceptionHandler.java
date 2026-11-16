package com.oih.idp.web;

import com.oih.idp.saml.SamlProtocolException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class SamlExceptionHandler {

    @ExceptionHandler(SamlProtocolException.class)
    public ResponseEntity<String> saml(SamlProtocolException ex) {
        HttpStatus status = "SAML_OVERSIZE".equals(ex.category()) ? HttpStatus.BAD_REQUEST : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body("SAML request rejected");
    }
}
