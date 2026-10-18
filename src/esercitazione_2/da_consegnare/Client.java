package esercitazione_2.da_consegnare;
// PutFileClient.java

import java.net.*;
import java.nio.file.Path;
import java.util.Arrays;

import java.io.*;

public class Client {
	static final String prefix = "[MultipleGetFile Client]: ";
	static final String USAGE = "Usage: java GetFileClient serverAddr serverPort";
	static final String storage = "src/file/client/";

	public static void main(String[] args) throws IOException {

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

		Socket socket = null;
		DataInputStream inSock = null;
		DataOutputStream outSock = null;
		{// Socket
			try {
				socket = new Socket(addr, port);
				socket.setSoTimeout(100000);
				System.out.println(prefix + "Creata la socket: " + socket);
				inSock = new DataInputStream(socket.getInputStream());
				outSock = new DataOutputStream(socket.getOutputStream());
			} catch (IOException e) {
				System.out.println(prefix + "Problemi nella creazione della socket: ");
				e.printStackTrace();
				// il client continua l'esecuzione riprendendo dall'inizio del ciclo
			}
		}

		System.out.println("===========================================================");
		try {
			boolean terminated = false;
			while (!terminated) {

				String mod = null;
				boolean get = false;
				String dirName = null;
				File dir = null;
				long minSizeFile = -1;
				BufferedReader stdIn = null;
				{// lettura input
					try {
						stdIn = new BufferedReader(new InputStreamReader(System.in));
						boolean input1 = false;
						while (!input1) {
							System.out.print(prefix + "Scegli se fare Get o Put, (G/P)+invio: ");
							mod = stdIn.readLine();
							if (!"G".equalsIgnoreCase(mod) && !"P".equalsIgnoreCase(mod))
								continue;
							if ((get = "G".equalsIgnoreCase(mod)))
								System.out.print(prefix + "Scegli il direttorio server che vuoi scaricare [GET] : ");
							else
								System.out.print(prefix + "Scegli il direttorio server che vuoi caricaricare [PUT] : ");
							if ((dirName = stdIn.readLine()) == null)
								continue;
							dir = new File(storage,dirName);
							if (!get && !dir.isDirectory()) {
								System.out.println(prefix + "Nome Directory non valido: " + dir.getAbsolutePath());
								continue;
							}
							System.out.print(prefix + "Immetti dimensione minima file: ");
							if ((minSizeFile = Integer.valueOf(stdIn.readLine())) < 1) {
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
				String risposta = null;
				int nFiles = -1;
				File[] files = null;
				System.out.println("========================[PREAMBOLO]========================");
				{ // PREAMBOLO
					try {
						outSock.writeUTF(mod);
						System.out.println(prefix + "Chiedo servizio: " + mod);
						if (get) {
							outSock.writeUTF(dirName);
							System.out.println(prefix + "Chiedo directory: " + dirName);
							risposta = inSock.readUTF();
							System.out.println(prefix + "Ottengo risposta: " + risposta);
							if ("[ASSENTE]".equals(risposta))
								continue;
							outSock.writeLong(minSizeFile);
							System.out.println(prefix + "Invio lunghezza minima: " + minSizeFile);
							nFiles = inSock.readInt();
							System.out.println(prefix + "Ottengo numero file: " + nFiles);
						} else {

							outSock.writeUTF(dirName);
							System.out.println(prefix + "Invio directory: " + dirName);
							files = dir.listFiles();
							nFiles = files.length;
							outSock.writeInt(nFiles);
							System.out.println(prefix + "Invio numero files: " + minSizeFile);
							
						}
					} catch (SocketTimeoutException ste) {
						System.err.println(prefix + "Timeout scattato in [PREAMBOLO]");
						ste.printStackTrace();
						terminated = true;
						break;
					} catch (IOException e) {
						System.out.println(prefix + "Problemi nella comunicazione del [PREAMBOLO]");
						e.printStackTrace();
						terminated = true;
						break;
					}

				}
				System.out.println("==========================================================="); // OK
				{// CORE
					try {
						if (get) {// get
							FileOutputStream fos = null;
							String conferma = null;
							String nomeFile = null;
							File curFile = null;

							for (int i = 0; i < nFiles; i++) {
								long size = -1;
								{ // PROLOGO
									nomeFile = inSock.readUTF();
									System.out.println(prefix + "Ricevo nome file : " + nomeFile);

									curFile = new File(storage + dirName, nomeFile);
									if (!curFile.isFile())
										risposta = "[ATTIVA]";
									else
										risposta = "[SALTA]";
									outSock.writeUTF(risposta);
									outSock.flush();
									System.out.println(prefix + "Invio risposta : " + risposta);
									if (risposta.equals("[SALTA]")) {
										System.out.println(
												"----------------------------------------------------------------");
										continue;
									}

									size = inSock.readLong();
									System.out.println(prefix + "Ricevo dimensione file " + size);

								}
								File parentDir = null;
								{ // TRASFERIMENTO FILE
									// crea se non c'è

									parentDir = curFile.getParentFile();
									if (parentDir != null && !parentDir.exists())
										parentDir.mkdirs();

									fos = new FileOutputStream(curFile);
									FileUtility.trasferisci_a_byte_file_binario(inSock, new DataOutputStream(fos),
											size);
									conferma = inSock.readUTF();
									System.out.println(prefix + "ottengo conferma trasmissione " + conferma);

								}
								System.out.println("----------------------------------------------------------------");

							}

						} // GET
						else {// put
							File cur = null;
							String fileName = null;
							long size = -1;

							FileInputStream fis = null;
							String conferma = null;

							for (int i = 0; i < files.length; i++) { // trasferimetno
								cur = files[i];
								if (!cur.isFile() || cur.length() < minSizeFile)
									continue;
								{ // PROLOGO

									System.out.println("----------------------------------------------------------");
									fileName = cur.getName();
									outSock.writeUTF(fileName);
									System.out.println(prefix + "chiedo se vuole " + fileName);
									risposta = inSock.readUTF();
									System.out.println(prefix + "ottengo risposta: " + risposta);
									if ("[SALTA]".equals(risposta))
										continue;
									size = cur.length();
									outSock.writeLong(size);
									System.out.println(prefix + "invio dimensione " + size);

								}
								{ // TRASFERIMENTO FILE

									fis = new FileInputStream(cur.getPath());
									FileUtility.trasferisci_a_byte_file_binario(new DataInputStream(fis), outSock,
											size);
									conferma = inSock.readUTF();
									System.out.println(prefix + "ottengo conferma trasmissione " + conferma);

								}
							}
						} // PUT
					} catch (FileNotFoundException e) {
						System.out.println(prefix + "Problemi nella creazione dello stream da File: ");
						e.printStackTrace();
						terminated = true;
						break;
					} catch (EOFException e) {
						terminated = true;
						break;
					} catch (SocketTimeoutException ste) {
						System.out.println(prefix + "Timeout scattato:");
						ste.printStackTrace();
						terminated = true;
						break;
					} catch (IOException e) {
						System.out.println(prefix + "Problemi nella comunicazione: ");
						e.printStackTrace();
						terminated = true;
						break;
					} catch (Exception e) {
						terminated = true;
						break;
					}
				}
				System.out.println("==============================================================");
				boolean input2 = false;
				String open = null;
				String cur = get ? "[GET]" : "[PUT]";
				{// KEEP asking
					while (!input2) {

						System.out.print(prefix + "Altre richieste " + cur + " (S/N)+invio :");
						open = stdIn.readLine();
						if (!"S".equalsIgnoreCase(open) && !"N".equalsIgnoreCase(open))
							continue;
						if ("N".equalsIgnoreCase(open)) {
							terminated = true;
							System.out.println(prefix + "Chiudo socket");
							socket.shutdownInput();
							socket.shutdownOutput();
							socket.close();
						} else
							System.out.println(prefix + "Mantengo aperta socket");
						input2 = true;
					}
				}
			}
			// while (!terminated)
			System.out.println(prefix + "termino...");
		} catch (

		Exception e) {
			System.err.println(prefix + "Errore irreversibile, il seguente: ");
			e.printStackTrace();
			System.err.println(prefix + "Chiudo!");
			System.exit(3);
		}
	} // main
} // PutFileClient
