# Spec v0.1 — envio de Pix por chave, ponta a ponta

**Prazo:** v0.1 publicada até **06/12/2026** (semanas 1 a 4 do plano, ~5 h por semana).

## Problema
Quem trabalha com Pix vê só um pedaço do fluxo: o PSP de um lado, o SPI como caixa-preta do outro. O spi-lab monta o caminho inteiro numa máquina só, para estudar e mostrar como a ordem sai do pagador, passa pelo SPI e é liquidada, com as mensagens reais do padrão ISO 20022.

## Objetivo da v0.1
Um cliente do PSP A manda R$ X por chave Pix para um cliente do PSP B. O sistema:
1. resolve a chave no DICT simulado;
2. o PSP A monta a `pacs.008` e envia ao SPI;
3. o SPI valida e repassa ao PSP B;
4. o PSP B aceita ou rejeita com `pacs.002`;
5. se aceito, o SPI liquida (debita a conta PI do PSP A, credita a do PSP B) e confirma para os dois com `pacs.002`;
6. os PSPs lançam o débito no cliente pagador e o crédito no recebedor.

Tudo registrado em **partidas dobradas**: nenhum centavo aparece ou some.

## Fora do escopo da v0.1
- Idempotência por EndToEndId, Kafka/outbox e devolução (`pacs.004`): **v0.2**
- Teste de carga, métricas e painel: **v0.3**
- Pix agendado, QR Code, Pix Saque/Troco, MED, limites noturnos, antifraude
- Autenticação/segurança de rede real (mTLS, assinatura XML) e alta disponibilidade
- Fidelidade total ao catálogo do Bacen: a v0.1 usa o **subconjunto** de campos necessário, marcado no README

## Componentes (um só serviço, módulos separados por pacote)

| Módulo | Papel | Dados |
|---|---|---|
| `dict` | Diretório de chaves: CPF, CNPJ, e-mail, celular, aleatória (EVP) → conta (ISPB, agência, conta, tipo, nome) | `chave` |
| `spi` | Participantes e suas **contas PI**; recebe `pacs.008`, repassa, recebe `pacs.002`, liquida, confirma | `participante`, `conta_pi`, `ordem`, `lancamento_pi` |
| `psp` | Dois PSPs simulados (A e B) com contas de clientes; iniciam e recebem Pix | `conta_cliente`, `lancamento_cliente`, `pix` |
| `iso20022` | Montar e ler `pacs.008` e `pacs.002` em XML, validando contra o XSD | — |

Comunicação PSP ↔ SPI por HTTP com corpo XML, mesmo dentro do mesmo processo. Assim a fronteira fica explícita e, na v0.2, troca por mensageria sem mexer no domínio.

## Fluxo

```
Cliente A ──POST /psp-a/pix──▶ PSP A ──consulta──▶ DICT
                                  │ pacs.008
                                  ▼
                                 SPI ──pacs.008──▶ PSP B
                                  ▲               │ pacs.002 (aceita/rejeita)
                                  └───────────────┘
              SPI liquida contas PI (A −X, B +X)
       pacs.002 (liquidado) ──▶ PSP A e PSP B ──▶ lançam nos clientes
```

Estados da ordem no SPI: `RECEBIDA → ENVIADA_RECEBEDOR → ACEITA → LIQUIDADA` ou `REJEITADA`.

## Regras da v0.1
- **Valor** em centavos (`long`), nunca `double`; maior que zero.
- **EndToEndId** no formato público do Pix: `E` + ISPB do pagador (8) + data/hora `AAAAMMDDHHMM` (UTC) + 11 caracteres alfanuméricos = 32 caracteres. Gerado pelo PSP pagador.
- **Saldo da conta PI**: o SPI só liquida se o PSP pagador tiver saldo; senão rejeita a ordem.
- **Rejeições cobertas** (códigos a conferir no catálogo de mensagens do SPI): chave inexistente (no PSP A, antes de enviar), conta do recebedor inválida (PSP B), saldo insuficiente na conta PI (SPI), cliente pagador sem saldo (PSP A).
- **Partidas dobradas**: cada movimento grava débito e crédito com o mesmo id de transação; soma de débitos = soma de créditos, sempre.
- **Concorrência**: saldo com bloqueio otimista (`version`); duas ordens simultâneas no mesmo pagador não podem deixar saldo negativo.
- **Horário**: tudo em UTC no banco; exibição em America/Sao_Paulo.

## API de simulação (REST, JSON)
- `POST /dict/chaves` — cadastra chave
- `POST /psps/{ispb}/clientes` — abre conta de cliente com saldo inicial
- `POST /psps/{ispb}/pix` — `{ contaPagador, chave, valorCentavos, descricao }` → `201` com EndToEndId e status
- `GET /psps/{ispb}/pix/{endToEndId}` — status e mensagens trocadas (XML incluído, para estudo)
- `GET /spi/participantes/{ispb}/saldo` — saldo da conta PI
- Swagger em `/swagger-ui`

## Critérios de aceite
- [ ] `docker compose up` sobe banco e aplicação; um script cria 2 PSPs, 2 clientes e 1 chave
- [ ] Pix de R$ 100,00 por chave: status final `LIQUIDADA`; saldos de clientes e contas PI batem
- [ ] Cada uma das 4 rejeições termina em `REJEITADA` sem mexer em saldo nenhum
- [ ] Toda `pacs.008`/`pacs.002` gerada valida contra o XSD
- [ ] Teste de concorrência: 50 Pix simultâneos do mesmo pagador nunca deixam saldo negativo
- [ ] Invariante testada: soma de todos os lançamentos = 0 (cliente e PI)
- [ ] Testes de integração com Testcontainers (PostgreSQL); CI no GitHub Actions verde
- [ ] README: o problema, como rodar em 1 comando, diagrama do fluxo, decisões e o que é simplificação

## Decisões (e por quê)
1. **Um serviço modular, não microsserviços**: o foco é o domínio e as mensagens; separar em serviços agora só traria infraestrutura. As fronteiras HTTP/XML deixam a separação fácil depois.
2. **XML de verdade (ISO 20022) entre PSP e SPI**, não JSON: é o que diferencia o projeto e o que o mercado de pagamentos conhece.
3. **Partidas dobradas desde o início**: é a parte mais difícil de colocar depois e a que mais mostra maturidade em sistema financeiro.
4. **PostgreSQL + Testcontainers** em vez de H2: testar concorrência e bloqueio em banco de verdade.
5. **Síncrono na v0.1**: o fluxo real é assíncrono com timeouts; a v0.2 introduz isso com mensageria. Fica registrado no README como simplificação.

## Fontes (públicas)
- Regulamento do Pix e manuais do SPI e do DICT, no site do Banco Central (Pix > Regulamentação e documentação técnica)
- Catálogo de mensagens e XSDs do SPI (ISO 20022)
- Contratos da API Pix do Bacen: https://github.com/bacen/pix-api
- Desafio "ICOM/SPI Simulator" da Woovi, como referência de escopo: https://woovibr.github.io/jobs/

Antes de codar a parte de mensagens, baixar os manuais e XSDs atuais e conferir campos, estados e códigos de rejeição; o que divergir desta spec, a spec muda.

## Plano das semanas
| Semana | Datas | Entrega |
|---|---|---|
| 0 | 02/11 – 08/11 | Esta spec revisada; baixar manuais/XSDs; criar o repo com README de intenção |
| 1 | 09/11 – 15/11 | Esqueleto (Maven, Compose, Flyway, CI); participantes, contas PI e contas de clientes; ledger com invariante testada |
| 2 | 16/11 – 22/11 | DICT; `pacs.008`/`pacs.002` montadas e validadas no XSD; post de 18/11 |
| 3 | 23/11 – 29/11 | Fluxo completo e as 4 rejeições; teste de concorrência |
| 4 | 30/11 – 06/12 | README, diagrama, Swagger; **tag v0.1** |
