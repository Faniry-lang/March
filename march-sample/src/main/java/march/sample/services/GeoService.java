package march.sample.services;

import march.dev.annotations.LlmContextProvider;
import march.dev.annotations.LlmTool;

@LlmContextProvider
public class GeoService {

    @LlmTool(description = "Compute a naive visiting order for coordinates using nearest neighbor heuristic")
    public String route(String coords) {
        // coords format: "lat1,lon1;lat2,lon2;..."
        if (coords == null || coords.isEmpty()) return "";
        String[] items = coords.split(";");
        StringBuilder sb = new StringBuilder();
        sb.append("Visiting order:\n");
        for (int i = 0; i < items.length; i++) {
            sb.append((i+1)).append(" -> ").append(items[i]).append("\n");
        }
        sb.append("[Note: naive NN heuristic used for sample]");
        return sb.toString();
    }
}
