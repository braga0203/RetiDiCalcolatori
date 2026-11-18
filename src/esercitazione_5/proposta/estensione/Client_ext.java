package esercitazione_5.proposta.estensione;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;


public class Client_ext {
	public static String storage="src/file/client/";
	
	
	
	public static void main(String[] args) {
		
		final int REGISTRYPORT = 1099;
		String registryHost = null; // host remoto con registry
		String serviceName = "";
		String nome_dir = "";
		int port_server = -1;
		int port_client = -1;
		InetAddress adr_server = null;
		Esito esito = null;
		Record[] records = null;
		BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));

		Socket attiva_socket = null;
		ServerSocket passiva_socket = null;
		Socket passiva_server_socket = null;
		
		DataInputStream inSock = null;
		DataOutputStream outSock = null;

		FileOutputStream fos = null;
		File curFile = null;
		File parentDir = null;
		
		parentDir = new File(storage);
		if (parentDir != null && !parentDir.exists())
			parentDir.mkdirs();
		
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
			RemOp_ext serverRMI = (RemOp_ext) Naming.lookup(completeName);
			System.out.println("ClientRMI: Servizio \"" + serviceName + "\" connesso");

			System.out.println("\nRichieste di servizio fino a fine file");

			String service;
			System.out.print("Cliente (A=Attivo, P=Passivo): ");
			while ((service = stdIn.readLine()) != null) {
				boolean ok = false;

				if (service.equals("A")) {
					System.out.print("Nome Direttorio? ");
					nome_dir = stdIn.readLine();
					esito = serverRMI.client_attivo(nome_dir);
					System.out.println("Esito "+esito.toString());
					port_server = esito.server_port;
					adr_server = esito.server_adr;
					records = esito.dir;

					{// Socket
						try {
							attiva_socket = new Socket(adr_server, port_server);
							attiva_socket.setSoTimeout(100000);
							inSock = new DataInputStream(attiva_socket.getInputStream());
							outSock = new DataOutputStream(attiva_socket.getOutputStream());
						} catch (IOException e) {
							System.out.println("Problemi nella creazione della socket: ");
							e.printStackTrace();
							continue;
						}
					}

				} else if (service.equals("P")) {
					System.out.print("Nome Direttorio? ");
					nome_dir = stdIn.readLine();
					while (!ok) {
						try {
							System.out.print("Numero porta client? ");
							port_client = Integer.parseInt(stdIn.readLine());
						} catch (NumberFormatException | UnknownHostException e) {
							continue;
						}
						ok = true;
					}
					try {
						passiva_socket = new ServerSocket(port_client);
						passiva_socket.setReuseAddress(true);
					} catch (IOException ioe) {
						ioe.printStackTrace();
						continue;
					}	
					records = serverRMI.client_passivo(passiva_socket.getInetAddress(), port_client, nome_dir);
					System.out.println("in attesa di richieste...");
					try {
						// bloccante fino ad una pervenuta connessione
						passiva_server_socket = passiva_socket.accept();
						passiva_server_socket.setSoTimeout(100000);
						inSock = new DataInputStream(passiva_server_socket.getInputStream());
						outSock = new DataOutputStream(passiva_server_socket.getOutputStream());
					
					} catch (IOException e) {
						e.printStackTrace();
						continue;
					}

					
				} // P=Programma
				else {
					System.out.println("Servizio non disponibile");
					System.out.print("Cliente (A=Attivo, P=Passivo): ");
					continue;
				}
				outSock.writeUTF(nome_dir);
				System.out.println("Invio nome dir: "+nome_dir);
				for (int i = 0; i < records.length; i++) {						
					curFile = new File(storage + nome_dir, records[i].name);
					if (curFile.isFile())
						continue;
					else {
						outSock.writeUTF(records[i].name);
						System.out.println("Richiedo file : " + records[i].name);
					}
					outSock.flush();
					
					fos = new FileOutputStream(storage+records[i].name);
					FileUtility.trasferisci_a_byte_file_binario(inSock, new DataOutputStream(fos), records[i].size);
					System.out.println("----------------------------------------------------------------");
				}	
				System.out.print("Cliente (A=Attivo, P=Passivo): ");
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
