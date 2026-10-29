package io.github.wallakscardoso.spilab;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PostgresDeTeste.class)
class DictApiTest {

    @Value("${local.server.port}")
    int porta;

    RestClient http;
    String ispb;
    String cpf;

    @BeforeEach
    void bancoComUmCliente() {
        http = RestClient.builder()
                .baseUrl("http://localhost:" + porta)
                .defaultStatusHandler(HttpStatusCode::isError, (req, res) -> { })
                .build();
        ispb = String.format("%08d", ThreadLocalRandom.current().nextInt(10_000_000, 99_999_999));
        cpf = String.valueOf(ThreadLocalRandom.current().nextLong(10_000_000_000L, 99_999_999_999L));
        post("/spi/participantes", Map.of("ispb", ispb, "nome", "Banco B", "aporteInicialCentavos", 0));
        post("/psps/" + ispb + "/clientes", Map.of(
                "agencia", "0001", "numero", "123", "tipo", "CACC",
                "titularNome", "Fulana de Tal", "titularDocumento", cpf,
                "depositoInicialCentavos", 0));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    ResponseEntity<Map> post(String uri, Object corpo) {
        return http.post().uri(uri).contentType(MediaType.APPLICATION_JSON).body(corpo).retrieve().toEntity(Map.class);
    }

    @SuppressWarnings("rawtypes")
    ResponseEntity<Map> registrar(String tipo, String chave) {
        Map<String, Object> corpo = new HashMap<>(Map.of("tipo", tipo, "ispb", ispb, "agencia", "0001", "numero", "123"));
        if (chave != null) {
            corpo.put("chave", chave);
        }
        return post("/dict/chaves", corpo);
    }

    static String emailNovo() {
        return "fulana." + UUID.randomUUID().toString().substring(0, 8) + "@exemplo.com.br";
    }

    @Test
    void registraEConsultaChaveDeEmailComCpfMascarado() {
        String email = emailNovo();
        assertThat(registrar("EMAIL", email).getStatusCode().value()).isEqualTo(201);

        @SuppressWarnings("unchecked")
        Map<String, Object> vinculo = http.get().uri("/dict/chaves/{c}", email).retrieve().body(Map.class);
        assertThat(vinculo)
                .containsEntry("ispb", ispb)
                .containsEntry("agencia", "0001")
                .containsEntry("numero", "123")
                .containsEntry("tipoConta", "CACC")
                .containsEntry("titularNome", "Fulana de Tal")
                .containsEntry("titularDocumento", "***." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-**");
    }

    @Test
    void chaveAleatoriaEhGeradaPeloDict() {
        var resposta = registrar("EVP", null);
        assertThat(resposta.getStatusCode().value()).isEqualTo(201);
        assertThat((String) resposta.getBody().get("chave"))
                .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

        assertThat(registrar("EVP", UUID.randomUUID().toString()).getStatusCode().value()).isEqualTo(422);
    }

    @Test
    void recusaFormatosForaDoManual() {
        assertThat(registrar("TELEFONE", "27999999999").getStatusCode().value()).isEqualTo(422);
        assertThat(registrar("TELEFONE", "+55" + ThreadLocalRandom.current().nextLong(10_000_000_000L, 99_999_999_999L))
                .getStatusCode().value()).isEqualTo(201);
        assertThat(registrar("EMAIL", "sem-arroba.com").getStatusCode().value()).isEqualTo(422);
        assertThat(registrar("EMAIL", "a".repeat(70) + "@ex.com.br").getStatusCode().value()).isEqualTo(422);
        assertThat(registrar("CPF", "123.456.789-01").getStatusCode().value()).isEqualTo(422);
    }

    @Test
    void chaveDeCpfSoDoProprioTitular() {
        String outroCpf = cpf.equals("98765432100") ? "98765432101" : "98765432100";
        assertThat(registrar("CPF", outroCpf).getStatusCode().value()).isEqualTo(422);
        assertThat(registrar("CPF", cpf).getStatusCode().value()).isEqualTo(201);
    }

    @Test
    void naoRegistraAMesmaChaveDuasVezes() {
        String email = emailNovo();
        registrar("EMAIL", email);
        assertThat(registrar("EMAIL", email).getStatusCode().value()).isEqualTo(422);
    }

    @Test
    void contaDeCpfTemNoMaximoCincoChaves() {
        for (int i = 0; i < 5; i++) {
            assertThat(registrar("EVP", null).getStatusCode().value()).isEqualTo(201);
        }
        var sexta = registrar("EVP", null);
        assertThat(sexta.getStatusCode().value()).isEqualTo(422);
        assertThat((String) sexta.getBody().get("detail")).contains("máximo de 5");
    }

    @Test
    void chaveInexistenteDa404() {
        assertThat(http.get().uri("/dict/chaves/{c}", emailNovo()).retrieve().toBodilessEntity()
                .getStatusCode().value()).isEqualTo(404);
    }
}
