package upb.worker.executor;

import upb.hpc.rmi.KMeansPartialResult;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public class CEngineManager {

    private final String workerId;
    private File cExecutable;

    public CEngineManager(String workerId) {
        this.workerId = workerId;
        ensureCompiledBinary();
    }

    private void ensureCompiledBinary() {
        boolean isWin = isWindows();
        String exeName = isWin ? "kmeans_worker.exe" : "kmeans_worker_linux";

        // Buscar en varias rutas relativas comunes
        String[] candidatePaths = {
                exeName,
                "../" + exeName,
                "../../" + exeName,
                "/usr/src/app/" + exeName,
                "/usr/src/app/kmeans_worker",
                "hpc-service/" + exeName
        };

        for (String p : candidatePaths) {
            File f = new File(p);
            if (f.exists() && f.canExecute()) {
                this.cExecutable = f;
                System.out.println("[" + workerId + "] Motor K-Means C encontrado: " + f.getAbsolutePath());
                return;
            }
        }

        // Si no se encuentra precompilado, intentar compilar kmeans_worker.c
        String[] cSources = {
                "kmeans_worker.c",
                "../kmeans_worker.c",
                "../../kmeans_worker.c",
                "/usr/src/app/kmeans_worker.c",
                "hpc-service/kmeans_worker.c"
        };

        File cSource = null;
        for (String s : cSources) {
            File f = new File(s);
            if (f.exists()) {
                cSource = f;
                break;
            }
        }

        if (cSource != null) {
            try {
                File targetExe = new File(cSource.getParentFile(), exeName);
                System.out.println("[" + workerId + "] Compilando motor C nativo con gcc -O3: " + cSource.getAbsolutePath());
                ProcessBuilder pb = new ProcessBuilder("gcc", "-O3", "-Wall", cSource.getAbsolutePath(), "-o", targetExe.getAbsolutePath());
                pb.inheritIO();
                Process p = pb.start();
                int code = p.waitFor();
                if (code == 0 && targetExe.exists()) {
                    this.cExecutable = targetExe;
                    System.out.println("[" + workerId + "] Compilacion C exitosa: " + targetExe.getAbsolutePath());
                    return;
                }
            } catch (Exception e) {
                System.err.println("[" + workerId + "] Error compilando kmeans_worker.c con gcc: " + e.getMessage());
            }
        }

        System.err.println("[" + workerId + "] ATENCION: No se pudo localizar o compilar kmeans_worker ejecutable.");
    }

    public KMeansPartialResult executeKMeans(String jobId, int iteration, List<double[]> points, List<double[]> centroids, int k) {
        if (points == null || points.isEmpty() || centroids == null || centroids.isEmpty()) {
            return new KMeansPartialResult(workerId, 0, new int[k], new double[k][2], 0, "Empty");
        }

        int N = points.size();
        int D = centroids.get(0).length;

        // Si el ejecutable C esta disponible, ejecutar nativamente
        if (cExecutable != null && cExecutable.exists()) {
            Path tempInput = null;
            try {
                tempInput = Files.createTempFile("kmeans_in_" + workerId + "_", ".txt");

                // Escribir archivo de entrada en formato estructurado de alto rendimiento
                try (BufferedWriter writer = Files.newBufferedWriter(tempInput, StandardCharsets.UTF_8)) {
                    writer.write(N + " " + D + " " + k + "\n");

                    // Escribir centroides
                    for (double[] c : centroids) {
                        for (int d = 0; d < D; d++) {
                            writer.write(c[d] + (d == D - 1 ? "" : " "));
                        }
                        writer.write("\n");
                    }

                    // Escribir puntos
                    for (double[] p : points) {
                        for (int d = 0; d < D; d++) {
                            writer.write(p[d] + (d == D - 1 ? "" : " "));
                        }
                        writer.write("\n");
                    }
                }

                ProcessBuilder pb = new ProcessBuilder(cExecutable.getAbsolutePath(), tempInput.toAbsolutePath().toString());
                pb.redirectErrorStream(true);
                Process process = pb.start();

                int[] counts = new int[k];
                double[][] sums = new double[k][D];
                long cExecTimeMs = 0;

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.startsWith("EXECUTION_TIME_MS:")) {
                            try {
                                double ms = Double.parseDouble(line.split(":")[1].trim());
                                cExecTimeMs = Math.round(ms);
                            } catch (Exception ignored) {}
                        } else if (line.startsWith("CLUSTER ")) {
                            // Formato: CLUSTER 0 COUNT: 500 SUM: 1234.56 7890.12
                            String[] parts = line.split("\\s+");
                            int clusterIdx = Integer.parseInt(parts[1]);
                            int count = Integer.parseInt(parts[3]);
                            counts[clusterIdx] = count;

                            // Leer sumas
                            int sumStartIdx = 5; // despues de SUM:
                            for (int d = 0; d < D && (sumStartIdx + d) < parts.length; d++) {
                                sums[clusterIdx][d] = Double.parseDouble(parts[sumStartIdx + d]);
                            }
                        }
                    }
                }

                process.waitFor();

                return new KMeansPartialResult(
                        workerId,
                        N,
                        counts,
                        sums,
                        cExecTimeMs,
                        "Native C Engine (gcc -O3)"
                );

            } catch (Exception e) {
                System.err.println("[" + workerId + "] Error ejecutando binario C: " + e.getMessage() + ". Usando fallback nativo.");
            } finally {
                if (tempInput != null) {
                    try {
                        Files.deleteIfExists(tempInput);
                    } catch (Exception ignored) {}
                }
            }
        }

        // Fallback robusto en Java si el binario C tuviera problemas de permisos
        long tStart = System.currentTimeMillis();
        int[] counts = new int[k];
        double[][] sums = new double[k][D];

        for (double[] pt : points) {
            int bestK = 0;
            double minDist = Double.MAX_VALUE;
            for (int ci = 0; ci < k; ci++) {
                double[] c = centroids.get(ci);
                double dist = 0.0;
                for (int d = 0; d < D; d++) {
                    double diff = pt[d] - c[d];
                    dist += diff * diff;
                }
                if (dist < minDist) {
                    minDist = dist;
                    bestK = ci;
                }
            }
            counts[bestK]++;
            for (int d = 0; d < D; d++) {
                sums[bestK][d] += pt[d];
            }
        }
        long tEnd = System.currentTimeMillis();

        return new KMeansPartialResult(
                workerId,
                N,
                counts,
                sums,
                tEnd - tStart,
                "Fallback Java Engine"
        );
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}
