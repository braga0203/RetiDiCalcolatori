package esercitazione_2.svolta;
// PutFileServer Sequenziale

import java.io.*;
import java.net.*;

public class PutFileServerSeq {
	public static final int PORT = 54321; // porta default per server

	static final String prefix = "[PutFile Server]: ";
	static final String USAGE = "Usage: java PutFileServerSeq or java PutFileServerSeq port";
	public static void main(String[] args) throws IOException {
		
		// Porta sulla quale ascolta il server
		int port = -1;
		{ // ARGS
			try {
				if (args.length == 1) {
					port = Integer.parseInt(args[0]);
					// controllo che la porta sia nel range consentito 1024-65535
					if (port < 1024 || port > 65535) {
						System.out.println(USAGE);
						System.exit(1);
					}
				} else if (args.length == 0) {
					port = PORT;
				} else {
					System.out.println(USAGE);
					System.exit(1);
				}
			} // try
			catch (Exception e) {
				System.out.println("Problemi, i seguenti: ");
				e.printStackTrace();
				System.out.println("Usage: java PutFileServerSeq or java PutFileServerSeq port");
				System.exit(1);
			}
		}

		/* preparazione socket e in/out stream */
		ServerSocket serverSocket = null;
		try {
//			serverSocket = new ServerSocket(port,2);
			serverSocket = new ServerSocket(port);
			serverSocket.setReuseAddress(true);
			System.out.println(prefix+"avviato ");
			System.out.println(prefix+"Creata la server socket: " + serverSocket);
		} catch (Exception e) {
			System.err.println(prefix+"Problemi nella creazione della server socket: " + e.getMessage());
			e.printStackTrace();
			System.exit(2);
		}
		try {
			// ciclo infinito server
			while (true) {
				Socket clientSocket = null;
				DataInputStream inSock = null;
				DataOutputStream outSock = null;

				System.out.println(prefix+"In attesa di richieste...");
				try {
					clientSocket = serverSocket.accept();
					clientSocket.setSoTimeout(30000); // timeout altrimenti server sequenziale si sospende
					System.out.println(prefix+"Connessione accettata: " + clientSocket);
				} catch (SocketTimeoutException te) {
					System.err.println(prefix+"Non ho ricevuto nulla dal client per 30 sec., interrompo "
							+ "la comunicazione e accetto nuove richieste.");
					// il server continua a fornire il servizio ricominciando dall'inizio
					continue;
				} catch (Exception e) {
					System.err.println(prefix+"Problemi nella accettazione della connessione: " + e.getMessage());
					e.printStackTrace();
					// il server continua a fornire il servizio ricominciando dall'inizio
					// del ciclo, se ci sono stati problemi
					continue;
				}

				// stream I/O e ricezione nome file
				String nomeFile;
				try {
					inSock = new DataInputStream(clientSocket.getInputStream());
					outSock = new DataOutputStream(clientSocket.getOutputStream());
					nomeFile = inSock.readUTF();
				} catch (SocketTimeoutException ste) {
					System.out.println(prefix+"Timeout scattato: ");
					ste.printStackTrace();
					clientSocket.close();
					System.out.print(prefix+"^D(Unix)/^Z(Win)+invio per uscire, solo invio per continuare: ");
					// il client continua l'esecuzione riprendendo dall'inizio del ciclo
					continue;
				} catch (IOException e) {
					System.out.println(prefix+"Problemi nella creazione degli stream di input/output " + "su socket: ");
					e.printStackTrace();
					// il server continua l'esecuzione riprendendo dall'inizio del ciclo
					continue;
				}

				// elaborazione e comunicazione esito
				FileOutputStream outFile = null;
				String esito;
				if (nomeFile == null) {
					System.out.println(prefix+"Problemi nella ricezione del nome del file: ");
					clientSocket.close();
					continue;
				} else {
					File curFile = new File(nomeFile);
					// controllo su file
					if (curFile.exists()) {
						try {
							esito = "File Sovrascritto";
							// distruggo il file da sovrascrivere
							curFile.delete();
						} catch (Exception e) {
							System.out.println(prefix+"Problemi nella notifica di file esistente: ");
							e.printStackTrace();
							continue;
						}
					} else
						esito = "Creato nuovo file";
					outFile = new FileOutputStream(nomeFile);
				}

				// ricezione file
				try {
					System.out.println(prefix+"Ricevo il file " + nomeFile + ":");
					/** NOTA: la funzione consuma l'EOF */
					FileUtility.trasferisci_a_byte_file_binario(inSock, new DataOutputStream(outFile));
					System.out.println(prefix+"Ricezione del file " + nomeFile + " terminata\n");
					outFile.close(); // chiusura file
					clientSocket.shutdownInput(); // chiusura socket (downstream)
					// ritorno esito positivo al client
					outSock.writeUTF(esito + ", file salvato lato server");
					clientSocket.shutdownOutput(); // chiusura socket (upstream)
					System.out.println(prefix+"Terminata connessione con " + clientSocket);
					clientSocket.close();
				} catch (SocketTimeoutException ste) {
					System.out.println(prefix+"Timeout scattato: ");
					ste.printStackTrace();
					clientSocket.close();
					System.out.print(prefix+"^D(Unix)/^Z(Win)+invio per uscire, solo invio per continuare: ");
					continue;
				} catch (Exception e) {
					System.err.println(prefix+"Problemi durante la ricezione e scrittura del file: " + e.getMessage());
					e.printStackTrace();
					clientSocket.close();
					System.out.println(prefix+"Terminata connessione con " + clientSocket);
					continue;
				}
			} // while (true)
		}
		// qui catturo le eccezioni non catturate all'interno del while
		// in seguito alle quali il server termina l'esecuzione
		catch (Exception e) {
			e.printStackTrace();
			// chiusura di stream e socket
			System.out.println(prefix+"Errore irreversibile, PutFileServerSeq: termino...");
			System.exit(3);
		}
	} // main
} // PutFileServerSeq
