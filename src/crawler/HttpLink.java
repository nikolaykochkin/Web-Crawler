package crawler;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpLink {
    private String url;
    private String protocol;
    private String root;
    private String folder;
    private String[] directories;
    private Pattern patternSkip = Pattern.compile("^(#|mailto:|file:|javascript:|ftp:)");

    public HttpLink(String url) {

        this.url = url.trim();

        Pattern pattern = Pattern.compile("(https?:)//");
        Matcher matcher = pattern.matcher(this.url);
        if (matcher.find()) {
            protocol = matcher.group(1);
        }

        pattern = Pattern.compile("(https?://.+?/)");
        matcher = pattern.matcher(this.url);
        if (matcher.find()) {
            root = matcher.group(1);
        } else {
            root = this.url + "/";
        }

        pattern = Pattern.compile("(https?://.+?/(.+)/)");
        matcher = pattern.matcher(this.url);
        if (matcher.find()) {
            directories = matcher.group(2).split("/");
            folder = matcher.group(1);
        } else {
            directories = new String[0];
            folder = root;
        }

    }

    public String getUrl() {
        return url;
    }

    public String getRelativeURL(String href) {

        Matcher matcher = patternSkip.matcher(href);
        if (matcher.find()) {
            return null;
        }

        if (href.startsWith("http")) {
            return href;
        }

        if (href.startsWith("//")) {
            return protocol + href;
        }

        if (href.startsWith("/")) {
            return root + href.replaceFirst("/", "");
        }

        if (href.startsWith("../")) {
            if (directories.length == 0) {
                return null;
            }
            int counter = 0;
            while (href.startsWith("../")) {
                counter++;
                href = href.replaceFirst("\\.\\./", "");
            }
            if (counter > directories.length) {
                return null;
            }
            StringBuilder stringBuilder = new StringBuilder(root);
            for (int i = 0; i < directories.length - counter; i++) {
                stringBuilder.append(directories[i]).append("/");
            }
            stringBuilder.append(href);
            return stringBuilder.toString();
        }

        return folder + href;

    }
}
