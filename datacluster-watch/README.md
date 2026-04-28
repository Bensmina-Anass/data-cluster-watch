# DataCluster Watch

Plateforme de monitoring temps réel pour cluster Big Data.
Architecture distribuée Java avec agents UDP/TCP, serveur central RMI/JDBC et interface JavaFX.

---

## Prérequis

| Outil           | Version minimale |
|-----------------|-----------------|
| Java JDK        | 17              |
| Apache Maven    | 3.9+            |
| MySQL           | 8.0+            |
| JavaFX SDK      | 21 (via Maven)  |

---

## Structure du projet

```
datacluster-watch/
├── pom.xml                        ← Parent POM
├── datacluster-common/            ← Modèles, interfaces RMI, utilitaires
├── datacluster-agent/             ← Agents de collecte / simulation
├── datacluster-server/            ← Serveur central (UDP/TCP/RMI/JDBC)
├── datacluster-client/            ← Interface JavaFX
└── sql/
    └── init.sql                   ← Script d'initialisation MySQL
```

---

## Installation

### 1. Base de données

```bash
mysql -u root -p < sql/init.sql
```

Cela crée la base `datacluster_watch`, l'utilisateur `datacluster` (mot de passe `datacluster123`)
et initialise les 5 nœuds, les seuils par défaut et l'utilisateur admin.

### 2. Configuration

Chaque module possède son fichier `src/main/resources/application.properties`.

**datacluster-server** — adapter les credentials DB si nécessaire :
```properties
db.url=jdbc:mysql://localhost:3306/datacluster_watch?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
db.username=datacluster
db.password=datacluster123
```

**datacluster-agent** — pointer vers l'hôte serveur si distant :
```properties
server.host=localhost
server.udp.port=5000
server.tcp.port=6000
```

**datacluster-client** — adresse du registre RMI :
```properties
server.host=localhost
server.rmi.port=1099
```

### 3. Build

```bash
cd datacluster-watch
mvn clean package -DskipTests
```

---

## Lancement

### Ordre de démarrage recommandé

1. **Serveur** (doit être lancé en premier)
2. **Agents** (se connectent au serveur)
3. **Client** (interface de monitoring)

### Serveur

```bash
java -jar datacluster-server/target/datacluster-server-standalone.jar
```

### Agents (mode normal)

```bash
java -jar datacluster-agent/target/datacluster-agent-standalone.jar
```

### Agents (mode chaos — injection de pannes)

```bash
java -jar datacluster-agent/target/datacluster-agent-standalone.jar --chaos
```

### Client JavaFX (via Maven)

```bash
cd datacluster-client
mvn javafx:run
```

Ou avec le JAR fat :
```bash
java -jar datacluster-client/target/datacluster-client-standalone.jar
```

> **Note** : le JAR fat JavaFX nécessite que les modules JavaFX soient sur le module-path.
> Utiliser de préférence `mvn javafx:run` pour le développement.

---

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                     AGENTS (×5)                         │
│  MetricsCollectorThread ──UDP──► Serveur :5000          │
│  TCPAlertSenderThread   ──TCP──► Serveur :6000          │
│  JobSimulator (ETL, ML, Logs, Clean, Stats)             │
└─────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│                     SERVEUR                             │
│  UDPReceiverModule      ← port 5000                     │
│  TCPReceiverModule      ← port 6000 (pool 10 threads)   │
│  MetricsProcessor       (cache ConcurrentHashMap)       │
│  AlertEngine            (Strategy: CPU/RAM/Disk/Jobs)   │
│  HeartbeatMonitor       (ScheduledExecutorService)      │
│  StatisticsEngine       (moving avg, z-score)           │
│  PersistenceModule      (HikariCP → MySQL)              │
│  RMI Registry           ← port 1099                     │
└─────────────────────────────────────────────────────────┘
                          │ RMI
                          ▼
┌─────────────────────────────────────────────────────────┐
│                     CLIENT JavaFX                       │
│  Dashboard / NodeList / NodeDetail / Alerts / Jobs      │
│  Stats / Config / Export (CSV + PDF)                    │
│  ScheduledService (refresh 3s)                          │
└─────────────────────────────────────────────────────────┘
```

---

## Services RMI (port 1099)

| Nom de binding     | Interface         | Description                        |
|--------------------|-------------------|------------------------------------|
| `ClusterService`   | IClusterService   | Nœuds, métriques, résumé cluster   |
| `AlertService`     | IAlertService     | Alertes, acquittement              |
| `JobService`       | IJobService       | Jobs Big Data                      |
| `StatsService`     | IStatsService     | Moyennes mobiles, z-scores         |
| `ConfigService`    | IConfigService    | Seuils d'alerte configurables      |

---

## Tests

```bash
mvn test
```

Tests inclus :
- `MetricTest` — sérialisation JSON et constructeur
- `StatisticsEngineTest` — moyennes mobiles, z-scores, heartbeat

---

## Seuils d'alerte par défaut

| Métrique | WARNING | CRITICAL |
|----------|---------|----------|
| CPU      | 75 %    | 90 %     |
| RAM      | 70 %    | 85 %     |
| Disque   | 80 %    | 95 %     |
| Jobs échoués | 1  | 3        |

Les seuils sont modifiables en temps réel depuis la vue **Configuration** du client.

---

## Simulation des agents

Chaque agent simule :
- **Métriques réalistes** : variation sinusoïdale (charge journalière), bruit gaussien, dérive progressive, impact des jobs actifs
- **Heartbeat** toutes les 10 s (distinct des métriques)
- **Jobs Big Data** : ETL_VENTES (10–30 min), TRAINING_ML (30–60 min), LOG_ANALYSIS (5–15 min), DATA_CLEANING (3–10 min), STAT_AGGREGATION (2–8 min)
- **Taux d'échec** : 5 % par job terminé
- **Mode chaos** (`--chaos`) : spikes CPU aléatoires (+40 %), latence simulée

---

## Identifiants par défaut

| Ressource      | Valeur           |
|----------------|------------------|
| DB utilisateur | `datacluster`    |
| DB mot de passe| `datacluster123` |
| Admin UI login | `admin`          |
| Admin UI mdp   | `admin` (SHA-256)|
