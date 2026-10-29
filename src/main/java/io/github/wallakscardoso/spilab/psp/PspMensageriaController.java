package io.github.wallakscardoso.spilab.psp;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Onde o SPI entrega mensagens a cada PSP. */
@RestController
public class PspMensageriaController {

    private final PixService pix;

    public PspMensageriaController(PixService pix) {
        this.pix = pix;
    }

    @PostMapping(path = "/psps/{ispb}/spi/pacs008", consumes = MediaType.APPLICATION_XML_VALUE,
            produces = MediaType.APPLICATION_XML_VALUE)
    public String ordem(@PathVariable String ispb, @RequestBody String xml) {
        return pix.receberOrdem(ispb, xml);
    }

    @PostMapping(path = "/psps/{ispb}/spi/pacs002", consumes = MediaType.APPLICATION_XML_VALUE)
    public void aviso(@PathVariable String ispb, @RequestBody String xml) {
        pix.receberAviso(ispb, xml);
    }
}
