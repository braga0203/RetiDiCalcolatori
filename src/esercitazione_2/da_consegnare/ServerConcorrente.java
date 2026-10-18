package esercitazione_2.da_consegnare;
// PutFileServer Concorrente

import java.io.*;
import java.net.*;

// Thread lanciato per ogni richiesta accettata
// versione per il trasferimento di file binari
class MultipleGetFileServerThread extends Thread {

	private Socket clientSocket = null;
	private static final String PREFIX = "[Server Thread]: ";

	static final String storage = "src/file/server";

	/**
	 * Constructor
	 * 
	 * @param clientSocket
	 */
	public MultipleGetFileServerThread(Socket clientSocket) {
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
				String mod = null;
				boolean get = false;
				File dirRichiesta = null;
				String nomeDir = null;
				String risposta = null;
				long minSizeFile = -1;
				File[] files = null;
				File curFile = null;
				int nFiles = 0;

				{ // PREAMBOLO
					try {
						mod = inSock.readUTF();
						System.out.println(PREFIX + "Viene richiesto servizio: " + mod);
						get = ("G".equalsIgnoreCase(mod));
						nomeDir = inSock.readUTF();
						System.out.println(PREFIX + "Ricevo nome directory : " + nomeDir);
						if (get) {// GET

							dirRichiesta = new File(storage, nomeDir);
							if (dirRichiesta.isDirectory())
								risposta = "[PRESENTE]";
							else
								risposta = "[ASSENTE]";

							outSock.writeUTF(risposta);
							outSock.flush();
							System.out.println(PREFIX + "Invio risposta : " + risposta);
							if (risposta.equals("[ASSENTE]"))
								continue;

							minSizeFile = inSock.readLong();
							System.out.println(PREFIX + "Ricevo lunghezza min : " + minSizeFile);

							files = dirRichiesta.listFiles();

							for (int i = 0; i < files.length; i++) {
								curFile = files[i];
								if (curFile.isFile() && curFile.length() > minSizeFile)
									nFiles++;
							}
							outSock.writeInt(nFiles);
							System.out.println(PREFIX + "Invio numero file : " + nFiles);
							outSock.flush();
						} else {
							nFiles = inSock.readInt();
							System.out.println(PREFIX + "Ricevo numero file : " + nFiles);
						}
					} catch (EOFException e) {
						terminato = true;
						continue;
					} catch (SocketTimeoutException ste) {
						System.out.println(PREFIX + "Timeout scattato: 3");
						ste.printStackTrace();
						terminato = true;
						continue;
					} catch (Exception e) {
						e.printStackTrace();
						terminato = true;
						continue;
					}
				}
				// nuova destinazione
				{ // CORE
					FileInputStream inFile = null;
					String nomeFile = null;
					long size = -1;
					System.out.println("===============================================================");

					String esito = "[POSITIVO]";
					FileOutputStream outFile = null;
					try {
						if (get) {// GET
							{// Trasferimento file
								for (int i = 0; i < files.length; i++) {
									curFile = files[i];
									if (!curFile.isFile() || curFile.length() < minSizeFile)
										continue;
									{ // PROLOGO
										try {
											nomeFile = curFile.getName();
											outSock.writeUTF(nomeFile);
											System.out.println(PREFIX + "chiedo se vuole " + nomeFile);
											risposta = inSock.readUTF();
											System.out.println(PREFIX + "ottengo risposta: " + risposta);
											if ("[SALTA]".equals(risposta))
												continue;
											size = curFile.length();
											outSock.writeLong(size);
											System.out.println(PREFIX + "invio dimensione " + size);

										} catch (SocketTimeoutException ste) {
											System.err.println(PREFIX + "Timeout scattato in [PROLOGO] ");
											ste.printStackTrace();
											break;
										} catch (IOException e) {
											System.out.println(PREFIX + "Problemi nella comunicazione del [PROLOGO] ");
											e.printStackTrace();
											break;
										}
									}
									{ // TRASFERIMENTO FILE
										try {
											inFile = new FileInputStream(curFile);
											FileUtility.trasferisci_a_byte_file_binario(new DataInputStream(inFile),
													outSock, size);
											inFile.close();
											outSock.flush();
											outSock.writeUTF(esito + ", file inviato lato server");
										} catch (FileNotFoundException e) {
											System.out.println(PREFIX
													+ "Problemi nella creazione dello stream di input da File: ");
											e.printStackTrace();
											break;
										} catch (SocketTimeoutException ste) {
											inFile.close();
											System.out.println(PREFIX + "Timeout scattato: ");
											ste.printStackTrace();
										} catch (Exception e) {
											System.err
													.println(PREFIX + "Problemi durante la lettura e l'invio del file: "
															+ e.getMessage());

											System.out.println(PREFIX + "Terminata connessione con " + clientSocket);

										}
									}

								}

							}

						} else {// PUT

							{// Trasferimento file
								for (int i = 0; i < nFiles; i++) {
									nomeFile = inSock.readUTF();
									System.out.println(PREFIX + "Ricevuto nome file " + nomeFile);
									if (nomeFile == null) {
										System.out.println(PREFIX + "Problemi nella ricezione del nome del file: ");
										return;
									}
									curFile = new File(storage +"\\"+nomeDir, nomeFile);

									if (curFile.exists())
										risposta = "[SALTA]";
									else
										risposta = "[AVVIA]";
									outSock.writeUTF(risposta);
									System.out.println(PREFIX + "Invio risposta " + risposta);
									if ("[SALTA]".equals(risposta))
										continue;
									size = inSock.readLong();
									System.out.println(PREFIX + "Ricevuto dimensione file " + size);
									outFile = new FileOutputStream(curFile);
									FileUtility.trasferisci_a_byte_file_binario(inSock, new DataOutputStream(outFile),
											size);
									// chiusura file
									outFile.flush();
									outFile.close();
									outSock.writeUTF(esito + ", file salvato lato server");
									outSock.flush();
								}
							}
						}

					} catch (EOFException eof) {
						terminato = true;
						continue;
					} catch (FileNotFoundException e) {
						System.out.println(PREFIX + "Problemi nella creazione dello stream di input da File: ");
						e.printStackTrace();
						break;
					} catch (SocketTimeoutException ste) {
						System.err.println(PREFIX + "Timeout scattato in [PROLOGO] ");
						ste.printStackTrace();
						break;
					} catch (IOException e) {
						System.out.println(PREFIX + "Problemi nella comunicazione del [PROLOGO] ");
						e.printStackTrace();
						break;
					} catch (Exception e) {
						System.err
								.println(PREFIX + "Problemi durante la lettura e l'invio del file: " + e.getMessage());
						e.printStackTrace();
						System.out.println(PREFIX + "Terminata connessione con " + clientSocket);

					}
				}
			}

			//
			System.out.println(PREFIX + "Chiudo socket");
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

public class ServerConcorrente {
	public static final int PORT = 1050; // default port
	private static final String PREFIX = "[MultiplePutFileServerCon]: ";
	private static final String USAGE = "Usage: java PutFileServerThread or java PutFileServerThread port";
	static final String STORAGE = "src/file/server";

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
		// nuova destinazione
		File dirStorage = new File(STORAGE);
		if (!dirStorage.exists())
			dirStorage.mkdirs();

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
				} catch (IOException e) {
					System.err.println(PREFIX + "problemi nella accettazione della connessione: " + e.getMessage());
					e.printStackTrace();
					continue;
				}

				// serizio delegato ad un nuovo thread
				try {
					new MultipleGetFileServerThread(clientSocket).start();
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
