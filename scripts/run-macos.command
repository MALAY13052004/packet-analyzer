#!/bin/bash
set -e
cd "$(dirname "$0")/.."
if ! command -v java >/dev/null 2>&1; then echo "Java 21+ is required."; read -r; exit 1; fi
if ! command -v mvn >/dev/null 2>&1; then echo "Maven is required to build PacketLab. Install Maven, then double-click this file again."; read -r; exit 1; fi
if command -v sudo >/dev/null 2>&1; then sudo -v || true; fi
mvn spring-boot:run
