package esercitazione_1.proposta.serverRowSwapper;

// LineClient.java

import java.io.*;
import java.net.*;
import java.util.StringTokenizer;

public class Client2 {
	static String prefix="[Client]: ";
	static String USAGE="Usage: java Client DiscoveryIP DiscoveryPort";
	public static void main(String[] args) {

		boolean connessione_DS = false;
		boolean inputUtente1 = false;
		boolean inputUtente2 = false;
		boolean connessione_RSS = false;

		int tentativi = 0;
		String nomeFile = null;
		
		String numlinea1_str = null;
		String numlinea2_str = null;
		int numlinea1 = -1;
		int numlinea2 = -1;

		int risultatoSwap=-1;
		
		InetAddress addr_DS = null;
		int port_DS = -1;
		InetAddress addr_RSS = null;
		int port_RSS = -1;
		String addr_RSS_str = null;
		
		DatagramSocket socket_DS = null;

		DatagramPacket packet_DS = null;
		DatagramPacket packet_RSS = null;
		byte[] data = null;
		byte[] buf = null;

		String lista= null;

		ByteArrayOutputStream boStream = null;
		DataOutputStream doStream = null;
		ByteArrayInputStream biStream = null;
		DataInputStream diStream = null;

		BufferedReader stdIn = null;
		StringTokenizer st = null;

		{// controllo parametri OK
			try {
				if (args.length == 2) {
					addr_DS = InetAddress.getByName(args[0]);
					port_DS = Integer.parseInt(args[1]);
				} else {
					System.out.println(USAGE);
					System.exit(1);
				}
			} catch (UnknownHostException e) {
				System.out.println(prefix+"Problemi nella determinazione dell'endpoint del server : ");
				e.printStackTrace();
				System.out.println(prefix+"interrompo...");
				System.exit(2);
			}
			System.out.println(prefix+"avviato...");
		}

		{// Creazione socket comunicazione DS
			try {
				socket_DS = new DatagramSocket();
				//socket_DS.setSoTimeout(300000);
				System.out.println(prefix+"Socket comunicazione Discovery Server: " + socket_DS);
			} catch (SocketException e) {
				System.out.println(prefix+"Problemi nella creazione della socket: ");
				e.printStackTrace();
				System.out.println(prefix+"interrompo...");
				System.exit(1);
			}
		}
		

		try {
			while (!connessione_DS) { // Comunicazione tra CLIENT E DISCOVERY SERVER
				{// Invio tentativo di connessione con DS
					data = null;
					buf = new byte[256];
					packet_DS = new DatagramPacket(buf, buf.length, addr_DS, port_DS);
					try {
						boStream = new ByteArrayOutputStream();
						doStream = new DataOutputStream(boStream);
						doStream.writeInt(tentativi);
						data = boStream.toByteArray();
						packet_DS.setData(data);
						socket_DS.send(packet_DS);
						boStream.close();
						doStream.close();
					} catch (IOException e) {
						System.out.println(prefix+"Problemi nell'invio della richiesta: ");
						tentativi++;
						continue;
					} finally {
						System.out.println(prefix+"Connessione con Discovery Server al tentativo "+tentativi);
					}
				}
				{// Ricezione di lista da DS
					buf = new byte[256];
					packet_DS = new DatagramPacket(buf, buf.length);
					try {// settaggio del buffer di ricezione
						socket_DS.receive(packet_DS);
						biStream = new ByteArrayInputStream(packet_DS.getData(), 0, packet_DS.getLength());
						diStream = new DataInputStream(biStream);
						lista = diStream.readUTF();
						biStream.close();
						diStream.close();
					} catch (IOException e) {
						System.out.println(prefix+"Problemi nella ricezione del datagramma con lista ");
						e.printStackTrace();
						continue;
					} finally {
						System.out.println(prefix+"Lista di files: "+lista);
					}
				}
				connessione_DS = true;
			}

			while (!inputUtente1) { // Input Utente per stabile ENDPOINT RSS
				stdIn = new BufferedReader(new InputStreamReader(System.in));
				{// Invio nome File a DS
					System.out.println(prefix+"Inserisci nome file tra quelli proposti(+invio): ");
					nomeFile = stdIn.readLine();

					data = null;
					buf = new byte[256];
					packet_DS = new DatagramPacket(buf, buf.length, addr_DS, port_DS);
					try {
						boStream = new ByteArrayOutputStream();
						doStream = new DataOutputStream(boStream);
						doStream.writeUTF(nomeFile);
						data = boStream.toByteArray();
						packet_DS.setData(data);
						socket_DS.send(packet_DS);
					} catch (IOException e) {
						System.out.println(prefix+"Problemi nell'invio della richiesta: ");
						e.printStackTrace();
						continue;
					} finally {
						System.out.println(prefix+"Richiesto end-point relativo a file "+nomeFile);
					}
				}
				{// Ricezione end-point RSS
					buf = new byte[256];
					packet_DS = new DatagramPacket(buf, buf.length);
					try {
						socket_DS.receive(packet_DS);
						biStream = new ByteArrayInputStream(packet_DS.getData(), 0, packet_DS.getLength());
						diStream = new DataInputStream(biStream);
						addr_RSS_str=diStream.readUTF();
						addr_RSS = InetAddress.getByName(addr_RSS_str);
						port_RSS = diStream.readInt();
						biStream.close();
						diStream.close();
					} catch (UnknownHostException host) {
						System.out.println(prefix+"Problemi nel interpretazione dell'IP ");
						host.printStackTrace();
						continue;
					} catch (IOException e) {
						System.out.println(prefix+"Problemi nella ricezione del datagramma: ");
						e.printStackTrace();
						continue;
					} finally {
						System.out.println(prefix+"Endpoint RowSwapperServer " + addr_RSS + ":" + port_RSS);
					}
				}
				inputUtente1 = true;
			}

			while (!inputUtente2) {// Input Utente per stabile righe
				{
					try {
						stdIn = new BufferedReader(new InputStreamReader(System.in));
						System.out.print(prefix+"Numero (maggiore di 0) della prima linea? ");
						numlinea1_str = stdIn.readLine();
						numlinea1=Integer.parseInt(numlinea1_str);	
						if (numlinea1 < 1)
							continue;
						System.out.print(prefix+"Numero (maggiore di 0)) della seconda linea? ");

						numlinea2_str = stdIn.readLine();
						numlinea2=Integer.parseInt(numlinea2_str);	
						if (numlinea1 < 1)
							continue;			
						stdIn.close();
					} catch (NumberFormatException e) {
						System.out.println(prefix+"Problemi nell'interazione da console: ");
						e.printStackTrace();
						continue;
					} catch (IOException e) {
					    System.out.println(prefix + "Problemi nell'interazione da console: ");
					    e.printStackTrace();
					    continue;
					}
				}
				inputUtente2 = true;
			
				{// Invio tentativo di connessione con DS
					data = null;
					buf = new byte[256];
					packet_RSS = new DatagramPacket(buf, buf.length, addr_RSS, port_RSS);
					try {
						boStream = new ByteArrayOutputStream();
						doStream = new DataOutputStream(boStream);
						doStream.writeInt(numlinea1);
						doStream.writeInt(numlinea2);
						data = boStream.toByteArray();
						packet_RSS.setData(data);
						socket_DS.send(packet_RSS);
					} catch (Exception e) {
						System.out.println(prefix+"Problemi nell'invio della richiesta: ");
						continue;
					} finally {
						System.out.println(prefix+"Richiestp swap di " + numlinea1 + " e " + numlinea2);
					}
				}
				{// Ricezione di lista da DS
					buf = new byte[256];
					packet_RSS = new DatagramPacket(buf, buf.length);
					try {// settaggio del buffer di ricezione
						socket_DS.receive(packet_RSS);
						biStream = new ByteArrayInputStream(packet_RSS.getData(), 0, packet_RSS.getLength());
						diStream = new DataInputStream(biStream);
						risultatoSwap = diStream.readInt();
						biStream.close();
						diStream.close();
					} catch (IOException e) {
						System.out.println(prefix+"Problemi nella ricezione del datagramma: ");
						e.printStackTrace();
						continue;
					} finally {
						System.out.print(prefix+"Swap ");
						if(risultatoSwap==0) {
							System.out.println("avvenuto con successo");
						}
						else
							System.out.println("fallito");
					}
				}
				connessione_RSS = true;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("Client: termino...");
	}
}