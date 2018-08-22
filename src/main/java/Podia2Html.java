import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * 
 * @see #validateCommandLine(String[])
 */
@SuppressWarnings("SameParameterValue")
public class Podia2Html {

    public static void main(String[] args) throws IOException {
        System.out.println("args = " + Arrays.toString(args));
        System.out.println("Working Directory = " + System.getProperty("user.dir"));

        final Podia2Html tool = new Podia2Html();

        tool.validateCommandLine(args);

        final String initialURL = args[0];
        int urlCount = -1;
        if (args.length == 2) {
            urlCount = Integer.parseInt(args[1]);
        }
        System.out.println("initialURL = " + initialURL);

        tool.parsePodia(initialURL, urlCount);
//        testJsoup();
    }

    private void validateCommandLine(String[] args) {
        if (args.length == 0 || args.length > 2) {
            System.out.println("Usage: ");
            System.out.println(Podia2Html.class.getName() + " <course url> <url count,optional>");
            System.out.println("If needed, set <url count> to 1 for initial test run. ");
            System.exit(0);
        }
    }

    private void parsePodia(String initialURL, int urlCount) throws IOException {
        final String host = getHostName(initialURL);

        final Map<String, String> cookies = loadCookies(Settings.COOKIES_FILE);
        //        System.out.println("cookies = " + cookies);

        String body = getHtmlBody(initialURL, cookies);
//        System.out.println("body = " + body);

        //select all links from side bar.
        Elements links = Jsoup.parse(body).select("div[class=sidebar-section-list]").select("a[href]");

        processLinks(host, cookies, links, urlCount);
//        printLinks(links);
    }

    private void processLinks(String host, Map<String, String> cookies, Elements links, int urlCount) throws IOException {
        int size = links.size();
        if (urlCount > 0 && urlCount < links.size()) {
            size = urlCount;
        }

        Document outDoc = null;
        Element outBody = null;

        for (int i = 0; i < size; i++) {
            Element link = links.get(i);
            
            final String href = link.attr("href");
            final String headingText = link.text();

            String body = getHtmlBody(host + href, cookies);

            final Document doc = Jsoup.parse(body);

            if (outDoc == null) {
                String title = extractTitle(doc);

                outDoc = Jsoup.parse("<html><head><title>" + title + "</title></head><body></body></html>");
                outBody = outDoc.select("body").get(0);
            }

            outBody.append("<h2>" + headingText + "</h2>");

            Element content = extractContent(doc);

            if (content != null) {
                outBody.append(content.toString().replaceAll("h1", "h3"));
            }
        }

        if (outDoc != null) {
            final String outMainFileName = Settings.MAIN_FILE_NAME;
            System.out.println("Writing file:" + outMainFileName);
            FileUtils.writeStringToFile(outMainFileName, outDoc.toString());
        }
    }

    private Element extractContent(Document doc) {
        //workaround for one page
        if (!doc.select("div[class=well text-center pv7 mb7]").isEmpty()) {
            final Elements select = doc.select("div[class=container]");
            if (!select.isEmpty()) {
                return select.get(0);
            }
        }

        final Elements select = doc.select("div[class=course-reader-content]");
        Element content = null;
        if (!select.isEmpty()) {
            content = select.get(0);
        } else {
            //Some sections that contain only file resource do not have course-reader-content section.
            //extracting higher level div's
            final Elements select1 = doc.select("div[class=row mb7]").select("div[class=col-sm-12 col-md-10 col-md-offset-1]");
            if (!select1.isEmpty()) {
                content = select1.get(0);
            } else {
                System.out.println("failed to extract content.");
            }
        }
        return content;
    }

    private String extractTitle(Document doc) {
        //adhoc course title extraction
        final Elements select = doc.select("div[class=user-course-navbar navbar navbar-fixed-top bg-confetti-primary]");
        String title = "";
        if (!select.isEmpty()) {
            title = select.get(0).text().replaceAll("Dashboard", "");
            System.out.println("Extracted title:" + title);
        }
        return title;
    }

    private String getHostName(String initialURL) throws MalformedURLException {
        final URL url = new URL(initialURL);
        String protocol = url.getProtocol();
        String host = url.getHost();
        int port = url.getPort();

        // if the port is not explicitly specified in the input, it will be -1.
        if (port == -1) {
            return String.format("%s://%s", protocol, host);
        } else {
            return String.format("%s://%s:%d", protocol, host, port);
        }
    }

    @SuppressWarnings("unused")
    private void printLinks(Elements links) {
        System.out.println("\nLinks: " + links.size());
        for (Element link : links) {
//            System.out.println(link.toString());
            final String temp = String.format(" * a: <%s>  (%s)", link.attr("href"), link.text());
            System.out.println(temp);
        }
    }

    private String getHtmlBody(String url, Map<String, String> cookies) throws IOException {
        final FileCache fileCache = FileCache.getInstance();
        String body = fileCache.getHtmlBody(url);
        if (body == null) {
            body = scrapHtmlBody(url, cookies);

            fileCache.saveHtmlBody(url, body);
            System.out.println("saved to file cache " + url);
        } else {
            System.out.println("loaded from file cache " + url);
        }
        return body;
    }

    private static String scrapHtmlBody(String url, Map<String, String> cookies) throws IOException {
        final Connection connection = Jsoup.connect(url);
        connection.userAgent(Settings.DEFAULT_USER_AGENT);

        connection.cookies(cookies);

        final Connection.Response resp = connection.method(Connection.Method.GET).execute();

        //new value of _coach_session cookie is returned with each response
        //but nothing happens if you keep passing old value to next request. So ignoring new value for now.
//        final Map<String, String> respCookies = resp.cookies();
//        System.out.println("respCookies = " + respCookies);

        return resp.body();
    }

    private static Map<String, String> loadCookies(@SuppressWarnings("SameParameterValue") String cookieFile) throws FileNotFoundException {
        Map<String, String> cookies = new LinkedHashMap<>();
        final Scanner scanner = new Scanner(new File(cookieFile));
        while(scanner.hasNextLine()){
            final String line = scanner.nextLine();

            final int pos = line.indexOf("=");
            String name = line.substring(0, pos);
            String value = line.substring(pos + 1);
            cookies.put(name, value);
        }
        scanner.close();
        return cookies;
    }

    @SuppressWarnings("unused")
    private static void testJsoup() throws IOException {
        Document doc = Jsoup.connect("http://en.wikipedia.org/").get();

        System.out.println(doc.title());
        Elements newsHeadlines = doc.select("#mp-itn b a");
        for (Element headline : newsHeadlines) {
            final String temp = String.format("%s\n\t%s",
                    headline.attr("title"), headline.absUrl("href"));
            System.out.println("temp = " + temp);
        }
    }
}
