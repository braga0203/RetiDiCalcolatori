package esercitazione_5.proposta.estensione;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;



public class ServerImpl_ext extends UnicastRemoteObject implements RemOp_ext {
	static final String STORAGE = "src/file/server";
	static final int server_port = 5000;

	ServerSocket serverSocket = null;

	protected ServerImpl_ext() throws RemoteException {
		super();
	}

	@Override
	public Record[] client_passivo(InetAddress client, int port, String dir_name) throws RemoteException {
		try {
			new Server_T(client, port).start();
		} catch (Exception e) {
			System.err.println(" problemi nel server thread: " + e.getMessage());
			e.printStackTrace();
		}
		return getDirectory(dir_name);
	}

	@Override
	public Esito client_attivo(String dir_name) throws RemoteException {
		Record[] r = this.getDirectory(dir_name);
		return new Esito(this.serverSocket.getInetAddress(), server_port, r);
	}

	// Avvio del Server RMI
	public static void main(String[] args) {

		final int REGISTRYPORT = 1099;
		// Avvia il registry integrato direttamente nella JVM del Server

		String registryHost = "localhost";
		String serviceName = "Server"; // lookup name...

		ServerImpl_ext serverRMI = null;
		// Registrazione del servizio RMI
		String completeName = "//" + registryHost + ":" + REGISTRYPORT + "/" + serviceName;
		try {

			Registry registry = LocateRegistry.createRegistry(REGISTRYPORT);
			serverRMI = new ServerImpl_ext();
			Naming.rebind(completeName, serverRMI);
			System.out.println("Server RMI: Servizio \"" + serviceName + "\" registrato");
		} catch (Exception e) {
			System.err.println("Server RMI \"" + serviceName + "\": " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}

		// nuova destinazione
		File dirStorage = new File(STORAGE);
		if (!dirStorage.exists())
			dirStorage.mkdirs();

		try {
			serverRMI.serverSocket = new ServerSocket(server_port);
			serverRMI.serverSocket.setReuseAddress(true);
			System.out.println("Avviata server Socket.. ");
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

		Socket clientSocket = null;
		try {
			while (true) {
				System.out.println("in attesa di richieste...");
				try {
					// bloccante fino ad una pervenuta connessione
					clientSocket = serverRMI.serverSocket.accept();
					clientSocket.setSoTimeout(100000);
					System.out.println("connessione accettata: " + clientSocket);
				} catch (IOException e) {
					System.err.println("problemi nella accettazione della connessione: " + e.getMessage());
					e.printStackTrace();
					continue;
				}

				// serizio delegato ad un nuovo thread
				try {
					new Server_T(clientSocket).start();
				} catch (Exception e) {
					System.err.println(" problemi nel server thread: " + e.getMessage());
					e.printStackTrace();
					continue;
				}

			} // while
		}
		// qui catturo le eccezioni non catturate all'interno del while
		// in seguito alle quali il server termina l'esecuzione
		catch (Exception e) {
			e.printStackTrace();
			// chiusura di stream e socket
			try {serverRMI.serverSocket.close();} catch (IOException e1) {}
			System.out.println("PutFileServerCon: termino...");
			System.exit(2);
		}
	}

	private Record[] getDirectory(String dir_name) {
		Record[] rs = null;
		File dir = new File(dir_name);
		File f[] = dir.listFiles();
		int count = 0;
		if (f != null)
			for (int i = 0; i < f.length; i++)
				if (f[i].isFile())
					count++;
		rs = new Record[count];
		for (int i = 0; i < f.length; i++)
			if (f[i].isFile())
				rs[i] = new Record(f[i].getName(), f[i].length());

		System.out.println("Get directory: " + Arrays.toString(rs));
		return rs;

	}

}


class Server_T extends Thread {
	private boolean attivo;
	static final String storage = "src/file/server";
	public InetAddress client;
	public int port;

	Socket clientSocket = null;

	DataInputStream inSock = null;
	DataOutputStream outSock = null;
	
	public Server_T(Socket clientSocket) {//
		attivo=false;
		System.out.println("Thread Server passivo");
		this.clientSocket=clientSocket;
	}
	public Server_T(InetAddress client, int port) {// attivo
		attivo=true;
		System.out.println("Thread Server attivo");
		this.client = client;
		this.port = port;
	}

	public void run() {
		if(attivo) {
			try {
				this.clientSocket = new Socket(client, port);
				this.clientSocket.setSoTimeout(100000);
				inSock = new DataInputStream(this.clientSocket.getInputStream());
				outSock = new DataOutputStream(this.clientSocket.getOutputStream());
			} catch (IOException e) {
				System.out.println("Problemi nella creazione della socket: ");
				e.printStackTrace();
				return;
			}
		}
		try {
			inSock = new DataInputStream(this.clientSocket.getInputStream());
			outSock = new DataOutputStream(this.clientSocket.getOutputStream());
		} catch (IOException e) {
			System.out.println("Problemi nella creazione della socket: ");
			e.printStackTrace();
			return;
		}
		try {
			String dir = inSock.readUTF();
			System.out.println("Richiesta directory : " + dir);
			boolean terminato = false;
			FileInputStream inFile = null;
			String nomeFile = null;
			File curFile;
			while (!terminato) {
				try {
					nomeFile = inSock.readUTF();
					System.out.println("Richiesta file : " + nomeFile);
					curFile = new File(dir, nomeFile);
					inFile = new FileInputStream(curFile);
					FileUtility.trasferisci_a_byte_file_binario(new DataInputStream(inFile), outSock, curFile.length());
				} catch (EOFException eof) {
					terminato = true;
					continue;
				} catch (FileNotFoundException e) {
					System.out.println("Problemi nella creazione dello stream di input da File: ");
					e.printStackTrace();
					break;
				} catch (SocketTimeoutException ste) {
					ste.printStackTrace();
					break;
				} catch (IOException e) {
					e.printStackTrace();
					break;
				} catch (Exception e) {
					System.err.println("Problemi durante la lettura e l'invio del file: " + e.getMessage());
					e.printStackTrace();
					System.out.println("Terminata connessione con " + clientSocket);

				}
			}

			//
			System.out.println("Chiudo socket");
			clientSocket.shutdownInput();
			clientSocket.shutdownOutput();
			clientSocket.close();
			System.out.println("==============================================================");
		}
		// qui catturo le eccezioni non catturate all'interno del while
		// in seguito alle quali il server termina l'esecuzione
		catch (Exception e) {
			e.printStackTrace();
			System.out.println("Errore irreversibile, PutFileServerThread: termino...");
			System.exit(3);
		}
	}

}
