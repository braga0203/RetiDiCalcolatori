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
		{// Server socket
			try {
//			serverSocket = new ServerSocket(port,2);
				serverSocket = new ServerSocket(port);
				serverSocket.setReuseAddress(true);
				System.out.println(prefix + "avviato... ");
				System.out.println(prefix + "Creata la server socket: " + serverSocket);
			} catch (Exception e) {
				System.err.println(prefix + "Problemi nella creazione della server socket: " + e.getMessage());
				e.printStackTrace();
				System.exit(2);
			}
		}
		FileOutputStream outFile = null;
		try {

			// ciclo infinito server

			while (true) {

				System.out.println("========================================================");
				Socket clientSocket = null;
				DataInputStream inSock = null;
				DataOutputStream outSock = null;
				{// Client socket
					System.out.println(prefix + "In attesa di richieste...");
					try {
						clientSocket = serverSocket.accept();
						clientSocket.setSoTimeout(100000); // timeout altrimenti server sequenziale si sospende
						System.out.println(prefix + "Connessione accettata: " + clientSocket);
						inSock = new DataInputStream(clientSocket.getInputStream());
						outSock = new DataOutputStream(clientSocket.getOutputStream());
					} catch (SocketTimeoutException te) {
						System.err.println(prefix + "Non ho ricevuto nulla dal client per 30 sec., interrompo "
								+ "la comunicazione e accetto nuove richieste.");
						continue;
					} catch (IOException e) {
						System.out
								.println(prefix + "Problemi nella creazione degli stream di input/output su socket: ");
						e.printStackTrace();
						System.exit(2);
						// il server continua l'esecuzione riprendendo dall'inizio del ciclo
					} catch (Exception e) {
						System.err.println(prefix + "Problemi nella accettazione della connessione: " + e.getMessage());
						e.printStackTrace();
						System.exit(2);
					}
				}
				boolean terminato = false;

				while (!terminato) {// trasmissione files
					System.out.println("--------------------------------------------------------");
					
					File curFile = null;
					String nomeFile = null;
					String risposta = null;
					long size = -1;
					{ // PROLOGO
						try {
							System.out.println("X");
							nomeFile = inSock.readUTF();
							System.out.println(prefix + "Ricevo nome file : " + nomeFile);
							
							curFile = new File(nomeFile);
							if (!curFile.isFile())
								risposta = "[ATTIVA]";
							else
								risposta = "[SALTA]";
							outSock.writeUTF(risposta);
							outSock.flush();
							System.out.println(prefix + "Invio risposta : " + risposta);
							if (risposta.equals("[SALTA]"))
								continue;

							size = inSock.readLong();
							System.out.println(prefix + "Ricevo dimensione file " + size);

						}  catch (EOFException e) {
							terminato = true;
							continue;
						}catch (SocketTimeoutException ste) {
							System.out.println(prefix + "Timeout scattato: 1");
							ste.printStackTrace();
							continue;
						} catch (Exception e) {
							terminato = true;
							continue;
						}
					}
					//nuova destinazione
					File dirNuova = new File("src/esercitazione_2/file");
					if(!dirNuova.exists())
						dirNuova.mkdirs();
					File fileOrdinato = new File(dirNuova,nomeFile);
					
					
					String esito = null;
					{// Trasferimento file
						try {

							outFile = new FileOutputStream(fileOrdinato);
							FileUtility.trasferisci_a_byte_file_binario(inSock, new DataOutputStream(outFile), size);
							outFile.flush();
							outFile.close();
							outSock.writeUTF(esito + ", file salvato lato server");
							outSock.flush();
						} catch (SocketTimeoutException ste) {
							outFile.close();
							System.out.println(prefix + "Timeout scattato: ");
							ste.printStackTrace();
						
						} catch (Exception e) {
							System.err.println(
									prefix + "Problemi durante la ricezione e scrittura del file: " + e.getMessage());

							System.out.println(prefix + "Terminata connessione con " + clientSocket);
							
						}
					}
				}
				
				System.out.println(prefix+"Chiudo socket");
				clientSocket.shutdownInput();
				clientSocket.shutdownOutput();
				clientSocket.close();
				System.out.println("========================================================");
				
			} // while (true)
		}
		// qui catturo le eccezioni non catturate all'interno del while
		// in seguito alle quali il server termina l'esecuzione
		catch (Exception e) {
			e.printStackTrace();
			// chiusura di stream e socket
			System.out.println(prefix + "Errore irreversibile, PutFileServerSeq: termino...");
			serverSocket.close();
			System.exit(3);
		}
	} // main
}
// PutFileServerSeq
