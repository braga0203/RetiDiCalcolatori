package esercitazione_5.proposta.base;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class RMIregistry {

	public static void main(String[] args) {
	    try {
	        int registryPort = 1099; // Porta di default RMI
	        
	        // 1. Istanzia l'oggetto remoto
	        ServerImpl server = new ServerImpl();

	        // 2. Avvia il registry integrato direttamente nella JVM del Server
	        Registry registry = LocateRegistry.createRegistry(registryPort);

	        // 3. Registra lo stub direttamente via Registry (senza Naming.rebind)
	        registry.rebind("Server", server);

	        System.out.println("ServerCongresso RMI pronto sulla porta " + registryPort);

	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}

}
