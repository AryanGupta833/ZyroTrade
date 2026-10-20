# Order and portfolio benchmark

Run the opt-in MySQL benchmark with:

```powershell
$env:TEMP='C:\Users\<you>\.codex\visualizations\<session>'
$env:DOCKER_HOST='npipe:////./pipe/dockerDesktopLinuxEngine'
$env:DOCKER_API_VERSION='1.44'
$env:TESTCONTAINERS_DOCKER_CLIENT_STRATEGY='org.testcontainers.dockerclient.EnvironmentAndSystemPropertyClientProviderStrategy'
.\mvnw.cmd "-Dmaven.repo.local=$env:TEMP\zyrotrade-m2" "-Dbuild.output.dir=$env:TEMP\zyrotrade-build" "-Dapi.version=1.44" "-Dtest=OrderPerformanceBenchmark" "-Drun.benchmarks=true" test
```

## Recorded result

Local run on 2026-10-20, Java 21.0.10, Docker Desktop 29.3.1, MySQL 8.0.36 Testcontainers, 8 worker threads, 80 warmup orders, 10 second measurement window:

| Operation | Count | Throughput | p50 | p95 | p99 |
|---|---:|---:|---:|---:|---:|
| Order submission | 476 | 47.16/sec | 144.376 ms | 251.517 ms | 309.149 ms |
| Portfolio P&L query | 476 | 47.16/sec | 11.448 ms | 26.128 ms | 43.047 ms |

Failures: 0; measured error rate: 0.000000.

This is a local service-layer benchmark with a disposable MySQL container, not an HTTP/load-balancer benchmark or a production capacity claim. Results depend on the host, container limits, and database settings. The two operation types run sequentially within each worker; throughput is reported per operation type over the same measurement interval.
