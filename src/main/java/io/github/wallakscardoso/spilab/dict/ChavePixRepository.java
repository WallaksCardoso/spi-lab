package io.github.wallakscardoso.spilab.dict;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ChavePixRepository extends JpaRepository<ChavePix, String> {

    long countByIspbAndAgenciaAndNumero(String ispb, String agencia, String numero);
}
