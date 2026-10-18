package esercitazione_2.proposta.mPut;
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
		
		Socket socket = null;
		DataInputStream inSock = null;
		DataOutputStream outSock = null;
		{// Socket
			try {
				socket = new Socket(addr, port);
				socket.setSoTimeout(30000);
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
				String nomeDir = null;
				int minSizeFile = -1;
				File dir = null;
				BufferedReader stdIn=null;
				{// lettura input
					try {
						stdIn = new BufferedReader(new InputStreamReader(System.in));
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
				

				File[] files = dir.listFiles();
				File cur = null;
				String fileName = null;
				String risposta = null;
				long size = -1;
				
				FileInputStream fis = null;
				String conferma = null;
				
				for (int i = 0; i < files.length; i++) { //trasferimetno
					cur = files[i];
					if (!cur.isFile() || cur.length() < minSizeFile)
						continue;
					{ // PROLOGO
						try {
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

						} catch (SocketTimeoutException ste) {
							System.err.println(prefix + "Timeout scattato in [PROLOGO] ");
							ste.printStackTrace();
							terminated=true;
							break;
						} catch (IOException e) {
							System.out.println(prefix + "Problemi nella comunicazione del [PROLOGO] ");
							e.printStackTrace();
							terminated=true;
							break;
						}
					}
					{ // TRASFERIMENTO FILE
						try {
							fis = new FileInputStream(cur.getPath());
							FileUtility.trasferisci_a_byte_file_binario(new DataInputStream(fis), outSock, size);
							conferma = inSock.readUTF();
							System.out.println(prefix + "ottengo conferma trasmissione " + conferma);
						} catch (FileNotFoundException e) {
							System.out.println(prefix + "Problemi nella creazione dello stream di input da File: ");
							e.printStackTrace();
							terminated=true;
							break;
						} catch (SocketTimeoutException ste) {
							System.out.println(prefix + "Timeout scattato in [TRASFERIMENTO] ");
							ste.printStackTrace();
							terminated=true;
							break;
						} catch (IOException e) {
							System.out.println(prefix + "Problemi nel trasferimento dello stream di input da File: ");
							e.printStackTrace();
							terminated=true;
							break;
						}
					}
				}
				System.out.println("==============================================================");
				boolean input2=false;
				String open = null;
				while (!input2) {
					System.out.print(prefix + "Altre richieste (S/N)+invio :");
					open=stdIn.readLine();
					if (!"S".equalsIgnoreCase(open)&&!"N".equalsIgnoreCase(open))
						continue;
					if("N".equalsIgnoreCase(open)) {
						terminated=true;
						System.out.println(prefix+"Chiudo socket");
						socket.shutdownInput();
						socket.shutdownOutput();
						socket.close();
					}
					else
						System.out.println(prefix+"Mantengo aperta socket");
					input2=true;
				}
				System.out.println("==============================================================");
			}
			
			//while (!terminated)
			System.out.println(prefix + "termino...");
		} catch (Exception e) {
			System.err.println(prefix + "Errore irreversibile, il seguente: ");
			e.printStackTrace();
			System.err.println(prefix + "Chiudo!");
			System.exit(3);
		}
	} // main
} // PutFileClient
