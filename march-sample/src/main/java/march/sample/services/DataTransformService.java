package march.sample.services;

import march.dev.annotations.LlmContextProvider;
import march.dev.annotations.LlmTool;
import java.util.StringJoiner;

@LlmContextProvider
public class DataTransformService {

    @LlmTool(description = "Given a 2D integer array encoded as rows separated by ';' and cols by ',', return CSV summary with row sums")
    public String summarizeTable(String table) {
        if (table == null || table.isEmpty()) return "";
        String[] rows = table.split(";");
        StringJoiner sj = new StringJoiner("\n");
        sj.add("row,sum");
        for (int i = 0; i < rows.length; i++) {
            String[] cols = rows[i].split(",");
            int sum = 0;
            for (String c : cols) sum += Integer.parseInt(c.trim());
            sj.add((i+1) + "," + sum);
        }
        return sj.toString();
    }
}
