#!/usr/bin/env bash
#
# Проверка готовности VPS к агентскому циклу. Ничего не меняет.
# Запускать от root: RUNNER_USER=ghrunner ./vps-verify.sh

set -uo pipefail

RUNNER_USER="${RUNNER_USER:-ghrunner}"
FAIL=0

ok()   { printf '\033[1;32m  ok \033[0m %s\n' "$*"; }
bad()  { printf '\033[1;31m FAIL\033[0m %s\n' "$*"; FAIL=$((FAIL+1)); }
warn() { printf '\033[1;33m warn\033[0m %s\n' "$*"; }
head_() { printf '\n\033[1;34m== %s\033[0m\n' "$*"; }

head_ "Инструменты, которые дёргают workflow цикла"
for t in git jq gh python3 docker node; do
  if command -v "$t" >/dev/null 2>&1; then ok "$t — $(command -v $t)"; else bad "$t не найден"; fi
done
# Эти два workflow'ы вызывают постоянно; на ubuntu-latest они есть из коробки,
# на своей машине их отсутствие валит джоб на первой же строке.
command -v gh >/dev/null || bad "без gh не отработает ни один шаг цикла"
command -v jq >/dev/null || bad "без jq не разберётся ни один вердикт"

head_ "CLI агентов"
for t in claude codex; do
  if command -v "$t" >/dev/null 2>&1; then ok "$t — $(command -v $t)"; else bad "$t не найден"; fi
done

head_ "Пользователь и права"
if id -u "$RUNNER_USER" >/dev/null 2>&1; then
  ok "пользователь $RUNNER_USER существует"
  id -nG "$RUNNER_USER" | tr ' ' '\n' | grep -qx docker \
    && ok "$RUNNER_USER состоит в группе docker" \
    || bad "$RUNNER_USER не в группе docker — Testcontainers не поднимутся"
  if sudo -l -U "$RUNNER_USER" 2>/dev/null | grep -q '(ALL'; then
    warn "$RUNNER_USER имеет sudo — на машине с токенами это лишнее"
  else
    ok "$RUNNER_USER без sudo"
  fi
else
  bad "нет пользователя $RUNNER_USER"
fi

head_ "Docker"
if docker info >/dev/null 2>&1; then
  ok "демон отвечает"
  sudo -u "$RUNNER_USER" -H docker info >/dev/null 2>&1 \
    && ok "$RUNNER_USER достучался до демона" \
    || bad "$RUNNER_USER не достучался до демона (перелогиньте сессию или перезапустите раннеры)"
else
  bad "демон Docker не отвечает"
fi

head_ "Авторизация Codex"
RUNNER_HOME=$(getent passwd "$RUNNER_USER" 2>/dev/null | cut -d: -f6)
if [ -n "${RUNNER_HOME:-}" ] && [ -s "${RUNNER_HOME}/.codex/auth.json" ]; then
  ok "найден ${RUNNER_HOME}/.codex/auth.json"
  OWNER=$(stat -c '%U' "${RUNNER_HOME}/.codex/auth.json")
  [ "$OWNER" = "$RUNNER_USER" ] && ok "владелец файла — $RUNNER_USER" || bad "владелец auth.json — $OWNER, а не $RUNNER_USER"
  PERM=$(stat -c '%a' "${RUNNER_HOME}/.codex/auth.json")
  [ "$PERM" = "600" ] && ok "права 600" || warn "права $PERM — в файле лежат токены подписки, поставь 600"
else
  bad "нет auth.json: выполни 'sudo -u $RUNNER_USER -H codex login --device-auth'"
fi

head_ "Авторизация Claude"
# На раннере ничего не нужно: токен приезжает из секрета репозитория в окружение джоба.
echo "  Секрет CLAUDE_CODE_OAUTH_TOKEN проверяется на стороне GitHub, не здесь."
echo "  Протух — выполни 'claude setup-token' на ноутбуке и обнови секрет."

head_ "Раннеры"
UNITS=$(systemctl list-units --type=service --all --no-legend 'actions.runner.*' 2>/dev/null | awk '{print $1}')
if [ -z "$UNITS" ]; then
  bad "не найдено ни одного сервиса actions.runner.*"
else
  while read -r U; do
    [ -n "$U" ] || continue
    STATE=$(systemctl is-active "$U" 2>/dev/null || true)
    [ "$STATE" = "active" ] && ok "$U — active" || bad "$U — $STATE"
  done <<< "$UNITS"
  echo "  Раннеров: $(echo "$UNITS" | grep -c .) (один раннер = один джоб за раз)"
fi

head_ "Ресурсы"
MEM_GB=$(( $(awk '/MemTotal/{print $2}' /proc/meminfo) / 1024 / 1024 ))
DISK_GB=$(df -BG --output=avail / | tail -1 | tr -dc '0-9')
N=$(echo "${UNITS:-}" | grep -c . || true)
NEED=$(( 4 + (N > 0 ? N : 1) * 4 ))
[ "$MEM_GB" -ge "$NEED" ] && ok "RAM ${MEM_GB} GB (нужно от ${NEED})" || bad "RAM ${MEM_GB} GB, нужно от ${NEED} при ${N} раннерах"
[ "$DISK_GB" -ge 40 ] && ok "свободно ${DISK_GB} GB" || warn "свободно ${DISK_GB} GB — кеши вырастут"
swapon --show 2>/dev/null | grep -q . && ok "swap включён" || warn "swap выключен: OOM-killer убьёт Gradle посреди прогона"

printf '\n'
if [ "$FAIL" -eq 0 ]; then
  printf '\033[1;32mВсё готово.\033[0m Переключай цикл переменной AGENT_RUNS_ON = self-hosted\n'
else
  printf '\033[1;31mПроблем: %s.\033[0m Не переключай AGENT_RUNS_ON, пока они есть.\n' "$FAIL"
  exit 1
fi
