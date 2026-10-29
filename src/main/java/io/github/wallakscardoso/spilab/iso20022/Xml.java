package io.github.wallakscardoso.spilab.iso20022;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/** Montagem e leitura de XML com DOM, sem geração de código a partir do XSD. */
final class Xml {

    private static final DateTimeFormatter ISO_NORMALIZADO =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(java.time.ZoneOffset.UTC);

    private final Document documento;
    private final String namespace;

    private Xml(Document documento, String namespace) {
        this.documento = documento;
        this.namespace = namespace;
    }

    static Xml novo(String namespace) {
        Document doc = construtor().newDocument();
        doc.setXmlStandalone(true);
        return new Xml(doc, namespace);
    }

    static Document ler(String xml) {
        try {
            return construtor().parse(new InputSource(new StringReader(xml)));
        } catch (SAXException e) {
            throw new MensagemInvalidaException("XML mal formado: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    Element raiz(String nome) {
        Element e = documento.createElementNS(namespace, nome);
        documento.appendChild(e);
        return e;
    }

    Element filho(Element pai, String nome) {
        Element e = documento.createElementNS(namespace, nome);
        pai.appendChild(e);
        return e;
    }

    Element filho(Element pai, String nome, String texto) {
        Element e = filho(pai, nome);
        e.setTextContent(texto);
        return e;
    }

    /** Cria a cadeia de elementos {@code a/b/c} e coloca o texto no último. */
    Element caminho(Element pai, String caminho, String texto) {
        Element atual = pai;
        for (String nome : caminho.split("/")) {
            atual = filho(atual, nome);
        }
        atual.setTextContent(texto);
        return atual;
    }

    Document documento() {
        return documento;
    }

    static String texto(Document doc) {
        try {
            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            t.setOutputProperty(OutputKeys.INDENT, "yes");
            t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            StringWriter out = new StringWriter();
            t.transform(new DOMSource(doc), new StreamResult(out));
            return out.toString();
        } catch (TransformerException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Texto do elemento no caminho {@code a/b/c} a partir de {@code base}; null se não existir. */
    static String valor(Element base, String caminho) {
        Element atual = base;
        for (String nome : caminho.split("/")) {
            atual = primeiroFilho(atual, nome);
            if (atual == null) {
                return null;
            }
        }
        return atual.getTextContent().trim();
    }

    static String obrigatorio(Element base, String caminho) {
        String v = valor(base, caminho);
        if (v == null || v.isEmpty()) {
            throw new MensagemInvalidaException("Campo obrigatório ausente: " + caminho);
        }
        return v;
    }

    static Element primeiroFilho(Element pai, String nome) {
        for (Node n = pai.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element e && nome.equals(e.getLocalName())) {
                return e;
            }
        }
        return null;
    }

    static String dataHora(Instant instante) {
        return ISO_NORMALIZADO.format(instante.truncatedTo(ChronoUnit.MILLIS));
    }

    static Instant dataHora(String texto) {
        return Instant.parse(texto);
    }

    static String valorEmReais(long centavos) {
        return BigDecimal.valueOf(centavos, 2).toPlainString();
    }

    static long centavos(String valorEmReais) {
        return new BigDecimal(valorEmReais).movePointRight(2).longValueExact();
    }

    private static DocumentBuilder construtor() {
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setNamespaceAware(true);
            f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            f.setExpandEntityReferences(false);
            return f.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException(e);
        }
    }
}
