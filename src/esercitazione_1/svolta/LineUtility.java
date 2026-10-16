package esercitazione_1.svolta;

/*
 * LineUtility.java
 *
 * Classe usata per definire alcuni metodi utili nella selezione delle linee di
 * un file
 *
 */

import java.io.*;

public class LineUtility {
	/**
	 * metodo per recuperare una certa linea di un certo file
	 * 
	 * @param nomeFile
	 * @param numLinea
	 * @return linea letta o "Linea non trovata..."
	 */
	static String getLine(String nomeFile, int numLinea) {
		String linea = null;
		BufferedReader in = null;

		if (numLinea <= 0)
			return linea = "Linea non trovata: numero linea maggiore di 0.";
		// associazione di uno stream di input al file da cui estrarre le linee
		try {
			System.out.println("File cercato di aprire: " + nomeFile);
			in = new BufferedReader(new FileReader(nomeFile));
			System.out.println("File aperto: " + nomeFile);
		} catch (FileNotFoundException e) {
			System.out.println("File non trovato: ");
			e.printStackTrace();

			return linea = "File non trovato";
		}
		try {
			for (int i = 1; i <= numLinea; i++) {
				linea = in.readLine();
				if (linea == null) {
					linea = "Linea non trovata";
					in.close();
					return linea;
				}
			}
		} catch (IOException e) {
			System.out.println("Linea non trovata: ");
			e.printStackTrace();
			return linea = "Linea non trovata";
		}
		System.out.println("Linea selezionata: " + linea);

		try {
			in.close();
		} catch (IOException e) {
			System.out.println("Errore nella chiusura del reader");
			e.printStackTrace();
		}
		return linea;
	} // getLine

	/**
	 * metodo per recuperare la linea successiva di un file aperto in precedenza
	 * 
	 * @param in
	 * @return linea
	 */
	static String getNextLine(BufferedReader in) {
		String linea = null;
		try {
			if ((linea = in.readLine()) == null) {
				in.close();
				linea = "Nessuna linea disponibile";
			}
		} catch (FileNotFoundException e) {
			System.out.println("File non trovato: ");
			e.printStackTrace();
			return linea = "File non trovato";
		} catch (IOException e) {
			System.out.println("Problemi nell'estrazione della linea: ");
			e.printStackTrace();
			linea = "Nessuna linea disponibile";
		}
		return linea;
	} // getNextLine
	
	public static int swapLines(File ori,File tmp, int numLinea1, int numLinea2) {
		
		int top=-1;
		int bot=-1;
		String linea_bot=null;
		String linea_top=null;
		String linea=null;
		BufferedReader in = null;
		BufferedWriter out=null;
		FileReader R=null;
				
		if(numLinea1==numLinea2)
			return 0;
		top = (numLinea1 > numLinea2) ? numLinea1 : numLinea2;
		bot = (top == numLinea1) ? numLinea2 : numLinea1;
		
		try {
			R = new FileReader(ori);
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			in = new BufferedReader(R);
			int i=1;
			for(;i<=bot;i++) {
				linea_bot=in.readLine();
				//System.out.println("Prima lettura: "+linea_bot);
				if(linea_bot==null)
					return 1;
			}
			for(;i<=top;i++) {
				linea_top=in.readLine();
				//System.out.println("Prima lettura: "+linea_top);
				if(linea_top==null)
					return 2;
			}
			in.close();
		} catch (IOException e) {
			//System.out.println("Eccezione "+e.getMessage());
			e.getStackTrace();
			return -1;
		}
		try {
			FileReader R2=new FileReader(ori);
			FileWriter W2=new FileWriter(tmp);
			in = new BufferedReader(R2);
			out = new BufferedWriter(W2);
			
			int i=1;
			while((linea=in.readLine())!=null) {
				//System.out.println("Seconda lettura: "+linea+", i: "+i);
				if(i==bot)
					out.write(linea_top+"\n");
				else if(i==top)
					out.write(linea_bot+"\n");
				else
					out.write(linea+"\n");
				i++;
			}
			in.close();
			out.close();
		} catch (IOException e) {
			return -1;
		}
		return 0;
	}

	
	
	
	
	
	
	
	
	
	
	
	
	
}