package xyz.dbotfactory.dbot.helper;

public class PrettyPrintHelper {
    public static String padRight(String inputString, int length) {
        if (inputString.length() >= length)
            return inputString;

        StringBuilder sb = new StringBuilder();
        sb.append(inputString);

        while (sb.length() < length)
            sb.append(' ');

        return sb.toString();
    }
}
