package march.sample.services;

import march.dev.annotations.LlmContextProvider;
import march.dev.annotations.LlmTool;

@LlmContextProvider
public class MatrixService {

    @LlmTool(description = "Multiply two square matrices provided as CSV rows separated by ';' and columns by ','")
    public String multiply(String aTable, String bTable) {
        try {
            String[] aRows = aTable.split(";");
            String[] bRows = bTable.split(";");
            int n = aRows.length;
            int[][] A = new int[n][n];
            int[][] B = new int[n][n];
            for (int i = 0; i < n; i++) {
                String[] cols = aRows[i].split(",");
                for (int j = 0; j < n; j++) A[i][j] = Integer.parseInt(cols[j].trim());
            }
            for (int i = 0; i < n; i++) {
                String[] cols = bRows[i].split(",");
                for (int j = 0; j < n; j++) B[i][j] = Integer.parseInt(cols[j].trim());
            }
            int[][] C = new int[n][n];
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    int sum = 0;
                    for (int k = 0; k < n; k++) sum += A[i][k] * B[k][j];
                    C[i][j] = sum;
                }
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    sb.append(C[i][j]);
                    if (j < n-1) sb.append(",");
                }
                if (i < n-1) sb.append(";");
            }
            return sb.toString();
        } catch (Exception e) {
            return "error: " + e.getMessage();
        }
    }
}
