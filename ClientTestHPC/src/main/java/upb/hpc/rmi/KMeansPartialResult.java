package upb.hpc.rmi;

import java.io.Serializable;

public class KMeansPartialResult implements Serializable {
    private static final long serialVersionUID = 1002L;

    private String workerId;
    private int pointsProcessed;
    private int[] clusterCounts;
    private double[][] clusterSums;
    private long executionTimeMs;
    private String engineInfo;

    public KMeansPartialResult() {}

    public KMeansPartialResult(String workerId, int pointsProcessed, int[] clusterCounts, double[][] clusterSums, long executionTimeMs, String engineInfo) {
        this.workerId = workerId;
        this.pointsProcessed = pointsProcessed;
        this.clusterCounts = clusterCounts;
        this.clusterSums = clusterSums;
        this.executionTimeMs = executionTimeMs;
        this.engineInfo = engineInfo;
    }

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }

    public int getPointsProcessed() {
        return pointsProcessed;
    }

    public void setPointsProcessed(int pointsProcessed) {
        this.pointsProcessed = pointsProcessed;
    }

    public int[] getClusterCounts() {
        return clusterCounts;
    }

    public void setClusterCounts(int[] clusterCounts) {
        this.clusterCounts = clusterCounts;
    }

    public double[][] getClusterSums() {
        return clusterSums;
    }

    public void setClusterSums(double[][] clusterSums) {
        this.clusterSums = clusterSums;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    public String getEngineInfo() {
        return engineInfo;
    }

    public void setEngineInfo(String engineInfo) {
        this.engineInfo = engineInfo;
    }
}
