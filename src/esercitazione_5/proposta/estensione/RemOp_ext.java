package esercitazione_5.proposta.estensione;

import java.net.InetAddress;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemOp_ext extends Remote{
	Record[] client_passivo(InetAddress client, int port,String dir_name) throws RemoteException;
	Esito client_attivo(String dir_name) throws RemoteException;
}
