package io.github.wallakscardoso.spilab.iso20022;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Valida as mensagens contra os XSDs oficiais do catálogo do SPI (ver {@code resources/bcb/README.md}).
 * A assinatura digital do cabeçalho fica fora do escopo, então só ela passa a ser opcional.
 */
public final class ValidadorXsd {

    private static final String ASSINATURA_OBRIGATORIA = "<xs:any namespace=\"http://www.w3.org/2000/09/xmldsig#\"/>";
    private static final String ASSINATURA_OPCIONAL =
            "<xs:any namespace=\"http://www.w3.org/2000/09/xmldsig#\" minOccurs=\"0\" processContents=\"lax\"/>";

    private static final Map<String, Schema> POR_NAMESPACE = Map.of(
            Pacs008.NAMESPACE, carregar("bcb/xsd/pacs.008.spi.1.16.xsd"),
            Pacs002.NAMESPACE, carregar("bcb/xsd/pacs.002.spi.1.18.xsd"));

    private ValidadorXsd() {
    }

    public static void validar(Document documento) {
        String namespace = documento.getDocumentElement().getNamespaceURI();
        Schema schema = POR_NAMESPACE.get(namespace);
        if (schema == null) {
            throw new MensagemInvalidaException("Mensagem de tipo não suportado: " + namespace);
        }
        try {
            schema.newValidator().validate(new DOMSource(documento));
        } catch (SAXException e) {
            throw new MensagemInvalidaException("Mensagem fora do XSD: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Schema carregar(String recurso) {
        try (InputStream in = ValidadorXsd.class.getClassLoader().getResourceAsStream(recurso)) {
            if (in == null) {
                throw new IllegalStateException("XSD não encontrado: " + recurso);
            }
            String xsd = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            if (!xsd.contains(ASSINATURA_OBRIGATORIA)) {
                throw new IllegalStateException("Estrutura da assinatura mudou no XSD " + recurso);
            }
            SchemaFactory fabrica = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            fabrica.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            fabrica.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            return fabrica.newSchema(new StreamSource(new StringReader(xsd.replace(ASSINATURA_OBRIGATORIA, ASSINATURA_OPCIONAL))));
        } catch (IOException | SAXException e) {
            throw new IllegalStateException("Falha ao carregar " + recurso, e);
        }
    }
}
