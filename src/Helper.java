import java.text.SimpleDateFormat;

public class Helper {

    // Create a date time formatter
    static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    // Log the input with timestamp
    public static void log(String log) {
        // Log with current time in milliseconds
        System.out.println("<Log at " + formatter.format(System.currentTimeMillis()) + "> " + log);
    }

}
