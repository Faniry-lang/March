package march.dev.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexUtils {
    public static String getContentIncludingDelimiters(
            String source, 
            String startDelimiter, 
            String endDelimiter) {
        
        String escapedStart = Pattern.quote(startDelimiter);
        String escapedEnd = Pattern.quote(endDelimiter);

        String regex = escapedStart + "(.*?)" + escapedEnd;
        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(source);

        if (matcher.find()) {
            return matcher.group(0);
        }
        return null; 
    }

    public static String getContentExcludingDelimiters(
            String source, 
            String startDelimiter, 
            String endDelimiter) {
        
        String escapedStart = Pattern.quote(startDelimiter);
        String escapedEnd = Pattern.quote(endDelimiter);

        String regex = escapedStart + "(.*?)" + escapedEnd;
        
        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL); 
        Matcher matcher = pattern.matcher(source);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null; 
    }
}
