package esercitazione_5.proposta.estensione;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.Arrays;

public class Esito implements Serializable {
	/**
	 * 
	 */
	public InetAddress server_adr;
	public int server_port;
	public Record[] dir;
	
	public Esito(InetAddress adr, int port,Record[] r) {
		this.server_adr=adr;
		this.server_port=port;
		this.dir=r;
	}

	@Override
	public String toString() {
		return "Esito [server_adr=" + server_adr + ", server_port=" + server_port + ", dir=" + Arrays.toString(dir)
				+ "]";
	}
	
	
	
	
}
