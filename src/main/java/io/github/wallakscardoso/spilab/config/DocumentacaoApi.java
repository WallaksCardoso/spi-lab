package io.github.wallakscardoso.spilab.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class DocumentacaoApi {

    @Bean
    OpenAPI spiLab() {
        return new OpenAPI().info(new Info()
                .title("spi-lab")
                .version("0.1.0")
                .description("""
                        Simulador de estudo do Pix. Ordem de uso: cadastrar participantes (SPI), abrir contas \
                        de clientes (PSP), registrar uma chave (DICT) e enviar o Pix. As rotas /spi/pacs008 e \
                        /psps/{ispb}/spi/* trocam XML ISO 20022 entre participantes e SPI.""")
                .license(new License().name("MIT").url("https://github.com/WallaksCardoso/spi-lab/blob/main/LICENSE")));
    }
}
