package march.dev.utils;

import java.util.Map;

public class TemplateUtils {

    public static String replace(String template, String placeholder, String content) {
        return template.replace(placeholder, content);
    }

    public static String multiReplace(String template, Map<String, String> replacement) {
        String result = template;

        for (Map.Entry<String, String> entry : replacement.entrySet()) {
            String placeholder = entry.getKey();
            String content = entry.getValue();
            result = result.replace(placeholder, content);
        }

        return result;
    }

    public static String getTagsContent(String template, String tagName, boolean tagsIncluded) {
        String openingTag = "<"+tagName+">";
        String closingTag = "<"+tagName+"/>";
        if(tagsIncluded) {
            return RegexUtils.getContentIncludingDelimiters(template, openingTag, closingTag);
        }
        return RegexUtils.getContentExcludingDelimiters(template, openingTag, closingTag);
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
