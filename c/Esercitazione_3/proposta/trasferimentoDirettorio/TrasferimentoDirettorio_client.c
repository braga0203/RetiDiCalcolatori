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
#include <dirent.h>
#include <errno.h>

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
    socklen_t len;
    int num1, port, sd;

    /* CONTROLLO ARGOMENTI */
    {
        if (argc != 3)
        {
            printf("Error: %s serverAddress serverPort\n", argv[0]);
            exit(1);
        }
    }
    /* INIZIALIZZAZIONE INDIRIZZO SERVER -------------------------- */
    {
        memset((char *)&servaddr, 0, sizeof(struct sockaddr_in));
        servaddr.sin_family = AF_INET;
        host = gethostbyname(argv[1]);
    }
    /* VERIFICA INTERO PORTA */
    {
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
    }
    /* VERIFICA HOST */
    {
        if (host == NULL)
        {
            printf("%s not found in /etc/hosts\n", argv[1]);
            exit(2);
        }
        else
        {
            servaddr.sin_addr.s_addr = ((struct in_addr *)(host->h_addr))->s_addr;
            servaddr.sin_port = htons(port);
        }
    }
    /* CREAZIONE SOCKET ------------------------------------ */
    {
        sd = socket(AF_INET, SOCK_STREAM, 0);
        if (sd < 0)
        {
            perror("apertura socket");
            exit(1);
        }
        printf("Client: creata la socket sd=%d\n", sd);
    }
    /* Operazione di BIND implicita nella connect */
    {
        if (connect(sd, (struct sockaddr *)&servaddr, sizeof(struct sockaddr_in)) < 0)
        {
            perror("connect");
            exit(1);
        }
        printf("Client: connect ok\n");
    }

    /* CORPO DEL CLIENT: Ciclo di invio e ricezione */

    printf("Inserisci FileName (Ctrl+D per uscire):\n");
    int termino = 0;
    while (termino != 1)
    {
        printf("---------------------------------------------\n");
        int input = 0;
        char mode, c;
        char dirName[LINE_LENGTH + 1];
        struct stat path_stat;
        while (input != 1)
        {
            while (printf("Direttorio: ") && scanf("%s", dirName) != 1)
                ;
            if (stat(dirName, &path_stat) != 0 || !S_ISDIR(path_stat.st_mode))
            {
                perror("Direttorio non esiste");
                continue;
            }
            printf("Direttorio scelto: %s\n", dirName);
            while ((c = getchar()) != '\n' && c != EOF)
                ;
            while (printf("Servizio (P/G): ") && scanf("%c", &mode) != 1)
                ;
            if (mode != 'G' && mode != 'P')
            {
                perror("Modalità non valida");
                continue;
            }
            printf("Modalità scelta: %c\n", mode);
            input = 1;
        }

        DIR *dir;
        struct dirent *dd;
        int preambolo = 0;
        int nread, nwrite;
        int size = 0;
        while (preambolo != 1)
        {
            if ((nwrite = write(sd, dirName, LINE_LENGTH)) < 0)
            {
                perror("Invio nome direttorio");
                continue;
            }
            printf("[OUT] Nome direttorio: %s\n", dirName);
            if ((nwrite = write(sd, &mode, sizeof(char))) < 0)
            {
                perror("Invio modalità");
                continue;
            }
            printf("[OUT] Modalita: %c\n", mode);

            if (mode == 'G')
            {
                if ((nread = read(sd, &size, sizeof(int))) < 0)
                {
                    perror("Ricezione dimensione directorio");
                    continue;
                }
                if (size == 0)
                {
                    perror("Direttorio insesistente");
                    continue;
                }
                printf("[IN] Numero file: %i \n", size);
            }
            else
            {

                if ((dir = opendir(dirName)) == NULL)
                {
                    perror("Apertura direttorio");
                    continue;
                }
                printf("Direttorio si apre correttamente!\n");
                while ((dd = readdir(dir)) != NULL)
                    if (dd->d_type == DT_REG)
                        size++;
                rewinddir(dir);
                if ((nwrite = write(sd, &size, sizeof(int))) < 0)
                {
                    perror("Invio dimensione direttorio");
                    continue;
                }
                printf("[OUT] Numero file: %i \n", size);
            }
            preambolo = 1;
        }
        int trasmissione = 0;
        char buffer[LINE_LENGTH];
        char fileName[LINE_LENGTH];
        char destName[2 * LINE_LENGTH];
        int fd;

        char serverDirPath[LINE_LENGTH + 8];
        off_t fileSize;
        while (trasmissione != 1)
        {
            if (mode == 'P')
            { // PUT
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
                        if ((nwrite = write(sd, dd->d_name, LINE_LENGTH)) < 0)
                        {
                            perror("Invio nome file");
                            continue;
                        }
                        printf("[OUT] Nome file: %s\n", dd->d_name);
                        if ((nwrite = write(sd, &path_stat.st_size, sizeof(off_t))) < 0)
                        {
                            perror("Invio dimensione file");
                            continue;
                        }
                        printf("[OUT] Dimensione file: %lo\n", path_stat.st_size);
                        while ((nread = read(fd, buffer, LINE_LENGTH)) > 0)
                        {
                            if ((nwrite = write(sd, buffer, nread)) < 0)
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
            else
            { // GET
                for (int i = 0; i < size; i++)
                {
                    if ((nread = read(sd, fileName, LINE_LENGTH)) < 0)
                    {
                        perror("Ricezione nome file");
                        continue;
                    }
                    printf("[IN] Nome file: %s \n", fileName);

                    snprintf(serverDirPath, sizeof(serverDirPath), "%s_CLIENT", dirName);

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
                    if ((nread = read(sd, &fileSize, sizeof(off_t))) < 0)
                    {
                        perror("Ricezione nome file");
                        continue;
                    }
                    printf("[IN] Dimensione file: %lo \n", fileSize);
                    for (off_t i = 0; i < fileSize; i++)
                    {
                        if ((nread = read(sd, &c, sizeof(char))) < 0)
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
            trasmissione = 1;
        }
        int conclusione = 0;
        while (conclusione != 1)
        {
            if (mode == 'P')
            {
                if (closedir(dir) < 0)
                {
                    perror("Chiusura direttorio");
                }
            }
            conclusione = 1;
        }
    }
    close(sd);
    printf("\nClient: termino...\n");
    exit(0);
}