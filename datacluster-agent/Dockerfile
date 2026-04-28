# ═══════════════════════════════════════════════════════════════
# DataCluster Watch — Agent
# Build multi-stage : Maven → JRE minimal
# ═══════════════════════════════════════════════════════════════

# ─── Stage 1 : compilation ───────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build

COPY pom.xml                          .
COPY datacluster-common/pom.xml       datacluster-common/
COPY datacluster-agent/pom.xml        datacluster-agent/
COPY datacluster-server/pom.xml       datacluster-server/
COPY datacluster-client/pom.xml       datacluster-client/

RUN mvn dependency:go-offline \
        -pl datacluster-agent -am \
        -q --no-transfer-progress

COPY datacluster-common/src   datacluster-common/src
COPY datacluster-agent/src    datacluster-agent/src

RUN mvn package \
        -pl datacluster-agent -am \
        -DskipTests -q --no-transfer-progress

# ─── Stage 2 : image runtime ─────────────────────────────────
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

COPY --from=builder \
     /build/datacluster-agent/target/datacluster-agent-standalone.jar \
     app.jar

ENV JAVA_OPTS=""
# Passer --chaos pour activer le mode chaos
ENV AGENT_ARGS=""

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar $AGENT_ARGS"]
