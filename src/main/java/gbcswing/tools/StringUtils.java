package gbcswing.tools;

import java.util.ArrayList;
import java.util.List;

public class StringUtils {

    public static List<String> toList(String[] lines) {
        List<String> codes = new ArrayList<String>();
        for (String code : lines) {
            codes.add(code);
        }
        return codes;
    }

    public static String mLines(List<String> lines) {
        StringBuilder stringBuilder = new StringBuilder();
        for (String code : lines) {
            if (stringBuilder.length() != 0) {
                stringBuilder.append("\n");
            }
            stringBuilder.append(code);
        }
        return stringBuilder.toString();
    }

    public static List<String> mLines(String nodeValue) {
        return toList(nodeValue.split("\n"));
    }
}
