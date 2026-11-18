package esercitazione_5.proposta.base;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.rmi.Naming;
import java.rmi.NotBoundException;

public class Client {

	public static void main(String[] args) {
		final int REGISTRYPORT = 1099;
		String registryHost = null; // host remoto con registry
		String serviceName = "";
		String nome_file = "";
		String limite = "";
		int lim = -1;
		int numero_righe = -1;
		Esito esito=null;
		BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));

		// Controllo dei parametri della riga di comando
		if (args.length != 2) {
			System.out.println("Sintassi: RMI_Registry_IP ServiceName");
			System.exit(1);
		}
		registryHost = args[0];
		serviceName = args[1];

		System.out.println("Invio richieste a " + registryHost + " per il servizio di nome " + serviceName);

		// Connessione al servizio RMI remoto
		try {
			String completeName = "//" + registryHost + ":" + REGISTRYPORT + "/" + serviceName;
			RemOp serverRMI = (RemOp) Naming.lookup(completeName);
			System.out.println("ClientRMI: Servizio \"" + serviceName + "\" connesso");

			System.out.println("\nRichieste di servizio fino a fine file");

			String service;
			System.out.print("Servizio (C=Conta righe, E=Elimina riga): ");

			/* ciclo accettazione richieste utente */
			while ((service = stdIn.readLine()) != null) {
				boolean ok = false;

				if (service.equals("C")) {
					System.out.print("Nome File? ");
					nome_file = stdIn.readLine();
					while (!ok) {
						System.out.print("Limite inferiore? ");
						limite = stdIn.readLine();
						try {
							lim = Integer.parseInt(limite);
						} catch (NumberFormatException e) {
							continue;
						}
						ok = true;
					}
					numero_righe = serverRMI.conta_righe(nome_file, lim);
					System.out.println("Numero righe di " + nome_file + ": " + numero_righe);
				} else if (service.equals("E")) {
					System.out.print("Nome File? ");
					nome_file = stdIn.readLine();
					while (!ok) {
						System.out.print("Numero riga da eliminare? ");
						limite = stdIn.readLine();
						try {
							lim = Integer.parseInt(limite);
						} catch (NumberFormatException e) {
							continue;
						}
						ok = true;
					}
					esito = serverRMI.elimina_riga(nome_file, lim);
					System.out.println("Esito " + esito.toString());
				} // P=Programma

				else
					System.out.println("Servizio non disponibile");

				System.out.print("Servizio (C=Conta righe, E=Elimina riga): ");
			} // while (!EOF), fine richieste utente

		} catch (NotBoundException nbe) {
			System.err.println("ClientRMI: il nome fornito non risulta registrato; " + nbe.getMessage());
			nbe.printStackTrace();
			System.exit(1);
		} catch (Exception e) {
			System.err.println("ClientRMI: " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
	}

}
