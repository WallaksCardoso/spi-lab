package io.github.wallakscardoso.spilab.razao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface TransacaoRepository extends JpaRepository<Transacao, UUID> {

    @Query("""
            select coalesce(sum(l.valorCentavos), 0) from Lancamento l
            where l.conta.livro = :livro
            """)
    long somaDosLancamentos(String livro);
}
