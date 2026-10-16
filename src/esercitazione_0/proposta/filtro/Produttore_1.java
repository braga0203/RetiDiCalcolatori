package esercitazione_0.proposta.filtro;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

// Produttore NON e' un filtro
public class Produttore_1 {
	public static void main(String[] args) {		
		BufferedReader in = null;
		
		if (args.length != 1){
			System.out.println("Utilizzo: produttore <inputFilename>");
			System.exit(0);
		}
			
		System.out.println("Inserisci il messaggio");
		in = new BufferedReader(new InputStreamReader(System.in));

		int input;
		FileWriter fout;
		try {

			fout = new FileWriter(args[0]);
			while((input=in.read())>=0) {// overhead della madonna
				fout.write(input);
			}		
			fout.close();
		} 
	    catch (IOException e) { 
			e.printStackTrace();
			System.exit(1); // uscita con errore, intero positivo a livello di sistema Unix
		}
	}
}

