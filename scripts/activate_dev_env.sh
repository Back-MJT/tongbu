#!/usr/bin/env bash
set -euo pipefail

if [ -n "${BASH_VERSION:-}" ]; then
  SCRIPT_PATH="${BASH_SOURCE[0]}"
elif [ -n "${ZSH_VERSION:-}" ]; then
  SCRIPT_PATH="${(%):-%x}"
else
  SCRIPT_PATH="$0"
fi

ROOT_DIR="$(cd "$(dirname "$SCRIPT_PATH")/.." && pwd)"

if [ ! -d "$ROOT_DIR/.venv" ]; then
  python3 -m venv "$ROOT_DIR/.venv"
fi

# shellcheck disable=SC1091
source "$ROOT_DIR/.venv/bin/activate"

export PROJECT_ROOT="$ROOT_DIR"
export MAVEN_OPTS="${MAVEN_OPTS:-}"
export PIP_CACHE_DIR="$ROOT_DIR/.pip-cache"
export PYTHONPYCACHEPREFIX="$ROOT_DIR/.pycache"
export NPM_CONFIG_CACHE="$ROOT_DIR/.npm-cache"
export npm_config_cache="$ROOT_DIR/.npm-cache"

ANDROID_STUDIO_JDK="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
if [ -z "${JAVA_HOME:-}" ] && [ -d "$ANDROID_STUDIO_JDK" ]; then
  export JAVA_HOME="$ANDROID_STUDIO_JDK"
  export PATH="$JAVA_HOME/bin:$PATH"
fi

NODE20_HOME="/opt/homebrew/opt/node@20"
if [ -d "$NODE20_HOME/bin" ]; then
  export PATH="$NODE20_HOME/bin:$PATH"
  export LDFLAGS="-L$NODE20_HOME/lib ${LDFLAGS:-}"
  export CPPFLAGS="-I$NODE20_HOME/include ${CPPFLAGS:-}"
fi

mkdir -p "$ROOT_DIR/.pip-cache" "$ROOT_DIR/.pycache" "$ROOT_DIR/.npm-cache" "$ROOT_DIR/.m2/repository"

echo "Activated Python venv: $VIRTUAL_ENV"
echo "Node target: $(cat "$ROOT_DIR/.nvmrc")"
echo "pip cache: $PIP_CACHE_DIR"
echo "python pycache: $PYTHONPYCACHEPREFIX"
echo "npm cache: $NPM_CONFIG_CACHE"
echo "Maven repo: $ROOT_DIR/.m2/repository"
if [ -n "${JAVA_HOME:-}" ]; then
  echo "JAVA_HOME: $JAVA_HOME"
fi
if command -v node >/dev/null 2>&1; then
  echo "node: $(node -v)"
fi
if command -v npm >/dev/null 2>&1; then
  echo "npm: $(npm -v)"
fi
if command -v mvn >/dev/null 2>&1; then
  echo "maven: $(mvn -version | head -1)"
fi
