package esercitazione_5.proposta.base;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemOp extends Remote {
	int conta_righe(String nomeFile, int inf) throws RemoteException;
	Esito elimina_riga(String nomeFile, int numero_riga) throws RemoteException;
}
