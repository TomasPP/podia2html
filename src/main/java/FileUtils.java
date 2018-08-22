import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

/**
 *
 */
@SuppressWarnings("WeakerAccess")
public class FileUtils {

    public static void writeStringToFile(String fileName, String fileContent) throws IOException {
        Path path = Paths.get(fileName);
        Files.write(path, fileContent.getBytes());
    }

    public static String readFileToString(String fileName) {
        try {
            Scanner s = new Scanner(new File(fileName)).useDelimiter("\\A");
            return s.hasNext() ? s.next() : "";
        } catch (FileNotFoundException e) {
            return null;
        }
    }
}
