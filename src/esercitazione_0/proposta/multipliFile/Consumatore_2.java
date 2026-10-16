package esercitazione_0.proposta.multipliFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

// Consumatore e' un filtro
public class Consumatore_2 {
	public static void main(String[] args) {

		if (args.length < 2) {
			System.out.println("Utilizzo: consumatore <inputFilename>");
			System.exit(0);
		}
		FileReader r[] = new FileReader[args.length - 1];
		FileWriter fout[] = new FileWriter[args.length - 1];
		try {
			for (int i = 0; i < args.length - 1; i++) {
				r[i] = new FileReader(args[i + 1]);
				fout[i] = new FileWriter("appoggio" + i + ".txt");
			}
		} catch (FileNotFoundException e) {
			System.out.println("File non trovato");
			System.exit(1);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(2);
		}
		int[] prefixString_values = args[0].chars().toArray();
		int prefixString_size = prefixString_values.length;
		Thread ts[] = new Thread[args.length - 1];
		for (int i = 0; i < args.length - 1; i++) {
			int task = i;
			ts[i] = new Thread(() -> {
				char ch;
				int x;
				Boolean check = true;
				try {
					while ((x = r[task].read()) >= 0) {
						check = true;
						ch = (char) x;
						for (int j = 0; check && j < prefixString_size; j++)
							if (prefixString_values[j] == x)
								check = false;
						if (check)
							fout[task].write(ch);
					}
					fout[task].close();
					r[task].close();
					FileReader r2 = new FileReader("appoggio" + task + ".txt");
					FileWriter fout2 = new FileWriter(args[task + 1]);

					while ((x = r2.read()) >= 0)
						fout2.write((char) x);
					fout2.close();
					r2.close();
					new File("appoggio" + task + ".txt").delete();
				} catch (IOException ex) {
					System.out.println("Errore di input");
					System.exit(2);
				}
			});
			ts[i].start();
		}

		try {
			for (int i = 0; i < args.length - 1; i++)
				ts[i].join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
