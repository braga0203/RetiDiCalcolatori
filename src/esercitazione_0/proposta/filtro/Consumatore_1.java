package esercitazione_0.proposta.filtro;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

// Consumatore e' un filtro
public class Consumatore_1 {
	public static void main(String[] args) {
		FileReader r = null;
		char ch;
		int x;
		Boolean check=true;
		if (args.length != 2){
			System.out.println("Utilizzo: consumatore <inputFilename>");
			System.exit(0);
		}
		int[] prefixString_values= args[0].chars().toArray();
		int prefixString_size= prefixString_values.length;
		try {
			r = new FileReader(args[1]);
		} catch(FileNotFoundException e){
			System.out.println("File non trovato");
			System.exit(1);
		}
		try {

			while ((x = r.read()) >= 0) { 
				check=true;
				ch = (char) x;
				for(int i=0;check && i<prefixString_size;i++)
					if(prefixString_values[i]==x)
						check=false;
				if(check)
					System.out.print(ch);
			}
			r.close();
		} catch(IOException ex){
			System.out.println("Errore di input");
			System.exit(2);
		}
}}
