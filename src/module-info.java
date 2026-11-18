/**
 * 
 */
/**
 * 
 */
module RetiDiCalcolatori {
	requires java.rmi;
	
	// Permette al modulo java.rmi di accedere all'interfaccia remota e ai tipi di ritorno
    exports esercitazione_5.svolta to java.rmi;
    exports esercitazione_5.proposta.base to java.rmi;
    exports esercitazione_5.proposta.estensione to java.rmi;
}