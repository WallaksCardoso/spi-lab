create table ordem (
    end_to_end_id   varchar(32)  primary key,
    ispb_pagador    varchar(8)   not null,
    ispb_recebedor  varchar(8)   not null,
    valor_centavos  bigint       not null,
    status          varchar(20)  not null,
    motivo          varchar(4),
    criada_em       timestamptz  not null,
    atualizada_em   timestamptz  not null
);

create table pix (
    id               uuid primary key,
    end_to_end_id    varchar(32)  not null,
    ispb             varchar(8)   not null,
    direcao          varchar(8)   not null,
    agencia          varchar(4)   not null,
    numero           varchar(20)  not null,
    valor_centavos   bigint       not null,
    chave            varchar(77),
    ispb_contraparte varchar(8)   not null,
    status           varchar(20)  not null,
    motivo           varchar(4),
    criado_em        timestamptz  not null,
    atualizado_em    timestamptz  not null,
    unique (end_to_end_id, ispb)
);

create table mensagem (
    id             uuid primary key,
    end_to_end_id  varchar(32),
    tipo           varchar(35)  not null,
    de             varchar(8)   not null,
    para           varchar(8)   not null,
    status         varchar(4),
    xml            text         not null,
    criada_em      timestamptz  not null
);

create index mensagem_e2e_idx on mensagem (end_to_end_id, criada_em);
