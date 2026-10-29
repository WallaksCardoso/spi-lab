package io.github.wallakscardoso.spilab.rede;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MensagemRepository extends JpaRepository<Mensagem, UUID> {

    List<Mensagem> findByEndToEndIdOrderByCriadaEmAsc(String endToEndId);
}
