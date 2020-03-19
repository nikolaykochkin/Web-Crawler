package crawler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebCrawlerWorker implements Runnable {

    WebCrawler.Task task;
    HtmlPage page;

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    private static final Pattern titlePattern = Pattern.compile("<title.*?>(.*?)</title>", Pattern.DOTALL);
    private static final Pattern linkPattern = Pattern.compile("href=\"(.*?)\"");

    public WebCrawlerWorker(WebCrawler.Task task, HtmlPage page) {
        this.task = task;
        this.page = page;
    }

    @Override
    public void run() {


        HttpLink link = new HttpLink(page.getUrl());

        String htmlText = getWebPage(link.getUrl());

        Matcher matcher = titlePattern.matcher(htmlText);

        if (matcher.find()) {
            page.setTitle(matcher.group(1));
            task.addPageToResult(page);
        }

        matcher = linkPattern.matcher(htmlText);

        int depth = page.getDepth() + 1;

        while (matcher.find()) {

            String url = link.getRelativeURL(matcher.group(1));
            if (url == null) {
                continue;
            }

            task.addNewTask(url, depth);
        }


        task.phaser.arrive();

    }

    public static String getWebPage(String url) {

        InputStream inputStream;

        try {
            URLConnection connection = new URL(url).openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:63.0) Gecko/20100101 Firefox/63.0");
            String contentType = connection.getContentType();
            if (contentType != null && contentType.matches(".*?text/html.*")) {
                inputStream = connection.getInputStream();
            } else {
                return "";
            }
        } catch (IOException e) {
            return e.getMessage();
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        StringBuilder stringBuilder = new StringBuilder();

        String nextLine;

        try {
            while ((nextLine = reader.readLine()) != null) {
                stringBuilder.append(nextLine);
                stringBuilder.append(LINE_SEPARATOR);
            }
        } catch (IOException e) {
            return e.getMessage();
        }

        return stringBuilder.toString();

    }
}
