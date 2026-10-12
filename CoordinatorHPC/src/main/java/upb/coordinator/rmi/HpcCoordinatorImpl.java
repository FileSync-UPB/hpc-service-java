package upb.coordinator.rmi;

import upb.hpc.rmi.*;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.*;

public class HpcCoordinatorImpl extends UnicastRemoteObject implements IHpcCoordinator {

    private final Map<String, IHpcWorker> workers = new ConcurrentHashMap<>();

    public HpcCoordinatorImpl() throws RemoteException {
        super();
    }

    public HpcCoordinatorImpl(int port) throws RemoteException {
        super(port);
    }

    @Override
    public void registerWorker(String workerId, IHpcWorker worker) throws RemoteException {
        workers.put(workerId, worker);
        System.out.println("[Coordinator RMI] Nodo trabajador REGISTRADO: " + workerId + " (Total activos: " + workers.size() + ")");
    }

    @Override
    public void unregisterWorker(String workerId) throws RemoteException {
        workers.remove(workerId);
        System.out.println("[Coordinator RMI] Nodo trabajador DADO DE BAJA: " + workerId + " (Total activos: " + workers.size() + ")");
    }

    @Override
    public int getActiveWorkerCount() throws RemoteException {
        cleanupDeadWorkers();
        return workers.size();
    }

    @Override
    public List<String> getActiveWorkerNames() throws RemoteException {
        cleanupDeadWorkers();
        return new ArrayList<>(workers.keySet());
    }

    public Map<String, Long> getWorkerPingLatencies() {
        Map<String, Long> latencies = new LinkedHashMap<>();
        for (Map.Entry<String, IHpcWorker> entry : workers.entrySet()) {
            try {
                long t0 = System.currentTimeMillis();
                boolean pong = entry.getValue().ping();
                long t1 = System.currentTimeMillis();
                latencies.put(entry.getKey(), pong ? Math.max(1L, t1 - t0) : -1L);
            } catch (Exception e) {
                latencies.put(entry.getKey(), -1L);
            }
        }
        return latencies;
    }

    private void cleanupDeadWorkers() {
        Iterator<Map.Entry<String, IHpcWorker>> it = workers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, IHpcWorker> entry = it.next();
            try {
                entry.getValue().ping();
            } catch (Exception e) {
                System.out.println("[Coordinator RMI] Desconectando worker inactivo: " + entry.getKey());
                it.remove();
            }
        }
    }

    @Override
    public KMeansResult executeKMeans(KMeansRequest request) throws RemoteException {
        cleanupDeadWorkers();

        long startTime = System.currentTimeMillis();
        String jobId = request.getJobId();
        List<double[]> points = request.getPoints();
        int k = request.getK();
        int maxIterations = request.getMaxIterations();
        int targetNodes = request.getTargetNodes();
        double tolerance = request.getTolerance() > 0 ? request.getTolerance() : 1e-4;

        if (points == null || points.isEmpty()) {
            throw new IllegalArgumentException("El dataset de puntos no puede estar vacio.");
        }
        if (k <= 0) {
            throw new IllegalArgumentException("K debe ser mayor a 0.");
        }
        if (workers.isEmpty()) {
            throw new IllegalStateException("No hay nodos trabajadores (Workers) registrados en el cluster HPC.");
        }

        // Seleccionar los trabajadores a utilizar segun targetNodes
        List<Map.Entry<String, IHpcWorker>> availableWorkers = new ArrayList<>(workers.entrySet());
        int nodesToUse = availableWorkers.size();
        if (targetNodes > 0) {
            nodesToUse = Math.min(targetNodes, availableWorkers.size());
        }
        if (nodesToUse <= 0) {
            nodesToUse = 1;
        }

        List<Map.Entry<String, IHpcWorker>> selectedWorkers = availableWorkers.subList(0, nodesToUse);

        System.out.println("\n[Coordinator RMI] ==================================================");
        System.out.println("[Coordinator RMI] EJECUTANDO TRABAJO DISTRIBUIDO: " + jobId);
        System.out.println("[Coordinator RMI] Puntos: " + points.size() + " | K: " + k + " | Max Iter: " + maxIterations);
        System.out.println("[Coordinator RMI] Nodos asignados: " + nodesToUse + " -> " +
                selectedWorkers.stream().map(Map.Entry::getKey).toList());
        System.out.println("[Coordinator RMI] ==================================================");

        int dim = points.get(0).length;

        // Inicializar centroides (seleccion equidistante)
        List<double[]> currentCentroids = new ArrayList<>();
        int step = points.size() / k;
        for (int i = 0; i < k; i++) {
            int idx = Math.min(i * step, points.size() - 1);
            currentCentroids.add(Arrays.copyOf(points.get(idx), dim));
        }

        // Particionar dataset entre los nodos seleccionados
        List<List<double[]>> partitions = partitionPoints(points, nodesToUse);

        int iterationsRun = 0;
        int[] finalCounts = new int[k];
        Map<String, Long> workerTotalTimes = new ConcurrentHashMap<>();
        for (Map.Entry<String, IHpcWorker> w : selectedWorkers) {
            workerTotalTimes.put(w.getKey(), 0L);
        }

        ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

        try {
            for (int iter = 1; iter <= maxIterations; iter++) {
                iterationsRun = iter;

                // Despacho paralelo a los nodos trabajadores
                List<CompletableFuture<KMeansPartialResult>> futures = new ArrayList<>();
                for (int i = 0; i < nodesToUse; i++) {
                    final int workerIdx = i;
                    final Map.Entry<String, IHpcWorker> workerEntry = selectedWorkers.get(i);
                    final List<double[]> partition = partitions.get(i);
                    final List<double[]> centroidsCopy = new ArrayList<>(currentCentroids);

                    CompletableFuture<KMeansPartialResult> future = CompletableFuture.supplyAsync(() -> {
                        try {
                            return workerEntry.getValue().computeKMeansPartition(
                                    jobId, workerIdx + 1, partition, centroidsCopy, k
                            );
                        } catch (Exception e) {
                            throw new CompletionException("Fallo en worker " + workerEntry.getKey() + ": " + e.getMessage(), e);
                        }
                    }, virtualExecutor);

                    futures.add(future);
                }

                // Esperar resultados de todos los nodos de esta iteración
                List<KMeansPartialResult> partialResults = futures.stream()
                        .map(CompletableFuture::join)
                        .toList();

                // Acumular sumas y conteos globales
                int[] globalCounts = new int[k];
                double[][] globalSums = new double[k][dim];

                for (KMeansPartialResult partial : partialResults) {
                    workerTotalTimes.compute(partial.getWorkerId(), (id, t) -> (t == null ? 0L : t) + partial.getExecutionTimeMs());

                    int[] pCounts = partial.getClusterCounts();
                    double[][] pSums = partial.getClusterSums();

                    for (int c = 0; c < k; c++) {
                        globalCounts[c] += pCounts[c];
                        for (int d = 0; d < dim; d++) {
                            globalSums[c][d] += pSums[c][d];
                        }
                    }
                }

                finalCounts = globalCounts;

                // Recalcular nuevos centroides y medir desplazamiento
                List<double[]> nextCentroids = new ArrayList<>();
                double maxCentroidShift = 0.0;

                for (int c = 0; c < k; c++) {
                    double[] newCentroid = new double[dim];
                    if (globalCounts[c] > 0) {
                        for (int d = 0; d < dim; d++) {
                            newCentroid[d] = globalSums[c][d] / globalCounts[c];
                        }
                    } else {
                        newCentroid = Arrays.copyOf(currentCentroids.get(c), dim);
                    }

                    double shiftSq = 0.0;
                    for (int d = 0; d < dim; d++) {
                        double diff = newCentroid[d] - currentCentroids.get(c)[d];
                        shiftSq += diff * diff;
                    }
                    if (shiftSq > maxCentroidShift) {
                        maxCentroidShift = shiftSq;
                    }
                    nextCentroids.add(newCentroid);
                }

                currentCentroids = nextCentroids;

                double shiftDist = Math.sqrt(maxCentroidShift);
                if (shiftDist < tolerance) {
                    System.out.println("[Coordinator RMI] Convergencia alcanzada en la iteracion " + iter + " (Shift: " + String.format("%.6f", shiftDist) + " < " + tolerance + ")");
                    break;
                }
            }
        } finally {
            virtualExecutor.shutdown();
        }

        long totalTime = System.currentTimeMillis() - startTime;

        System.out.println("[Coordinator RMI] FIN TRABAJO " + jobId + " | Tiempo total HPC: " + totalTime + " ms | Iteraciones: " + iterationsRun);
        System.out.println("--------------------------------------------------");

        return new KMeansResult(
                jobId,
                nodesToUse,
                points.size(),
                k,
                iterationsRun,
                currentCentroids,
                finalCounts,
                totalTime,
                workerTotalTimes,
                "SUCCESS"
        );
    }

    private List<List<double[]>> partitionPoints(List<double[]> points, int numPartitions) {
        List<List<double[]>> partitions = new ArrayList<>(numPartitions);
        for (int i = 0; i < numPartitions; i++) {
            partitions.add(new ArrayList<>());
        }

        int total = points.size();
        int baseSize = total / numPartitions;
        int remainder = total % numPartitions;

        int start = 0;
        for (int i = 0; i < numPartitions; i++) {
            int currentSize = baseSize + (i < remainder ? 1 : 0);
            int end = start + currentSize;
            partitions.get(i).addAll(points.subList(start, end));
            start = end;
        }

        return partitions;
    }
}
