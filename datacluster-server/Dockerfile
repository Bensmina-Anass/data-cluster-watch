# ═══════════════════════════════════════════════════════════════
# DataCluster Watch — Server
# Build multi-stage : Maven → JRE minimal
# ═══════════════════════════════════════════════════════════════

# ─── Stage 1 : compilation ───────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build

# Copie des POM en premier pour profiter du cache des dépendances Maven
COPY pom.xml                          .
COPY datacluster-common/pom.xml       datacluster-common/
COPY datacluster-agent/pom.xml        datacluster-agent/
COPY datacluster-server/pom.xml       datacluster-server/
COPY datacluster-client/pom.xml       datacluster-client/

RUN mvn dependency:go-offline \
        -pl datacluster-server -am \
        -q --no-transfer-progress

# Copie des sources puis build
COPY datacluster-common/src   datacluster-common/src
COPY datacluster-server/src   datacluster-server/src

RUN mvn package \
        -pl datacluster-server -am \
        -DskipTests -q --no-transfer-progress

# ─── Stage 2 : image runtime ─────────────────────────────────
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

COPY --from=builder \
     /build/datacluster-server/target/datacluster-server-standalone.jar \
     app.jar

# UDP métriques | TCP alertes | RMI registry
EXPOSE 5000/udp 6000/tcp 1099/tcp

ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
