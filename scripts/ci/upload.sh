#!/usr/bin/env bash

# ======================================================
# CI - Telegram APK Upload (local Bot API server)
# ======================================================

set -euo pipefail

: "${TELEGRAM_API_ID:?}" "${TELEGRAM_API_HASH:?}" "${TELEGRAM_BOT_TOKEN:?}" "${TELEGRAM_DM_CHAT:?}"

APK_DIR="${APK_DIR:-out}"
API_BASE="http://localhost:8081"
MAX_RETRIES="${TELEGRAM_MAX_RETRIES:-3}"
API_TIMEOUT="${TELEGRAM_API_TIMEOUT:-900}"
RESPONSE_FILE="$(mktemp)"

cleanup() {
    rm -f "$RESPONSE_FILE"
    docker rm -f tg-bot-api >/dev/null 2>&1 || true
}
trap cleanup EXIT

shopt -s nullglob
apks=("$APK_DIR"/*.apk)
[ "${#apks[@]}" -gt 0 ] || { echo "no APK found in $APK_DIR" >&2; exit 1; }

docker run -d --name tg-bot-api -p 8081:8081 \
    -e TELEGRAM_API_ID \
    -e TELEGRAM_API_HASH \
    -e TELEGRAM_LOCAL=true \
    aiogram/telegram-bot-api:latest >/dev/null

ready=0
for _ in $(seq 1 30); do
    if curl -sf "$API_BASE/bot${TELEGRAM_BOT_TOKEN}/getMe" >/dev/null; then
        ready=1
        break
    fi
    sleep 1
done
if [ "$ready" != "1" ]; then
    docker logs tg-bot-api >&2
    exit 1
fi

send_apk() {
    local apk="$1" name abi ver sha caption attempt=1 http_code
    name="$(basename "$apk")"
    abi="arm64-v8a"
    case "$name" in *armeabi-v7a*) abi="armeabi-v7a" ;; esac
    ver="${VER_NAME%-*}"
    sha="${VER_NAME##*-}"
    caption="$(printf '<code>LumineGram</code>\n<code>%s</code>\n<code>%s</code>\n<code>%s</code>' "$ver" "$sha" "$abi")"

    while [ "$attempt" -le "$MAX_RETRIES" ]; do
        http_code=$(curl -s -o "$RESPONSE_FILE" -w "%{http_code}" \
            --max-time "$API_TIMEOUT" --retry 0 \
            -X POST "$API_BASE/bot${TELEGRAM_BOT_TOKEN}/sendDocument" \
            -F "chat_id=${TELEGRAM_DM_CHAT}" \
            --form-string "caption=${caption}" \
            --form-string "parse_mode=HTML" \
            -F "document=@${apk};filename=${name}") || http_code="000"

        if [ "$http_code" = "200" ] && grep -q '"ok":true' "$RESPONSE_FILE"; then
            echo "sent $name"
            return 0
        fi

        case "$http_code" in
            000|429|500|502|503|504)
                echo "send $name failed: HTTP $http_code, attempt $attempt/$MAX_RETRIES" >&2
                ;;
            *)
                echo "send $name failed: HTTP $http_code (non-retryable)" >&2
                cat "$RESPONSE_FILE" >&2
                return 1
                ;;
        esac

        [ "$attempt" -lt "$MAX_RETRIES" ] && sleep $((2 ** attempt))
        attempt=$((attempt + 1))
    done
    return 1
}

for apk in "${apks[@]}"; do
    send_apk "$apk"
done
