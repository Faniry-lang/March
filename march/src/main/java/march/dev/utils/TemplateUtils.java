package march.dev.utils;

public class TemplateUtils {

    public static void replace(String template, String placeholder, String content) {
        template.replace(placeholder, content);
    }

    public static String getMessageTemplate() {
        StringBuilder sb = new StringBuilder();
        sb.append("<message>");
            sb.append("<role>");
                sb.append("<role-placeholder/>");
            sb.append("</role>");
            sb.append("<content>");
                sb.append("<content-placeholder/>");
            sb.append("</content>");
        sb.append("</message>");    
        return sb.toString();    
    }
}
