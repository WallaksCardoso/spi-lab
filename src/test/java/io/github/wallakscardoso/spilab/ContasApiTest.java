package io.github.wallakscardoso.spilab;

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
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PostgresDeTeste.class)
class ContasApiTest {

    @Value("${local.server.port}")
    int porta;

    @Autowired
    Razao razao;

    RestClient http;

    @BeforeEach
    void cliente() {
        http = RestClient.builder()
                .baseUrl("http://localhost:" + porta)
                .defaultStatusHandler(HttpStatusCode::isError, (req, res) -> { })
                .build();
    }

    static String ispbNovo() {
        return String.format("%08d", ThreadLocalRandom.current().nextInt(10_000_000, 99_999_999));
    }

    int post(String uri, Object corpo) {
        return http.post().uri(uri).contentType(MediaType.APPLICATION_JSON).body(corpo)
                .retrieve().toBodilessEntity().getStatusCode().value();
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> get(String uri) {
        return http.get().uri(uri).retrieve().body(Map.class);
    }

    @Test
    void cadastraParticipanteComAporteNaContaPi() {
        String ispb = ispbNovo();

        assertThat(post("/spi/participantes", Map.of("ispb", ispb, "nome", "PSP Teste", "aporteInicialCentavos", 1_000_000)))
                .isEqualTo(201);

        assertThat(get("/spi/participantes/" + ispb + "/saldo")).containsEntry("saldoCentavos", 1_000_000);
        assertThat(razao.somaDoLivro(Spi.LIVRO)).isZero();
    }

    @Test
    void recusaIspbRepetidoOuMalFormatado() {
        String ispb = ispbNovo();
        post("/spi/participantes", Map.of("ispb", ispb, "nome", "PSP Teste", "aporteInicialCentavos", 0));

        assertThat(post("/spi/participantes", Map.of("ispb", ispb, "nome", "De novo", "aporteInicialCentavos", 0)))
                .isEqualTo(422);
        assertThat(post("/spi/participantes", Map.of("ispb", "123", "nome", "Curto", "aporteInicialCentavos", 0)))
                .isEqualTo(400);
    }

    @Test
    void abreContaDeClienteComDepositoInicial() {
        String ispb = ispbNovo();
        post("/spi/participantes", Map.of("ispb", ispb, "nome", "PSP Teste", "aporteInicialCentavos", 0));

        int status = post("/psps/" + ispb + "/clientes", Map.of(
                "agencia", "0001", "numero", "123456", "tipo", "CACC",
                "titularNome", "Fulano da Silva", "titularDocumento", "12345678901",
                "depositoInicialCentavos", 25_000));

        assertThat(status).isEqualTo(201);
        assertThat(get("/psps/" + ispb + "/clientes/0001/123456"))
                .containsEntry("saldoCentavos", 25_000)
                .containsEntry("tipo", "CACC");
        assertThat(razao.somaDoLivro(Psp.livro(ispb))).isZero();
    }

    @Test
    void aceitaCnpjAlfanumericoERecusaDocumentoInvalido() {
        String ispb = ispbNovo();
        post("/spi/participantes", Map.of("ispb", ispb, "nome", "PSP Teste", "aporteInicialCentavos", 0));

        assertThat(post("/psps/" + ispb + "/clientes", Map.of(
                "agencia", "0001", "numero", "1", "tipo", "CACC",
                "titularNome", "Empresa X", "titularDocumento", "12ABC34501DE35",
                "depositoInicialCentavos", 0))).isEqualTo(201);
        assertThat(post("/psps/" + ispb + "/clientes", Map.of(
                "agencia", "0001", "numero", "2", "tipo", "CACC",
                "titularNome", "Documento ruim", "titularDocumento", "123.456.789-01",
                "depositoInicialCentavos", 0))).isEqualTo(400);
    }

    @Test
    void contaDeClienteExigeParticipanteCadastrado() {
        assertThat(post("/psps/" + ispbNovo() + "/clientes", Map.of(
                "agencia", "0001", "numero", "9", "tipo", "CACC",
                "titularNome", "Sem PSP", "titularDocumento", "12345678901",
                "depositoInicialCentavos", 0))).isEqualTo(404);
    }
}
