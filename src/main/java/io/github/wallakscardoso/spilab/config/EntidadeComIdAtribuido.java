package io.github.wallakscardoso.spilab.config;

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

/**
 * As entidades geram o próprio id (UUID, ISPB). Sem isto o Spring Data trata toda entidade com
 * id preenchido como existente e faz merge em vez de insert.
 */
@MappedSuperclass
public abstract class EntidadeComIdAtribuido<ID> implements Persistable<ID> {

    @Transient
    private boolean nova = true;

    @Override
    public boolean isNew() {
        return nova;
    }

    @PostPersist
    @PostLoad
    void marcarComoPersistida() {
        nova = false;
    }
}
