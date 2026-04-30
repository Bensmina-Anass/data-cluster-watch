# ═══════════════════════════════════════════════════════════════
# DataCluster Watch — Client JavaFX
#
# Usage (X11 forwarding) :
#   xhost +local:docker
#   docker compose --profile client up --build client
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

# Download the linux-classifier JARs (contain the native .so libraries).
# The non-classifier JARs (Java classes) were already fetched by go-offline.
RUN mvn dependency:get -Dartifact=org.openjfx:javafx-base:21:jar:linux     -q --no-transfer-progress && \
    mvn dependency:get -Dartifact=org.openjfx:javafx-graphics:21:jar:linux -q --no-transfer-progress && \
    mvn dependency:get -Dartifact=org.openjfx:javafx-controls:21:jar:linux -q --no-transfer-progress && \
    mvn dependency:get -Dartifact=org.openjfx:javafx-fxml:21:jar:linux     -q --no-transfer-progress && \
    mkdir -p /javafx-mods && \
    cp /root/.m2/repository/org/openjfx/javafx-base/21/javafx-base-21.jar                 /javafx-mods/ && \
    cp /root/.m2/repository/org/openjfx/javafx-base/21/javafx-base-21-linux.jar           /javafx-mods/ && \
    cp /root/.m2/repository/org/openjfx/javafx-graphics/21/javafx-graphics-21.jar         /javafx-mods/ && \
    cp /root/.m2/repository/org/openjfx/javafx-graphics/21/javafx-graphics-21-linux.jar   /javafx-mods/ && \
    cp /root/.m2/repository/org/openjfx/javafx-controls/21/javafx-controls-21.jar         /javafx-mods/ && \
    cp /root/.m2/repository/org/openjfx/javafx-controls/21/javafx-controls-21-linux.jar   /javafx-mods/ && \
    cp /root/.m2/repository/org/openjfx/javafx-fxml/21/javafx-fxml-21.jar                 /javafx-mods/ && \
    cp /root/.m2/repository/org/openjfx/javafx-fxml/21/javafx-fxml-21-linux.jar           /javafx-mods/

# ─── Runtime ─────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app

# System libs required by JavaFX for X11 rendering
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        libgtk-3-0 \
        libx11-6 \
        libxtst6 \
        libgl1 \
        fonts-dejavu-core && \
    rm -rf /var/lib/apt/lists/*

# JavaFX 21 modules (classes + linux natives) on the module path
COPY --from=builder /javafx-mods /app/javafx-mods

# App fat JAR (openjfx excluded from shade to avoid module conflicts)
COPY --from=builder \
     /build/datacluster-client/target/datacluster-client-standalone.jar \
     app.jar

ENV DISPLAY=:0
ENV JAVA_OPTS="--module-path /app/javafx-mods --add-modules javafx.controls,javafx.fxml,javafx.graphics"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
