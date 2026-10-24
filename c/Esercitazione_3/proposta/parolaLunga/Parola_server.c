#include <arpa/inet.h>
#include <errno.h>
#include <netdb.h>
#include <netinet/in.h>

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <sys/wait.h>

#include <unistd.h>

#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>

#define LINE_LENGTH 256

typedef struct
{
    char name[LINE_LENGTH];
} Request;

// void lenght_lenghtest_word(int sd);

int main(int argc, char **argv)
{
    int sd, port, num1, ris;
    const int on = 1;
    socklen_t len;
    struct sockaddr_in cliaddr, servaddr;
    struct hostent *clienthost;
    Request req;
    char line[LINE_LENGTH];

    /* CONTROLLO ARGOMENTI */
    if (argc != 2)
    {
        printf("Error: %s port\n", argv[0]);
        exit(1);
    }

    num1 = 0;
    while (argv[1][num1] != '\0')
    {
        if ((argv[1][num1] < '0') || (argv[1][num1] > '9'))
        {
            printf("Argomento non intero\n");
            printf("Error: %s port\n", argv[0]);
            exit(2);
        }
        num1++;
    }

    port = atoi(argv[1]);
    if (port < 1024 || port > 65535)
    {
        printf("1024 <= port <= 65535\n");
        exit(2);
    }

    /* INIZIALIZZAZIONE INDIRIZZO SERVER */
    memset((char *)&servaddr, 0, sizeof(servaddr));
    servaddr.sin_family = AF_INET;
    servaddr.sin_addr.s_addr = INADDR_ANY;
    servaddr.sin_port = htons(port);

    /* CREAZIONE SOCKET */
    sd = socket(AF_INET, SOCK_DGRAM, 0);
    if (sd < 0)
    {
        perror("creazione socket");
        exit(1);
    }
    // asincrono non c'è una risposta
    // sincrono c'è una risposta
    // bloccante il cliente aspetta risposta
    // sincorono non bloccante il cliente non aspetta risposta

    if (setsockopt(sd, SOL_SOCKET, SO_REUSEADDR, &on, sizeof(on)) < 0)
    {
        perror("set opzioni socket");
        exit(1);
    }

    if (bind(sd, (struct sockaddr *)&servaddr, sizeof(servaddr)) < 0)
    {
        perror("bind socket");
        exit(1);
    }

    printf("Server avviato e in ascolto sulla porta %d...\n", port);

    /* CICLO RICEZIONE */
    for (;;)
    {
        len = sizeof(cliaddr);
        if (recvfrom(sd, &req, sizeof(req), 0, (struct sockaddr *)&cliaddr, &len) < 0)
        {
            perror("recvfrom");
            continue;
        }

        printf("Filename ricevuto: %s\n", req.name);

        int fdDiff;
        if ((fdDiff = open(req.name, O_RDONLY)) < 0)
        {
            fprintf(stderr, "P2(pid=%d): Errore nell'apertura di Fdiff.\n", getpid());
            ris = -1;
        }
        else
        {
            char el;
            int i = 0;
            int lung = 0;
            while (read(fdDiff, &el, sizeof(char)) > 0)
            {
                if (el != 32 && el != '\n')
                {
                    line[i] = el;
                    i++;
                }
                else
                {
                    lung = lung > i ? lung : i;
                    i = 0;
                }
            }
            printf("Max lung %i\n", lung);
            ris = lung; // Risposta di esempio
        }
        clienthost = gethostbyaddr((char *)&cliaddr.sin_addr, sizeof(cliaddr.sin_addr), AF_INET);
        if (clienthost == NULL)
        {
            printf("Richiesta da IP: %s:%i\n", inet_ntoa(cliaddr.sin_addr), (unsigned)ntohs(cliaddr.sin_port));
        }
        else
        {
            printf("Richiesta da Host: %s:%i\n", clienthost->h_name, (unsigned)ntohs(cliaddr.sin_port));
        }

        if (sendto(sd, &ris, sizeof(ris), 0, (struct sockaddr *)&cliaddr, len) < 0)
        {
            perror("sendto");
            continue;
        }
        printf("Risposta inviata al client.\n\n");
    }

    close(sd);
    return 0;
}

/*
void lenght_lenghtest_word(int sd) {
    // Servono 3 pipe per collegare 4 comandi
    int pipe1[2], pipe2[2], pipe3[2];

    if (pipe(pipe1) < 0 || pipe(pipe2) < 0 || pipe(pipe3) < 0) {
        perror("Errore nella creazione delle pipe");
        exit(EXIT_FAILURE);
    }

    // --- COMANDO 1: grep -oE '[[:alpha:]]+' file.txt ---
    if (fork() == 0) {
        close(0);
        close(1);
        dup(sd);
        dup(pipe1[1]);
        // Redirige stdout su pipe1

        // Chiude tutte le estremità delle pipe
        close(pipe1[0]); close(pipe1[1]);
        close(pipe2[0]); close(pipe2[1]);
        close(pipe3[0]); close(pipe3[1]);

        char *cmd1[] = {"grep", "-oE", "[[:alpha:]]+", "file.txt", NULL};
        execvp(cmd1[0], cmd1);
        perror("Errore execvp grep");
        exit(EXIT_FAILURE);
    }

    // --- COMANDO 2: awk '{ print length, $0 }' ---
    if (fork() == 0) {
        // Redirige stdin da pipe1 e stdout su pipe2
        dup2(pipe1[0], STDIN_FILENO);
        dup2(pipe2[1], STDOUT_FILENO);

        close(pipe1[0]); close(pipe1[1]);
        close(pipe2[0]); close(pipe2[1]);
        close(pipe3[0]); close(pipe3[1]);

        char *cmd2[] = {"awk", "{ print length, $0 }", NULL};
        execvp(cmd2[0], cmd2);
        perror("Errore execvp awk");
        exit(EXIT_FAILURE);
    }

    // --- COMANDO 3: sort -rn ---
    if (fork() == 0) {
        // Redirige stdin da pipe2 e stdout su pipe3
        dup2(pipe2[0], STDIN_FILENO);
        dup2(pipe3[1], STDOUT_FILENO);

        close(pipe1[0]); close(pipe1[1]);
        close(pipe2[0]); close(pipe2[1]);
        close(pipe3[0]); close(pipe3[1]);

        char *cmd3[] = {"sort", "-rn", NULL};
        execvp(cmd3[0], cmd3);
        perror("Errore execvp sort");
        exit(EXIT_FAILURE);
    }

    // --- COMANDO 4: head -n 1 ---
    if (fork() == 0) {
        // Redirige stdin da pipe3 (stdout resta lo standard output del terminale)
        dup2(pipe3[0], sd);

        close(pipe1[0]); close(pipe1[1]);
        close(pipe2[0]); close(pipe2[1]);
        close(pipe3[0]); close(pipe3[1]);

        char *cmd4[] = {"head", "-n", "1", NULL};
        execvp(cmd4[0], cmd4);
        perror("Errore execvp head");
        exit(EXIT_FAILURE);
    }

    // --- PROCESSO PADRE ---
    // Chiude tutte le estremità delle pipe per permettere la propagazione dell'EOF
    close(pipe1[0]); close(pipe1[1]);
    close(pipe2[0]); close(pipe2[1]);
    close(pipe3[0]); close(pipe3[1]);

    // Attende la terminazione di tutti e 4 i processi figli
    for (int i = 0; i < 4; i++) {
        wait(NULL);
    }
}
    */