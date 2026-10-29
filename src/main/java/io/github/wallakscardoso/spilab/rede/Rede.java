package io.github.wallakscardoso.spilab.rede;

import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Leva o XML de um participante a outro por HTTP. Tudo roda no mesmo processo, mas a fronteira
 * fica igual à de sistemas separados: cada lado só conhece o XML que recebe.
 */
@Component
public class Rede {

    private static final Pattern E2E = Pattern.compile("<(?:Orgnl)?EndToEndId>([^<]+)</");
    private static final Pattern TIPO = Pattern.compile("<MsgDefIdr>([^<]+)</");
    private static final Pattern DE = Pattern.compile("<Fr>.*?<Id>([^<]+)</Id>", Pattern.DOTALL);
    private static final Pattern PARA = Pattern.compile("<To>.*?<Id>([^<]+)</Id>", Pattern.DOTALL);
    private static final Pattern STATUS = Pattern.compile("<TxSts>([^<]+)</");

    private final Environment ambiente;
    private final MensagemRepository mensagens;
    private final Clock relogio;
    private volatile RestClient http;

    public Rede(Environment ambiente, MensagemRepository mensagens, Clock relogio) {
        this.ambiente = ambiente;
        this.mensagens = mensagens;
        this.relogio = relogio;
    }

    /** Envia o XML e devolve o XML da resposta (vazio se o destino só confirmou o recebimento). */
    public String enviar(String caminho, String xml) {
        registrar(xml);
        String resposta = http().post().uri(caminho)
                .contentType(MediaType.APPLICATION_XML)
                .accept(MediaType.APPLICATION_XML)
                .body(xml)
                .retrieve()
                .body(String.class);
        if (resposta != null && !resposta.isBlank()) {
            registrar(resposta);
        }
        return resposta;
    }

    public List<Mensagem> trocadas(String endToEndId) {
        return mensagens.findByEndToEndIdOrderByCriadaEmAsc(endToEndId);
    }

    private void registrar(String xml) {
        mensagens.save(new Mensagem(extrair(E2E, xml, null), extrair(TIPO, xml, "?"), extrair(DE, xml, "?"),
                extrair(PARA, xml, "?"), extrair(STATUS, xml, null), xml, relogio.instant()));
    }

    private static String extrair(Pattern p, String xml, String padrao) {
        Matcher m = p.matcher(xml);
        return m.find() ? m.group(1) : padrao;
    }

    private RestClient http() {
        if (http == null) {
            String porta = ambiente.getProperty("local.server.port", ambiente.getProperty("server.port", "8080"));
            http = RestClient.builder().baseUrl("http://localhost:" + porta).build();
        }
        return http;
    }
}
