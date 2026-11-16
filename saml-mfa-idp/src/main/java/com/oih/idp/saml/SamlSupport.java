package com.oih.idp.saml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public final class SamlSupport {

    public static final String SAML = "urn:oasis:names:tc:SAML:2.0:assertion";
    public static final String SAMLP = "urn:oasis:names:tc:SAML:2.0:protocol";
    public static final String MD = "urn:oasis:names:tc:SAML:2.0:metadata";
    public static final String RSA_SHA256 = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256";
    public static final String RSA_SHA1 = "http://www.w3.org/2000/09/xmldsig#rsa-sha1";

    static {
        org.apache.xml.security.Init.init();
    }

    private SamlSupport() {
    }

    public static Document newDocument() {
        try {
            DocumentBuilderFactory factory = hardenedFactory();
            return factory.newDocumentBuilder().newDocument();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    public static Document parse(byte[] xml) {
        if (xml == null || xml.length == 0) {
            throw new SamlProtocolException("SAML_REQUEST_INVALID", "Empty SAML");
        }
        try {
            DocumentBuilderFactory factory = hardenedFactory();
            return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
        } catch (SamlProtocolException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SamlProtocolException("SAML_REQUEST_INVALID", "Malformed SAML XML");
        }
    }

    public static byte[] serialize(Document document) {
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            transformer.transform(new DOMSource(document), new StreamResult(out));
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    public static Element element(Document doc, String ns, String local) {
        return doc.createElementNS(ns, qualified(ns, local));
    }

    public static void attr(Element element, String name, String value) {
        if (value != null) {
            element.setAttribute(name, value);
        }
    }

    public static String attr(Element element, String name) {
        return element.hasAttribute(name) ? element.getAttribute(name) : null;
    }

    public static Element child(Element parent, String ns, String local) {
        NodeList nodes = parent.getElementsByTagNameNS(ns, local);
        return nodes.getLength() == 0 ? null : (Element) nodes.item(0);
    }

    public static String text(Element parent, String ns, String local) {
        Element child = child(parent, ns, local);
        return child == null ? null : child.getTextContent();
    }

    public static void sign(Element signable, X509Certificate certificate, PrivateKey key) {
        try {
            if (!signable.hasAttribute("ID") || signable.getAttribute("ID").isBlank()) {
                throw new IllegalStateException("Missing ID");
            }
            signable.setIdAttributeNS(null, "ID", true);
            signable.setIdAttribute("ID", true);
            String id = signable.getAttribute("ID");
            XMLSignatureFactory factory = XMLSignatureFactory.getInstance("DOM");
            List<Transform> transforms = new ArrayList<>();
            transforms.add(factory.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null));
            transforms.add(factory.newTransform(CanonicalizationMethod.EXCLUSIVE, (TransformParameterSpec) null));
            Reference reference = factory.newReference(
                    "#" + id,
                    factory.newDigestMethod(DigestMethod.SHA256, null),
                    transforms,
                    null,
                    null);
            SignedInfo signedInfo = factory.newSignedInfo(
                    factory.newCanonicalizationMethod(CanonicalizationMethod.EXCLUSIVE, (C14NMethodParameterSpec) null),
                    factory.newSignatureMethod(RSA_SHA256, null),
                    Collections.singletonList(reference));
            KeyInfoFactory kif = factory.getKeyInfoFactory();
            X509Data x509 = kif.newX509Data(List.of(certificate));
            KeyInfo keyInfo = kif.newKeyInfo(List.of(x509));
            XMLSignature signature = factory.newXMLSignature(signedInfo, keyInfo);
            DOMSignContext context = new DOMSignContext(key, signable);
            context.setDefaultNamespacePrefix("ds");
            org.w3c.dom.Node issuer = child(signable, SAML, "Issuer");
            org.w3c.dom.Node next = issuer != null ? issuer.getNextSibling() : signable.getFirstChild();
            if (next != null) {
                context.setNextSibling(next);
            }
            signature.sign(context);
        } catch (Exception ex) {
            throw new IllegalStateException("SAML signature failed", ex);
        }
    }

    public static String signatureAlgorithm(Element root) {
        Element sig = child(root, XMLSignature.XMLNS, "Signature");
        if (sig == null) {
            return null;
        }
        Element signedInfo = child(sig, XMLSignature.XMLNS, "SignedInfo");
        if (signedInfo == null) {
            return null;
        }
        Element method = child(signedInfo, XMLSignature.XMLNS, "SignatureMethod");
        return method == null ? null : method.getAttribute("Algorithm");
    }

    public static byte[] decodePost(String saml) {
        try {
            return Base64.getMimeDecoder().decode(saml.replace(" ", "+"));
        } catch (Exception ex) {
            throw new SamlProtocolException("SAML_REQUEST_INVALID", "Invalid SAML encoding");
        }
    }

    public static byte[] decodeRedirect(String saml) {
        try {
            byte[] deflated = Base64.getDecoder().decode(saml.replace(" ", "+"));
            Inflater inflater = new Inflater(true);
            inflater.setInput(deflated);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            while (!inflater.finished()) {
                int n = inflater.inflate(buf);
                if (n == 0 && inflater.needsInput()) {
                    break;
                }
                out.write(buf, 0, n);
            }
            inflater.end();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new SamlProtocolException("SAML_REQUEST_INVALID", "Invalid deflated SAML");
        }
    }

    public static String encodePost(byte[] xml) {
        return Base64.getEncoder().encodeToString(xml);
    }

    public static String encodeRedirect(byte[] xml) {
        Deflater deflater = new Deflater(Deflater.DEFLATED, true);
        deflater.setInput(xml);
        deflater.finish();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        while (!deflater.finished()) {
            int n = deflater.deflate(buf);
            out.write(buf, 0, n);
        }
        return Base64.getEncoder().encodeToString(out.toByteArray());
    }

    private static DocumentBuilderFactory hardenedFactory() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setExpandEntityReferences(false);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        return factory;
    }

    private static String qualified(String ns, String local) {
        if (SAML.equals(ns)) {
            return "saml:" + local;
        }
        if (SAMLP.equals(ns)) {
            return "samlp:" + local;
        }
        if (MD.equals(ns)) {
            return "md:" + local;
        }
        return local;
    }
}
