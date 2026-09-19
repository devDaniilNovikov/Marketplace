#!/usr/bin/env bash
#
# Развёртывание VPS под self-hosted раннеры агентского цикла.
#
# Ставит: Docker, JDK 25, Node, gh, jq, CLI claude и codex, N экземпляров
# actions-runner как systemd-сервисы под отдельным непривилегированным пользователем.
#
# Запускать от root на чистой Ubuntu 22.04/24.04 или Debian 12.
# Скрипт идемпотентен: повторный запуск доводит состояние, а не ломает.
#
#   REPO=devDaniilNovikov/Marketplace RUNNER_COUNT=2 ./vps-bootstrap.sh
#
# ВАЖНО: ставить раннер ТОЛЬКО на приватный репозиторий. На публичном PR из
# форка выполнит произвольный код на этой машине — с твоими подписками и токенами.

set -euo pipefail

REPO="${REPO:?укажи REPO=owner/repo}"
RUNNER_USER="${RUNNER_USER:-ghrunner}"
RUNNER_COUNT="${RUNNER_COUNT:-2}"
RUNNER_LABELS="${RUNNER_LABELS:-self-hosted,linux,marketplace}"
RUNNER_ROOT="${RUNNER_ROOT:-/opt/actions-runner}"
SWAP_GB="${SWAP_GB:-4}"
JDK_VERSION="${JDK_VERSION:-25}"

log()  { printf '\n\033[1;34m==>\033[0m %s\n' "$*"; }
warn() { printf '\033[1;33m[!]\033[0m %s\n' "$*" >&2; }
die()  { printf '\033[1;31m[x]\033[0m %s\n' "$*" >&2; exit 1; }

[ "$(id -u)" -eq 0 ] || die "запускай от root"
. /etc/os-release
case "${ID:-}" in ubuntu|debian) ;; *) die "поддерживаются только Ubuntu и Debian, найдено: ${ID:-неизвестно}" ;; esac

case "$(uname -m)" in
  x86_64)  RUNNER_ARCH=x64 ;;
  aarch64) RUNNER_ARCH=arm64 ;;
  *) die "неподдерживаемая архитектура: $(uname -m)" ;;
esac

# --------------------------------------------------------------------------
log "Проверка ресурсов"
# --------------------------------------------------------------------------
MEM_GB=$(( $(awk '/MemTotal/{print $2}' /proc/meminfo) / 1024 / 1024 ))
DISK_GB=$(df -BG --output=avail / | tail -1 | tr -dc '0-9')
echo "RAM: ${MEM_GB} GB, свободно на /: ${DISK_GB} GB, раннеров планируется: ${RUNNER_COUNT}"

NEED_MEM=$(( 4 + RUNNER_COUNT * 4 ))
[ "$MEM_GB" -ge "$NEED_MEM" ] || warn "мало памяти: ${MEM_GB} GB при ${RUNNER_COUNT} раннерах. Gradle с -Xmx3g плюс контейнеры Postgres и Redis не поместятся, ожидай OOM. Нужно от ${NEED_MEM} GB."
[ "$DISK_GB" -ge 40 ] || warn "мало диска: ${DISK_GB} GB. Docker-образы, кеш Gradle и рабочие каталоги раннеров съедят это за пару недель."

# --------------------------------------------------------------------------
log "Базовые пакеты"
# --------------------------------------------------------------------------
export DEBIAN_FRONTEND=noninteractive
apt-get update -qq
# gh, jq и python3 нужны workflow'ам цикла. На ubuntu-latest они предустановлены,
# на голой Ubuntu — нет, и джобы падают на первой же команде gh.
apt-get install -y -qq --no-install-recommends \
  ca-certificates curl wget gnupg git jq python3 unzip tar sudo >/dev/null

# --------------------------------------------------------------------------
log "Swap (${SWAP_GB} GB)"
# --------------------------------------------------------------------------
# Дешёвая страховка от OOM-killer, который иначе убьёт Gradle посреди прогона.
if [ "$SWAP_GB" -gt 0 ] && ! swapon --show | grep -q '/swapfile'; then
  if fallocate -l "${SWAP_GB}G" /swapfile 2>/dev/null \
     || dd if=/dev/zero of=/swapfile bs=1M count=$((SWAP_GB * 1024)) status=none 2>/dev/null; then
    chmod 600 /swapfile
    mkswap -q /swapfile >/dev/null && swapon /swapfile
    grep -q '^/swapfile' /etc/fstab || echo '/swapfile none swap sw 0 0' >> /etc/fstab
    echo "swap создан"
  else
    warn "не удалось создать swap — не фатально, но при нехватке памяти OOM-killer убьёт Gradle посреди прогона"
    rm -f /swapfile
  fi
else
  echo "swap уже есть или отключён параметром"
fi

# --------------------------------------------------------------------------
log "Пользователь ${RUNNER_USER}"
# --------------------------------------------------------------------------
# Без sudo намеренно: на этой машине лежат токен репозитория и обе подписки.
if ! id -u "$RUNNER_USER" >/dev/null 2>&1; then
  useradd --create-home --shell /bin/bash "$RUNNER_USER"
  echo "создан"
else
  echo "уже есть"
fi
RUNNER_HOME=$(getent passwd "$RUNNER_USER" | cut -d: -f6)

# --------------------------------------------------------------------------
log "Docker"
# --------------------------------------------------------------------------
if ! command -v docker >/dev/null 2>&1; then
  install -m 0755 -d /etc/apt/keyrings
  curl -fsSL "https://download.docker.com/linux/${ID}/gpg" -o /etc/apt/keyrings/docker.asc
  chmod a+r /etc/apt/keyrings/docker.asc
  echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/${ID} ${VERSION_CODENAME} stable" \
    > /etc/apt/sources.list.d/docker.list
  apt-get update -qq
  apt-get install -y -qq docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin >/dev/null
  echo "установлен"
else
  echo "уже есть: $(docker --version)"
fi
systemctl enable --now docker >/dev/null 2>&1 || true

# Testcontainers требуют доступ к демону Docker. Членство в группе docker
# фактически равносильно root на хосте — это осознанный размен на машине,
# которая больше ничего не делает. Путь ужесточения — rootless Docker, см. ops/README.md.
usermod -aG docker "$RUNNER_USER"
warn "пользователь ${RUNNER_USER} добавлен в группу docker: это фактически root на этом хосте"

# --------------------------------------------------------------------------
log "JDK ${JDK_VERSION} (Temurin)"
# --------------------------------------------------------------------------
# Не обязателен: actions/setup-java скачает тулчейн в кеш раннера сам.
# Но первый прогон без него дольше, а при недоступности Adoptium остаётся запасной путь.
if ! ls /usr/lib/jvm 2>/dev/null | grep -q "temurin-${JDK_VERSION}"; then
  # --yes: иначе gpg откажется перезаписать ключ и уронит повторный запуск скрипта.
  if curl -fsSL https://packages.adoptium.net/artifactory/api/gpg/key/public \
       | gpg --dearmor --yes -o /etc/apt/keyrings/adoptium.gpg 2>/dev/null; then
    echo "deb [signed-by=/etc/apt/keyrings/adoptium.gpg] https://packages.adoptium.net/artifactory/deb ${VERSION_CODENAME} main" \
      > /etc/apt/sources.list.d/adoptium.list
    apt-get update -qq || true
  else
    warn "репозиторий Adoptium недоступен"
  fi
  if apt-get install -y -qq "temurin-${JDK_VERSION}-jdk" >/dev/null 2>&1; then
    echo "установлен"
  else
    warn "temurin-${JDK_VERSION}-jdk недоступен в репозитории Adoptium для ${VERSION_CODENAME}. Не критично: actions/setup-java скачает тулчейн сам. Но проверь, что в ci.yml distribution/java-version резолвятся."
  fi
else
  echo "уже есть"
fi

# --------------------------------------------------------------------------
log "Node LTS"
# --------------------------------------------------------------------------
if ! command -v node >/dev/null 2>&1; then
  curl -fsSL https://deb.nodesource.com/setup_22.x | bash - >/dev/null
  apt-get install -y -qq nodejs >/dev/null
  echo "установлен: $(node --version)"
else
  echo "уже есть: $(node --version)"
fi

# --------------------------------------------------------------------------
log "GitHub CLI"
# --------------------------------------------------------------------------
if ! command -v gh >/dev/null 2>&1; then
  install -m 0755 -d /etc/apt/keyrings
  curl -fsSL https://cli.github.com/packages/githubcli-archive-keyring.gpg \
    -o /etc/apt/keyrings/githubcli.gpg
  chmod a+r /etc/apt/keyrings/githubcli.gpg
  echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/githubcli.gpg] https://cli.github.com/packages stable main" \
    > /etc/apt/sources.list.d/github-cli.list
  apt-get update -qq
  apt-get install -y -qq gh >/dev/null
  echo "установлен: $(gh --version | head -1)"
else
  echo "уже есть: $(gh --version | head -1)"
fi

# --------------------------------------------------------------------------
log "CLI агентов"
# --------------------------------------------------------------------------
npm install -g --silent @anthropic-ai/claude-code @openai/codex >/dev/null 2>&1 \
  || warn "не удалось поставить один из CLI глобально — поставь вручную и перезапусти"
command -v claude >/dev/null && echo "claude: $(claude --version 2>/dev/null || echo установлен)"
command -v codex  >/dev/null && echo "codex:  $(codex --version 2>/dev/null || echo установлен)"

# --------------------------------------------------------------------------
log "Раннеры (${RUNNER_COUNT} шт.)"
# --------------------------------------------------------------------------
if [ -z "${RUNNER_TOKEN:-}" ]; then
  cat >&2 <<EOF

Нужен токен регистрации раннера. Он живёт один час, поэтому не хранится в скрипте.
Получи его на машине, где ты залогинен в gh с правами админа репозитория:

  gh api -X POST repos/${REPO}/actions/runners/registration-token --jq .token

и запусти снова:

  REPO=${REPO} RUNNER_COUNT=${RUNNER_COUNT} RUNNER_TOKEN=<токен> $0

EOF
  die "RUNNER_TOKEN не задан"
fi

RUNNER_VERSION=$(curl -fsSL https://api.github.com/repos/actions/runner/releases/latest | jq -r .tag_name | sed 's/^v//')
[ -n "$RUNNER_VERSION" ] || die "не удалось определить версию actions/runner"
TARBALL="actions-runner-linux-${RUNNER_ARCH}-${RUNNER_VERSION}.tar.gz"
CACHE="/tmp/${TARBALL}"
[ -f "$CACHE" ] || curl -fsSL -o "$CACHE" \
  "https://github.com/actions/runner/releases/download/v${RUNNER_VERSION}/${TARBALL}"
echo "actions-runner ${RUNNER_VERSION} (${RUNNER_ARCH})"

HOSTNAME_SHORT=$(hostname -s)
for i in $(seq 1 "$RUNNER_COUNT"); do
  DIR="${RUNNER_ROOT}/runner-${i}"
  NAME="${HOSTNAME_SHORT}-${i}"

  if [ -f "${DIR}/.runner" ]; then
    echo "  runner-${i}: уже настроен, пропускаю"
    continue
  fi

  mkdir -p "$DIR"
  tar xzf "$CACHE" -C "$DIR"
  chown -R "${RUNNER_USER}:${RUNNER_USER}" "$DIR"

  # config.sh отказывается работать от root — отсюда sudo -u.
  sudo -u "$RUNNER_USER" -H bash -c "cd '$DIR' && ./config.sh \
      --unattended --replace \
      --url 'https://github.com/${REPO}' \
      --token '${RUNNER_TOKEN}' \
      --name '${NAME}' \
      --labels '${RUNNER_LABELS}' \
      --work '_work'" >/dev/null

  # svc.sh install ставит systemd-юнит, работающий от указанного пользователя.
  # Это важно: $HOME сервиса — домашний каталог ghrunner, значит codex найдёт
  # там свой ~/.codex/auth.json.
  ( cd "$DIR" && ./svc.sh install "$RUNNER_USER" >/dev/null && ./svc.sh start >/dev/null )
  echo "  runner-${i}: настроен и запущен как ${NAME}"
done

# --------------------------------------------------------------------------
log "Готово"
# --------------------------------------------------------------------------
cat <<EOF

Осталось два шага руками — авторизация подписок. Оба делаются один раз.

1) Codex. На сервере от имени ${RUNNER_USER}:

     sudo -u ${RUNNER_USER} -H codex login --device-auth

   Команда напечатает короткий код, подтверждаешь его с телефона или ноутбука.
   Если упрётся в запрет — включи device-code вход в настройках безопасности
   аккаунта ChatGPT. Запасной путь: залогинься на ноутбуке и скопируй
   ~/.codex/auth.json в ${RUNNER_HOME}/.codex/auth.json (файл не привязан к хосту),
   затем: chown -R ${RUNNER_USER}:${RUNNER_USER} ${RUNNER_HOME}/.codex

2) Claude. Ничего на сервере делать не нужно: токен приходит из секрета
   репозитория CLAUDE_CODE_OAUTH_TOKEN, он у тебя уже настроен. Если протух —
   на ноутбуке выполни 'claude setup-token' и обнови секрет. Токен живёт около года.

Затем переключи цикл на эту машину одной переменной репозитория:

     AGENT_RUNS_ON = self-hosted

Проверка состояния: ops/vps-verify.sh
EOF
