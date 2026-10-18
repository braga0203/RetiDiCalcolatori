package esercitazione_2.proposta.mGet;
// PutFileClient.java

import java.net.*;
import java.nio.file.Path;
import java.util.Arrays;

import java.io.*;

public class MultipleGetFileClient {
	static final String prefix = "[MultipleGetFile Client]: ";
	static final String USAGE = "Usage: java GetFileClient serverAddr serverPort";
	static final String storage = "src/file/client/";

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

		try {

			while (!terminated) {

				System.out.println("===========================================================");
				// oggetti defualt input
				boolean input1 = false;

				String dirName = null;
				long minSizeFile = -1;

				BufferedReader stdIn = null;

				{// lettura input
					try {
						stdIn = new BufferedReader(new InputStreamReader(System.in));
						while (!input1) {
							System.out.print(prefix
									+ "^D(Unix)/^Z(Win)+invio per uscire, oppure immetti nome direttorio (DA OTTENERE): ");
							if ((dirName = stdIn.readLine()) == null)
								continue;

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
				String risposta = null;
				File dir = null;
				int nFiles = -1;
				System.out.println("==============================================================");
				{ // PREAMBOLO
					try {
						outSock.writeUTF(dirName);
						System.out.println(prefix + "chiedo di ottenere direcotory:" + dirName);
						risposta = inSock.readUTF();
						System.out.println(prefix + "ottengo risposta: " + risposta);
						if ("[ASSENTE]".equals(risposta))
							continue;
						dir = new File(dirName);
						outSock.writeLong(minSizeFile);
						System.out.println(prefix + "invio lunghezza minima: " + minSizeFile);
						nFiles = inSock.readInt();
						System.out.println(prefix + "ottengo numero file : " + nFiles);
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
				System.out.println("==============================================================");

				FileOutputStream fos = null;
				String conferma = null;
				for (int i = 0; i < nFiles; i++) {
					String nomeFile = null;
					File curFile = null;
					long size = -1;
					{ // PROLOGO
						try {
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
								System.out.println("----------------------------------------------------------------");
								continue;
							}

							size = inSock.readLong();
							System.out.println(prefix + "Ricevo dimensione file " + size);

						} catch (EOFException e) {
							terminated = true;
							break;
						} catch (SocketTimeoutException ste) {
							System.out.println(prefix + "Timeout scattato: 1");
							ste.printStackTrace();
							terminated = true;
							break;
						} catch (Exception e) {
							terminated = true;
							break;
						}
					}
					File parentDir=null;
					{ // TRASFERIMENTO FILE
						try {
							//crea se non c'è
							
							parentDir=curFile.getParentFile();
							if(parentDir!=null && !parentDir.exists()) 
								parentDir.mkdirs();
							
							fos = new FileOutputStream(curFile);
							FileUtility.trasferisci_a_byte_file_binario(inSock, new DataOutputStream(fos), size);
							conferma = inSock.readUTF();
							System.out.println(prefix + "ottengo conferma trasmissione " + conferma);

						} catch (FileNotFoundException e) {
							System.out.println(prefix + "Problemi nella creazione dello stream di input da File: ");
							e.printStackTrace();
							terminated = true;
							break;
						} catch (SocketTimeoutException ste) {
							System.out.println(prefix + "Timeout scattato in [TRASFERIMENTO] ");
							ste.printStackTrace();
							terminated = true;
							break;
						} catch (IOException e) {
							System.out.println(prefix + "Problemi nel trasferimento dello stream di input da File: ");
							e.printStackTrace();
							terminated = true;
							break;
						}
					}
					System.out.println("----------------------------------------------------------------");

				}

				System.out.println("==============================================================");
				boolean input2 = false;
				String open = null;
				while (!input2) {
					System.out.print(prefix + "Altre richieste (S/N)+invio :");
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
				System.out.println("==============================================================");
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
