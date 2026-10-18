package esercitazione_2.proposta.mGet;
// PutFileServer Sequenziale

import java.io.*;
import java.net.*;

public class MultipleGetFileServerSeq {
	public static final int PORT = 54321; // porta default per server
	static final String prefix = "[MultipleGetFile Server]: ";
	static final String USAGE = "Usage: java MultipleGetFileServerSeq or java MultipleGetFileServerSeq port";
	static final String storage = "src/file/server";

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

		// nuova destinazione
		File dirStorage = new File(storage);
		if (!dirStorage.exists())
			dirStorage.mkdirs();

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
					System.out.println("=======================================================");

					File dirRichiesta = null;
					String nomeDir = null;
					String risposta = null;
					long minSizeFile = -1;
					File[] files = null;
					File cur = null;
					
					int nFiles=0;
					
					{ // PREAMBOLO
						try {
							nomeDir = inSock.readUTF();
							System.out.println(prefix + "Ricevo nome directory : " + nomeDir);

							dirRichiesta = new File(storage, nomeDir);
							if (dirRichiesta.isDirectory())
								risposta = "[PRESENTE]";
							else
								risposta = "[ASSENTE]";

							outSock.writeUTF(risposta);
							outSock.flush();
							System.out.println(prefix + "Invio risposta : " + risposta);
							if (risposta.equals("[ASSENTE]"))
								continue;

							minSizeFile = inSock.readLong();
							System.out.println(prefix + "Ricevo lunghezza min : " + minSizeFile);
						
							files = dirRichiesta.listFiles();
							
							for (int i = 0; i < files.length; i++) {
								cur = files[i];
								if (cur.isFile() && cur.length() > minSizeFile)
									nFiles++;
							}
							outSock.writeInt(nFiles);
							System.out.println(prefix + "Invio numero file : " + nFiles);
							outSock.flush();
						} catch (EOFException e) {
							terminato=true;
							continue;
						} catch (SocketTimeoutException ste) {
							System.out.println(prefix + "Timeout scattato: 1");
							ste.printStackTrace();
							terminato=true;
							continue;
						} catch (Exception e) {
							e.printStackTrace();
							terminato = true;
							continue;
						}
					}

					FileInputStream inFile = null;
					String nomeFile = null;
					String conferma = "[POSITIVO]";
					long size = -1;
					System.out.println("===============================================================");
					{// Trasferimento file
						for (int i = 0; i < files.length; i++) {
							cur = files[i];
							if (!cur.isFile() || cur.length() < minSizeFile)
								continue;
							{ // PROLOGO
								try {
									nomeFile = cur.getName();
									outSock.writeUTF(nomeFile);
									System.out.println(prefix + "chiedo se vuole " + nomeFile);
									risposta = inSock.readUTF();
									System.out.println(prefix + "ottengo risposta: " + risposta);
									if ("[SALTA]".equals(risposta))
										continue;
									size = cur.length();
									outSock.writeLong(size);
									System.out.println(prefix + "invio dimensione " + size);

								} catch (SocketTimeoutException ste) {
									System.err.println(prefix + "Timeout scattato in [PROLOGO] ");
									ste.printStackTrace();
									break;
								} catch (IOException e) {
									System.out.println(prefix + "Problemi nella comunicazione del [PROLOGO] ");
									e.printStackTrace();
									break;
								}
							}
							{ // TRASFERIMENTO FILE
								try {
									inFile = new FileInputStream(cur);
									FileUtility.trasferisci_a_byte_file_binario(new DataInputStream(inFile), outSock,
											size);
									inFile.close();
									outSock.flush();
									if (i == files.length)
										conferma = "[FINITO]";
									outSock.writeUTF(conferma + ", file inviato lato server");
								} catch (FileNotFoundException e) {
									System.out.println(
											prefix + "Problemi nella creazione dello stream di input da File: ");
									e.printStackTrace();
									break;
								} catch (SocketTimeoutException ste) {
									inFile.close();
									System.out.println(prefix + "Timeout scattato: ");
									ste.printStackTrace();

								} catch (Exception e) {
									System.err.println(prefix + "Problemi durante la lettura e l'invio del file: "
											+ e.getMessage());

									System.out.println(prefix + "Terminata connessione con " + clientSocket);

								}
							}

							System.out.println("----------------------------------------------------------");

						}
					}

				}
				System.out.println(prefix + "Chiudo client socket");
				clientSocket.shutdownInput();
				clientSocket.shutdownOutput();
				clientSocket.close();
				
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
