#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <time.h>

#if defined(_WIN32) || defined(_WIN64)
#include <windows.h>
static double get_time_ms() {
    LARGE_INTEGER freq, count;
    QueryPerformanceFrequency(&freq);
    QueryPerformanceCounter(&count);
    return (double)count.QuadPart * 1000.0 / (double)freq.QuadPart;
}
#else
#include <sys/time.h>
static double get_time_ms() {
    struct timeval tv;
    gettimeofday(&tv, NULL);
    return (double)tv.tv_sec * 1000.0 + (double)tv.tv_usec / 1000.0;
}
#endif

/*
 * K-Means Worker C Engine:
 * Formato de entrada (archivo o stdin):
 * Linea 1: N D K
 *   N = Número de puntos
 *   D = Dimensiones (ej. 2)
 *   K = Número de centroides
 * Siguientes K líneas: coordenadas del centroide k (D valores float/double)
 * Siguientes N líneas: coordenadas del punto i (D valores float/double)
 *
 * Salida:
 * LINEA 1: EXECUTION_TIME_MS: <ms>
 * LINEA 2: POINTS_PROCESSED: <N>
 * Siguientes K líneas:
 * CLUSTER <k> COUNT: <count> SUM: <sum_d0> <sum_d1> ...
 */

int main(int argc, char *argv[]) {
    FILE *in = stdin;
    if (argc > 1) {
        in = fopen(argv[1], "r");
        if (!in) {
            fprintf(stderr, "Error: No se pudo abrir el archivo de entrada %s\n", argv[1]);
            return 1;
        }
    }

    int N = 0, D = 0, K = 0;
    if (fscanf(in, "%d %d %d", &N, &D, &K) != 3) {
        fprintf(stderr, "Error leyendo cabecera N D K\n");
        if (in != stdin) fclose(in);
        return 1;
    }

    if (N <= 0 || D <= 0 || K <= 0) {
        fprintf(stderr, "Parametros invalidos: N=%d D=%d K=%d\n", N, D, K);
        if (in != stdin) fclose(in);
        return 1;
    }

    // Alocar memoria para centroides
    double *centroids = (double *)malloc(K * D * sizeof(double));
    for (int k = 0; k < K; k++) {
        for (int d = 0; d < D; d++) {
            if (fscanf(in, "%lf", &centroids[k * D + d]) != 1) {
                fprintf(stderr, "Error leyendo centroide %d dim %d\n", k, d);
                free(centroids);
                if (in != stdin) fclose(in);
                return 1;
            }
        }
    }

    // Alocar memoria para puntos
    double *points = (double *)malloc(N * D * sizeof(double));
    for (int i = 0; i < N; i++) {
        for (int d = 0; d < D; d++) {
            if (fscanf(in, "%lf", &points[i * D + d]) != 1) {
                fprintf(stderr, "Error leyendo punto %d dim %d\n", i, d);
                free(centroids);
                free(points);
                if (in != stdin) fclose(in);
                return 1;
            }
        }
    }

    if (in != stdin) {
        fclose(in);
    }

    // Inicializar acumuladores por cluster
    int *counts = (int *)calloc(K, sizeof(int));
    double *sums = (double *)calloc(K * D, sizeof(double));

    double t_start = get_time_ms();

    // Bucle principal de K-Means (asignación y acumulación)
    for (int i = 0; i < N; i++) {
        int best_k = 0;
        double min_dist_sq = 1e30;

        for (int k = 0; k < K; k++) {
            double dist_sq = 0.0;
            for (int d = 0; d < D; d++) {
                double diff = points[i * D + d] - centroids[k * D + d];
                dist_sq += diff * diff;
            }
            if (dist_sq < min_dist_sq) {
                min_dist_sq = dist_sq;
                best_k = k;
            }
        }

        counts[best_k]++;
        for (int d = 0; d < D; d++) {
            sums[best_k * D + d] += points[i * D + d];
        }
    }

    double t_end = get_time_ms();
    double exec_time = t_end - t_start;

    // Imprimir resultados estructurados
    printf("EXECUTION_TIME_MS: %.3f\n", exec_time);
    printf("POINTS_PROCESSED: %d\n", N);
    for (int k = 0; k < K; k++) {
        printf("CLUSTER %d COUNT: %d SUM:", k, counts[k]);
        for (int d = 0; d < D; d++) {
            printf(" %.6f", sums[k * D + d]);
        }
        printf("\n");
    }

    free(centroids);
    free(points);
    free(counts);
    free(sums);

    return 0;
}
