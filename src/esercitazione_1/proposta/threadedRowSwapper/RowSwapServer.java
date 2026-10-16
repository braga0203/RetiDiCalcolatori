package esercitazione_1.proposta.threadedRowSwapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.StringTokenizer;

import esercitazione_1.svolta.LineUtility;

public class RowSwapServer implements Runnable {
	private int port;
	private String fileName;

	DatagramSocket socket = null;
	DatagramPacket packet = null;

	byte[] buf = new byte[256];

	public RowSwapServer(int p, String fn) {
		this.port = p;
		this.fileName = fn;
	}

	@Override
	public void run() {
		try {
			socket = new DatagramSocket(port);
			packet = new DatagramPacket(buf, buf.length);
			System.out.println("Creata la socket: " + socket + " - Thread: " + Thread.currentThread());
		} catch (SocketException e) {
			System.out.println("Problemi nella creazione della socket: ");
			e.printStackTrace();
			System.exit(1);
		}

		try {
			int numLinea1 = -1;
			int numLinea2 = -1;
			String richiesta = null;
			ByteArrayInputStream biStream = null;
			DataInputStream diStream = null;
			StringTokenizer st = null;

			while (true) {
				System.out.println("\tRow_Swap_Server in attesa di richieste...");

				try {// ricezione del datagramma
					packet.setData(buf);
					socket.receive(packet);
				} catch (IOException e) {
					System.err.println("\tProblemi nella ricezione del datagramma: " + e.getMessage());
					e.printStackTrace();
					continue;
					// il server continua a fornire il servizio ricominciando dall'inizio
					// del ciclo
				}

				try {
					biStream = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
					diStream = new DataInputStream(biStream);
					richiesta = diStream.readUTF();
					st = new StringTokenizer(richiesta);
					numLinea1 = Integer.parseInt(st.nextToken());
					numLinea2 = Integer.parseInt(st.nextToken());
					System.out.println("\tRichiesta swap di linee " + numLinea1 + " e " + numLinea2);
				} catch (Exception e) {
					System.err.println("\tProblemi nella lettura della richiesta: " + richiesta);
					e.printStackTrace();
					continue;
					// il server continua a fornire il servizio ricominciando dall'inizio
					// del ciclo
				}
				int r = -10;
				try {
					File original = new File(this.fileName);
					File tempFile = new File(original.getParent(), "TMP_" + original.getName());
					// Questo genererà correttamente qualcosa tipo "src/TMP_saggezza.txt" invece di
					// "TMP_src\saggezza.txt"
					Path p_tmp = tempFile.toPath();
					Path p = original.toPath();

					r = LineUtility.swapLines(original, tempFile, numLinea1, numLinea2);
					System.out.println("\tRisultato swapping: "+r);
					
					try {
						// Sostituisce il file originale con quello temporaneo atomicamente (se
						// supportato dal file system)
						Files.move(p_tmp, p, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
					} catch (IOException e) {
						// Fallback nel caso in cui ATOMIC_MOVE non sia supportato dal file system
						// specifico
						try {
							Files.move(p_tmp, p, StandardCopyOption.REPLACE_EXISTING);
						} catch (IOException ex) {
							ex.printStackTrace();
							// Gestione dell'errore di sostituzione
						}
					}

				} catch (Exception e) {
					System.err.println("\tProblemi nello swapping");
					e.printStackTrace();
					continue;
					// il server continua a fornire il servizio ricominciando dall'inizio
					// del ciclo
				}

			} // while

		}
		// qui catturo le eccezioni non catturate all'interno del while
		// in seguito alle quali il server termina l'esecuzione
		catch (Exception e) {
			e.printStackTrace();

			System.out.println("RowSwapServer: termino...");
			socket.close();
		}

	}

}
