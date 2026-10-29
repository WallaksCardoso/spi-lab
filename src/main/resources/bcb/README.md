# Arquivos do Banco Central

Copiados sem alteração das "Definições detalhadas das mensagens do Catálogo de Mensagens do SPI", versão 5.13.2 (02/10/2026), publicadas em https://www.bcb.gov.br/estabilidadefinanceira/comunicacaodados.

- `xsd/pacs.008.spi.1.16.xsd`: ordem de pagamento
- `xsd/pacs.002.spi.1.18.xsd`: relatório de status

Única diferença na validação: o XSD exige a assinatura digital no cabeçalho (`AppHdr/Sgntr`), que está fora do escopo do projeto. Ao carregar o XSD, o validador torna esse campo opcional; o arquivo em disco continua o oficial.
