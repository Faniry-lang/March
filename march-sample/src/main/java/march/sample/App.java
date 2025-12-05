package march.sample;

import java.util.Map;

import march.dev.data.Tool;
import march.dev.utils.ProviderScan;

/**
 * Hello world!
 */
public class App {
    public static void main(String[] args) {

        try {
            Map<String, Tool> tools = ProviderScan.scanTools("march.sample.services");
            for(Map.Entry<String, Tool> entry : tools.entrySet()) {
                System.out.println("[Tool name] "+entry.getKey());
                System.out.println("[Tool desc] "+entry.getValue().getDescription());
                System.out.println("[Tool provider] "+entry.getValue().getProviderName());
                System.out.println("[Tool params] ");
                for(String param : entry.getValue().getParams().keySet()) {
                    System.out.println(param);
                }
                System.out.println();
            }
        } catch(Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
    }
}
