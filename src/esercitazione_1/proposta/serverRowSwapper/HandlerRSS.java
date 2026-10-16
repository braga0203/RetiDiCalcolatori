package esercitazione_1.proposta.serverRowSwapper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class HandlerRSS implements Runnable {
	private final static String I = "ISCRIZIONE";
	private final static String C = "CANCELLAZIONE";
	int porta;
	Monitor m;
	String prefix ="[Handler_RSS]: ";

	public HandlerRSS(int porta, Monitor monitor) {
		super();
		this.m = monitor;
		this.porta=porta;
	}

	@Override
	public void run() {
		DatagramSocket socket_RSS = null;
		DatagramPacket packet_RSS = null;
		byte[] buf = null;
		byte[] data = null;

		String addr_RSS_str = null;
		InetAddress addr_RSS = null;
		int port_RSS = -1;
		String nomeFile = null;
		int portaServizio = -1;

		int servizioRichiesto = -1;
		int servizioRisultato = -1;
		String service = null;

		ByteArrayOutputStream boStream = null;
		DataOutputStream doStream = null;
		ByteArrayInputStream biStream = null;
		DataInputStream diStream = null;
		
		
		{// Creazione Socket
			try {
				socket_RSS = new DatagramSocket(porta);
				System.out.println(prefix+"Socket comunicazione Client "+socket_RSS);
			} catch (SocketException e) {
				System.out.println(prefix+"Problemi nella creazione della socket: ");
				e.printStackTrace();
				System.exit(1);
			}
		}
		try {
			while (true) {
				{// Ricezione tentativo di connessione
					buf = new byte[256];
					packet_RSS = new DatagramPacket(buf, buf.length);
					try {// settaggio del buffer di ricezione
						socket_RSS.receive(packet_RSS);
						port_RSS = packet_RSS.getPort();
						addr_RSS = packet_RSS.getAddress();
						addr_RSS_str = packet_RSS.getAddress().getHostAddress();
						biStream = new ByteArrayInputStream(packet_RSS.getData(), 0, packet_RSS.getLength());
						diStream = new DataInputStream(biStream);
						
						servizioRichiesto = diStream.readInt();
						nomeFile = diStream.readUTF();
						portaServizio = diStream.readInt();
						
						if (servizioRichiesto != 0 && servizioRichiesto != 1) {
							System.out.println(prefix+"Servizio non riconosciuto");
							continue;
						}
						if (nomeFile == null) {
							System.out.println(prefix+"Problemi nella lettura del datagramma: stringhe");
							continue;
						}
						biStream.close();
						diStream.close();
					} catch (IOException e) {
						System.out.println(prefix+"Problemi nella ricezione del datagramma: ");
						e.printStackTrace();
						continue;
					} finally {
						System.out.println(prefix+"RowSwapServer " + addr_RSS + ":" + port_RSS);
						if (servizioRichiesto == 1)
							service = I;
						else if (servizioRichiesto == 0)
							service = C;
						System.out.println(prefix+"Richiede servizio: " + service+", "+ nomeFile + ", " + portaServizio);
					}
				}

				{// Invio lista file
					buf = new byte[256];
					packet_RSS = new DatagramPacket(buf, buf.length, addr_RSS,port_RSS);
					try {
						if (servizioRichiesto == 1)
							servizioRisultato = m.iscrizione(nomeFile, portaServizio, addr_RSS_str);
						else
							servizioRisultato = m.cancellazione(nomeFile);
						boStream = new ByteArrayOutputStream();
						doStream = new DataOutputStream(boStream);
						doStream.writeInt(servizioRisultato);
						data = boStream.toByteArray();
						packet_RSS.setData(data);
						socket_RSS.send(packet_RSS);
						boStream.close();
						doStream.close();
					} catch (IOException e) {
						System.out.println(prefix+"Problemi nell'invio della risposta: ");
						continue;
						
					} finally {
						System.out.print(""+prefix+service+" ");
						if(servizioRisultato==0) 
							System.out.println("avvennuta con successo");
						else {
							System.out.println("fallita");
							continue;
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
