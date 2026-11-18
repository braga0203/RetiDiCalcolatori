package esercitazione_5.proposta.base;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.StringTokenizer;


public class ServerImpl extends UnicastRemoteObject implements RemOp{

	protected ServerImpl() throws RemoteException {
		super();
		
	}

	@Override
	public int conta_righe(String nomeFile, int inf) throws RemoteException {
		String riga;
		StringTokenizer st = null;
		int count_righe=0;
		try (BufferedReader br = new BufferedReader(new FileReader(nomeFile))) {
			while((riga=br.readLine()) != null) {
				st = new StringTokenizer(riga);
				if(st.countTokens()>inf)
					count_righe++;
			}	
        } catch (IOException e) {
            e.printStackTrace();
        }
		return count_righe;
	}
	

	@Override
	public Esito elimina_riga(String nomeFile, int numero) throws RemoteException {
        Path fileTemporaneo = Paths.get(Path.of(nomeFile)+ ".tmp");
        try (BufferedReader reader = Files.newBufferedReader(Path.of(nomeFile));
             BufferedWriter writer = Files.newBufferedWriter(fileTemporaneo)) {

            String rigaCorrente;
            int rigaAttuale = 1; // Conteggio a partire da 1
            boolean rigaEliminata = false;

            while ((rigaCorrente = reader.readLine()) != null) {
                if (rigaAttuale == numero) {
                    rigaEliminata = true; // Salta la scrittura di questa riga
                } else {
                    writer.write(rigaCorrente);
                    writer.newLine();
                }
                rigaAttuale++;
            }

            if (!rigaEliminata) {
                System.out.println("Attenzione: La riga " + numero + " non è presente nel file.");
            }
            Files.move(fileTemporaneo, Path.of(nomeFile), StandardCopyOption.REPLACE_EXISTING);
        }catch(Exception e) {
        	
        }

        // Sostituisce il file originale con il file temporaneo
        
        return new Esito(nomeFile);
    }
	// Avvio del Server RMI
		public static void main(String[] args) {
			
			final int REGISTRYPORT = 1099;
			String registryHost = "localhost";
			String serviceName = "Server"; // lookup name...

			// Registrazione del servizio RMI
			String completeName = "//" + registryHost + ":" + REGISTRYPORT + "/" + serviceName;
			try {
				ServerImpl serverRMI = new ServerImpl();
				Naming.rebind(completeName, serverRMI);
				System.out.println("Server RMI: Servizio \"" + serviceName + "\" registrato");
			} catch (Exception e) {
				System.err.println("Server RMI \"" + serviceName + "\": " + e.getMessage());
				e.printStackTrace();
				System.exit(1);
			}
		}

}


