# Spec v0.1 — envio de Pix por chave, ponta a ponta

**Prazo:** v0.1 publicada até **06/12/2026** (semanas 1 a 4 do plano, ~5 h por semana).

## Problema
Quem trabalha com Pix vê só um pedaço do fluxo: o PSP de um lado, o SPI como caixa-preta do outro. O spi-lab monta o caminho inteiro numa máquina só, para estudar e mostrar como a ordem sai do pagador, passa pelo SPI e é liquidada, com as mensagens reais do padrão ISO 20022.

## Objetivo da v0.1
Um cliente do PSP A manda R$ X por chave Pix para um cliente do PSP B. O sistema:
1. resolve a chave no DICT simulado;
2. o PSP A monta a `pacs.008` e envia ao SPI;
3. o SPI valida e repassa ao PSP B;
4. o PSP B aceita (`pacs.002` com status `ACSP`) ou rejeita (`RJCT` com código de motivo);
5. se aceito, o SPI liquida (debita a conta PI do PSP A, credita a do PSP B) e confirma com `pacs.002`: `ACSC` para o PSP pagador e `ACCC` para o PSP recebedor;
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
| `dict` | Diretório de chaves: CPF, CNPJ, e-mail, celular, aleatória → conta (ISPB, agência, conta, tipo, nome) | `chave` |
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
                                  ▲               │ pacs.002 ACSP (aceita) ou RJCT (rejeita)
                                  └───────────────┘
              SPI liquida contas PI (A −X, B +X)
       pacs.002 ACSC ──▶ PSP A        pacs.002 ACCC ──▶ PSP B
                         └──────── lançam nos clientes ────────┘
```

Estados da ordem no SPI: `RECEBIDA → ENVIADA_RECEBEDOR → ACEITA → LIQUIDADA` ou `REJEITADA`.

Status da `pacs.002` (tabela de domínios do catálogo 5.13): `ACSP` = aceita pelo recebedor; `RJCT` = rejeitada (SPI ou recebedor); `ACSC` = SPI avisa o PSP **pagador** que liquidou; `ACCC` = SPI avisa o PSP **recebedor** que liquidou.

## Mensagens (catálogo do SPI 5.13.2)
- Versões dos XSDs: `pacs.008.spi.1.16` e `pacs.002.spi.1.18` (namespace `https://www.bcb.gov.br/pi/pacs.008/1.16` e equivalente).
- Toda mensagem vai num `Envelope` com `AppHdr` (cabeçalho: `Fr` e `To` com o ISPB de quem envia e de quem recebe, `BizMsgIdr`, `MsgDefIdr`, `CreDt`, `Sgntr`) e `Document`.
- `BizMsgIdr`/`MsgId`: `M` + ISPB (8) + 23 caracteres = 32. Nos exemplos oficiais o SPI aparece com o ISPB `00038166` (Banco Central).
- `pacs.008` usa `IntrBkSttlmAmt Ccy="BRL"` com duas casas (`1000.00`): o domínio guarda centavos e converte só na borda; `LclInstrm/Prtry = DICT` quando a ordem vem de chave; `CdtrAcct/Prxy/Id` leva a chave.
- `Sgntr` vai vazio: assinatura XML fica fora do escopo.
- Exemplos oficiais de cada mensagem estão no pacote (`exemplos/pacs008`, `exemplos/pacs002`): servem de base para os testes.

## Regras da v0.1
- **Valor** em centavos (`long`), nunca `double`; maior que zero.
- **EndToEndId** no formato público do Pix: `E` + ISPB do pagador (8) + data/hora `AAAAMMDDHHMM` (UTC) + 11 caracteres alfanuméricos = 32 caracteres. Gerado pelo PSP pagador.
- **Saldo da conta PI**: o SPI só liquida se o PSP pagador tiver saldo; senão rejeita a ordem.
- **Chaves** (Manual Operacional do DICT 8.5): celular `+` E.164 (ex. `+5527999999999`); e-mail até 77 caracteres; CPF com 11 dígitos; **CNPJ com 14 caracteres alfanuméricos** (já no formato novo); aleatória = UUID gerado pelo DICT. Sem pontos ou traços. Limite: 5 chaves por conta de CPF e 20 por conta de CNPJ.
- **Rejeições cobertas**:

| Caso | Onde | Resultado |
|---|---|---|
| Chave inexistente | PSP A, antes de enviar | erro local; nenhuma mensagem sai |
| Cliente pagador sem saldo | PSP A, antes de enviar | erro local; nenhuma mensagem sai |
| Conta do recebedor inválida | PSP B | `pacs.002` `RJCT` com `AC03` |
| Saldo insuficiente na conta PI do PSP A | SPI | `pacs.002` `RJCT` com `AM04` |
| Valor zero | SPI | `RJCT` com `AM01` |
| EndToEndId mal formatado | SPI | `RJCT` com `FF08` |

- **Tempo** (Manual de Tempos do Pix): a transação tem no máximo 40 segundos; vira regra de timeout (`AB03`) na v0.2, quando o fluxo ficar assíncrono.
- **Partidas dobradas**: cada movimento grava débito e crédito com o mesmo id de transação; soma de débitos = soma de créditos, sempre.
- **Concorrência**: cada lançamento trava as contas envolvidas (`select ... for update`) sempre na mesma ordem, o que evita espera em círculo; o `version` continua como segunda proteção. Duas ordens simultâneas no mesmo pagador não podem deixar saldo negativo.
- **Reserva**: o PSP pagador tira o valor do cliente e guarda numa conta de trânsito antes de mandar a `pacs.008`; liquidado, vai para a contrapartida do SPI; rejeitado, volta ao cliente.
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
- [ ] Cada uma das 6 rejeições da tabela termina em `REJEITADA`, com o código certo, sem mexer em saldo nenhum
- [ ] Toda `pacs.008`/`pacs.002` gerada valida contra o XSD oficial (`pacs.008.spi.1.16`, `pacs.002.spi.1.18`), e os exemplos oficiais são lidos sem erro
- [ ] Teste de concorrência: 50 Pix simultâneos do mesmo pagador nunca deixam saldo negativo
- [ ] Invariante testada: soma de todos os lançamentos = 0 (cliente e PI)
- [ ] Testes de integração com Testcontainers (PostgreSQL); CI no GitHub Actions verde
- [ ] README: o problema, como rodar em 1 comando, diagrama do fluxo, decisões e o que é simplificação

## Decisões (e por quê)
1. **Um serviço modular, não microsserviços**: o foco é o domínio e as mensagens; separar em serviços agora só traria infraestrutura. As fronteiras HTTP/XML deixam a separação fácil depois.
2. **XML de verdade (ISO 20022) entre PSP e SPI**, não JSON: é o que diferencia o projeto e o que o mercado de pagamentos conhece.
3. **Partidas dobradas desde o início**: é a parte mais difícil de colocar depois e a que mais mostra maturidade em sistema financeiro.
4. **PostgreSQL + Testcontainers** em vez de H2: testar concorrência e bloqueio em banco de verdade.
5. **Spring Boot 4.1**: a linha 3.5 parou de receber atualizações em junho de 2026; projeto novo começa na versão suportada.
6. **Trava pessimista no lugar da otimista**: com 50 Pix disputando as mesmas contas (trânsito, PI), a otimista falharia o tempo todo e exigiria novas tentativas; travar na mesma ordem é mais simples e não corre risco de deadlock.
7. **Mensagem fora do XSD volta como erro HTTP 400**: o SPI de verdade responde com `admi.002`, que fica fora do escopo da v0.1.
8. **Síncrono na v0.1**: o fluxo real é assíncrono com timeouts; a v0.2 introduz isso com mensageria. Fica registrado no README como simplificação.

## Fontes (públicas)
Conferidas em 2026-10-29 (cópias locais em `docs/bcb/`, fora do git):
- Definições detalhadas das mensagens do SPI, versão 5.13.2 (02/10/2026), com XSDs, exemplos e tabela de domínios: https://www.bcb.gov.br/content/estabilidadefinanceira/cedsfn/Catalogos/spi.5.13.2.zip
- Catálogo de Serviços do SFN, Volume VI (Pagamentos Instantâneos), versão 5.13: https://www.bcb.gov.br/content/estabilidadefinanceira/cedsfn/Catalogos/Catalogo_de_Servicos_do_SFN_Volume_VI_Versao_513.pdf
- Manual das Interfaces de Comunicação 1.12: https://www.bcb.gov.br/content/estabilidadefinanceira/cedsfn/Manual%20das%20Interfaces%20de%20Comunica%C3%A7%C3%A3o-1.12.pdf
- Manual Operacional do DICT 8.5: https://www.bcb.gov.br/content/estabilidadefinanceira/pix/Regulamento_Pix/X_ManualOperacionaldoDICT.pdf
- Manual de Tempos do Pix: https://www.bcb.gov.br/content/estabilidadefinanceira/pix/Regulamento_Pix/IX_ManualdeTemposdoPix.pdf
- Página de comunicação de dados do Bacen (versões novas do catálogo): https://www.bcb.gov.br/estabilidadefinanceira/comunicacaodados
- Contratos da API Pix do Bacen: https://github.com/bacen/pix-api
- Desafio "ICOM/SPI Simulator" da Woovi, como referência de escopo: https://woovibr.github.io/jobs/

## Plano das semanas
| Semana | Datas | Entrega |
|---|---|---|
| 0 | 02/11 – 08/11 | ✓ Spec conferida com os manuais e XSDs (adiantado em 29/10); ✓ repo criado com README de intenção |
| 1 | 09/11 – 15/11 | ✓ Esqueleto (Maven, Compose, Flyway, CI); participantes, contas PI e contas de clientes; razão com invariante testada (adiantado em 29/10) |
| 2 | 16/11 – 22/11 | ✓ DICT e ✓ `pacs.008`/`pacs.002` montadas e validadas no XSD (adiantados em 29/10) |
| 3 | 23/11 – 29/11 | ✓ Fluxo completo, rejeições (AC03, AM04, AM01, FF08, RC09, RC10) e teste de 50 Pix simultâneos (adiantado em 29/10) |
| 4 | 30/11 – 06/12 | README, diagrama, Swagger; **tag v0.1** |
