create table participante (
    ispb        varchar(8)     primary key,
    nome        varchar(120) not null,
    criado_em   timestamptz  not null
);

create table conta_cliente (
    id                 uuid primary key,
    ispb               varchar(8)     not null references participante (ispb),
    agencia            varchar(4)   not null,
    numero             varchar(20)  not null,
    tipo               varchar(4)   not null,
    titular_nome       varchar(140) not null,
    titular_documento  varchar(14)  not null,
    conta_id           uuid         not null unique references conta (id),
    unique (ispb, agencia, numero)
);
