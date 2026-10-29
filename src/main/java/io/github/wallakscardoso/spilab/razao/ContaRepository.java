package io.github.wallakscardoso.spilab.razao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface ContaRepository extends JpaRepository<Conta, UUID> {

    Optional<Conta> findByLivroAndCodigo(String livro, String codigo);

    boolean existsByLivroAndCodigo(String livro, String codigo);

    @Query("select coalesce(sum(c.saldoCentavos), 0) from Conta c where c.livro = :livro")
    long somaDosSaldos(String livro);
}
