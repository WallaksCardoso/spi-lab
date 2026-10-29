package io.github.wallakscardoso.spilab;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PostgresDeTeste.class)
class DocumentacaoApiTest {

    @Value("${local.server.port}")
    int porta;

    @Test
    void publicaAEspecificacaoOpenApiComAsRotasDoPix() {
        String api = RestClient.create("http://localhost:" + porta).get().uri("/v3/api-docs").retrieve().body(String.class);

        assertThat(api).contains("\"title\":\"spi-lab\"", "/psps/{ispb}/pix", "/dict/chaves", "/spi/pacs008");
    }

    @Test
    void publicaATelaDoSwagger() {
        var resposta = RestClient.create("http://localhost:" + porta).get().uri("/swagger-ui/index.html")
                .retrieve().toEntity(String.class);

        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
    }
}
