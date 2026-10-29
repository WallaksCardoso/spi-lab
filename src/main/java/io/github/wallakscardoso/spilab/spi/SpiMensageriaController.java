package io.github.wallakscardoso.spilab.spi;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Porta de entrada do SPI para os participantes: XML ISO 20022 entra, XML sai. */
@RestController
public class SpiMensageriaController {

    private final SpiProcessamento processamento;

    public SpiMensageriaController(SpiProcessamento processamento) {
        this.processamento = processamento;
    }

    @PostMapping(path = "/spi/pacs008", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String receber(@RequestBody String xml) {
        return processamento.receber(xml);
    }
}
