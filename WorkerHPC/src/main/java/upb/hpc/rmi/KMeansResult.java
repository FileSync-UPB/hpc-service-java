package upb.hpc.rmi;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class KMeansResult implements Serializable {
    private static final long serialVersionUID = 1003L;

    private String jobId;
    private int nodesUsed;
    private int totalPoints;
    private int k;
    private int iterationsRun;
    private List<double[]> finalCentroids;
    private int[] clusterSizes;
    private long totalComputationTimeMs;
    private Map<String, Long> workerExecutionTimesMs;
    private String status;

    public KMeansResult() {}

    public KMeansResult(String jobId, int nodesUsed, int totalPoints, int k, int iterationsRun,
                        List<double[]> finalCentroids, int[] clusterSizes, long totalComputationTimeMs,
                        Map<String, Long> workerExecutionTimesMs, String status) {
        this.jobId = jobId;
        this.nodesUsed = nodesUsed;
        this.totalPoints = totalPoints;
        this.k = k;
        this.iterationsRun = iterationsRun;
        this.finalCentroids = finalCentroids;
        this.clusterSizes = clusterSizes;
        this.totalComputationTimeMs = totalComputationTimeMs;
        this.workerExecutionTimesMs = workerExecutionTimesMs;
        this.status = status;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public int getNodesUsed() {
        return nodesUsed;
    }

    public void setNodesUsed(int nodesUsed) {
        this.nodesUsed = nodesUsed;
    }

    public int getTotalPoints() {
        return totalPoints;
    }

    public void setTotalPoints(int totalPoints) {
        this.totalPoints = totalPoints;
    }

    public int getK() {
        return k;
    }

    public void setK(int k) {
        this.k = k;
    }

    public int getIterationsRun() {
        return iterationsRun;
    }

    public void setIterationsRun(int iterationsRun) {
        this.iterationsRun = iterationsRun;
    }

    public List<double[]> getFinalCentroids() {
        return finalCentroids;
    }

    public void setFinalCentroids(List<double[]> finalCentroids) {
        this.finalCentroids = finalCentroids;
    }

    public int[] getClusterSizes() {
        return clusterSizes;
    }

    public void setClusterSizes(int[] clusterSizes) {
        this.clusterSizes = clusterSizes;
    }

    public long getTotalComputationTimeMs() {
        return totalComputationTimeMs;
    }

    public void setTotalComputationTimeMs(long totalComputationTimeMs) {
        this.totalComputationTimeMs = totalComputationTimeMs;
    }

    public Map<String, Long> getWorkerExecutionTimesMs() {
        return workerExecutionTimesMs;
    }

    public void setWorkerExecutionTimesMs(Map<String, Long> workerExecutionTimesMs) {
        this.workerExecutionTimesMs = workerExecutionTimesMs;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
