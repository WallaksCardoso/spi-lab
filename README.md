# spi-lab

Simulador de estudo do Pix: participantes, chaves, ordens de pagamento em ISO 20022 e liquidação nas contas de cada participante, montados numa máquina só.

> **Em construção.** Este repositório começa pela especificação. A v0.1 está prevista para dezembro de 2026.

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

Detalhes, regras, critérios de aceite e decisões em [`docs/spec-v0.1.md`](docs/spec-v0.1.md).

## Roteiro

| Versão | Entrega |
|---|---|
| v0.1 | Pix por chave, mensagens ISO 20022 validadas no XSD, liquidação em partidas dobradas, 4 tipos de rejeição |
| v0.2 | Idempotência por EndToEndId, Kafka com outbox, devolução (`pacs.004`) |
| v0.3 | Teste de carga com números no README, métricas e painel |

## Stack

Java 21, Spring Boot 3, Maven, PostgreSQL, Flyway, JUnit 5, Testcontainers, Docker Compose.

## Licença

[MIT](LICENSE)
