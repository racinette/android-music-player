package helper;

/**
 * Created by prett on 2/23/2018.
 */

public class Methods {
    public static String formatTitle(final String title){
        final String lineSeparator = System.getProperty("line.separator");
        final String space = " ";
        String newTitle = title.trim();
        newTitle = newTitle.replace(lineSeparator, space);
        return newTitle;
    }
}
