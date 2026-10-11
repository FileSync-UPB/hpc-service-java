package upb.hpc.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Interfaz RMI del Nodo Principal (Coordinador HPC).
 * Permite recibir trabajos distribuidos vía RMI y gestionar el registro de nodos trabajadores.
 */
public interface IHpcCoordinator extends Remote {

    /**
     * Registra un nodo trabajador en el cluster HPC.
     */
    void registerWorker(String workerId, IHpcWorker worker) throws RemoteException;

    /**
     * Da de baja un nodo trabajador.
     */
    void unregisterWorker(String workerId) throws RemoteException;

    /**
     * Ejecuta el trabajo K-Means distribuido a través de RMI.
     * El Coordinador distribuye las particiones a los nodos trabajadores correspondientes.
     */
    KMeansResult executeKMeans(KMeansRequest request) throws RemoteException;

    /**
     * Retorna el número de trabajadores activos conectados.
     */
    int getActiveWorkerCount() throws RemoteException;

    /**
     * Retorna los nombres de los trabajadores activos conectados.
     */
    List<String> getActiveWorkerNames() throws RemoteException;
}
