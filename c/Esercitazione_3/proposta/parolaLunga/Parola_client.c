#include <netdb.h>
#include <netinet/in.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <unistd.h>

#define LINE_LENGTH 256

typedef struct {
    char name[LINE_LENGTH];
} Request;


//check file testo finisce con .txt

int main(int argc, char **argv) {
    struct hostent *host;
    struct sockaddr_in clientaddr, servaddr;
    int port, sd, num1, ris, i;
    socklen_t len;
    Request req;

    /* CONTROLLO ARGOMENTI */
    if (argc != 3) {
        printf("Error: %s serverAddress serverPort\n", argv[0]);
        exit(1);
    }

    /* VERIFICA INTERO PORTA */
    num1 = 0;
    while (argv[2][num1] != '\0') {
        if ((argv[2][num1] < '0') || (argv[2][num1] > '9')) {
            printf("Secondo argomento non intero\n");
            printf("Error: %s serverAddress serverPort\n", argv[0]);
            exit(2);
        }
        num1++;
    }

    port = atoi(argv[2]);
    if (port < 1024 || port > 65535) {
        printf("%s = porta scorretta...\n", argv[2]);
        exit(2);
    }

    /* INIZIALIZZAZIONE STRUTTURA SERVER */
    memset((char *)&servaddr, 0, sizeof(servaddr));
    servaddr.sin_family = AF_INET;
    servaddr.sin_port = htons(port);

    host = gethostbyname(argv[1]);
    if (host == NULL) {
        printf("%s not found in /etc/hosts\n", argv[1]);
        exit(2);
    }
    servaddr.sin_addr.s_addr = ((struct in_addr *)(host->h_addr_list[0]))->s_addr;

    /* INIZIALIZZAZIONE STRUTTURA CLIENT */
    memset((char *)&clientaddr, 0, sizeof(clientaddr));
    clientaddr.sin_family = AF_INET;
    clientaddr.sin_addr.s_addr = INADDR_ANY;
    clientaddr.sin_port = 0;

    /* CREAZIONE SOCKET */
    sd = socket(AF_INET, SOCK_DGRAM, 0);
    if (sd < 0) {
        perror("apertura socket");
        exit(1);
    }
    printf("Client: creata la socket sd=%d\n", sd);

    /* BIND SOCKET */
    if (bind(sd, (struct sockaddr *)&clientaddr, sizeof(clientaddr)) < 0) {
        perror("bind socket");
        exit(1);
    }

    /* CORPO DEL CLIENT: Ciclo di invio e ricezione */
    printf("Inserisci FileName (Ctrl+D per uscire):\n");
    while (printf("FileName: ") && scanf("%255s", req.name) == 1) {
        
        i=0;
        while(req.name[i]!='\0'){
            i++;
        }
        if (i >= 4 &&
            req.name[i - 4] == '.' &&
            req.name[i - 3] == 't' &&
            req.name[i - 2] == 'x' &&
            req.name[i - 1] == 't') {
            printf("Estensione .txt valida!\n");
        } else {
            printf("Estensione non valida o stringa troppo corta.\n");
            continue;
        }

        printf("File name letta: %s\n", req.name);

        len = sizeof(servaddr);
        if (sendto(sd, &req, sizeof(req), 0, (struct sockaddr *)&servaddr, len) < 0) {
            perror("sendto");
            continue;
        }

        printf("Attesa del risultato...\n");
        len = sizeof(servaddr); // Reinizializzare len prima di recvfrom
        if (recvfrom(sd, &ris, sizeof(ris), 0, (struct sockaddr *)&servaddr, &len) < 0) {
            perror("recvfrom");
            continue;
        }

        printf("Risultato ricevuto dal server: %d\n\n", ris);
    }

    close(sd);
    printf("\nClient: termino...\n");
    exit(0);
}