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

#define MAX_ARGS 64
#define BUF_SIZE 512
#define PATH_ASSOLUTO "/bin/"
#define ENVIRONMENT { "PATH=/bin:/usr/bin", "USER=test_user", NULL }
#define LENGTH_FILE_NAME 256
#define LENGTH_LINE 1024
#define max(a, b) ((a) > (b) ? (a) : (b))
/********************************************************/
int tokenizza_comando(char *cmd_line, char **argv) {
    int i = 0;  
    // 2. Tokenizzazione tramite strtok
    char *token = strtok(cmd_line, " \t");
    while (token != NULL && i < MAX_ARGS - 1) {
        argv[i++] = token;
        token = strtok(NULL, " \t");
    }
    // 3. FONDAMENTALE: L'array deve terminare con NULL
    argv[i] = NULL;
    return i; // Ritorna il numero di argomenti estratti
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
    char parola[LENGTH_FILE_NAME],comando[LENGTH_FILE_NAME];
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
    //signal(SIGCHLD, gestore);

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
            
            printf("Server: comando remoto via TCP\n");
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
            
            
            int status;
            if (fork() == 0)
            { /* processo figlio che serve la richiesta di operazione */
                
                close(listenfd);
                //printf("Dentro il figlio, pid=%i\n", getpid());
                if (read(connfd,comando,LENGTH_FILE_NAME) < 0) {
                    perror("write");
                    continue;
                }
                char *argv[MAX_ARGS];
                // Parsing della stringa di testo
                int argc = tokenizza_comando(comando, argv);
                int pid_figlio;
                close(1);
                dup(connfd);
                close(connfd);


                char path_assoluto[LENGTH_LINE];
                snprintf(path_assoluto, sizeof(path_assoluto), "%s%s", "/bin/", argv[0]);
                char *custom_env[] = ENVIRONMENT;
        
                execve(path_assoluto, argv, custom_env);
                perror("execve");
                close(connfd);
                exit(1);
            }else{
                close(connfd);
            }
            printf("Controllo 3");
        } /* fine gestione richieste di file */

        /* GESTIONE RICHIESTE DI CONTEGGIO ------------------------------------------ */

        if (FD_ISSET(udpfd, &rset))
        {
            printf("Server: comando remoto via UDP\n");
            
            len = sizeof(struct sockaddr_in);
            if (recvfrom(udpfd, &comando, sizeof(comando), 0, (struct sockaddr *)&cliaddr, &len) < 0)
            {
                perror("recvfrom");
                continue;
            }
            printf("Comando: %s\n", comando);
            
            char *argv[MAX_ARGS];

            // Parsing della stringa di testo
            printf("Comando: \"%s\"\n", comando);
            int argc = tokenizza_comando(comando, argv);
            int pid_figlio,status;
            if ((pid_figlio=fork()) == 0)
            { /* processo figlio che serve la richiesta di operazione */

                printf("Dentro il figlio, pid=%i\n", getpid());
                char path_assoluto[LENGTH_LINE];
                snprintf(path_assoluto, sizeof(path_assoluto), "%s%s", "/bin/", argv[0]);
                // Definizione di un ambiente custom terminato anch'esso da NULL
                char *custom_env[] = ENVIRONMENT;
        
                execve(path_assoluto, argv, custom_env);
                perror("execve");
                exit(1);
            }else{
                waitpid(pid_figlio,&status,0);
                if (WIFEXITED(status))
                    printf("Terminazione volontaria di %d con stato %d\n", pid_figlio, WEXITSTATUS(status));
                else if (WIFSIGNALED(status))
                    printf("Terminazione involontaria  per segnale %d\n", WTERMSIG(status)); 
                if (sendto(udpfd,&status,sizeof(status), 0, (struct sockaddr *)&cliaddr, len) < 0)
                {
                    perror("scrittura socket");
                    continue; // se questo invio fallisce il client torna all'inzio del ciclo
                }
            } 
        } /* fine gestione richieste di conteggio */
        /* padre chiude la socket dell'operazione */
    } /* ciclo for della select */
}
