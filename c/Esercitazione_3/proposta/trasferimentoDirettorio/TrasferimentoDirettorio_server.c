/* Server che riceve un file e lo ridirige ordinato al client */

#include <arpa/inet.h>
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
#include <sys/stat.h>
#include <unistd.h>

#include <stdio.h>
#include <unistd.h>
#include <fcntl.h>
#include <errno.h>

#define LINE_LENGTH 256
/********************************************************/
void gestore(int signo)
{
    int stato;
    printf("\nEsecuzione gestore di SIGCHLD\n");
    wait(&stato);
}
/********************************************************/

int main(int argc, char **argv)
{
    int listen_sd, conn_sd;
    int port, len, num;
    const int on = 1;
    struct sockaddr_in cliaddr, servaddr;
    struct hostent *host;

    /* CONTROLLO ARGOMENTI ---------------------------------- */
    {
        if (argc != 2)
        {
            printf("Error: %s port\n", argv[0]);
            exit(1);
        }
        else
        {
            num = 0;
            while (argv[1][num] != '\0')
            {
                if ((argv[1][num] < '0') || (argv[1][num] > '9'))
                {
                    printf("Secondo argomento non intero\n");
                    exit(2);
                }
                num++;
            }
            port = atoi(argv[1]);
            if (port < 1024 || port > 65535)
            {
                printf("Error: %s port\n", argv[0]);
                printf("1024 <= port <= 65535\n");
                exit(2);
            }
        }
    }

    /* INIZIALIZZAZIONE INDIRIZZO SERVER ----------------------------------------- */
    {
        memset((char *)&servaddr, 0, sizeof(servaddr));
        servaddr.sin_family = AF_INET;
        servaddr.sin_addr.s_addr = INADDR_ANY;
        servaddr.sin_port = htons(port);
    }
    /* CREAZIONE E SETTAGGI SOCKET D'ASCOLTO --------------------------------------- */
    {
        listen_sd = socket(AF_INET, SOCK_STREAM, 0);
        if (listen_sd < 0)
        {
            perror("creazione socket ");
            exit(1);
        }
        printf("Server: creata la socket d'ascolto per le richieste di ordinamento, fd=%d\n",
               listen_sd);

        if (setsockopt(listen_sd, SOL_SOCKET, SO_REUSEADDR, &on, sizeof(on)) < 0)
        {
            perror("set opzioni socket d'ascolto");
            exit(1);
        }
        printf("Server: set opzioni socket d'ascolto ok\n");

        if (bind(listen_sd, (struct sockaddr *)&servaddr, sizeof(servaddr)) < 0)
        {
            perror("bind socket d'ascolto");
            exit(1);
        }
        printf("Server: bind socket d'ascolto ok\n");

        if (listen(listen_sd, 5) < 0) // creazione coda d'ascolto
        {
            perror("listen");
            exit(1);
        }
        printf("Server: listen ok\n");
    }
    /* AGGANCIO GESTORE PER EVITARE FIGLI ZOMBIE*/
    signal(SIGCHLD, gestore);
    /* CICLO DI RICEZIONE RICHIESTE --------------------------------------------- */
    for (;;)
    {
        /* ACCEPT DI CONNESSIONI CON CLIENT -----------------------------------------*/
        {
            len = sizeof(cliaddr);
            if ((conn_sd = accept(listen_sd, (struct sockaddr *)&cliaddr, &len)) < 0)
            {
                if (errno == EINTR)
                {
                    perror("Forzo la continuazione della accept");
                    continue;
                }
                else
                    exit(1);
            }
        }

        if (fork() == 0)
        { // figlio
            /*Chiusura FileDescr non utilizzati e ridirezione STDIN/STDOUT*/
            close(listen_sd);

            // Otteniamo il nome logico dell'host
            host = gethostbyaddr((char *)&cliaddr.sin_addr, sizeof(cliaddr.sin_addr), AF_INET);
            if (host == NULL)
            {
                // Se non c'è nome logico (es. no DNS), usiamo l'indirizzo IP
                printf("Server (figlio): host client e' %s\n", inet_ntoa(cliaddr.sin_addr));
            }
            else
            {
                printf("Server (figlio): host client e' %s \n", host->h_name);
            }

            int termino = 0;
            while (termino != 1)
            {
                int input = 0;
                char mode;
                char dirName[LINE_LENGTH + 1];
                DIR *dir;
                struct dirent *dd;
                struct stat path_stat;
                int preambolo = 0;
                int nread, nwrite;
                int size = 0;
                while (preambolo != 1)
                {
                    if ((nread = read(conn_sd, dirName, LINE_LENGTH)) < 0)
                    {
                        termino=1;
                        continue;
                    }
                    printf("[IN] Nome direttorio: %s\n", dirName);
                    if ((nread = read(conn_sd, &mode, sizeof(char))) < 0)
                    {
                        perror("Ricezione modalità");
                        continue;
                    }
                    printf("[IN] Modalita: %c\n", mode);

                    if (mode == 'G')
                    {
                        if ((dir = opendir(dirName)) == NULL)
                        {
                            perror("Apertura direttorio");
                            continue;
                        }
                        printf("Aperto direttorio %s\n", dirName);
                        while ((dd = readdir(dir)) != NULL)
                            if (dd->d_type == DT_REG)
                                size++;
                        if (size == 0)
                        {
                            perror("Direttorio insesistente");
                            continue;
                        }
                        rewinddir(dir);
                        if ((nwrite = write(conn_sd, &size, sizeof(int))) < 0)
                        {
                            perror("Invio dimensione directorio");
                            continue;
                        }
                        printf("[OUT] Numero file: %i \n", size);
                    }
                    else
                    {
                        if ((nread = read(conn_sd, &size, sizeof(int))) < 0)
                        {
                            perror("Ricezione dimensione directorio");
                            continue;
                        }
                        printf("[IN] Numero files: %i\n", size);
                        if (size == 0)
                        {
                            perror("Direttorio insesistente");
                            continue;
                        }
                    }
                    preambolo = 1;
                }

                int trasmissione = 0;
                char buffer[LINE_LENGTH];
                char fileName[LINE_LENGTH];
                char destName[2 * LINE_LENGTH];
                char serverDirPath[LINE_LENGTH + 8];
                int fd;
                off_t fileSize;
                char c;
                while (trasmissione != 1)
                {
                    if (mode == 'P')
                    { // PUT
                        for (int i = 0; i < size; i++)
                        {
                            if ((nread = read(conn_sd, fileName, LINE_LENGTH)) < 0)
                            {
                                perror("Ricezione nome file");
                                continue;
                            }
                            printf("[IN] Nome file: %s \n", fileName);

                            snprintf(serverDirPath, sizeof(serverDirPath), "%s_SERVER", dirName);

                            // 1. Create the directory if it doesn't exist
                            if (mkdir(serverDirPath, 0755) < 0)
                            {
                                if (errno != EEXIST)
                                {
                                    perror("Errore creazione directory SERVER");
                                    continue;
                                }
                            }

                            // 2. Format the complete file destination path
                            snprintf(destName, 2 * LINE_LENGTH + 8, "%s/%s", serverDirPath, fileName);

                            // 3. Open/create the file
                            if ((fd = open(destName, O_WRONLY | O_CREAT | O_TRUNC, 0644)) < 0)
                            {
                                perror("Errore creazione file");
                                continue;
                            }
                            printf("File %s si apre correttamente!\n", destName);
                            if ((nread = read(conn_sd, &fileSize, sizeof(off_t))) < 0)
                            {
                                perror("Ricezione nome file");
                                continue;
                            }
                            printf("[IN] Dimensione file: %lo \n", fileSize);
                            for (off_t i = 0; i < fileSize; i++)
                            {
                                if ((nread = read(conn_sd, &c, sizeof(char))) < 0)
                                {
                                    perror("Ricezione file");
                                }
                                if ((nwrite = write(fd, &c, sizeof(char))) < 0)
                                {
                                    perror("Scrittura file");
                                }
                            }
                            printf("[IN] Fine ricezione file\n");
                            close(fd);
                        }
                    }
                    else
                    {
                        while ((dd = readdir(dir)) != NULL)
                {
                    if (dd->d_type == DT_REG)
                    {

                        if ((fd = openat(dirfd(dir), dd->d_name, O_RDONLY)) < 0)
                        {
                            perror("Apertura file");
                            continue;
                        }
                        fstat(fd, &path_stat);
                        if ((nwrite = write(conn_sd, dd->d_name, LINE_LENGTH)) < 0)
                        {
                            perror("Invio nome file");
                            continue;
                        }
                        printf("[OUT] Nome file: %s\n", dd->d_name);
                        if ((nwrite = write(conn_sd, &path_stat.st_size, sizeof(off_t))) < 0)
                        {
                            perror("Invio dimensione file");
                            continue;
                        }
                        printf("[OUT] Dimensione file: %lo\n", path_stat.st_size);
                        while ((nread = read(fd, buffer, LINE_LENGTH)) > 0)
                        {
                            if ((nwrite = write(conn_sd, buffer, nread)) < 0)
                            {
                                perror("Invio file");
                                continue;
                            }
                        }
                        printf("[OUT] Fine trasferimento file\n");
                        close(fd);
                    }
                }
                    }
                    trasmissione = 1;
                }
                int conclusione = 0;
                while (conclusione != 1)
                {
                    if (mode == 'G')
                    {
                        if (closedir(dir) < 0)
                        {
                            perror("Chiusura direttorio");
                        }

                    }
                    conclusione=1;
                }
                printf("Termino\n");
            }
            /*
                // Redirect socket input
                //close(0);
                //close(1);
                //dup(conn_sd);
                //dup(conn_sd);
                //close(conn_sd);
                int n, nread;
                char c;
                int r = 1;
                if((nread = read(conn_sd, &n, sizeof(int)))<0){
                    perror("nread");
                }
                printf("Server (figlio): devo eliminare riga file %d\n", n);
                int nwrite;
                printf("\n");

                while((nread=read(conn_sd, &c, sizeof(c)))>0){
                    if (n != r){
                        nwrite = write(1, &c, sizeof(c));
                        nwrite = write(conn_sd, &c, sizeof(c));
                    }
                    if (c == '\n')
                        r++;
                }
                printf("\n\nServer (figlio): file trasferito\n");



                printf("Server (figlio): termino\n");

                exit(1);
                // se arrivo qui errore
    */
        } // figlio
        close(conn_sd); // padre chiude socket di connessione non di scolto
    } // ciclo for infinito
}
