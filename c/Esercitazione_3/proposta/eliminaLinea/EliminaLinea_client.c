#include <netdb.h>
#include <netinet/in.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <fcntl.h>

#define LINE_LENGTH 256

typedef struct
{
    char name[LINE_LENGTH];
    int n_linea;
} Request;

// check file testo finisce con .txt

int main(int argc, char **argv)
{
    struct hostent *host;
    struct sockaddr_in clientaddr, servaddr;
    int port, sd, num1, ris, i;
    socklen_t len;
    Request req;
    char buffer[LINE_LENGTH];

    /* CONTROLLO ARGOMENTI */
    if (argc != 3)
    {
        printf("Error: %s serverAddress serverPort\n", argv[0]);
        exit(1);
    }
     /* INIZIALIZZAZIONE INDIRIZZO SERVER -------------------------- */
    memset((char *)&servaddr, 0, sizeof(struct sockaddr_in));
    servaddr.sin_family = AF_INET;
    host                = gethostbyname(argv[1]);


    /* VERIFICA INTERO PORTA */
    num1 = 0;
    while (argv[2][num1] != '\0')
    {
        if ((argv[2][num1] < '0') || (argv[2][num1] > '9'))
        {
            printf("Secondo argomento non intero\n");
            printf("Error: %s serverAddress serverPort\n", argv[0]);
            exit(2);
        }
        num1++;
    }

    port = atoi(argv[2]);
    if (port < 1024 || port > 65535)
    {
        printf("%s = porta scorretta...\n", argv[2]);
        exit(2);
    }
if (host == NULL) {
        printf("%s not found in /etc/hosts\n", argv[1]);
        exit(2);
    } else {
        servaddr.sin_addr.s_addr = ((struct in_addr *)(host->h_addr))->s_addr;
        servaddr.sin_port        = htons(port);
    }

    /* CREAZIONE SOCKET ------------------------------------ */
    sd = socket(AF_INET, SOCK_STREAM, 0);
    if (sd < 0)
    {
        perror("apertura socket");
        exit(1);
    }
    printf("Client: creata la socket sd=%d\n", sd);

    /* Operazione di BIND implicita nella connect */
    if (connect(sd, (struct sockaddr *)&servaddr, sizeof(struct sockaddr_in)) < 0)
    {
        perror("connect");
        exit(1);
    }
    printf("Client: connect ok\n");

    /* CORPO DEL CLIENT: Ciclo di invio e ricezione */
    printf("Inserisci FileName (Ctrl+D per uscire):\n");
    int fD;
    while (printf("FileName: ") && scanf("%255s", req.name) == 1)
    {
        i = 0;
        while (req.name[i] != '\0')
        {
            i++;
        }
        if (i >= 4 &&
            req.name[i - 4] == '.' &&
            req.name[i - 3] == 't' &&
            req.name[i - 2] == 'x' &&
            req.name[i - 1] == 't')
        {
            printf("Estensione .txt valida!\n");
        }
        else
        {
            printf("Estensione non valida o stringa troppo corta.\n");
            continue;
        }
        if ((fD = open(req.name, O_RDONLY)) < 0)
        {
            printf("File non si apre\n");
            continue;
        }else{
            printf("File ORIGINAL si apre correttamente!\n");
            break;
        }
    }
    printf("File name letto: %s\n", req.name);

    int ok,nwrite;
    char c;
    while (printf("Numero linee: ") && (scanf("%i", &req.n_linea)) == 1)
    {
        printf("Numero linea letto: %d\n", req.n_linea);
        if((nwrite = write(sd, &req.n_linea, sizeof(int)))<0){
            perror("nwrite");
            continue;
        }
        printf("Client: invio numero linea\n");
        
        int fD1;
        if ((fD1 = open("tmp.txt", O_WRONLY | O_CREAT | O_TRUNC, 0644)) < 0)
        {
            printf("File non si apre\n");
            continue;
        }else{
            printf("File TMP si apre correttamente!\n");
        }
        int nread,nwrite;
        printf("\n");
        while((nread = read(fD, buffer, LINE_LENGTH))>0){
            nwrite = write(sd, buffer, nread);
            nwrite = write(1, buffer, nread);
        }
        printf("\n\nFile trasferito\n\n");
        shutdown(sd,1);
        while((nread = read(sd, buffer, LINE_LENGTH))>0){
            nwrite = write(fD1, buffer, nread);
            nwrite = write(1, buffer, nread);
        }
        printf("\n");
        shutdown(sd,0);
        close(fD1);
        close(fD);
        if (rename("tmp.txt", req.name) < 0) {
            perror("Errore durante la rename");
            return -1;
        }
        break;
        
    }

    close(sd);
    printf("\nClient: termino...\n");
    exit(0);
}