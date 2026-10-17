package esercitazione_2.proposta.multipleFile;

/* FileUtility.java */

import java.io.*;

public class FileUtility {
	private static final String prefix="[FileUtility]: "; 
	/**
	 * Nota: sorgente e destinazione devono essere correttamente aperti e chiusi
	 * da chi invoca questa funzione.
	 *  
	 */
	static protected void trasferisci_a_byte_file_binario(DataInputStream src,
			DataOutputStream dest, long size) throws IOException {
	
		// ciclo di lettura da sorgente e scrittura su destinazione
	    int buffer;   
	    int i=0;
	    try {
    		System.out.println(prefix+"Inizio trasferisci_a_byte_file_binario");
    		for(;i<size;i++){
    	    	buffer=src.read();
	    		dest.write(buffer);
	    		if(buffer<0) {
	        		System.out.println(prefix+"Errore");
	    			break;
	    		}
	    	}
    		System.out.println(prefix+"Fine trasferisci_a_byte_file_binario\t\tsize:"+size+"\t\ttrasf:"+i);
	    	dest.flush();
	    }
	    catch (EOFException e) {
	    	System.out.println(prefix+"Problemi, i seguenti: ");
	    	e.printStackTrace();
	    }
	}
}