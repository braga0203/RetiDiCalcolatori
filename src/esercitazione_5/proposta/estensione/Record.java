package esercitazione_5.proposta.estensione;

import java.io.Serializable;

public class Record implements Serializable{
		public String name;
		public long size;
		
	public Record(String name, long size) {
			super();
			this.name = name;
			this.size = size;
	}

	@Override
	public String toString() {
		return "Record [name=" + name + ", size=" + size + "]";
	}
		
}