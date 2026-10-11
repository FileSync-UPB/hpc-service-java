package upb.client;

import upb.client.config.Config;
import upb.hpc.rmi.IHpcCoordinator;
import upb.hpc.rmi.KMeansRequest;
import upb.hpc.rmi.KMeansResult;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;

public class ClientMain {

    public static void main(String[] args) {
        String host = Config.getCoordinatorHost();
        int rmiPort = 1099;

        if (args.length > 0 && !args[0].isBlank()) {
            host = args[0];
        }
        if (args.length > 1) {
            try {
                rmiPort = Integer.parseInt(args[1]);
            } catch (NumberFormatException ignored) {}
        }

        System.out.println("================================================================================");
        System.out.println("  HPC CLUSTER - BENCHMARK K-MEANS DISTRIBUIDO EN C VIA RMI");
        System.out.println("================================================================================");
        System.out.println("[Client RMI] Conectando a Coordinador -> rmi://" + host + ":" + rmiPort + "/HpcCoordinator");

        try {
            Registry registry = LocateRegistry.getRegistry(host, rmiPort);
            IHpcCoordinator coordinator = (IHpcCoordinator) registry.lookup("HpcCoordinator");

            int activeWorkers = coordinator.getActiveWorkerCount();
            List<String> workerNames = coordinator.getActiveWorkerNames();
            System.out.println("[Client RMI] CONEXION EXITOSA. Nodos trabajadores activos (" + activeWorkers + "): " + workerNames);

            if (activeWorkers < 1) {
                System.err.println("[Client RMI] Error: Se requiere al menos 1 nodo trabajador activo para ejecutar el benchmark.");
                return;
            }

            // 1. Generar Dataset Cientifico: 40,000 puntos 2D en 4 clusters gaussianos
            int totalPoints = 40000;
            int k = 4;
            int maxIterations = 15;
            System.out.println("\n[Dataset] Generando " + totalPoints + " puntos sinteticos en 4 centroides naturales...");
            List<double[]> points = generateSyntheticDataset(totalPoints);
            System.out.println("[Dataset] Generacion completada. Dimension: 2D | K: " + k + " | Max Iteraciones: " + maxIterations);

            // 2. Ejecutar Pruebas en 1, 2, 3 y 4 Nodos
            int maxNodesToTest = Math.min(4, activeWorkers);
            if (activeWorkers < 4) {
                System.out.println("\n[Aviso] Se detectaron " + activeWorkers + " nodos activos. Se probaran de 1 a " + activeWorkers + " nodos.");
            }

            List<BenchmarkRun> runs = new ArrayList<>();

            for (int nodes = 1; nodes <= maxNodesToTest; nodes++) {
                System.out.println("\n--------------------------------------------------------------------------------");
                System.out.println(">>> EJECUTANDO PRUEBA CON " + nodes + " NODO(S) DISTRIBUIDO(S)");
                System.out.println("--------------------------------------------------------------------------------");

                String jobId = "JOB-KMEANS-" + nodes + "NODES-" + UUID.randomUUID().toString().substring(0, 5);
                KMeansRequest request = new KMeansRequest(jobId, points, k, maxIterations, nodes);

                long clientStart = System.currentTimeMillis();
                KMeansResult result = coordinator.executeKMeans(request);
                long clientEnd = System.currentTimeMillis();
                long totalRtt = clientEnd - clientStart;

                System.out.println("  -> Estado: " + result.getStatus());
                System.out.println("  -> Nodos utilizados: " + result.getNodesUsed());
                System.out.println("  -> Iteraciones para converger: " + result.getIterationsRun());
                System.out.println("  -> Tiempo Computo HPC (Servidor): " + result.getTotalComputationTimeMs() + " ms");
                System.out.println("  -> Tiempo Total Cliente (RMI RTT): " + totalRtt + " ms");
                System.out.println("  -> Tiempos por Nodo Trabajador (Motor C):");
                if (result.getWorkerExecutionTimesMs() != null) {
                    result.getWorkerExecutionTimesMs().forEach((wId, tMs) ->
                            System.out.println("       * " + wId + ": " + tMs + " ms")
                    );
                }
                System.out.println("  -> Centroides Finales K=" + k + ":");
                for (int ci = 0; ci < result.getFinalCentroids().size(); ci++) {
                    double[] c = result.getFinalCentroids().get(ci);
                    int count = result.getClusterSizes()[ci];
                    System.out.printf("       Cluster %d: (%.2f, %.2f) -> %d puntos\n", ci, c[0], c[1], count);
                }

                runs.add(new BenchmarkRun(nodes, result.getTotalComputationTimeMs(), totalRtt, result.getIterationsRun()));

                // Pausa breve entre pruebas
                Thread.sleep(1000);
            }

            // 3. Tabla Resumen y Metricas de Speedup y Eficiencia
            printBenchmarkTable(runs);

        } catch (Exception e) {
            System.err.println("\n[Client RMI] Error ejecutando benchmark: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static List<double[]> generateSyntheticDataset(int count) {
        List<double[]> list = new ArrayList<>(count);
        Random rand = new Random(42); // Semilla fija para reproducibilidad

        // 4 Centros naturales
        double[][] centers = {
                {25.0, 25.0},
                {75.0, 25.0},
                {25.0, 75.0},
                {75.0, 75.0}
        };

        for (int i = 0; i < count; i++) {
            double[] center = centers[i % centers.length];
            // Distribución normal alrededor del centro
            double x = center[0] + rand.nextGaussian() * 6.0;
            double y = center[1] + rand.nextGaussian() * 6.0;
            list.add(new double[]{x, y});
        }
        return list;
    }

    private static void printBenchmarkTable(List<BenchmarkRun> runs) {
        if (runs.isEmpty()) return;

        double baseHpcTime = runs.get(0).hpcTimeMs;
        double baseRttTime = runs.get(0).rttTimeMs;

        System.out.println("\n================================================================================");
        System.out.println("               TABLA COMPARATIVA DE RENDIMIENTO Y ESCALABILIDAD HPC             ");
        System.out.println("================================================================================");
        System.out.printf("%-7s | %-15s | %-15s | %-10s | %-10s | %-10s\n",
                "Nodos", "Tiempo HPC(ms)", "Tiempo RTT(ms)", "Iteraciones", "Speedup", "Eficiencia");
        System.out.println("--------------------------------------------------------------------------------");

        for (BenchmarkRun run : runs) {
            double speedup = baseHpcTime > 0 ? (baseHpcTime / Math.max(1, run.hpcTimeMs)) : 1.0;
            double efficiency = (speedup / run.nodes) * 100.0;

            System.out.printf("%-7d | %-15d | %-15d | %-10d | %-9.2fx | %-9.1f%%\n",
                    run.nodes,
                    run.hpcTimeMs,
                    run.rttTimeMs,
                    run.iterations,
                    speedup,
                    efficiency
            );
        }
        System.out.println("================================================================================\n");
    }

    private static class BenchmarkRun {
        int nodes;
        long hpcTimeMs;
        long rttTimeMs;
        int iterations;

        BenchmarkRun(int nodes, long hpcTimeMs, long rttTimeMs, int iterations) {
            this.nodes = nodes;
            this.hpcTimeMs = hpcTimeMs;
            this.rttTimeMs = rttTimeMs;
            this.iterations = iterations;
        }
    }
}
