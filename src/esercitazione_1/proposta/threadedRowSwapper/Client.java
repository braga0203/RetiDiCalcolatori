package esercitazione_1.proposta.threadedRowSwapper;

// LineClient.java

import java.io.*;
import java.net.*;

public class Client {

	public static void main(String[] args) {

		InetAddress addr = null;
		int port = -1;

		{// controllo parametri
			try {
				if (args.length == 2) {
					addr = InetAddress.getByName(args[0]);
					port = Integer.parseInt(args[1]);
					System.out.println(" " + port + ":" + addr);
				} else {
					System.out.println("Usage: java Client DiscoveryIP DiscoveryPort");
					System.exit(1);
				}
			} catch (UnknownHostException e) {
				System.out.println("Problemi nella determinazione dell'endpoint del server : ");
				e.printStackTrace();
				System.out.println("Client: interrompo...");
				System.exit(2);
			}
		}

		DatagramSocket socket = null;

		// creazione della socket datagram, settaggio timeout di 30s
		// e creazione datagram packet
		try {
			socket = new DatagramSocket();
			socket.setSoTimeout(30000);
			System.out.println("\nClient: avviato");
			System.out.println("Creata la socket: " + socket);
		} catch (SocketException e) {
			System.out.println("Problemi nella creazione della socket: ");
			e.printStackTrace();
			System.out.println("Client: interrompo...");
			System.exit(1);
		}

		BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
		System.out.print("\n^D(Unix)/^Z(Win)+invio per uscire, altrimenti inserisci nome file (con estensione): ");

		try {
			ByteArrayOutputStream boStream = null;
			DataOutputStream doStream = null;
			byte[] data = null;
			String nomeFile = null;
			String richiesta = null;
			String risposta = null;

			DatagramPacket packet_DS_TO = null;

			byte[] buf = new byte[256];

			packet_DS_TO = new DatagramPacket(buf, buf.length, addr, port);
			while ((nomeFile = stdIn.readLine()) != null) {

				richiesta = nomeFile;
				// riempimento e invio del pacchetto
				try {
					boStream = new ByteArrayOutputStream();
					doStream = new DataOutputStream(boStream);
					doStream.writeUTF(richiesta);
					data = boStream.toByteArray();
					packet_DS_TO.setData(data);
					socket.send(packet_DS_TO);

					System.out.println("Richiesta: "+richiesta);
					System.out.println("Inviata a " + addr + ", " + port);
				} catch (IOException e) {
					System.out.println("Problemi nell'invio della richiesta: ");
					e.printStackTrace();
					System.out.print(
							"\n^D(Unix)/^Z(Win)+invio per uscire, altrimenti inserisci nome file (con estensione): ");
					continue;
					// il client continua l'esecuzione riprendendo dall'inizio del ciclo
				}

				byte[] buf2 = new byte[256];
				DatagramPacket packet_DS_BACK = new DatagramPacket(buf2, buf2.length);
				try {
					// settaggio del buffer di ricezione
					packet_DS_BACK.setData(buf2);
					socket.receive(packet_DS_BACK);
					// sospensiva solo per i millisecondi indicati, dopodich� solleva una
					// SocketException
				} catch (IOException e) {
					System.out.println("Problemi nella ricezione del datagramma: ");
					e.printStackTrace();
					System.out.print(
							"\n^D(Unix)/^Z(Win)+invio per uscire, altrimenti inserisci nome file (con estensione): ");
					continue;
					// il client continua l'esecuzione riprendendo dall'inizio del ciclo
				}

				ByteArrayInputStream biStream = null;
				DataInputStream diStream = null;
				try {
					biStream = new ByteArrayInputStream(packet_DS_BACK.getData(), 0, packet_DS_BACK.getLength());
					diStream = new DataInputStream(biStream);
					risposta = diStream.readUTF();
					System.out.println("Risposta: "+risposta);
				} catch (IOException e) {
					System.out.println("Problemi nella lettura della risposta: ");
					e.printStackTrace();
					System.out.print(
							"\n^D(Unix)/^Z(Win)+invio per uscire, altrimenti inserisci nome file (con estensione): ");
					continue;
					// il client continua l'esecuzione riprendendo dall'inizio del ciclo
				}

				int n_porta = Integer.parseInt(risposta);
				if(n_porta==-1) {
					System.out.println("File non riconosciuto");
					System.out.print(
							"\n^D(Unix)/^Z(Win)+invio per uscire, altrimenti inserisci nome file (con estensione): ");
					
					continue;
				}
				DatagramPacket packet_RSS_TO = new DatagramPacket(buf2, buf2.length, addr, n_porta);

				int numLinea1, numLinea2;
				boolean ok = true;
				while (ok) {
					try {
						System.out.print("Numero della prima linea? ");
						numLinea1 = Integer.parseInt(stdIn.readLine());
						System.out.print("Numero della seconda linea? ");
						numLinea2 = Integer.parseInt(stdIn.readLine());
						richiesta = "" + numLinea1 + " " + numLinea2;
						ok = false;
					} catch (Exception e) {
						System.out.println("Problemi nell'interazione da console: ");
					}
				}

				boStream = new ByteArrayOutputStream();
				doStream = new DataOutputStream(boStream);
				try {
					doStream.writeUTF(richiesta);
					data = boStream.toByteArray();
					packet_RSS_TO.setData(data);
					socket.send(packet_RSS_TO);
					System.out.println("Richiesta: "+richiesta);
					System.out.println("Inviata a " + addr + ", " + n_porta);
				} catch (IOException e) {
					System.out.println("Problemi nell'invio della richiesta: ");
					e.printStackTrace();
					System.out.print(
							"\n^D(Unix)/^Z(Win)+invio per uscire, altrimenti inserisci nome file (con estensione): ");
					continue;
					// il client continua l'esecuzione riprendendo dall'inizio del ciclo
				}
				// tutto ok, pronto per nuova richiesta
				System.out.print(
						"\n^D(Unix)/^Z(Win)+invio per uscire, altrimenti inserisci nome file (con estensione): ");
			} // while
		}
		// qui catturo le eccezioni non catturate all'interno del while
		// in seguito alle quali il client termina l'esecuzione
		catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println("Client: termino...");
		socket.close();
	}
}