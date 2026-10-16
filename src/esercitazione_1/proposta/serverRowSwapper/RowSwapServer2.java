package esercitazione_1.proposta.serverRowSwapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;


public class RowSwapServer2 {
	static String prefix = "[RowSwapServer]: ";
	private static final int ISCRIVIMI = 1;
	private static final int CANCELLAMI = 0;
	private static final String USAGE = "RS IPDS portDS portRS nomeFile";

	public static void main(String[] args) {

		InetAddress addr_DS = null;
		int port_DS = -1;
		int port_RSS = -1;
		String fileName = null;

		boolean registrato = false;
		boolean terminato = false;

		int rispostaServizio = -1;

		DatagramSocket socket_DS = null;

		DatagramPacket packet_DS = null;
		byte[] data = null;
		byte[] buf = null;

		ByteArrayOutputStream boStream = null;
		DataOutputStream doStream = null;
		ByteArrayInputStream biStream = null;
		DataInputStream diStream = null;

		BufferedReader stdIn = null;

		Thread t = null;
		
		{// controllo parametri OK
			try {
				if (args.length == 4) {
					addr_DS = InetAddress.getByName(args[0]);
					port_DS = Integer.parseInt(args[1]);
					port_RSS = Integer.parseInt(args[2]);
					fileName = args[3];
				} else {
					System.out.println(USAGE);
					System.exit(1);
				}
			} catch (UnknownHostException e) {
				System.out.println(prefix + " Errore parametri invocazione : ");
				e.printStackTrace();
				System.out.println(prefix + "interrompo...");
				System.exit(2);
			}
			System.out.println(prefix + "avviato... ");
		}

		{// Creazione socket comunicazione DS
			try {
				socket_DS = new DatagramSocket();
				// socket_DS.setSoTimeout(300000);
			} catch (SocketException e) {
				System.out.println(prefix + "Problemi nella creazione della socket: ");
				e.printStackTrace();
				System.out.println("RowSwapServer: interrompo...");
				System.exit(1);
			} finally {
				System.out.println(prefix + "Socket comunicazione Discovery Server:" + socket_DS);
			}
		}

		try {
			while (!registrato) { // Comunicazione tra CLIENT E DISCOVERY SERVER
				{// Invio tentativo di iscrizione al DS
					data = null;
					buf = new byte[256];
					packet_DS = new DatagramPacket(buf, buf.length, addr_DS, port_DS);

					try {
						boStream = new ByteArrayOutputStream();
						doStream = new DataOutputStream(boStream);

						doStream.writeInt(ISCRIVIMI);
						doStream.writeUTF(fileName);
						doStream.writeInt(port_RSS);

						data = boStream.toByteArray();
						packet_DS.setData(data);
						socket_DS.send(packet_DS);
						boStream.close();
						doStream.close();
					} catch (IOException e) {
						System.out.println(prefix + "Problemi nell'invio della richiesta: ");
						continue;
					} finally {
						System.out.println(
								prefix + "Tentativo di ISCRIZIONE al DiscoveryServer " + addr_DS + ":" + port_DS);
					}
				}
				{// Ricezione risposta iscrizione
					buf = new byte[256];
					packet_DS = new DatagramPacket(buf, buf.length);
					try {// settaggio del buffer di ricezione
						socket_DS.receive(packet_DS);
						biStream = new ByteArrayInputStream(packet_DS.getData(), 0, packet_DS.getLength());
						diStream = new DataInputStream(biStream);
						rispostaServizio = diStream.readInt();
						biStream.close();
						diStream.close();
					} catch (NumberFormatException n) {
						System.out.println(prefix + "Problemi nella lettura del datagramma: ");
						n.printStackTrace();
						continue;
					} catch (IOException e) {
						System.out.println(prefix + "Problemi nella ricezione del datagramma: ");
						e.printStackTrace();
						continue;
					} finally {
						System.out.print(prefix + "ISCRIZIONE al Discovery Server ");
					}
					if (rispostaServizio != 0) {
						System.out.println("fallita");
						continue;
					} else
						System.out.println("avvenuta con successo");

				}
				registrato = true;
			}

			// Creao thread gestione swapping
			System.out.println(prefix + "Avvio Thread per gestione riga Comando");
			t = new Thread(new HandlerRichiesteClient(port_RSS, fileName));
			t.start();

			while (!terminato) { // Comunicazione tra CLIENT E DISCOVERY SERVER
				{ // Input che precede la chiusura del processo

					stdIn = new BufferedReader(new InputStreamReader(System.in));
					System.out.println(prefix + "Vuoi terminare (S/N + Invio):");
					while (!stdIn.readLine().equalsIgnoreCase("S")) {
						System.out.println("Vuoi terminare premi (S):");
					}
					System.out.println(prefix+"Termino il Thread [HandlerRichiesteClient]");
					t.interrupt();	
					try {
						t.join();
					}catch(InterruptedException e) {
						e.printStackTrace();
					}
				}
				{// Invio tentativo di cancellazione al DS
					data = null;
					buf = new byte[256];
					packet_DS = new DatagramPacket(buf, buf.length, addr_DS, port_DS);

					try {
						boStream = new ByteArrayOutputStream();
						doStream = new DataOutputStream(boStream);

						doStream.writeInt(CANCELLAMI);
						doStream.writeUTF(fileName);
						doStream.writeInt(port_RSS);

						data = boStream.toByteArray();
						packet_DS.setData(data);
						socket_DS.send(packet_DS);
						boStream.close();
						doStream.close();
					} catch (IOException e) {
						System.out.println("Problemi nell'invio della richiesta: ");
						continue;
					} finally {
						System.out.println(
								prefix + "Tentativo di CANCELLAZIONE al DiscoveryServer " + addr_DS + ":" + port_DS);
					}

				}
				{// Ricezione risposta cancellazione
					buf = new byte[256];
					packet_DS = new DatagramPacket(buf, buf.length);
					try {// settaggio del buffer di ricezione
						socket_DS.receive(packet_DS);
						biStream = new ByteArrayInputStream(packet_DS.getData(), 0, packet_DS.getLength());
						diStream = new DataInputStream(biStream);
						rispostaServizio = diStream.readInt();
						biStream.close();
						diStream.close();
					} catch (IOException e) {
						System.out.println("Problemi nella ricezione del datagramma: ");
						e.printStackTrace();
						continue;
					} finally {
						System.out.print(prefix + "CANCELLAZIONE dal Discovery Server ");
					}
					if (rispostaServizio != 0) {
						System.out.println("fallita");
						continue;
					} else
						System.out.println("avvenuta con successo");
				}
				socket_DS.close();
				terminato = true;
				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println(prefix+"terminato...");
		
	}

}
