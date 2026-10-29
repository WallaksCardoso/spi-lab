package io.github.wallakscardoso.spilab.psp;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ContaClienteRepository extends JpaRepository<ContaCliente, UUID> {

    Optional<ContaCliente> findByIspbAndAgenciaAndNumero(String ispb, String agencia, String numero);

    boolean existsByIspbAndAgenciaAndNumero(String ispb, String agencia, String numero);
}
