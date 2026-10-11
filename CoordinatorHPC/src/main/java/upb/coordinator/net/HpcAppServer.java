package upb.coordinator.net;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import upb.hpc.rmi.IHpcCoordinator;
import upb.hpc.rmi.KMeansRequest;
import upb.hpc.rmi.KMeansResult;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;
import java.util.concurrent.Executors;

/**
 * Servidor de Aplicaciones Java para HPC (RMI Gateway).
 * Dado que RMI es un protocolo nativo y exclusivo de Java, este Servidor de Aplicaciones
 * actúa como puente de comunicación para el cliente web, traduciendo peticiones HTTP
 * en invocaciones remotas RMI hacia el Coordinador HPC y sus nodos esclavos.
 */
public class HpcAppServer {

    private final int port;
    private final String rmiHost;
    private final int rmiPort;
    private final String rmiServiceName;
    private final ObjectMapper mapper;
    private HttpServer httpServer;

    public HpcAppServer(int port, String rmiHost, int rmiPort, String rmiServiceName) {
        this.port = port;
        this.rmiHost = (rmiHost != null && !rmiHost.isBlank()) ? rmiHost : "localhost";
        this.rmiPort = rmiPort;
        this.rmiServiceName = rmiServiceName;
        this.mapper = new ObjectMapper();
    }

    public void start() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        httpServer.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

        // Rutas del Servidor de Aplicaciones
        httpServer.createContext("/api/hpc/status", this::handleStatus);
        httpServer.createContext("/api/hpc/workers", this::handleWorkers);
        httpServer.createContext("/api/hpc/code-template", this::handleCodeTemplate);
        httpServer.createContext("/api/hpc/jobs/execute", this::handleExecuteJob);
        httpServer.createContext("/api/hpc/benchmark", this::handleBenchmark);

        httpServer.start();
        System.out.println("[HpcAppServer] Servidor de Aplicaciones Java HTTP activo en puerto " + port +
                " -> Traduciendo peticiones a Java RMI (" + rmiHost + ":" + rmiPort + "/" + rmiServiceName + ")");
    }

    public void stop() {
        if (httpServer != null) {
            httpServer.stop(1);
        }
    }

    private IHpcCoordinator getCoordinator() throws Exception {
        Registry registry = LocateRegistry.getRegistry(rmiHost, rmiPort);
        return (IHpcCoordinator) registry.lookup(rmiServiceName);
    }

    // 1. Estado y workers activos
    private void handleStatus(HttpExchange exchange) throws IOException {
        if (handleCors(exchange)) return;
        try {
            IHpcCoordinator coordinator = getCoordinator();
            int count = coordinator.getActiveWorkerCount();
            List<String> workers = coordinator.getActiveWorkerNames();

            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("status", "ONLINE");
            resp.put("serviceName", "Servicios de Computación de Alto Rendimiento");
            resp.put("activeWorkerCount", count);
            resp.put("workers", workers);
            resp.put("computationalEngine", "Motor de Aceleración Nativa");
            resp.put("architecture", "Clúster Distribuido Transparente");

            sendJsonResponse(exchange, 200, resp);
        } catch (Exception e) {
            sendError(exchange, 500, "Error conectando con la infraestructura de cómputo: " + e.getMessage());
        }
    }

    private void handleWorkers(HttpExchange exchange) throws IOException {
        if (handleCors(exchange)) return;
        try {
            IHpcCoordinator coordinator = getCoordinator();
            List<String> workers = coordinator.getActiveWorkerNames();
            int count = coordinator.getActiveWorkerCount();

            List<Map<String, Object>> workerList = new ArrayList<>();
            for (String wName : workers) {
                Map<String, Object> wMap = new HashMap<>();
                wMap.put("name", wName);
                wMap.put("status", "ONLINE");
                wMap.put("role", "Instancia de Cómputo Paralelo");
                wMap.put("engine", "Aceleración Nativa en Memoria");
                workerList.add(wMap);
            }

            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("count", count);
            resp.put("workers", workerList);
            sendJsonResponse(exchange, 200, resp);
        } catch (Exception e) {
            sendError(exchange, 500, "Error consultando instancias de cómputo: " + e.getMessage());
        }
    }

    // 2. Plantillas de código
    private void handleCodeTemplate(HttpExchange exchange) throws IOException {
        if (handleCors(exchange)) return;
        String query = exchange.getRequestURI().getQuery();
        String type = "kmeans";
        if (query != null && query.contains("type=")) {
            for (String param : query.split("&")) {
                if (param.startsWith("type=")) {
                    type = param.substring(5).trim().toLowerCase();
                }
            }
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("type", type);
        resp.put("language", "c");
        resp.put("compiler", "gcc -O3 -lm");
        resp.put("code", getCodeTemplateForType(type));
        sendJsonResponse(exchange, 200, resp);
    }

    // 3. Ejecutar Trabajo recibido desde el cliente
    private void handleExecuteJob(HttpExchange exchange) throws IOException {
        if (handleCors(exchange)) return;
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            sendError(exchange, 405, "Método no permitido. Use POST.");
            return;
        }

        try {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            JsonNode root = mapper.readTree(body);

            String jobId = root.has("jobId") ? root.get("jobId").asText() :
                    "JOB-" + UUID.randomUUID().toString().substring(0, 8);
            String jobName = root.has("jobName") ? root.get("jobName").asText() : "K-Means C Job";
            String code = root.has("code") ? root.get("code").asText() : "";
            int k = root.has("k") ? root.get("k").asInt() : 4;
            int maxIterations = root.has("maxIterations") ? root.get("maxIterations").asInt() : 15;
            int targetNodes = root.has("targetNodes") ? root.get("targetNodes").asInt() : 0;
            int numPoints = root.has("numPoints") ? root.get("numPoints").asInt() : 40000;
            double tolerance = root.has("tolerance") ? root.get("tolerance").asDouble() : 1e-4;

            // Generar o tomar dataset de puntos
            List<double[]> points;
            if (root.has("points") && root.get("points").isArray() && !root.get("points").isEmpty()) {
                points = new ArrayList<>();
                for (JsonNode pNode : root.get("points")) {
                    if (pNode.isArray() && pNode.size() >= 2) {
                        points.add(new double[]{pNode.get(0).asDouble(), pNode.get(1).asDouble()});
                    }
                }
            } else {
                points = generateSyntheticDataset(numPoints);
            }

            System.out.println("[HpcAppServer] Peticion HTTP recibida: '" + jobName + "' (ID: " + jobId + ")");
            System.out.println("[HpcAppServer] Enviando a Coordinador via RMI -> Nodos Solicitados: " + targetNodes + " | Puntos: " + points.size() + " | K: " + k);

            IHpcCoordinator coordinator = getCoordinator();
            KMeansRequest rmiRequest = new KMeansRequest(jobId, points, k, maxIterations, targetNodes, tolerance);

            long appStart = System.currentTimeMillis();
            // Invocacion REMOTA RMI
            KMeansResult rmiResult = coordinator.executeKMeans(rmiRequest);
            long appEnd = System.currentTimeMillis();
            long totalRtt = appEnd - appStart;

            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("jobId", rmiResult.getJobId());
            resp.put("jobName", jobName);
            resp.put("status", rmiResult.getStatus());
            resp.put("nodesUsed", rmiResult.getNodesUsed());
            resp.put("totalPoints", rmiResult.getTotalPoints());
            resp.put("k", rmiResult.getK());
            resp.put("iterationsRun", rmiResult.getIterationsRun());
            resp.put("totalComputationTimeMs", rmiResult.getTotalComputationTimeMs());
            resp.put("appServerRmiRttMs", totalRtt);
            resp.put("workerExecutionTimesMs", rmiResult.getWorkerExecutionTimesMs());
            resp.put("finalCentroids", rmiResult.getFinalCentroids());
            resp.put("clusterSizes", rmiResult.getClusterSizes());
            resp.put("submittedCodeLength", code.length());

            sendJsonResponse(exchange, 200, resp);

        } catch (Exception e) {
            System.err.println("[HpcAppServer] Error ejecutando trabajo: " + e.getMessage());
            e.printStackTrace();
            sendError(exchange, 500, "Error en ejecucion RMI de HPC: " + e.getMessage());
        }
    }

    // 4. Benchmark Automatizado Multi-Nodo (1, 2, 3 y 4 Nodos)
    private void handleBenchmark(HttpExchange exchange) throws IOException {
        if (handleCors(exchange)) return;
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            sendError(exchange, 405, "Método no permitido. Use POST.");
            return;
        }

        try {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            JsonNode root = body.isBlank() ? null : mapper.readTree(body);

            int numPoints = (root != null && root.has("numPoints")) ? root.get("numPoints").asInt() : 40000;
            int k = (root != null && root.has("k")) ? root.get("k").asInt() : 4;
            int maxIterations = (root != null && root.has("maxIterations")) ? root.get("maxIterations").asInt() : 15;

            IHpcCoordinator coordinator = getCoordinator();
            int activeWorkers = coordinator.getActiveWorkerCount();
            int maxNodesToTest = Math.min(4, Math.max(1, activeWorkers));

            List<double[]> points = generateSyntheticDataset(numPoints);

            List<Map<String, Object>> benchmarkRuns = new ArrayList<>();
            double baseHpcTime = 0.0;

            for (int nodes = 1; nodes <= maxNodesToTest; nodes++) {
                String testJobId = "BENCH-" + nodes + "NODES-" + UUID.randomUUID().toString().substring(0, 5);
                KMeansRequest request = new KMeansRequest(testJobId, points, k, maxIterations, nodes);

                long start = System.currentTimeMillis();
                KMeansResult result = coordinator.executeKMeans(request);
                long rtt = System.currentTimeMillis() - start;

                long hpcTime = result.getTotalComputationTimeMs();
                if (nodes == 1) {
                    baseHpcTime = (double) hpcTime;
                }

                double speedup = baseHpcTime > 0 ? (baseHpcTime / Math.max(1.0, (double) hpcTime)) : 1.0;
                double efficiency = (speedup / nodes) * 100.0;

                Map<String, Object> runMap = new LinkedHashMap<>();
                runMap.put("nodes", nodes);
                runMap.put("hpcTimeMs", hpcTime);
                runMap.put("rttTimeMs", rtt);
                runMap.put("iterations", result.getIterationsRun());
                runMap.put("speedup", Math.round(speedup * 100.0) / 100.0);
                runMap.put("efficiency", Math.round(efficiency * 10.0) / 10.0);
                runMap.put("workerTimes", result.getWorkerExecutionTimesMs());
                runMap.put("clusterSizes", result.getClusterSizes());
                runMap.put("finalCentroids", result.getFinalCentroids());

                benchmarkRuns.add(runMap);
            }

            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("status", "SUCCESS");
            resp.put("totalPoints", numPoints);
            resp.put("k", k);
            resp.put("maxIterations", maxIterations);
            resp.put("activeWorkersDetected", activeWorkers);
            resp.put("runs", benchmarkRuns);

            sendJsonResponse(exchange, 200, resp);

        } catch (Exception e) {
            System.err.println("[HpcAppServer] Error en benchmark: " + e.getMessage());
            e.printStackTrace();
            sendError(exchange, 500, "Error ejecutando benchmark RMI: " + e.getMessage());
        }
    }

    private static List<double[]> generateSyntheticDataset(int count) {
        List<double[]> list = new ArrayList<>(count);
        Random rand = new Random(42);
        double[][] centers = {
                {25.0, 25.0},
                {75.0, 25.0},
                {25.0, 75.0},
                {75.0, 75.0}
        };

        for (int i = 0; i < count; i++) {
            double[] center = centers[i % centers.length];
            double x = center[0] + rand.nextGaussian() * 6.0;
            double y = center[1] + rand.nextGaussian() * 6.0;
            list.add(new double[]{x, y});
        }
        return list;
    }

    private static String getCodeTemplateForType(String type) {
        if ("matrix".equalsIgnoreCase(type)) {
            return """
#include <stdio.h>
#include <stdlib.h>

/* Multiplicación Paralela de Matrices por Descomposición de Bloques: C = A x B */
int main(int argc, char *argv[]) {
    int N = 512; /* Dimensión de matriz N x N */
    printf("Iniciando multiplicación distribuida de matrices (Dimensión: %d x %d)...\\n", N, N);

    double *A = (double *)malloc(N * N * sizeof(double));
    double *B = (double *)malloc(N * N * sizeof(double));
    double *C = (double *)calloc(N * N, sizeof(double));

    for (int i = 0; i < N; i++) {
        for (int j = 0; j < N; j++) {
            A[i * N + j] = (i + j) * 0.01;
            B[i * N + j] = (i - j) * 0.01;
        }
    }

    /* Bucle de cálculo con optimización de memoria por filas */
    for (int i = 0; i < N; i++) {
        for (int k = 0; k < N; k++) {
            double a_ik = A[i * N + k];
            for (int j = 0; j < N; j++) {
                C[i * N + j] += a_ik * B[k * N + j];
            }
        }
    }

    printf("OPERACIONES_COMPLETADAS: %ld FLOPS\\n", 2L * N * N * N);
    printf("ESTADO: MATRIZ_COMPLETADA CON EXITO\\n");
    free(A); free(B); free(C);
    return 0;
}
""";
        } else if ("fft".equalsIgnoreCase(type)) {
            return """
#include <stdio.h>
#include <stdlib.h>
#include <math.h>

#define PI 3.14159265358979323846

/* Transformada Rápida de Fourier (FFT Radix-2) para Procesamiento de Señales */
typedef struct { double real; double imag; } Complex;

void fft_radix2(Complex *x, int N) {
    if (N <= 1) return;
    Complex *even = (Complex *)malloc(N / 2 * sizeof(Complex));
    Complex *odd  = (Complex *)malloc(N / 2 * sizeof(Complex));

    for (int i = 0; i < N / 2; i++) {
        even[i] = x[i * 2];
        odd[i]  = x[i * 2 + 1];
    }
    fft_radix2(even, N / 2);
    fft_radix2(odd, N / 2);

    for (int k = 0; k < N / 2; k++) {
        double theta = -2.0 * PI * k / N;
        Complex w = { cos(theta), sin(theta) };
        Complex t = { w.real * odd[k].real - w.imag * odd[k].imag,
                      w.real * odd[k].imag + w.imag * odd[k].real };
        x[k] = (Complex){ even[k].real + t.real, even[k].imag + t.imag };
        x[k + N / 2] = (Complex){ even[k].real - t.real, even[k].imag - t.imag };
    }
    free(even); free(odd);
}

int main(int argc, char *argv[]) {
    int N = 1024;
    printf("Iniciando procesamiento espectral FFT (%d muestras)...\\n", N);
    Complex *signal = (Complex *)malloc(N * sizeof(Complex));
    for (int i = 0; i < N; i++) {
        signal[i].real = sin(2 * PI * 50 * i / N) + 0.5 * cos(2 * PI * 120 * i / N);
        signal[i].imag = 0.0;
    }
    fft_radix2(signal, N);
    printf("FFT_COMPLETADA: %d muestras transformadas en dominio frecuencial.\\n", N);
    free(signal);
    return 0;
}
""";
        } else if ("custom".equalsIgnoreCase(type)) {
            return """
#include <stdio.h>
#include <stdlib.h>

/* Algoritmo Científico Genérico para Cómputo Distribuido */
int main(int argc, char *argv[]) {
    printf("Iniciando kernel computacional genérico...\\n");
    long items = 1000000;
    double accumulator = 0.0;

    for (long i = 1; i <= items; i++) {
        accumulator += 1.0 / (double)(i * i);
    }

    printf("PROCESAMIENTO_EXITOSO: %ld elementos evaluados.\\n", items);
    printf("RESULTADO_ACUMULADO: %.10f\\n", accumulator);
    return 0;
}
""";
        }
        return loadDefaultCCode();
    }

    private static String loadDefaultCCode() {
        String[] potentialPaths = {
                "/usr/src/app/kmeans_worker.c",
                "kmeans_worker.c",
                "../kmeans_worker.c",
                "../../kmeans_worker.c"
        };
        for (String p : potentialPaths) {
            try {
                Path path = Path.of(p);
                if (Files.exists(path)) {
                    return Files.readString(path, StandardCharsets.UTF_8);
                }
            } catch (Exception ignored) {}
        }

        return """
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>

/* Algoritmo de Agrupamiento K-Means (Aceleración Nativa) */
int main(int argc, char *argv[]) {
    int N, D, K;
    if (scanf("%d %d %d", &N, &D, &K) != 3) return 1;

    double *centroids = (double *)malloc(K * D * sizeof(double));
    for (int i = 0; i < K * D; i++) scanf("%lf", &centroids[i]);

    int *counts = (int *)calloc(K, sizeof(int));
    double *sums = (double *)calloc(K * D, sizeof(double));

    for (int i = 0; i < N; i++) {
        double pt[2];
        for (int d = 0; d < D; d++) scanf("%lf", &pt[d]);

        int best_k = 0;
        double min_dist = 1e30;
        for (int k = 0; k < K; k++) {
            double dist = 0.0;
            for (int d = 0; d < D; d++) {
                double diff = pt[d] - centroids[k * D + d];
                dist += diff * diff;
            }
            if (dist < min_dist) { min_dist = dist; best_k = k; }
        }
        counts[best_k]++;
        for (int d = 0; d < D; d++) sums[best_k * D + d] += pt[d];
    }

    printf("POINTS_PROCESSED: %d\\n", N);
    for (int k = 0; k < K; k++) {
        printf("CLUSTER %d COUNT: %d SUM:", k, counts[k]);
        for (int d = 0; d < D; d++) printf(" %.6f", sums[k * D + d]);
        printf("\\n");
    }
    return 0;
}
""";
    }

    private static boolean handleCors(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");

        if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return true;
        }
        return false;
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, Object data) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        byte[] bytes = mapper.writeValueAsString(data).getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        Map<String, Object> err = new HashMap<>();
        err.put("error", message);
        err.put("status", "ERROR");
        byte[] bytes = mapper.writeValueAsString(err).getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
