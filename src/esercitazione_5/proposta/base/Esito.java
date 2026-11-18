package esercitazione_5.proposta.base;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

public class Esito implements Serializable {
	public String nomeFileModificato;
	public long numeroRighe;
	
	public Esito(String nomeFileModificato) {
		super();
		this.nomeFileModificato = nomeFileModificato;
		this.numeroRighe = numeroRighe();
	}
	private long numeroRighe() {
		long numeroRighe=-1;
		try (InputStream is = new BufferedInputStream(
				new FileInputStream(this.nomeFileModificato)
			)) {
            byte[] buffer = new byte[8192];
            int count = 0;
            int read;
            boolean haCaratteri = false;

            while ((read = is.read(buffer)) != -1) {
                haCaratteri = true;
                for (int i = 0; i < read; ++i) {
                    if (buffer[i] == '\n') {
                        count++;
                    }
                }
            }
            // Gestisce l'eventuale ultima riga senza \n finale
            numeroRighe = (count == 0 && haCaratteri) ? 1 : count;
            System.out.println("Numero di righe: " + numeroRighe);
        } catch (IOException e) {
            e.printStackTrace();
        }
		return numeroRighe;
	}
	@Override
	public String toString() {
		return "Esito [nomeFileModificato=" + nomeFileModificato + ", numeroRighe=" + numeroRighe + "]";
	}
	
	
	
}
