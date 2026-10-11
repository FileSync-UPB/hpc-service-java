package upb.hpc.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Interfaz RMI del Nodo Trabajador (Worker HPC).
 * El Coordinador invoca remotamente este método para computar cada iteración sobre su partición.
 */
public interface IHpcWorker extends Remote {

    /**
     * Identificador del trabajador (ej. Worker-1, Worker-Node-2).
     */
    String getWorkerId() throws RemoteException;

    /**
     * Computa una partición del dataset K-Means utilizando el motor nativo en C.
     */
    KMeansPartialResult computeKMeansPartition(String jobId, int iteration, List<double[]> pointsPartition, List<double[]> centroids, int k) throws RemoteException;

    /**
     * Heartbeat / comprobación de estado de salud.
     */
    boolean ping() throws RemoteException;
}
