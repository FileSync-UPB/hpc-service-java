package upb.hpc.rmi;

import java.io.Serializable;
import java.util.List;

public class KMeansRequest implements Serializable {
    private static final long serialVersionUID = 1001L;

    private String jobId;
    private List<double[]> points;
    private int k;
    private int maxIterations;
    private int targetNodes; // 1, 2, 3, 4 (0 = usar todos los disponibles)
    private double tolerance;

    public KMeansRequest() {}

    public KMeansRequest(String jobId, List<double[]> points, int k, int maxIterations, int targetNodes) {
        this(jobId, points, k, maxIterations, targetNodes, 1e-4);
    }

    public KMeansRequest(String jobId, List<double[]> points, int k, int maxIterations, int targetNodes, double tolerance) {
        this.jobId = jobId;
        this.points = points;
        this.k = k;
        this.maxIterations = maxIterations;
        this.targetNodes = targetNodes;
        this.tolerance = tolerance;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public List<double[]> getPoints() {
        return points;
    }

    public void setPoints(List<double[]> points) {
        this.points = points;
    }

    public int getK() {
        return k;
    }

    public void setK(int k) {
        this.k = k;
    }

    public int getMaxIterations() {
        return maxIterations;
    }

    public void setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
    }

    public int getTargetNodes() {
        return targetNodes;
    }

    public void setTargetNodes(int targetNodes) {
        this.targetNodes = targetNodes;
    }

    public double getTolerance() {
        return tolerance;
    }

    public void setTolerance(double tolerance) {
        this.tolerance = tolerance;
    }
}
