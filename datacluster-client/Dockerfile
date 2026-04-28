# ═══════════════════════════════════════════════════════════════
# DataCluster Watch — Client JavaFX
#
# Usage (X11 forwarding) :
#   xhost +local:docker
#   docker compose --profile client up client
#
# Prérequis hôte Linux : variable DISPLAY définie (ex. :0)
# ═══════════════════════════════════════════════════════════════

FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build

COPY pom.xml                          .
COPY datacluster-common/pom.xml       datacluster-common/
COPY datacluster-agent/pom.xml        datacluster-agent/
COPY datacluster-server/pom.xml       datacluster-server/
COPY datacluster-client/pom.xml       datacluster-client/

RUN mvn dependency:go-offline \
        -pl datacluster-client -am \
        -q --no-transfer-progress

COPY datacluster-common/src    datacluster-common/src
COPY datacluster-client/src    datacluster-client/src

RUN mvn package \
        -pl datacluster-client -am \
        -DskipTests -q --no-transfer-progress

# ─── Runtime : JDK complet + libs GTK/X11 pour JavaFX ────────
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app

RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        openjfx \
        libgtk-3-0 \
        libx11-6 \
        libxtst6 \
        libgl1 \
        fonts-dejavu-core && \
    rm -rf /var/lib/apt/lists/*

COPY --from=builder \
     /build/datacluster-client/target/datacluster-client-standalone.jar \
     app.jar

ENV DISPLAY=:0
ENV JAVA_OPTS="--add-modules javafx.controls,javafx.fxml,javafx.graphics"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
