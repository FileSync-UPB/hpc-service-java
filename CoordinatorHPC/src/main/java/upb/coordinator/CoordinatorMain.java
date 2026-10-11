package upb.coordinator;

import upb.coordinator.config.Config;
import upb.coordinator.net.HpcAppServer;
import upb.coordinator.net.TCPServer;
import upb.coordinator.rmi.HpcCoordinatorImpl;
import upb.hpc.rmi.IHpcCoordinator;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class CoordinatorMain {
    public static final int DEFAULT_RMI_PORT = 1099;
    public static final String RMI_BIND_NAME = "HpcCoordinator";

    public static void main(String[] args) {
        int rmiPort = DEFAULT_RMI_PORT;
        int tcpPort = Config.getPort();

        if (args.length > 0) {
            try {
                rmiPort = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {}
        }
        if (args.length > 1) {
            try {
                tcpPort = Integer.parseInt(args[1]);
            } catch (NumberFormatException ignored) {}
        }

        String rmiHost = System.getenv("RMI_HOST");
        if (rmiHost != null && !rmiHost.isBlank()) {
            System.setProperty("java.rmi.server.hostname", rmiHost);
            System.out.println("[Coordinator HPC] RMI Host configurado: " + rmiHost);
        }

        System.out.println("==================================================");
        System.out.println("[Coordinator HPC] Iniciando Nodo Maestro / Principal");
        System.out.println("[Coordinator HPC] Puerto RMI: " + rmiPort + " | Servicio: " + RMI_BIND_NAME);
        System.out.println("[Coordinator HPC] Puerto TCP Secundario: " + tcpPort);
        System.out.println("==================================================");

        try {
            // 1. Iniciar registro RMI
            Registry registry;
            try {
                registry = LocateRegistry.createRegistry(rmiPort);
                System.out.println("[Coordinator RMI] Registro RMI creado localmente en puerto: " + rmiPort);
            } catch (Exception e) {
                registry = LocateRegistry.getRegistry(rmiPort);
                System.out.println("[Coordinator RMI] Conectado a registro RMI existente en puerto: " + rmiPort);
            }

            // 2. Crear y registrar el servicio remoto del Coordinador
            HpcCoordinatorImpl coordinator = new HpcCoordinatorImpl();
            registry.rebind(RMI_BIND_NAME, coordinator);
            System.out.println("[Coordinator RMI] Servicio publicado con exito: rmi://0.0.0.0:" + rmiPort + "/" + RMI_BIND_NAME);

            // 3. Iniciar Servidor de Aplicaciones Java para peticiones HTTP del Cliente Web -> RMI
            final int finalTcpPort = tcpPort;
            final int finalRmiPort = rmiPort;
            Thread appServerThread = new Thread(() -> {
                try {
                    HpcAppServer appServer = new HpcAppServer(finalTcpPort, "localhost", finalRmiPort, RMI_BIND_NAME);
                    appServer.start();
                } catch (Exception e) {
                    System.err.println("[Coordinator HPC] Error iniciando HpcAppServer HTTP: " + e.getMessage());
                }
            }, "Coordinator-HpcAppServer-Thread");
            appServerThread.setDaemon(true);
            appServerThread.start();

            System.out.println("[Coordinator HPC] Listo y a la espera de Nodos Trabajadores (1..4), Clientes RMI y Peticiones Web HTTP en :" + tcpPort + ".\n");

            // Mantener proceso vivo
            Thread.currentThread().join();

        } catch (Exception e) {
            System.err.println("[Coordinator HPC] Error fatal iniciando el Coordinador: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
