package esercitazione_1.proposta.serverRowSwapper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import esercitazione_1.svolta.LineUtility;

public class HandlerRichiesteClient implements Runnable {

	static String prefix = "[HandlerRichiesteClient]: ";
	int port = -1;
	String fileName = null;

	public HandlerRichiesteClient(int port, String fileName) {
		super();
		this.port = port;
		this.fileName = fileName;
	}

	@Override
	public void run() {

		InetAddress ip_C = null;
		int port_C = -1;

		int n1 = -1;
		int n2 = -1;
		int risultatoSwap = -1;

		DatagramSocket socket_C = null;
		DatagramPacket packet_C = null;
		byte[] data = null;
		byte[] buf = null;

		ByteArrayOutputStream boStream = null;
		DataOutputStream doStream = null;
		ByteArrayInputStream biStream = null;
		DataInputStream diStream = null;
		
		Boolean close=false;
		{// Creazione socket comunicazione C
			try {
				socket_C = new DatagramSocket(port);
				socket_C.setSoTimeout(5000);
			} catch (SocketException e) {
				System.out.println(prefix + "Problemi nella creazione della socket: ");
				e.printStackTrace();
				System.out.println(prefix + "interrompo...");
				System.exit(1);
			} finally {
				System.out.println(prefix + "Socket comunicazione Discovery Server:" + socket_C);
			}
		}
		while (true) { // Comunicazione tra CLIENT E DISCOVERY SERVER
			{// Ricezione di lista da DS
				buf = new byte[256];
				packet_C = new DatagramPacket(buf, buf.length);
				try {// settaggio del buffer di ricezione
					socket_C.receive(packet_C);
					ip_C = packet_C.getAddress();
					port_C = packet_C.getPort();
					biStream = new ByteArrayInputStream(packet_C.getData(), 0, packet_C.getLength());
					diStream = new DataInputStream(biStream);
					n1 = diStream.readInt();
					n2 = diStream.readInt();
					biStream.close();
					diStream.close();
					System.out.println(prefix + "Client "+ip_C+":"+port_C);
					System.out.println(prefix+"Richiede swap delle linee " + n1 + " e " + n2);			
				} catch (SocketTimeoutException e) {
					//System.out.println(prefix+"Timeout expired");
					if (Thread.currentThread().isInterrupted()) {// Controllo interrupt padre
						socket_C.close();
						close=true;
					}else 
						continue;
				} catch (IOException e) {
					System.out.println("Problemi nella ricezione del datagramma: ");
					e.printStackTrace();
					continue;
				}
			}
			if(close)
				break;
			{// Swapping delle righe
				try {
					File original = new File(this.fileName);
					File tempFile = new File(original.getParent(), "TMP_" + original.getName());
					Path p_tmp = tempFile.toPath();
					Path p = original.toPath();
					risultatoSwap = LineUtility.swapLines(original, tempFile, n1, n2);
					try {
						Files.move(p_tmp, p, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
					} catch (IOException e) {
						try {
							Files.move(p_tmp, p, StandardCopyOption.REPLACE_EXISTING);
						} catch (IOException ex) {
							ex.printStackTrace();
						}
					}
					System.out.print(prefix+"Swapping ");
					if(risultatoSwap==0) 
						System.out.println("avvenuto con successo");
					else {
						System.out.println("fallito");
						continue;
					}
				} catch (Exception e) {
					System.err.println(prefix+"Problemi nello swapping");
					e.printStackTrace();
					continue;
				}
			}
			{// Invio risultato servizio
				buf = new byte[256];
				packet_C = new DatagramPacket(buf, buf.length, ip_C, port_C);
				try {// settaggio del buffer di ricezione
					boStream = new ByteArrayOutputStream();
					doStream = new DataOutputStream(boStream);
					doStream.writeInt(risultatoSwap);
					data = boStream.toByteArray();
					packet_C.setData(data);
					socket_C.send(packet_C);
					biStream.close();
					diStream.close();
				} catch (IOException e) {
					System.out.println("Problemi nel invio del datagramma: ");
					e.printStackTrace();
					continue;
				} finally {
					System.out.print(prefix + "SWAPPING delle linee ");
					if (risultatoSwap == 0)
						System.out.println("avvenuto con successo");
					else
						System.out.println("fallito");
				}
			}
		}
		System.out.println(prefix+"terminato...");
	}
}
