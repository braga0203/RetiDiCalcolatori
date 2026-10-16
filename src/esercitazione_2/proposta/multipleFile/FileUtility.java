package esercitazione_2.proposta.multipleFile;

/* FileUtility.java */

import java.io.*;

public class FileUtility {

	/**
	 * Nota: sorgente e destinazione devono essere correttamente aperti e chiusi
	 * da chi invoca questa funzione.
	 *  
	 */
	static protected void trasferisci_a_byte_file_binario(DataInputStream src,
			DataOutputStream dest, long size) throws IOException {
	
		// ciclo di lettura da sorgente e scrittura su destinazione
	    int buffer;    
	    try {
	    	// esco dal ciclo all lettura di un valore negativo -> EOF
	    	// N.B.: la funzione consuma l'EOF
    		System.out.println("\tInizio\t trasferisci_a_byte_file_binario");
    		for(int i=0;i<size;i++){
    	    	buffer=src.read();
	    		dest.write(buffer);
	    	}
    		System.out.println("\tFine\t trasferisci_a_byte_file_binario\t\tsize:"+size);
	    	dest.flush();
	    }
	    catch (EOFException e) {
	    	System.out.println("Problemi, i seguenti: ");
	    	e.printStackTrace();
	    }
	}
}