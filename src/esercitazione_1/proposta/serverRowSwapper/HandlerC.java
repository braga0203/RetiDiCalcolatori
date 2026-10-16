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
import java.util.Arrays;
import java.util.StringTokenizer;

public class HandlerC implements Runnable {
	String prefix = "[Handler_C]: ";
	int porta=-1;
	Monitor m;
	

	public HandlerC(int porta, Monitor monitor) {
		super();
		this.m = monitor;
		this.porta = porta;
	}

	@Override
	public void run() {

		DatagramSocket socket_C = null;
		DatagramPacket packet_C = null;
		byte[] buf = null;
		byte[] data = null;
		
		int tentativi=-1;
		InetAddress addr_C=null;
		int port_C=-1;
		
		int port_RSS=-1;
		String addr_RSS=null;
		
		String endpoint=null;
		
		ByteArrayOutputStream boStream = null;
		DataOutputStream doStream = null;
		ByteArrayInputStream biStream = null;
		DataInputStream diStream = null;
		StringTokenizer st =null;
		
		Boolean connessione = false;
		String risposta = null;
		String nomeFile = null;
		{// Creazione Socket
			try {
				socket_C = new DatagramSocket(porta);
				System.out.println(prefix+"Socket comunicazione Client: " + socket_C);
			} catch (SocketException e) {
				System.out.println(prefix+"Problemi nella creazione della socket: ");
				e.printStackTrace();
				System.exit(1);
			}
		}
		try {
			while (!connessione) {

				{// Ricezione tentativo di connessioe
					buf = new byte[256];
					packet_C = new DatagramPacket(buf, buf.length);
					try {// settaggio del buffer di ricezione
						socket_C.receive(packet_C);
						port_C=packet_C.getPort();
						addr_C=packet_C.getAddress();
						biStream = new ByteArrayInputStream(packet_C.getData(), 0, packet_C.getLength());
						diStream = new DataInputStream(biStream);
						tentativi = diStream.readInt();
						biStream.close();
						diStream.close();
					} catch (IOException e) {
						System.out.println(prefix+"Problemi nella ricezione del datagramma: ");
						e.printStackTrace();
						continue;
					} finally {
						System.out.println(prefix+"Richiesta Client letta al Tentativo: " + tentativi);
					}
				}

				{// Invio conferma
					risposta = Arrays.toString(m.nomiFile);
					buf = new byte[256];
					packet_C = new DatagramPacket(buf, buf.length, addr_C, port_C);
					
					try {
						boStream = new ByteArrayOutputStream();
						doStream = new DataOutputStream(boStream);
						doStream.writeUTF(risposta);
						data = boStream.toByteArray();
						packet_C.setData(data);
						socket_C.send(packet_C);
						boStream.close();
						doStream.close();
					} catch (IOException e) {
						System.out.println(prefix+"Problemi nell'invio della richiesta: ");
						continue;
					} finally {
						System.out.println(prefix+"Files disponibili: " + risposta);
					}
				}
				
				{// Ricezione file selezionato
					buf = new byte[256];
					packet_C = new DatagramPacket(buf,buf.length);
					try {// settaggio del buffer di ricezione
						socket_C.receive(packet_C);
						biStream = new ByteArrayInputStream(packet_C.getData(), 0, packet_C.getLength());
						diStream = new DataInputStream(biStream);
						nomeFile = diStream.readUTF();
						biStream.close();
						diStream.close();
					} catch (IOException e) {
						System.out.println("Problemi nel invio del datagramma: ");
						e.printStackTrace();
						continue;
					} finally {
						System.out.println(prefix+"Richiesta file: "+nomeFile);
					}
					
				}
				{// Invio porta
					buf = new byte[256];
					packet_C = new DatagramPacket(buf, buf.length, addr_C, port_C);
					try {
						boStream = new ByteArrayOutputStream();
						doStream = new DataOutputStream(boStream);
						endpoint=m.portaRSSof(nomeFile); //Richiesta Monitor
						st= new StringTokenizer(endpoint);

						addr_RSS=st.nextToken();
						port_RSS=Integer.valueOf(st.nextToken());
						doStream.writeUTF(addr_RSS);
						doStream.writeInt(port_RSS);
						data = boStream.toByteArray();
						packet_C.setData(data);
						socket_C.send(packet_C);
						boStream.close();
						doStream.close();
					} catch (NumberFormatException e) {
						System.out.println(prefix+"Problemi nell'invio della richiesta: ");
						continue;
					
					} catch (IOException e) {
						System.out.println(prefix+"Problemi nell'invio della richiesta: ");
						continue;
					} finally {
						System.out.println(prefix+"Invio porta RSS: "+port_RSS);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}