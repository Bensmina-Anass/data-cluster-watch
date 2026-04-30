# DataCluster Watch — Demo & Explanation Guide
### Distributed Systems Project — Java Sockets + RMI

---

## What the project does

DataCluster Watch is a **distributed real-time cluster monitoring system**. It simulates a compute cluster of 5 nodes and monitors them using three distinct Java networking mechanisms:

- `DatagramSocket` / `DatagramPacket` (UDP) for high-frequency metric streaming
- `ServerSocket` / `Socket` (TCP) for reliable critical alert delivery
- **Java RMI** (`LocateRegistry`, `Remote` interfaces) for the client/server query interface

All three are implemented from scratch using Java's standard networking APIs, with manual thread management and concurrent data structures from `java.util.concurrent`.

---

## Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                        dcw-agent (1 JVM)                     │
│                                                              │
│  5 NodeAgent instances running in parallel threads:          │
│  master-01 | worker-01 | worker-02 | worker-03 | storage-01  │
│                                                              │
│  Each agent runs 3 threads:                                  │
│  ├─ MetricsCollectorThread  → simulates CPU/RAM/disk         │
│  ├─ UDPSenderThread         → DatagramSocket → :5000 (UDP)   │
│  └─ TCPAlertSenderThread    → Socket         → :6000 (TCP)   │
└───────────────────────────┬──────────────────────────────────┘
                            │ UDP (metrics + heartbeat)
                            │ TCP (critical alerts)
                    ┌───────▼────────────────┐
                    │     dcw-server (1 JVM) │
                    │                        │
                    │  UDPReceiverModule      │ ← DatagramSocket thread
                    │  TCPReceiverModule      │ ← ServerSocket + thread pool
                    │  HeartbeatMonitor       │ ← node failure detection
                    │  AlertEngine            │ ← Strategy pattern
                    │  MetricsProcessor       │
                    │  HikariCP → MySQL       │
                    │                        │
                    │  RMI Registry :1099 ───┼──► dcw-client (JavaFX)
                    │  5 Remote services      │    via LocateRegistry.getRegistry()
                    └────────────────────────┘
```

---

## How to run

```bash
cd datacluster-watch

# Build images and start mysql + server + agent
docker compose up --build

# In another terminal — verify all containers are healthy
docker compose ps

# Launch the JavaFX GUI (requires X11)
xhost +local:docker
docker compose --profile client up client

# Stop everything
docker compose down

# Stop and wipe the database
docker compose down -v
```

The server waits for MySQL to pass its health check before starting — this is handled by the `depends_on: condition: service_healthy` in docker-compose.yml.

---

## Java concepts to explain

### 1. UDP Sockets — `DatagramSocket` / `DatagramPacket`

**Agent side** (`UDPSenderThread`): one `DatagramSocket` is opened once and reused for all sends. Each metric is serialized to JSON, converted to bytes, and wrapped in a `DatagramPacket` targeted at the server's IP and port 5000.

```java
// Agent side — UDPSenderThread.java
socket = new DatagramSocket();                          // unbound, ephemeral port
InetAddress address = InetAddress.getByName(serverHost);
byte[] data = payload.getBytes(StandardCharsets.UTF_8);
DatagramPacket packet = new DatagramPacket(data, data.length, address, serverPort);
socket.send(packet);
```

**Server side** (`UDPReceiverModule`): a `DatagramSocket` bound to port 5000 blocks on `socket.receive(packet)` in a dedicated thread. When a packet arrives it is parsed (JSON) and dispatched as either a METRIC or HEARTBEAT.

```java
// Server side — UDPReceiverModule.java
socket = new DatagramSocket(port);                      // bound to :5000
byte[] buffer = new byte[65_535];
while (!isInterrupted()) {
    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
    socket.receive(packet);                             // blocks until data arrives
    String payload = new String(packet.getData(), 0, packet.getLength(), UTF_8);
    dispatch(payload);
}
```

**Why UDP for metrics?** Metrics are sent every 3 seconds. Losing one packet is acceptable — the next one arrives 3 seconds later. UDP has no connection setup overhead and no retransmission delay, which matters when 5 agents are sending concurrently.

---

### 2. TCP Sockets — `ServerSocket` / `Socket` with ACK

**Agent side** (`TCPAlertSenderThread`): for each CRITICAL alert, a new `Socket` connection is opened to the server, the alert is sent as a JSON line, and the agent waits for an `"ACK"` response before closing the connection.

```java
// Agent side — TCPAlertSenderThread.java
try (Socket socket = new Socket(serverHost, serverPort);
     PrintWriter  out = new PrintWriter(socket.getOutputStream(), true);
     BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

    out.println(JsonSerializer.toJson(alert));  // send alert as JSON line
    String ack = in.readLine();                 // block until server replies
    if (!"ACK".equals(ack)) { /* log warning */ }
}
```

**Server side** (`TCPReceiverModule`): a `ServerSocket` loops on `accept()` in its own thread. Each accepted connection is handed off to a fixed thread pool (`ExecutorService`) so the accept loop is never blocked by slow clients.

```java
// Server side — TCPReceiverModule.java
serverSocket = new ServerSocket(port);
while (!isInterrupted()) {
    Socket client = serverSocket.accept();          // blocks until agent connects
    pool.submit(() -> handleClient(client));        // offload to thread pool
}

// Inside handleClient:
String line = in.readLine();                        // read the alert JSON
Alert alert = JsonSerializer.fromJson(line, Alert.class);
alertDAO.save(alert);
out.println("ACK");                                 // confirm receipt
```

**Why TCP for critical alerts?** A CRITICAL alert must not be silently lost. TCP guarantees delivery, ordering, and provides the ACK mechanism — the agent knows the server received and persisted the alert. Also, alerts are rare events (not every 3 seconds) so the connection overhead is acceptable.

---

### 3. Producer-Consumer with `BlockingQueue`

Both sender threads use a `LinkedBlockingQueue` to decouple the metric producer (`MetricsCollectorThread`) from the network sender:

```java
// UDPSenderThread — consumer blocks here when queue is empty
String payload = queue.take();   // blocks until an item is available

// MetricsCollectorThread — producer never blocks
queue.offer(jsonPayload);        // drops silently if queue is full (capacity: 512)
```

This is the classic **producer-consumer pattern** implemented with Java's `java.util.concurrent` — no `wait()`/`notify()` needed. The `BlockingQueue` handles all synchronization internally.

---

### 4. Java Threads — explicit `Thread` subclassing

All network modules extend `Thread` directly and override `run()`. Each thread is named (for debugging) and marked as a **daemon thread** so they don't prevent JVM shutdown.

```java
public class UDPSenderThread extends Thread {
    public UDPSenderThread(String host, int port) {
        super("udp-sender");    // named thread
        setDaemon(true);        // won't block JVM shutdown
        ...
    }
}
```

Shutdown is cooperative: the main thread calls `interrupt()`, and each thread's loop checks `isInterrupted()` and handles `InterruptedException` cleanly.

The agent runs **5 × 3 = 15 threads** (3 per node agent), plus the main thread. The server runs additional threads for the thread pool, heartbeat monitor, and a shutdown hook thread.

---

### 5. Java RMI — Remote Method Invocation

RMI allows the JavaFX client to call methods on the server as if they were local Java objects, transparently over the network.

**Server side**: creates a registry on port 1099 and binds 5 named services:

```java
// ServerMain.java
Registry registry = LocateRegistry.createRegistry(rmiPort);  // starts the RMI registry
registry.bind("ClusterService", new ClusterServiceImpl(...));
registry.bind("AlertService",   new AlertServiceImpl(...));
registry.bind("JobService",     new JobServiceImpl(...));
registry.bind("StatsService",   new StatsServiceImpl(...));
registry.bind("ConfigService",  new ConfigServiceImpl(...));
```

Each service implements a `Remote` interface (from `java.rmi`):

```java
// IClusterService.java
public interface IClusterService extends Remote {
    List<Node>    getAllNodes()              throws RemoteException;
    Metric        getLatestMetric(String id) throws RemoteException;
    ClusterSummary getClusterSummary()       throws RemoteException;
}
```

**Client side** (`RmiClientService`): looks up the registry and calls remote methods directly — the RMI stub handles serialization and the network call transparently.

```java
Registry registry = LocateRegistry.getRegistry(host, port);
IClusterService cluster = (IClusterService) registry.lookup("ClusterService");
List<Node> nodes = cluster.getAllNodes();  // this is actually a network call
```

The client refreshes these calls every 3 seconds to keep the GUI up to date.

---

### 6. Concurrent state — `ConcurrentHashMap`

The `AlertEngine` uses a `ConcurrentHashMap` for two things that are read/written from multiple threads:

```java
// AlertEngine.java
private final Map<String, ThresholdConfig> thresholdCache = new ConcurrentHashMap<>();
private final Map<String, Long>            cooldownMap    = new ConcurrentHashMap<>();
```

`thresholdCache` — the RMI `ConfigService` can update thresholds at runtime (from the GUI) while the alert engine is simultaneously evaluating incoming metrics. `ConcurrentHashMap` makes this safe without explicit locking.

`cooldownMap` — tracks `lastFiredTime` per `(nodeId, alertType)` pair. Without this, a node stuck at 95% CPU would generate an alert every 3 seconds (one per metric), flooding the database. The 60-second cooldown prevents that.

---

### 7. Heartbeat and node failure detection

Each agent sends a `HEARTBEAT` JSON message via UDP every 10 seconds:

```json
{"type":"HEARTBEAT","nodeId":"worker-02","timestamp":1714500000000}
```

The `HeartbeatMonitor` runs on the server and checks every 15 seconds whether any node's last heartbeat is older than 30 seconds. If so, the node is marked `OFFLINE` in the database and a `NODE_DOWN` alert is generated.

To demonstrate failure detection live:

```bash
docker stop dcw-agent
docker compose logs -f server
# After ~30 seconds you will see:
# INFO: Node worker-01 marked OFFLINE (no heartbeat)
# INFO: Node worker-02 marked OFFLINE (no heartbeat)
# ...

docker start dcw-agent
# Nodes return to ACTIVE once heartbeats resume
```

---

### 8. AlertEngine — Strategy design pattern

The `AlertEngine` holds a list of `AlertStrategy` objects evaluated on every incoming metric:

```java
this.strategies = List.of(
    new CpuAlertStrategy(),
    new RamAlertStrategy(),
    new DiskAlertStrategy(),
    new FailedJobsAlertStrategy()
);
```

Each strategy encapsulates its own threshold check and returns `Optional<Alert>`. Adding a new alert type (e.g., network latency) requires only a new class — the engine itself doesn't change.

Default thresholds (live-configurable via RMI `ConfigService`):

| Metric | WARNING | CRITICAL |
|--------|---------|----------|
| CPU | 75% | 90% |
| RAM | 70% | 85% |
| Disk | 80% | 95% |

Alerts come from **two sources**:
- **Agent-side** (TCP): the agent detects CRITICAL thresholds locally and sends immediately
- **Server-side** (AlertEngine): the server evaluates WARNING thresholds on every received metric

---

## Verify data is being persisted

```bash
docker exec -it dcw-mysql mysql -u datacluster -pdatacluster123 \
  -e "SELECT COUNT(*) AS total_metrics FROM metrics;
      SELECT COUNT(*) AS total_alerts FROM alerts;
      SELECT node_id, level, type, created_at
        FROM alerts ORDER BY created_at DESC LIMIT 10;" \
  datacluster_watch
```

---

## Chaos mode

The agent supports `--chaos` mode, which randomly injects CPU/RAM spikes (+40–60%) with 3% probability per tick. To enable it, uncomment in `docker-compose.yml`:

```yaml
# AGENT_ARGS: "--chaos"
```

Then restart only the agent:

```bash
docker compose up -d --force-recreate agent
docker compose logs -f server   # watch CRITICAL alerts spike
```

---

## Key points to say to the teacher

| Topic | What to say |
|-------|-------------|
| **UDP vs TCP** | UDP for metrics — stateless, low overhead, acceptable loss. TCP for alerts — reliable, ACK-confirmed, connection per alert. |
| **Socket API** | `DatagramSocket`+`DatagramPacket` for UDP. `ServerSocket.accept()` loop + `Socket` streams (wrapping `InputStream`/`OutputStream` with `BufferedReader`/`PrintWriter`) for TCP. |
| **Concurrency** | `BlockingQueue` for producer-consumer between collector and sender threads. `ConcurrentHashMap` for shared state accessed from multiple threads without locks. `ExecutorService` thread pool for TCP connections. |
| **Java RMI** | `LocateRegistry.createRegistry()` on server, `registry.bind()` for each service. Client uses `LocateRegistry.getRegistry()` + `registry.lookup()`. Remote interfaces extend `java.rmi.Remote`, all methods declare `RemoteException`. |
| **Failure detection** | Passive heartbeat: server detects node silence, not the agent self-reporting death. This works even if the agent crashes or the network partitions. |
| **Strategy pattern** | Each alert type is an independent strategy class. Engine iterates all strategies on every metric. Adding a new alert type = new class, no engine changes. |
| **Thread design** | All threads extend `Thread`, named for debugging, set as daemons. Shutdown is cooperative via `interrupt()` + `isInterrupted()` checks. |
