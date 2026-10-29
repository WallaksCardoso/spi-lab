# spi-lab

Simulador de estudo do Pix: participantes, chaves, ordens de pagamento em ISO 20022 e liquidação nas contas de cada participante, montados numa máquina só.

> **Em construção.** A v0.1 está prevista para dezembro de 2026. Já existe: o razão em partidas dobradas, os participantes do SPI com suas contas PI, as contas de clientes dos PSPs e o DICT (chaves Pix).

## Por quê

Quem trabalha com Pix costuma ver só um pedaço do fluxo: o PSP de um lado e o SPI como caixa-preta do outro. O spi-lab monta o caminho inteiro, do pagador ao recebedor, para estudar e mostrar como uma ordem de pagamento sai de um participante, passa pelo SPI e é liquidada, com as mensagens do padrão ISO 20022 (`pacs.008`, `pacs.002`).

Tudo aqui é feito a partir da **documentação pública do Banco Central**. É um projeto de estudo, não um sistema de produção, e não tem relação com nenhum participante real do arranjo.

## O que a v0.1 vai fazer

Um Pix por chave, ponta a ponta:

1. o PSP pagador consulta a chave no DICT simulado;
2. monta a `pacs.008` e envia ao SPI;
3. o SPI repassa ao PSP recebedor, que aceita ou rejeita com `pacs.002`;
4. se aceito, o SPI liquida as contas PI dos dois participantes e confirma;
5. os PSPs lançam o valor nos clientes.

Todo movimento é registrado em partidas dobradas: a soma dos lançamentos é sempre zero.

Detalhes, regras, critérios de aceite e decisões em [`docs/spec-v0.1.md`](docs/spec-v0.1.md). A spec foi conferida com o catálogo de mensagens do SPI 5.13.2 e com os manuais do Banco Central (links na própria spec).

## Roteiro

| Versão | Entrega |
|---|---|
| v0.1 | Pix por chave, mensagens ISO 20022 validadas no XSD, liquidação em partidas dobradas, 6 casos de rejeição com os códigos do catálogo |
| v0.2 | Idempotência por EndToEndId, Kafka com outbox, devolução (`pacs.004`) |
| v0.3 | Teste de carga com números no README, métricas e painel |

## Como rodar

Precisa de Java 21+ e Docker.

```bash
docker compose up -d        # PostgreSQL
./mvnw spring-boot:run      # aplicação em http://localhost:8080
./mvnw verify               # testes (sobem um PostgreSQL próprio via Testcontainers)
```

Exemplo:

```bash
curl -X POST localhost:8080/spi/participantes -H 'Content-Type: application/json'   -d '{"ispb":"11111111","nome":"PSP A","aporteInicialCentavos":100000000}'

curl -X POST localhost:8080/psps/11111111/clientes -H 'Content-Type: application/json'   -d '{"agencia":"0001","numero":"123","tipo":"CACC","titularNome":"Fulano","titularDocumento":"12345678901","depositoInicialCentavos":50000}'

curl -X POST localhost:8080/dict/chaves -H 'Content-Type: application/json'   -d '{"tipo":"EMAIL","chave":"fulano@exemplo.com.br","ispb":"11111111","agencia":"0001","numero":"123"}'

curl localhost:8080/dict/chaves/fulano@exemplo.com.br
```

No Windows, nomes com acento no `curl` da linha de comando podem chegar fora de UTF-8; mande o corpo de um arquivo (`--data-binary @corpo.json`).

## DICT

Chaves de CPF, CNPJ (já no formato alfanumérico), e-mail (até 77 caracteres), celular (`+` E.164) e aleatória (UUID gerado pelo DICT), conforme o Manual Operacional do DICT 8.5. Chave de CPF ou CNPJ só do próprio titular; até 5 chaves por conta de CPF e 20 por conta de CNPJ. A consulta devolve o CPF do titular mascarado (`***.456.789-**`).

## Como o dinheiro é registrado

Cada livro (o SPI e cada PSP) tem suas contas no razão. Toda transação movimenta duas ou mais contas do mesmo livro, e a soma das partidas é sempre zero: o aporte numa conta PI sai da conta `STR` (reservas bancárias); o depósito na conta de um cliente sai da conta `CAIXA` do PSP. Contas de dinheiro de alguém (PI, cliente) nunca ficam negativas; se uma partida deixaria saldo negativo, a transação inteira é desfeita.

## Stack

Java 21, Spring Boot 4.1, Maven, PostgreSQL 17, Flyway, JUnit 5, Testcontainers, Docker Compose, GitHub Actions.

## Licença

[MIT](LICENSE)
