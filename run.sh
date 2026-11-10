#!/usr/bin/env bash
set -euo pipefail

project_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
jar_path="$project_dir/ai-tooling/target/ai-tooling-1.0-SNAPSHOT.jar"

if [[ ${1:-} == --help ]]; then
  printf 'Usage: %s [mode=DIVIDEND_WATCH|EDGAR] [stock.ticker=AAPL] [property=value ...]\n' "$0"
  printf 'Build first with mvn package. Reads exported DIVIDENDWATCH_* variables, MODE and STOCK_TICKER.\n'
  exit 0
fi

if [[ ! -f "$jar_path" || ! -d "$project_dir/ai-tooling/target/lib" ]]; then
  printf 'Build the application and its dependencies first: mvn -f "%s/pom.xml" -pl ai-tooling -am package\n' "$project_dir" >&2
  exit 1
fi

properties=(
  "dividendwatch.browser.executable.path=${DIVIDENDWATCH_BROWSER_EXECUTABLE_PATH:-${DIVIDENDWATCH_BROWSER_EXECUTABLE:-/usr/bin/chromium-browser}}"
  "dividendwatch.debug=${DIVIDENDWATCH_DEBUG:-true}"
  "dividendwatch.headless=${DIVIDENDWATCH_HEADLESS:-false}"
  "dividendwatch.password=${DIVIDENDWATCH_PASSWORD:-}"
  "dividendwatch.post.login.settle.seconds=${DIVIDENDWATCH_POST_LOGIN_SETTLE_SECONDS:-10}"
  "dividendwatch.timeout.seconds=${DIVIDENDWATCH_TIMEOUT_SECONDS:-60}"
  "dividendwatch.username=${DIVIDENDWATCH_USERNAME:-bbelovic@gmail.com}"
  "mode=${MODE:-DIVIDEND_WATCH}"
  "stock.ticker=${STOCK_TICKER:-AAPL}"
)

for property in "$@"; do
  if [[ "$property" != *=* || "$property" == =* || "$property" == -* ]]; then
    printf 'Expected property=value without -D; received: %s\n' "${property%%=*}" >&2
    exit 2
  fi
  properties+=("$property")
done

java_command=java
if [[ -n ${JAVA_HOME:-} ]]; then
  java_command="$JAVA_HOME/bin/java"
fi

exec "$java_command" "${properties[@]/#/-D}" -jar "$jar_path"
