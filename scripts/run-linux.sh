#!/usr/bin/env bash
set -e
cd "$(dirname "$0")/.."
command -v java >/dev/null 2>&1 || { echo "Java 21+ is required."; exit 1; }
command -v mvn >/dev/null 2>&1 || { echo "Maven is required to build PacketLab."; exit 1; }
if command -v sudo >/dev/null 2>&1; then sudo -v || true; fi
mvn spring-boot:run
