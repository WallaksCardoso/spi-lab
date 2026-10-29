create table conta (
    id                uuid primary key,
    livro             varchar(20)  not null,
    codigo            varchar(60)  not null,
    descricao         varchar(120) not null,
    permite_negativo  boolean      not null,
    saldo_centavos    bigint       not null default 0,
    versao            bigint       not null default 0,
    unique (livro, codigo),
    check (permite_negativo or saldo_centavos >= 0)
);

create table transacao (
    id         uuid primary key,
    livro      varchar(20)  not null,
    descricao  varchar(200) not null,
    criada_em  timestamptz  not null
);

create table lancamento (
    id              uuid primary key,
    transacao_id    uuid   not null references transacao (id),
    conta_id        uuid   not null references conta (id),
    valor_centavos  bigint not null check (valor_centavos <> 0)
);

create index lancamento_transacao_idx on lancamento (transacao_id);
create index lancamento_conta_idx on lancamento (conta_id);
