package esercitazione_2.proposta.multipleFile;
// PutFileClient.java

import java.net.*;
import java.nio.file.Path;
import java.util.Arrays;
import java.io.*;

public class MultiplePutFileClient {
	static final String prefix = "[MultiplePutFile Client]: ";
	static final String USAGE = "Usage: java PutFileClient serverAddr serverPort";

	public static void main(String[] args) throws IOException {

		// Condizioni ciclo che delimitano le fasi
		boolean terminated = false;
		// args
		InetAddress addr = null;
		int port = -1;
		{// check args
			try { // check args
				if (args.length == 2) {
					addr = InetAddress.getByName(args[0]);
					port = Integer.parseInt(args[1]);
				} else {
					System.out.println(USAGE);
					System.exit(1);
				}
			} // try
				// Per esercizio si possono dividere le diverse eccezioni
			catch (Exception e) {
				System.out.println("Problemi, i seguenti: ");
				e.printStackTrace();
				System.out.println(USAGE);
				System.exit(2);
			}

		}
		System.out.println(prefix + "avviato...");

		// oggetti utilizzati nella lettura input
		//
		boolean input1 = true;
		String nomeDir = "src/svolta";
		int minSizeFile = 1;
		File dir = new File(nomeDir);
		//
		try {
			while (!terminated) {

				System.out
						.println("===========================================================");
				{// lettura input
					try {
						BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
						while (!input1) {
							System.out.print(
									prefix + "^D(Unix)/^Z(Win)+invio per uscire, oppure immetti nome direttorio: ");
							if ((nomeDir = stdIn.readLine()) == null)
								continue;
							dir = new File(nomeDir);
							if (!dir.isDirectory()) {
								System.out.println(prefix + "Directory: " + dir.getAbsolutePath());
								System.out.println(prefix + "Nome dir non valido");
								continue;
							}
							System.out.print(prefix + "immetti dimensione minima file: ");
							minSizeFile = Integer.valueOf(stdIn.readLine());
							if (minSizeFile < 1) {
								System.out.println(prefix + "File size non valido");
								continue;
							}
							input1 = true;
						}
					} catch (Exception e) {
						System.out.println(prefix + "Problemi nella lettura input: ");
						e.printStackTrace();
					}
				}

				// oggetti utilizzati nella comunicazione del input
				Socket socket = null;

				{// creazione socket
					try {
						socket = new Socket(addr, port);
						socket.setSoTimeout(30000);
						System.out.println(prefix + "Creata la socket: " + socket);
					} catch (Exception e) {
						System.out.println(prefix + "Problemi nella creazione della socket: ");
						e.printStackTrace();
						System.exit(2);
						// il client continua l'esecuzione riprendendo dall'inizio del ciclo
					}

				}
				// Sostituisci il blocco { // trasmit files ... } dentro
				// MultiplePutFileClient.java con questo:

				{ // trasmit files
					try {
						final DataInputStream inSock = new DataInputStream(socket.getInputStream());
						final DataOutputStream outSock = new DataOutputStream(socket.getOutputStream());
						final int effectiveMinSizeFile = minSizeFile;
						
						
						
						Arrays.stream(dir.listFiles()).filter(File::isFile)
								.filter(f -> f.length() > effectiveMinSizeFile).forEachOrdered(f -> {
									String name = f.getName();
									try {
										System.out
												.println("----------------------------------------------------------");
										System.out.println(prefix + "Client chiede al Server se vuole " + name);
										outSock.writeUTF(name);

										String risposta = inSock.readUTF();
										System.out.println(prefix + "Server risponde " + risposta + " " + name);

										if ("[ATTIVA]".equals(risposta)) {
											outSock.writeLong(f.length());

											try (FileInputStream fis = new FileInputStream(f)) {
												System.out.println(prefix + "Inviato alla socket: ");
												FileUtility.trasferisci_a_byte_file_binario(new DataInputStream(fis),
														outSock, f.length());
											}

											// Legge la conferma finale inviata dal server per riallineare il protocollo
											String conferma = inSock.readUTF();
											System.out.println(prefix + risposta + " -> " + conferma);
										}
									} catch (Exception e) {
										System.out
												.println(prefix + "Problemi nel trasferimento del file " + name + ": ");
										e.printStackTrace();
									}
								});

						socket.shutdownOutput(); // chiusura stream in upstream
						System.out.println(prefix + "Trasmissione dei file terminata ");

						// CORREZIONE PROBLEMA 2: resetta input1 per chiedere la nuova directory al giro
						// successivo
						input1 = false;

					} catch (FileNotFoundException e) {
						System.out.println(prefix + "Problemi nella creazione dello stream di input da File: ");
						e.printStackTrace();
						socket.close();
					} catch (SocketTimeoutException ste) {
						System.out.println(prefix + "Timeout scattato: ");
						ste.printStackTrace();
						socket.close();
					} catch (IOException e) {
						System.out.println(prefix + "Problemi nel trasferimento dello stream di input da File: ");
						e.printStackTrace();
						socket.close();
					}
				}
			}
			System.out.println(prefix + "PutFileClient: termino...");
		} catch (Exception e) {
			System.err.println(prefix + "Errore irreversibile, il seguente: ");
			e.printStackTrace();
			System.err.println(prefix + "Chiudo!");
			System.exit(3);
		}
	} // main
} // PutFileClient
