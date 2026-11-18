/* Server Select
 * 	Un solo figlio per tutti i file.
 */

#include <dirent.h>
#include <errno.h>
#include <fcntl.h>
#include <netdb.h>
#include <netinet/in.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <unistd.h>

#define LENGTH_FILE_NAME 256
#define LENGTH_LINE 1024
#define max(a, b) ((a) > (b) ? (a) : (b))

/********************************************************/

int rimuovi_parola_file(const char *nome_file, const char *parola) {
    FILE *fin = fopen(nome_file, "r");
    if (!fin) {
        perror("fopen sorgente");
        return -1;
    }

    /* Creazione del file temporaneo univoco */
    char temp_path[] = "/tmp/mod_file_XXXXXX";
    int fd_temp = mkstemp(temp_path);
    if (fd_temp < 0) {
        perror("mkstemp");
        fclose(fin);
        return -1;
    }

    FILE *ftemp = fdopen(fd_temp, "w");
    if (!ftemp) {
        perror("fdopen");
        close(fd_temp);
        fclose(fin);
        return -1;
    }

    char riga[1024];
    size_t len_parola = strlen(parola);
    int conteggio = 0;

    /* Lettura riga per riga */
    while (fgets(riga, sizeof(riga), fin) != NULL) {
        char *pos = riga;
        char *match;

        while ((match = strstr(pos, parola)) != NULL) {
            /* Scrive la parte di testo prima della parola trovata */
            fwrite(pos, 1, match - pos, ftemp);
            
            conteggio++;                   /* Incrementa il contatore delle rimozioni */
            pos = match + len_parola;      /* Salta oltre la parola cercata */
        }
        /* Scrive il resto della riga */
        fputs(pos, ftemp);
    }

    fclose(fin);
    fclose(ftemp); /* Chiude anche fd_temp */

    /* Sostituisce il file originale con quello temporaneo */
    if (rename(temp_path, nome_file) < 0) {
        perror("rename");
        unlink(temp_path);
        return -1;
    }

    return conteggio;
}

/********************************************************/
void gestore(int signo)
{
    int stato;
    printf("esecuzione gestore di SIGCHLD\n");
    wait(&stato);
}
/********************************************************/

int main(int argc, char **argv)
{
    int listenfd, connfd, udpfd, fd_file, nready, maxfdp1;
    const int on = 1;
    char zero = 0, buff[LENGTH_FILE_NAME], nome_file[LENGTH_FILE_NAME], nome_dir[LENGTH_FILE_NAME];
    char parola[LENGTH_FILE_NAME];
    fd_set rset;
    int len, nread, nwrite, num, port;
    struct sockaddr_in cliaddr, servaddr;

    /* CONTROLLO ARGOMENTI ---------------------------------- */
    if (argc != 2)
    {
        printf("Error: %s port\n", argv[0]);
        exit(1);
    }
    nread = 0;
    while (argv[1][nread] != '\0')
    {
        if ((argv[1][nread] < '0') || (argv[1][nread] > '9'))
        {
            printf("Terzo argomento non intero\n");
            exit(2);
        }
        nread++;
    }
    port = atoi(argv[1]);
    if (port < 1024 || port > 65535)
    {
        printf("Porta scorretta...");
        exit(2);
    }

    /* INIZIALIZZAZIONE INDIRIZZO SERVER ----------------------------------------- */
    memset((char *)&servaddr, 0, sizeof(servaddr));
    servaddr.sin_family = AF_INET;
    servaddr.sin_addr.s_addr = INADDR_ANY;
    servaddr.sin_port = htons(port);

    printf("Server avviato\n");

    /* CREAZIONE SOCKET TCP ------------------------------------------------------ */
    listenfd = socket(AF_INET, SOCK_STREAM, 0);
    if (listenfd < 0)
    {
        perror("apertura socket TCP ");
        exit(1);
    }
    printf("Creata la socket TCP d'ascolto, fd=%d\n", listenfd);
    /* ABILITA IL RIUTILIZZO DELL'INDIRIZZO/PORTA */
    if (setsockopt(listenfd, SOL_SOCKET, SO_REUSEADDR, &on, sizeof(on)) < 0)
    {
        perror("set opzioni socket TCP");
        exit(2);
    }
    printf("Set opzioni socket TCP ok\n");

    if (bind(listenfd, (struct sockaddr *)&servaddr, sizeof(servaddr)) < 0)
    {
        perror("bind socket TCP");
        exit(3);
    }
    printf("Bind socket TCP ok\n");

    if (listen(listenfd, 5) < 0)
    {
        perror("listen");
        exit(4);
    }
    printf("Listen ok\n");

    /* CREAZIONE SOCKET UDP ------------------------------------------------ */
    udpfd = socket(AF_INET, SOCK_DGRAM, 0);
    if (udpfd < 0)
    {
        perror("apertura socket UDP");
        exit(5);
    }
    printf("Creata la socket UDP, fd=%d\n", udpfd);

    if (setsockopt(udpfd, SOL_SOCKET, SO_REUSEADDR, &on, sizeof(on)) < 0)
    {
        perror("set opzioni socket UDP");
        exit(6);
    }
    printf("Set opzioni socket UDP ok\n");

    if (bind(udpfd, (struct sockaddr *)&servaddr, sizeof(servaddr)) < 0)
    {
        perror("bind socket UDP");
        exit(7);
    }
    printf("Bind socket UDP ok\n");

    /* AGGANCIO GESTORE PER EVITARE FIGLI ZOMBIE -------------------------------- */
    signal(SIGCHLD, gestore);

    /* PULIZIA E SETTAGGIO MASCHERA DEI FILE DESCRIPTOR ------------------------- */
    FD_ZERO(&rset);
    maxfdp1 = max(listenfd, udpfd) + 1;

    /* CICLO DI RICEZIONE EVENTI DALLA SELECT ----------------------------------- */
    for (;;)
    {
        FD_SET(listenfd, &rset);
        FD_SET(udpfd, &rset);

        if ((nready = select(maxfdp1, &rset, NULL, NULL, NULL)) < 0)
        {
            if (errno == EINTR)
                continue;
            else
            {
                perror("select");
                exit(8);
            }
        }

        /* GESTIONE RICHIESTE DI GET DI UN FILE ------------------------------------- */
        if (FD_ISSET(listenfd, &rset))
        {
            printf("Ricevuta richiesta di analisi di un direttorio\n");
            len = sizeof(struct sockaddr_in);
            if ((connfd = accept(listenfd, (struct sockaddr *)&cliaddr, &len)) < 0)
            {
                if (errno == EINTR)
                    continue;
                else
                {
                    perror("accept");
                    exit(9);
                }
            }

            if (fork() == 0)
            { /* processo figlio che serve la richiesta di operazione */
                close(listenfd);
                printf("Dentro il figlio, pid=%i\n", getpid());

                for (;;)
                {
                    if ((read(connfd, &nome_dir, sizeof(nome_file))) <= 0)
                    {
                        perror("read");
                        break;
                    }
                    printf("Richiesto analisi direttorio: %s\n", nome_dir);
                    char riga[2 * LENGTH_FILE_NAME + 1];
                    DIR *dir, *dir_figlio;
                    struct dirent *dd, *dd_f;
                    int count = 0;
                    dir = opendir(nome_dir);
                    if (dir == NULL)
                    {
                        count = -1;
                        char errore[] = "Errore: directory insesistente\n";
                        write(connfd, errore, sizeof(errore));
                        write(1, errore, sizeof(errore));
                        continue;
                    }
                    else
                    {
                        while ((dd = readdir(dir)) != NULL)
                        {
                            if ((dd->d_type == DT_DIR) &&
                                (strcmp(dd->d_name, "..") != 0) &&
                                (strcmp(dd->d_name, ".") != 0))
                            {
                                char new_path[LENGTH_FILE_NAME*2];
                                snprintf(new_path, sizeof(new_path), "%s/%s", nome_dir, dd->d_name);
                                printf("Sotto directory aperta: %s\n",new_path);
                                dir_figlio = opendir(new_path);
                                if (dir_figlio == NULL)
                                    continue;
                                else{
                                    while ((dd_f = readdir(dir_figlio)) != NULL)
                                    {
                                        if (dd_f->d_type == DT_REG)
                                        {
                                            snprintf(riga, sizeof(riga), "%s/%s\n", dd->d_name, dd_f->d_name);
                                            
                                            printf("File Sotto directory: %s",riga);
                                            if (write(connfd, riga, strlen(riga)) < 0)
                                            {
                                                perror("write temp file");
                                            }
                                            count++;
                                        }
                                    }
                                }
                                closedir(dir_figlio);
                            }
                        }
                        /* invio al client segnale di terminazione: zero binario */
                        char finito[] = "Finito\n";
                        write(connfd,finito, sizeof(finito));
                        write(1, finito, sizeof(finito));
                    } // else
                    write(connfd,&zero,1);
                } // for
                printf("Figlio %i: chiudo connessione e termino\n", getpid());
                close(connfd);
                exit(0);
            } // figlio
        } /* fine gestione richieste di file */

        /* GESTIONE RICHIESTE DI CONTEGGIO ------------------------------------------ */

        if (FD_ISSET(udpfd, &rset))
        {
            printf("Server: ricevuta richiesta di cancellazione riga\n");
            len = sizeof(struct sockaddr_in);
            if (recvfrom(udpfd, &nome_file, sizeof(nome_file), 0, (struct sockaddr *)&cliaddr, &len) < 0)
            {
                perror("recvfrom");
                continue;
            }
            printf("Nome file: %s\n", nome_file);
            if (recvfrom(udpfd, &parola, sizeof(parola), 0, (struct sockaddr *)&cliaddr, &len) < 0)
            {
                perror("recvfrom");
                continue;
            }
            printf("Parola da eliminare: %s\n", parola);
            num = rimuovi_parola_file(nome_file,parola);
            printf("Risultato del conteggio: %i\n", num);

            /*
             * Cosa accade se non commentiamo le righe di codice qui sotto?
             * Cambia, dal punto di vista del tempo di attesa del client,
             * l'ordine col quale serviamo le due possibili richieste?
             * Cosa cambia se utilizziamo questa realizzazione, piuttosto
             * che la seconda?
             *
             */
            /*
            printf("Inizio dummy cicle\n");
            int i;
            for(i=0; i<100000; i++){ printf("Scrivo\n"); }
            printf("Fine dummy cicle\n");
            */

            if (sendto(udpfd, &num, sizeof(num), 0, (struct sockaddr *)&cliaddr, len) < 0)
            {
                perror("sendto");
                continue;
            }

        } /* fine gestione richieste di conteggio */
        /* padre chiude la socket dell'operazione */
        close(connfd);
    } /* ciclo for della select */
}
