package esercitazione_0.proposta.multipliFile;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

public class Produttore_2 {
	public static void main(String[] args) {		
		BufferedReader in = null;
		
		if (args.length < 1){
			System.out.println("Utilizzo: produttore <inputFilename>");
			System.exit(0);
		}
			
		System.out.println("Inserisci il messaggio formattato x:");
		in = new BufferedReader(new InputStreamReader(System.in));

		FileWriter fout[]=new FileWriter[args.length];
		
		int nFile_value;
		String nFile_string;
		String inputl = null;
		
		try {
			for(int i=0;i<args.length;i++) 
				fout[i] = new FileWriter(args[i]);
			
			while ((inputl = in.readLine()) != null) {
				nFile_string=inputl.substring(0, inputl.indexOf(":"));
				try {
					nFile_value= Integer.parseInt(nFile_string)-1;
					inputl+="\n";
					if(nFile_value<args.length)
						fout[nFile_value].write(inputl);
					
				}
				catch (NumberFormatException nfe) { 
					nfe.printStackTrace(); 
					System.exit(1); // uscita con errore, intero positivo a livello di sistema Unix
				}
			}
			for(int i=0;i<args.length;i++) 
				fout[i].close();
		} 
	    catch (IOException e) { 
			e.printStackTrace();
			System.exit(1); // uscipta con errore, intero positivo a livello di sistema Unix
		}
	}
}

