package io.github.wallakscardoso.spilab.spi;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrdemRepository extends JpaRepository<Ordem, String> {
}
