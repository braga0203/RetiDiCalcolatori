package esercitazione_1.proposta.serverRowSwapper;

import java.net.InetAddress;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Monitor {

	int MAX_N_RSS;
	int N_RSS;
	private Lock lock; 
    // Conditions for coordination
    private Condition[] used;
	int[] porte;
	String[] indirizzi;
	String[] nomiFile;
	
	public Monitor(int n_server) {
		super();
		MAX_N_RSS = n_server;
		N_RSS=0;
		
		porte = new int[MAX_N_RSS];
		nomiFile = new String[MAX_N_RSS];	
		indirizzi = new String[MAX_N_RSS];
		
		lock=new ReentrantLock();
		used=new Condition[MAX_N_RSS];
		for(int i =0;i<MAX_N_RSS;i++) {
			porte[i]=-1;
			nomiFile[i]=null;
			used[i]=lock.newCondition();
		}
		
	}
	public String portaRSSof(String richiesta) throws IllegalArgumentException{
		//lock.lock();
		//try {
		//	while () {
		//} finally {
		//	lock.unlock();
		//}
		for (int i=0;i<this.N_RSS;i++) {
			if(richiesta.equals(this.nomiFile[i]))
				return ""+indirizzi[i]+" "+porte[i];
		}
		return null;
	}
	
	public int iscrizione(String nomeFile, int porta_RSS, String ip_RSS) throws IllegalArgumentException{
		if(this.N_RSS<this.MAX_N_RSS) {
			for(int i=0;i<this.N_RSS;i++) 
				if(nomeFile.equals(this.nomiFile[i]))
					return 1;
			this.nomiFile[this.N_RSS]=nomeFile;
			this.porte[this.N_RSS]=porta_RSS;
			this.indirizzi[this.N_RSS]=ip_RSS;
			this.N_RSS++;
			return 0;
		}

		return 1;
	}
	public int cancellazione(String nomeFile) throws IllegalArgumentException{
		int idx=-1;
		for(int i=0;i<this.N_RSS;i++) 
			if(nomeFile.equals(this.nomiFile[i]))
				idx=i;
		if(idx==-1)
			return 1;
		
		this.nomiFile[idx]=null;
		this.porte[idx]=-1;
		this.N_RSS++;
		return 0;
	}
    
	
	
	
	
	
}
