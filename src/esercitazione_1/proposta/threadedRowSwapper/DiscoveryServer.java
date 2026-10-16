package esercitazione_1.proposta.threadedRowSwapper;

import java.io.BufferedReader;

//DiscoveryServer.java

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.StringTokenizer;

public class DiscoveryServer {

	// porta nel range consentito 1024-65535!
	private static final int BOT_PORT = 1024;
	private static final int TOP_PORT = 65535;
	private static final String USAGE = "Usage: java DiscoveryServer [serverPort>1024] {[nomefileX] [portX>1024], ... }";

	public static void main(String[] args) {

		System.out.println("DiscoveryServer: avviato");

		DatagramSocket socket = null;

		DatagramPacket packet_C = null;
		DatagramPacket packet_RSS = null;

		byte[] buf = new byte[256];

		int port = -1;
		int actual_size = -1;

		File file = null;

		int[] porte = null;
		String[] nomiFile = null;

		{// Controllo argomenti
			if (args.length < 3 || ((args.length % 2) == 0)) { // se minori di 3 o pari errore
				System.out.println(USAGE);
				System.exit(1);
			}
			actual_size = (args.length - 1) / 2;
			porte = new int[actual_size];
			nomiFile = new String[actual_size];

			try {
				port = Integer.parseInt(args[0]);
				if (port < BOT_PORT || port > TOP_PORT) { // controllo che la porta sia nel range consentito 1024-65535
					System.out.println(USAGE);
					System.exit(2);
				}
				for (int i = 0; i < actual_size; i++) {
					porte[i] = Integer.parseInt(args[(i + 1) * 2]);
					// controllo che la porta sia nel range consentito 1024-65535
					if (porte[i] < BOT_PORT || porte[i] > TOP_PORT) {
						System.out.println(USAGE);
						System.exit(3);
					}
					for (int j = 0; j < i; j++) {
						if (porte[i] == porte[j]) {
							System.out.println("Porta ripetuta: " + porte[i]);
							System.exit(4);
						}
					}
					nomiFile[i] = args[i * 2 + 1];
					file = new File(nomiFile[i]);
					if (!file.exists() || !file.isFile()) {
						System.out.println("File non trovato: " + file.getAbsolutePath());
						System.exit(5);
					}
				}
			} catch (NumberFormatException e) {
				System.out.println("Usage: java DiscoveryServer [serverPort>1024]");
				System.exit(6);
			}
		}

		for (int i = 0; i < actual_size; i++) {
			System.out.println("Avvio il thread: " + porte[i] + " " + nomiFile[i]);
			Thread t = new Thread(new RowSwapServer(porte[i], nomiFile[i]));
			t.start();
		}

		try {
			socket = new DatagramSocket(port);
			packet_C = new DatagramPacket(buf, buf.length);
			System.out.println("Creata la socket DiscoveryServer: " + socket);
		} catch (SocketException e) {
			System.out.println("Problemi nella creazione della socket: ");
			e.printStackTrace();
			System.exit(1);
		}

		try {
			String nomeFile = null;
			int porta = -1;
			String portaString = null;
			String richiesta = null;
			ByteArrayInputStream biStream = null;
			DataInputStream diStream = null;
			StringTokenizer st = null;
			byte[] data = null;

			while (true) {
				System.out.println("\nDiscovery_Server in attesa di richieste...");

				try { // ricezione del datagramma
					packet_C.setData(buf);
					socket.receive(packet_C);
				} catch (IOException e) {
					System.err.println("Problemi nella ricezione del datagramma: " + e.getMessage());
					e.printStackTrace();
					continue;
					// il server continua a fornire il servizio ricominciando dall'inizio
					// del ciclo
				}

				try { // lettura datagramma
					biStream = new ByteArrayInputStream(packet_C.getData(), 0, packet_C.getLength());
					diStream = new DataInputStream(biStream);
					richiesta = diStream.readUTF();

					st = new StringTokenizer(richiesta);
					nomeFile = st.nextToken();
					System.out.println("Richiesta porta relativa a questo file: " + nomeFile);
				} catch (Exception e) {
					System.err.println("Problemi nella lettura della richiesta: " + richiesta);
					e.printStackTrace();
					continue;
					// il server continua a fornire il servizio ricominciando dall'inizio
					// del ciclo
				}
				// System.out.println(Arrays.toString(porte));
				// System.out.println(Arrays.toString(nomiFile));
				int i;
				for (i = 0; i < actual_size; i++)
					if (nomeFile.equals(nomiFile[i])) {
						porta = porte[i];
						break;
					}
				
				if(porta==-1) {
					System.out.println("File richiesto non disponibile");
				}else {
					System.out.println("Porta relativa a questo file: " + porta);
				}
					
				byte[] buf2 = new byte[256];

				ByteArrayOutputStream boStream = null;
				DataOutputStream doStream = null;
				try { // invio datagram di risposta
					portaString = String.valueOf(porta);
					packet_RSS = new DatagramPacket(buf2, buf2.length, packet_C.getAddress(), packet_C.getPort());
					boStream = new ByteArrayOutputStream();
					doStream = new DataOutputStream(boStream);
					doStream.writeUTF(portaString);
					data = boStream.toByteArray();

					packet_RSS.setData(data, 0, data.length);
					socket.send(packet_RSS);

					System.out.println("Invio risposta: " + portaString);
				} catch (IOException e) {
					System.err.println("Problemi nell'invio della risposta: " + e.getMessage());
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
		}
		System.out.println("DiscoveryServer: termino...");
		socket.close();
	}
}