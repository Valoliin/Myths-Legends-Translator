#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
# Java 21 uniquement pour ce processus et ses sous-processus.
JAVA_HOME="/usr/lib/jvm/java-21-openjdk-amd64"
if [[ ! -x "$JAVA_HOME/bin/java" ]]; then
  echo "Java 21 introuvable : installe openjdk-21-jdk." >&2
  exit 1
fi
export JAVA_HOME
export PATH="$JAVA_HOME/bin:$PATH"
if [[ -x ./gradlew ]]; then
  GRADLE=(./gradlew)
elif [[ -x /opt/gradle-8.14.3/bin/gradle ]]; then
  GRADLE=(/opt/gradle-8.14.3/bin/gradle)
else
  echo "Gradle 8.14.3 ou ./gradlew introuvable. Le gradle système 4.4.1 ne fonctionne pas avec Fabric Loom." >&2
  exit 1
fi
exec "${GRADLE[@]}" "${@:-build}"
