package io.github.wallakscardoso.spilab.razao;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContaRepository extends JpaRepository<Conta, UUID> {

    /** Trava as contas sempre na mesma ordem (por código) para que duas transações não se esperem em círculo. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Conta c where c.livro = :livro and c.codigo in :codigos order by c.codigo")
    List<Conta> travar(String livro, Collection<String> codigos);

    Optional<Conta> findByLivroAndCodigo(String livro, String codigo);

    boolean existsByLivroAndCodigo(String livro, String codigo);

    @Query("select coalesce(sum(c.saldoCentavos), 0) from Conta c where c.livro = :livro")
    long somaDosSaldos(String livro);
}
