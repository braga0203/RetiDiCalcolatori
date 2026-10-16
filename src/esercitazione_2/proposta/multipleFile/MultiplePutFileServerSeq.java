package esercitazione_2.proposta.multipleFile;
// PutFileServer Sequenziale

import java.io.*;
import java.net.*;

public class MultiplePutFileServerSeq {
	public static final int PORT = 54321; // porta default per server
	static final String prefix = "[MultiplePutFile Server]: ";
	static final String USAGE = "Usage: java MultiplePutFileServerSeq or java MultiplePutFileServerSeq port";

	public static void main(String[] args) throws IOException {
		// Porta sulla quale ascolta il server
		int port = -1;
		{/* controllo argomenti */
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
				System.out.println(prefix + "Problemi, i seguenti: ");
				e.printStackTrace();
				System.out.println(USAGE);
				System.exit(1);
			}
		}

		ServerSocket serverSocket = null;
		{// preparazione socket e in/out stream
			try {
//			serverSocket = new ServerSocket(port,2);
				serverSocket = new ServerSocket(port);
				serverSocket.setReuseAddress(true);
				System.out.println(prefix + "Avviato ");
				System.out.println(prefix + "Creata la server socket: " + serverSocket);
			} catch (Exception e) {
				System.err.println(prefix + "Problemi nella creazione della server socket: " + e.getMessage());
				e.printStackTrace();
				System.exit(2);
			}
		}
		FileOutputStream outFile = null;
		try {
			Socket clientSocket = null;
			DataInputStream inSock = null;
			DataOutputStream outSock = null;
			// ciclo infinito server

			String nomeFile = null;
			String esito = "[SALTA]";
			while (true) {
				{// Connessione accettata
					System.out.println(prefix + "In attesa di richieste...");
					try {
						clientSocket = serverSocket.accept();
						clientSocket.setSoTimeout(30000); // timeout altrimenti server sequenziale si sospende
						System.out.println(prefix + "Connessione accettata: " + clientSocket);
						inSock = new DataInputStream(clientSocket.getInputStream());
						outSock = new DataOutputStream(clientSocket.getOutputStream());
					} catch (SocketTimeoutException te) {
						System.err.println(prefix + "Non ho ricevuto nulla dal client per 30 sec., interrompo "
								+ "la comunicazione e accetto nuove richieste.");
						System.exit(3);
					} catch (IOException e) {
						System.out
								.println(prefix + "Problemi nella creazione degli stream di input/output su socket: ");
						e.printStackTrace();
						// il server continua l'esecuzione riprendendo dall'inizio del ciclo
					} catch (Exception e) {
						System.err.println(prefix + "Problemi nella accettazione della connessione: " + e.getMessage());
						e.printStackTrace();
						System.exit(2);
					}
				}
				boolean terminato=false;
				while (!terminato) {// trasmissione files
					System.out.println("Check");
					{
						
						// stream I/O e ricezione nome file
						try {

							System.out.println("Check");
							nomeFile = inSock.readUTF();
							System.out.println(prefix + "Ricevo dal Client : " + nomeFile);
							if (nomeFile == null) {
								System.out.println(prefix + "1-Problemi nella ricezione del nome del file: ");
								terminato = true;
							}
							// controllo su file
							if (!(new File(nomeFile)).isFile())
								esito = "[ATTIVA]";
							else
								esito = "[SALTA]";
							outSock.writeUTF(esito);
							System.out.println(prefix + "Invio al Cliente : " + esito + " " + nomeFile);
							if (esito.equals("[SALTA]"))
								continue;
						} catch (SocketTimeoutException ste) {
							System.out.println(prefix + "Timeout scattato: ");
							ste.printStackTrace();
							System.out.print(prefix + "^D(Unix)/^Z(Win)+invio per uscire, solo invio per continuare: ");
							terminato = true;
						} catch (EOFException e) {
							System.out.println(prefix + "Terminata trasmission");
							terminato = true;
							continue;
						} catch (Exception e) {
							System.out.println(prefix + "2_Problemi nella ricezione del nome del file: ");
							e.printStackTrace();
							Thread.currentThread();
							terminato = true;
							continue;
						}
					}

					// ricezione file

					// clientSocket.shutdownInput(); // chiusura socket (downstream)
					// ritorno esito positivo al client
					// clientSocket.shutdownOutput(); // chiusura socket (upstream)
					// clientSocket.close();
					long size = -1;
					try {
						outFile = new FileOutputStream(nomeFile);
						size = inSock.readLong();
						System.out.println(prefix + "Ricevo il file " + nomeFile + " di lunghezza " + size + " :");
						/** NOTA: la funzione consuma l'EOF */
						FileUtility.trasferisci_a_byte_file_binario(inSock, new DataOutputStream(outFile), size);
						System.out.println(prefix + "Ricezione del file " + nomeFile + " terminata");
						outFile.close(); // chiusura file
						outSock.writeUTF(prefix + " " + esito + ", file salvato lato server");
						System.out.println(prefix + "Terminata connessione con " + clientSocket);
					} catch (SocketTimeoutException ste) {
						outFile.close();
						System.out.println(prefix + "Timeout scattato: ");
						ste.printStackTrace();
						// clientSocket.close();
						System.out.print(prefix + "^D(Unix)/^Z(Win)+invio per uscire, solo invio per continuare: ");
						terminato = true;
					} catch (Exception e) {
						if (outFile != null)
							outFile.close();
						System.err.println(
								prefix + "3_Problemi durante la ricezione e scrittura del file: " + e.getMessage());
						e.printStackTrace();
						// clientSocket.close();
						System.out.println(prefix + "Terminata connessione con " + clientSocket);
						terminato = true;
					}
				}
				clientSocket.shutdownOutput();
			} // while (true)
		}
		// qui catturo le eccezioni non catturate all'interno del while
		// in seguito alle quali il server termina l'esecuzione
		catch (Exception e) {
			outFile.close(); // chiusura file
			e.printStackTrace();
			// chiusura di stream e socket
			System.out.println(prefix + "Errore irreversibile, PutFileServerSeq: termino...");
			serverSocket.close();
			System.exit(3);
		}
	} // main
}
// PutFileServerSeq
