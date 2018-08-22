import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 */
@SuppressWarnings("WeakerAccess")
public class FileCache {

    private static FileCache fileCache;

    private FileCache() throws FileNotFoundException {
        final File outputDir = new File(Settings.OUTPUT_FILES_DIR);
        //noinspection ResultOfMethodCallIgnored
        outputDir.mkdirs();
        if (!outputDir.exists()) {
            throw new FileNotFoundException(Settings.OUTPUT_FILES_DIR);
        }

    }

    public static synchronized  FileCache getInstance() throws FileNotFoundException {
        if (fileCache == null) {
            fileCache = new FileCache();
        }
        return fileCache;
    }
    public String getHtmlBody(String url) {
        String fileName = getFileName(url);
//        System.out.println("fileName = " + fileName);

        return FileUtils.readFileToString(fileName);
    }

    private static String getFileName(String url) {
        final int hashCode = url.hashCode();
        final String lastSegmentName = url.substring(url.lastIndexOf('/') + 1);
        return Settings.OUTPUT_FILES_DIR + hashCode + "_" + lastSegmentName + Settings.HTML_EXTENSION;
    }

    public void saveHtmlBody(String url, String body) throws IOException {
        String fileName = getFileName(url);
//        System.out.println("fileName = " + fileName);

        FileUtils.writeStringToFile(fileName, body);
    }
}
