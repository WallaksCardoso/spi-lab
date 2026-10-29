package io.github.wallakscardoso.spilab;

import io.github.wallakscardoso.spilab.iso20022.Cabecalho;
import io.github.wallakscardoso.spilab.iso20022.Identificadores;
import io.github.wallakscardoso.spilab.iso20022.Pacs002;
import io.github.wallakscardoso.spilab.iso20022.Pacs008;
import io.github.wallakscardoso.spilab.psp.Psp;
import io.github.wallakscardoso.spilab.razao.Razao;
import io.github.wallakscardoso.spilab.spi.Spi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PostgresDeTeste.class)
class PixFluxoTest {

    @Value("${local.server.port}")
    int porta;

    @Autowired
    Razao razao;

    RestClient http;
    String bancoA;
    String bancoB;
    String chaveMaria;

    static String ispbNovo() {
        return String.format("%08d", ThreadLocalRandom.current().nextInt(10_000_000, 99_999_999));
    }

    static String cpfNovo() {
        return String.valueOf(ThreadLocalRandom.current().nextLong(10_000_000_000L, 99_999_999_999L));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    ResponseEntity<Map> post(String uri, Object corpo) {
        return http.post().uri(uri).contentType(MediaType.APPLICATION_JSON).body(corpo).retrieve().toEntity(Map.class);
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> get(String uri) {
        return http.get().uri(uri).retrieve().body(Map.class);
    }

    long saldoCliente(String ispb, String agencia, String numero) {
        return ((Number) get("/psps/" + ispb + "/clientes/" + agencia + "/" + numero).get("saldoCentavos")).longValue();
    }

    long saldoPi(String ispb) {
        return ((Number) get("/spi/participantes/" + ispb + "/saldo").get("saldoCentavos")).longValue();
    }

    void banco(String ispb, long aportePi) {
        post("/spi/participantes", Map.of("ispb", ispb, "nome", "Banco " + ispb, "aporteInicialCentavos", aportePi));
    }

    void conta(String ispb, String numero, long deposito) {
        post("/psps/" + ispb + "/clientes", Map.of("agencia", "0001", "numero", numero, "tipo", "CACC",
                "titularNome", "Cliente " + numero, "titularDocumento", cpfNovo(), "depositoInicialCentavos", deposito));
    }

    @SuppressWarnings("rawtypes")
    ResponseEntity<Map> pix(String ispb, String numero, String chave, long valor) {
        return post("/psps/" + ispb + "/pix", Map.of("agencia", "0001", "numero", numero, "chave", chave,
                "valorCentavos", valor, "descricao", "teste"));
    }

    @BeforeEach
    void cenario() {
        http = RestClient.builder()
                .baseUrl("http://localhost:" + porta)
                .defaultStatusHandler(HttpStatusCode::isError, (req, res) -> { })
                .build();
        bancoA = ispbNovo();
        bancoB = ispbNovo();
        banco(bancoA, 1_000_000_00);
        banco(bancoB, 1_000_000_00);
        conta(bancoA, "1001", 500_00);
        conta(bancoB, "2002", 0);
        chaveMaria = "maria." + UUID.randomUUID().toString().substring(0, 8) + "@exemplo.com.br";
        post("/dict/chaves", Map.of("tipo", "EMAIL", "chave", chaveMaria, "ispb", bancoB, "agencia", "0001", "numero", "2002"));
    }

    void livrosFechamEmZero() {
        assertThat(razao.somaDoLivro(Spi.LIVRO)).isZero();
        assertThat(razao.somaDoLivro(Psp.livro(bancoA))).isZero();
        assertThat(razao.somaDoLivro(Psp.livro(bancoB))).isZero();
    }

    @Test
    void pixPorChaveLiquidaEODinheiroChegaNaMaria() {
        var resposta = pix(bancoA, "1001", chaveMaria, 150_00);

        assertThat(resposta.getStatusCode().value()).isEqualTo(201);
        assertThat(resposta.getBody()).containsEntry("status", "LIQUIDADO").containsEntry("ispbContraparte", bancoB);
        String e2e = (String) resposta.getBody().get("endToEndId");

        assertThat(saldoCliente(bancoA, "0001", "1001")).isEqualTo(350_00);
        assertThat(saldoCliente(bancoB, "0001", "2002")).isEqualTo(150_00);
        assertThat(saldoPi(bancoA)).isEqualTo(1_000_000_00 - 150_00);
        assertThat(saldoPi(bancoB)).isEqualTo(1_000_000_00 + 150_00);
        livrosFechamEmZero();

        assertThat(get("/psps/" + bancoB + "/pix/" + e2e)).containsEntry("status", "LIQUIDADO").containsEntry("direcao", "RECEBIDO");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> mensagens = (List<Map<String, Object>>) get("/psps/" + bancoA + "/pix/" + e2e).get("mensagens");
        assertThat(mensagens).extracting(m -> m.get("tipo") + " " + m.get("de") + ">" + m.get("para") + " " + m.get("status"))
                .containsExactly(
                        "pacs.008.spi.1.16 " + bancoA + ">" + Spi.ISPB + " null",
                        "pacs.008.spi.1.16 " + Spi.ISPB + ">" + bancoB + " null",
                        "pacs.002.spi.1.18 " + bancoB + ">" + Spi.ISPB + " ACSP",
                        "pacs.002.spi.1.18 " + Spi.ISPB + ">" + bancoB + " ACCC",
                        "pacs.002.spi.1.18 " + Spi.ISPB + ">" + bancoA + " ACSC");
    }

    @Test
    void chaveInexistenteParaAntesDeQualquerMensagem() {
        assertThat(pix(bancoA, "1001", "ninguem@exemplo.com.br", 100).getStatusCode().value()).isEqualTo(404);
        assertThat(saldoCliente(bancoA, "0001", "1001")).isEqualTo(500_00);
    }

    @Test
    void clienteSemSaldoParaAntesDeQualquerMensagem() {
        assertThat(pix(bancoA, "1001", chaveMaria, 500_01).getStatusCode().value()).isEqualTo(422);
        assertThat(saldoCliente(bancoA, "0001", "1001")).isEqualTo(500_00);
        livrosFechamEmZero();
    }

    @Test
    void contaPiSemSaldoRejeitaComAM04EDevolveAoCliente() {
        String bancoPobre = ispbNovo();
        banco(bancoPobre, 100_00);
        conta(bancoPobre, "3003", 1_000_00);

        var resposta = pix(bancoPobre, "3003", chaveMaria, 200_00);

        assertThat(resposta.getBody()).containsEntry("status", "REJEITADO").containsEntry("motivo", "AM04");
        assertThat(saldoCliente(bancoPobre, "0001", "3003")).isEqualTo(1_000_00);
        assertThat(saldoCliente(bancoB, "0001", "2002")).isZero();
        assertThat(saldoPi(bancoPobre)).isEqualTo(100_00);
        String e2e = (String) resposta.getBody().get("endToEndId");
        assertThat(get("/psps/" + bancoB + "/pix/" + e2e)).containsEntry("status", "REJEITADO").containsEntry("motivo", "AM04");
        assertThat(razao.somaDoLivro(Psp.livro(bancoPobre))).isZero();
        livrosFechamEmZero();
    }

    @Test
    void valorZeroRejeitaComAM01() {
        var resposta = pix(bancoA, "1001", chaveMaria, 0);
        assertThat(resposta.getBody()).containsEntry("status", "REJEITADO").containsEntry("motivo", "AM01");
        assertThat(saldoCliente(bancoA, "0001", "1001")).isEqualTo(500_00);
    }

    Pacs002 enviarAoSpi(String endToEndId, String contaRecebedor) {
        Instant agora = Instant.now();
        Pacs008 ordem = new Pacs008(
                new Cabecalho(bancoA, Spi.ISPB, Identificadores.msgId(bancoA), Pacs008.TIPO, agora),
                endToEndId, 10_00, agora,
                new Pacs008.Pagador("Cliente 1001", "11122233344", bancoA, "0001", "1001", "CACC"),
                new Pacs008.Recebedor("55566677788", bancoB, "0001", contaRecebedor, "CACC", null),
                null);
        String xml = http.post().uri("/spi/pacs008").contentType(MediaType.APPLICATION_XML).body(ordem.paraXml())
                .retrieve().body(String.class);
        return Pacs002.deXml(xml);
    }

    @Test
    void contaDoRecebedorInexistenteRejeitaComAC03() {
        Pacs002 resposta = enviarAoSpi(Identificadores.endToEndId(bancoA, Instant.now()), "999999");
        assertThat(resposta.status()).isEqualTo(Pacs002.Status.RJCT);
        assertThat(resposta.motivo()).isEqualTo("AC03");
        assertThat(saldoPi(bancoA)).isEqualTo(1_000_000_00);
    }

    @Test
    void endToEndIdDeOutroParticipanteRejeitaComFF08() {
        String deOutroBanco = Identificadores.endToEndId(ispbNovo(), Instant.now());
        Pacs002 resposta = enviarAoSpi(deOutroBanco, "2002");
        assertThat(resposta.status()).isEqualTo(Pacs002.Status.RJCT);
        assertThat(resposta.motivo()).isEqualTo("FF08");
        assertThat(saldoCliente(bancoB, "0001", "2002")).isZero();
    }

    @Test
    void cinquentaPixSimultaneosNuncaDeixamSaldoNegativo() throws Exception {
        conta(bancoA, "4004", 30 * 10_00);
        List<Future<Integer>> resultados = new ArrayList<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(50)) {
            for (int i = 0; i < 50; i++) {
                resultados.add(pool.submit(() -> pix(bancoA, "4004", chaveMaria, 10_00).getStatusCode().value()));
            }
        }
        long liquidados = 0;
        long semSaldo = 0;
        for (Future<Integer> r : resultados) {
            int status = r.get();
            if (status == 201) {
                liquidados++;
            } else if (status == 422) {
                semSaldo++;
            }
        }

        assertThat(liquidados).isEqualTo(30);
        assertThat(semSaldo).isEqualTo(20);
        assertThat(saldoCliente(bancoA, "0001", "4004")).isZero();
        assertThat(saldoCliente(bancoB, "0001", "2002")).isEqualTo(30 * 10_00);
        livrosFechamEmZero();
    }
}
