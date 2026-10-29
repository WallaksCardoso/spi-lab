create table chave_pix (
    chave              varchar(77)  primary key,
    tipo               varchar(9)   not null,
    ispb               varchar(8)   not null,
    agencia            varchar(4)   not null,
    numero             varchar(20)  not null,
    tipo_conta         varchar(4)   not null,
    titular_nome       varchar(140) not null,
    titular_documento  varchar(14)  not null,
    criada_em          timestamptz  not null
);

create index chave_pix_conta_idx on chave_pix (ispb, agencia, numero);
