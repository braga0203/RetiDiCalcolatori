package esercitazione_2.proposta.mPut;
// PutFileServer Concorrente

import java.io.*;
import java.net.*;

// Thread lanciato per ogni richiesta accettata
// versione per il trasferimento di file binari
class MultiplePutFileServerThread extends Thread {

	private Socket clientSocket = null;
	private static final String PREFIX = "[Server Thread]: ";

	/**
	 * Constructor
	 * 
	 * @param clientSocket
	 */
	public MultiplePutFileServerThread(Socket clientSocket) {
		this.clientSocket = clientSocket;
	}

	public void run() {
		DataInputStream inSock = null;
		DataOutputStream outSock = null;
		{// Server socket
			try {
				inSock = new DataInputStream(clientSocket.getInputStream());
				outSock = new DataOutputStream(clientSocket.getOutputStream());
			} catch (IOException ioe) {
				System.out.println(PREFIX + "Problemi nella creazione degli stream di input/output " + "su socket: ");
				ioe.printStackTrace();
				// il server continua l'esecuzione riprendendo dall'inizio del ciclo
				return;
			}
		}

		try {
			boolean terminato = false;
			while (!terminato) {
				System.out.println("========================================================");
				String nomeFile = null;
				String risposta = null;
				File curFile = null;
				long size = -1;
				{// PROLOGO
					{ // lettura nome file
						try {
							// creazione stream di input e out da socket
							nomeFile = inSock.readUTF();
							System.out.println(PREFIX + "Ricevuto nome file " + nomeFile);
							if (nomeFile == null) {
								System.out.println(PREFIX + "Problemi nella ricezione del nome del file: ");
								return;
							}
							curFile = new File(nomeFile);
							if (curFile.exists())
								risposta = "[SALTA]";
							else
								risposta = "[AVVIA]";
							outSock.writeUTF(risposta);
							System.out.println(PREFIX + "Invio risposta" + risposta);
							if ("[SALTA]".equals(risposta))
								continue;
							size = inSock.readLong();
							System.out.println(PREFIX + "Ricevuto dimensione file " + size);
						}
						catch (EOFException eof) {
							terminato=true;
							continue;
						}
						catch (SocketTimeoutException ste) {
							System.out.println("Timeout scattato:");
							ste.printStackTrace();
							terminato=true;
							continue;
						} catch (IOException ioe) {
							
							terminato=true;
							continue;
						} 
					}

				}
				//nuova destinazione
				File dirNuova = new File("src/esercitazione_2/file");
				if(!dirNuova.exists())
					dirNuova.mkdirs();
				File fileOrdinato = new File(dirNuova,nomeFile);
				
				String esito = "[POSITIVO]";
				FileOutputStream outFile = null;
				// ciclo di ricezione dal client, salvataggio file e stamapa a video
				{// Trasfile
					try {
						outFile = new FileOutputStream(fileOrdinato);
						FileUtility.trasferisci_a_byte_file_binario(inSock, new DataOutputStream(outFile), size);
						// chiusura file
						outFile.flush();
						outFile.close();
						outSock.writeUTF(esito + ", file salvato lato server");
						outSock.flush();
					} catch (SocketTimeoutException ste) {
						System.out.println("Timeout scattato:1");
						ste.printStackTrace();

						terminato=true;
						continue;
					} catch (Exception e) {
						System.err.println("\nProblemi durante la ricezione e scrittura del file: " + e.getMessage());
						e.printStackTrace();
						System.out.println("Terminata connessione con " + clientSocket);
						terminato=true;
						continue;
					}
				}
			}
		
			//
			System.out.println(PREFIX+"Chiudo socket");
			clientSocket.shutdownInput();
			clientSocket.shutdownOutput();
			clientSocket.close();
			System.out.println("==============================================================");
		}
		// qui catturo le eccezioni non catturate all'interno del while
		// in seguito alle quali il server termina l'esecuzione
		catch (Exception e) {
			e.printStackTrace();
			System.out.println("Errore irreversibile, PutFileServerThread: termino...");
			System.exit(3);
		}
	} // run

} // PutFileServerThread class

public class MultiplePutFileServerCon {
	public static final int PORT = 1050; // default port
	private static final String PREFIX = "[MultiplePutFileServerCon]: ";
	private static final String USAGE = "Usage: java PutFileServerThread or java PutFileServerThread port";

	public static void main(String[] args) throws IOException {

		int port = -1;
		{/* controllo argomenti */
			try {
				if (args.length == 1) {
					port = Integer.parseInt(args[0]);
					if (port < 1024 || port > 65535) {
						System.out.println("Usage: java LineServer [serverPort>1024]");
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
				System.out.println(PREFIX + "Problemi, i seguenti: ");
				e.printStackTrace();
				System.out.println(USAGE);
				System.exit(1);
			}
		}

		ServerSocket serverSocket = null;
		Socket clientSocket = null;
		{// setup socket
			try {
				serverSocket = new ServerSocket(port);
				serverSocket.setReuseAddress(true);
				System.out.println(PREFIX + "avviato.. ");
				System.out.println(PREFIX + "creata la server socket: " + serverSocket);
			} catch (Exception e) {
				System.err.println(PREFIX + "problemi nella creazione della server socket: " + e.getMessage());
				e.printStackTrace();
				System.exit(1);
			}
		}

		try {
			while (true) {
				System.out.println(PREFIX + "in attesa di richieste...");

				try {
					// bloccante fino ad una pervenuta connessione
					clientSocket = serverSocket.accept();
					clientSocket.setSoTimeout(100000);
					System.out.println(PREFIX + "connessione accettata: " + clientSocket);
				}catch (IOException e) {
					System.err.println(PREFIX + "problemi nella accettazione della connessione: " + e.getMessage());
					e.printStackTrace();
					continue;
				}

				// serizio delegato ad un nuovo thread
				try {
					new MultiplePutFileServerThread(clientSocket).start();
				} catch (Exception e) {
					System.err.println(PREFIX + " problemi nel server thread: " + e.getMessage());
					e.printStackTrace();
					continue;
				}

			} // while
		}
		// qui catturo le eccezioni non catturate all'interno del while
		// in seguito alle quali il server termina l'esecuzione
		catch (Exception e) {
			e.printStackTrace();
			// chiusura di stream e socket
			serverSocket.close();
			System.out.println("PutFileServerCon: termino...");
			System.exit(2);
		}

	}
} // PutFileServerCon class
