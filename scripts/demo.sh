#!/usr/bin/env bash
# Um Pix de ponta a ponta contra a aplicação rodando (docker compose up -d --build).
# Cria dois bancos, um cliente em cada, a chave da recebedora e manda o Pix.
# Usa ISPBs e chave novos a cada execução, então pode rodar de novo à vontade.
set -euo pipefail

URL="${SPI_LAB_URL:-http://localhost:8080}"
CORPO="$(mktemp)"
trap 'rm -f "$CORPO"' EXIT

post() {
    printf '%s' "$2" > "$CORPO"
    curl -sf -X POST "$URL$1" -H 'Content-Type: application/json; charset=utf-8' --data-binary @"$CORPO"
}

campo() {
    sed -E "s/.*\"$1\":\"?([^\",}]*)\"?.*/\1/"
}

reais() {
    local c="$1"
    printf 'R$ %d,%02d' $((c / 100)) $((c % 100))
}

saldo_cliente() {
    curl -sf "$URL/psps/$1/clientes/0001/$2" | campo saldoCentavos
}

saldo_pi() {
    curl -sf "$URL/spi/participantes/$1/saldo" | campo saldoCentavos
}

printf 'Esperando a aplicação em %s' "$URL"
for _ in $(seq 1 60); do
    if curl -s -o /dev/null "$URL/v3/api-docs"; then break; fi
    printf '.'; sleep 2
done
echo

A=$(printf '1%07d' $((RANDOM * RANDOM % 10000000)))
B=$(printf '2%07d' $((RANDOM * RANDOM % 10000000)))
CHAVE="maria.$RANDOM$RANDOM@exemplo.com.br"

post /spi/participantes "{\"ispb\":\"$A\",\"nome\":\"Banco A\",\"aporteInicialCentavos\":100000000}" > /dev/null
post /spi/participantes "{\"ispb\":\"$B\",\"nome\":\"Banco B\",\"aporteInicialCentavos\":100000000}" > /dev/null
post "/psps/$A/clientes" '{"agencia":"0001","numero":"1001","tipo":"CACC","titularNome":"João Souza","titularDocumento":"11122233344","depositoInicialCentavos":50000}' > /dev/null
post "/psps/$B/clientes" '{"agencia":"0001","numero":"2002","tipo":"CACC","titularNome":"Maria Lima","titularDocumento":"55566677788","depositoInicialCentavos":0}' > /dev/null
post /dict/chaves "{\"tipo\":\"EMAIL\",\"chave\":\"$CHAVE\",\"ispb\":\"$B\",\"agencia\":\"0001\",\"numero\":\"2002\"}" > /dev/null

echo "Banco A ($A): João com $(reais "$(saldo_cliente "$A" 1001)")"
echo "Banco B ($B): Maria com $(reais "$(saldo_cliente "$B" 2002)"), chave $CHAVE"
echo
echo "João manda R\$ 150,00 para $CHAVE..."
PIX=$(post "/psps/$A/pix" "{\"agencia\":\"0001\",\"numero\":\"1001\",\"chave\":\"$CHAVE\",\"valorCentavos\":15000,\"descricao\":\"Almoço de sexta\"}")
E2E=$(echo "$PIX" | campo endToEndId)
echo "Status: $(echo "$PIX" | campo status) (EndToEndId $E2E)"
echo
echo "João:  $(reais "$(saldo_cliente "$A" 1001)")    Maria: $(reais "$(saldo_cliente "$B" 2002)")"
echo "Conta PI do Banco A: $(reais "$(saldo_pi "$A")")    Conta PI do Banco B: $(reais "$(saldo_pi "$B")")"
echo
echo "Mensagens ISO 20022 trocadas:"
curl -sf "$URL/psps/$A/pix/$E2E" | grep -oE '"tipo":"[^"]+","de":"[^"]+","para":"[^"]+","status":("[^"]+"|null)' \
    | sed -E 's/"tipo":"([^"]+)","de":"([^"]+)","para":"([^"]+)","status":"?([^"]+)"?/  \1  \2 -> \3  \4/' \
    | sed -e "s/$A/Banco A/g" -e "s/$B/Banco B/g" -e 's/00038166/SPI/g' -e 's/  null$/  (ordem de pagamento)/'
echo
echo "XML completo: curl $URL/psps/$A/pix/$E2E"
echo "Swagger:      $URL/swagger-ui"
