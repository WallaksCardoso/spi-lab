package io.github.wallakscardoso.spilab.psp;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PixRepository extends JpaRepository<Pix, UUID> {

    Optional<Pix> findByEndToEndIdAndIspb(String endToEndId, String ispb);
}
