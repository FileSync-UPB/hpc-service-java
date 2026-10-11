package upb.worker;

import upb.hpc.rmi.IHpcCoordinator;
import upb.worker.config.Config;
import upb.worker.net.WorkerTCPClient;
import upb.worker.rmi.HpcWorkerImpl;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.UUID;

public class WorkerMain {
    public static void main(String[] args) {
        String workerId = "Worker-" + UUID.randomUUID().toString().substring(0, 5);
        String host = Config.getCoordinatorHost();
        int rmiPort = 1099;
        int tcpPort = Config.getCoordinatorPort();

        if (args.length > 0 && !args[0].isBlank()) {
            workerId = args[0];
        }
        if (args.length > 1 && !args[1].isBlank()) {
            host = args[1];
        }
        if (args.length > 2) {
            try {
                rmiPort = Integer.parseInt(args[2]);
            } catch (NumberFormatException ignored) {}
        }

        String rmiHost = System.getenv("RMI_HOST");
        if (rmiHost != null && !rmiHost.isBlank()) {
            System.setProperty("java.rmi.server.hostname", rmiHost);
            System.out.println("[Worker HPC] RMI Host configurado: " + rmiHost);
        }

        System.out.println("==================================================");
        System.out.println("[Worker HPC] Iniciando Nodo Trabajador: " + workerId);
        System.out.println("[Worker HPC] Coordinador Destino -> " + host + ":" + rmiPort + " (RMI)");
        System.out.println("==================================================");

        try {
            // 1. Instanciar servicio remoto del Worker
            HpcWorkerImpl workerService = new HpcWorkerImpl(workerId);

            // 2. Conectar al registro RMI del Coordinador
            Registry registry = LocateRegistry.getRegistry(host, rmiPort);
            IHpcCoordinator coordinator = (IHpcCoordinator) registry.lookup("HpcCoordinator");

            // 3. Registrar el Worker en el Coordinador
            coordinator.registerWorker(workerId, workerService);
            System.out.println("[" + workerId + "] REGISTRADO EXITOSAMENTE en el Coordinador RMI.");

            // 4. Hook de apagado para desregistrar
            final String finalWorkerId = workerId;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    coordinator.unregisterWorker(finalWorkerId);
                    System.out.println("[" + finalWorkerId + "] Desregistrado del Coordinador.");
                } catch (Exception ignored) {}
            }));

            // 5. Opcional: Iniciar cliente TCP en background para retrocompatibilidad
            final String fHost = host;
            final int fTcpPort = tcpPort;
            final String fId = workerId;
            Thread tcpThread = new Thread(() -> {
                try {
                    WorkerTCPClient client = new WorkerTCPClient(fHost, fTcpPort, fId);
                    client.start();
                } catch (Exception ignored) {}
            }, "Worker-TCP-" + workerId);
            tcpThread.setDaemon(true);
            tcpThread.start();

            System.out.println("[" + workerId + "] Listo para ejecutar particiones K-Means en C.\n");

            // Mantener worker en ejecucion
            Thread.currentThread().join();

        } catch (Exception e) {
            System.err.println("[" + workerId + "] Error conectando con Coordinador RMI: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
