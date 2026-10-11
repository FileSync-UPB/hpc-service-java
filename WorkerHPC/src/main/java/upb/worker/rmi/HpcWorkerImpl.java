package upb.worker.rmi;

import upb.hpc.rmi.IHpcWorker;
import upb.hpc.rmi.KMeansPartialResult;
import upb.worker.executor.CEngineManager;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

public class HpcWorkerImpl extends UnicastRemoteObject implements IHpcWorker {

    private final String workerId;
    private final CEngineManager cEngineManager;

    public HpcWorkerImpl(String workerId) throws RemoteException {
        super();
        this.workerId = workerId;
        this.cEngineManager = new CEngineManager(workerId);
    }

    public HpcWorkerImpl(String workerId, int port) throws RemoteException {
        super(port);
        this.workerId = workerId;
        this.cEngineManager = new CEngineManager(workerId);
    }

    @Override
    public String getWorkerId() throws RemoteException {
        return workerId;
    }

    @Override
    public KMeansPartialResult computeKMeansPartition(String jobId, int iteration, List<double[]> pointsPartition, List<double[]> centroids, int k) throws RemoteException {
        long tStart = System.currentTimeMillis();
        System.out.println("[" + workerId + "] Recibida particion para Job: " + jobId + " | Iteracion: " + iteration +
                " | Puntos: " + (pointsPartition != null ? pointsPartition.size() : 0));

        KMeansPartialResult result = cEngineManager.executeKMeans(jobId, iteration, pointsPartition, centroids, k);

        long tTotal = System.currentTimeMillis() - tStart;
        System.out.println("[" + workerId + "] Particion completada en " + tTotal + " ms (" + result.getEngineInfo() + ")");

        return result;
    }

    @Override
    public boolean ping() throws RemoteException {
        return true;
    }
}
