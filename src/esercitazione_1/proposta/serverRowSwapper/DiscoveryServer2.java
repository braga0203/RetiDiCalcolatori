package esercitazione_1.proposta.serverRowSwapper;

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

public class DiscoveryServer2 {
	// porta nel range consentito 1024-65535!
	private static final int BOT_PORT = 1024;
	private static final int TOP_PORT = 65535;
	private static final String USAGE = "Usage: java DiscoveryServer [portaRichiesteClient>1024] [portaRegistrazioneRS>1024]";
	static String prefix="[Discovery Server]:";
	public static void main(String[] args) {

		
		int port_RSS = -1;
		int port_C = -1;

		{// Controllo argomenti
			if (args.length != 2) {
				System.out.println(USAGE);
				System.exit(1);
			}
			try {
				port_C = Integer.parseInt(args[0]);
				if (port_C < BOT_PORT || port_C > TOP_PORT) {
					System.out.println(USAGE);
					System.exit(2);
				}
				port_RSS = Integer.parseInt(args[1]);
				if (port_RSS < BOT_PORT || port_RSS > TOP_PORT) {
					System.out.println(USAGE);
					System.exit(2);
				}
			} catch (NumberFormatException e) {
				System.out.println(USAGE);
				System.exit(6);
			}
		}

		System.out.println(prefix+" avviato con RSS_PORT:"+port_RSS+" e C_PORT:"+port_C);

		Monitor m = new Monitor(10);

		Thread gestioneRSS = new Thread(new HandlerRSS(port_RSS, m));
		gestioneRSS.start();
		Thread gestioneC = new Thread(new HandlerC(port_C, m));
		gestioneC.start();

	}
}
