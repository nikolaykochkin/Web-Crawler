package crawler;

public class HtmlPage {
    private String url;
    private int depth;
    private String title;

    public HtmlPage(String url, int depth) {
        this.url = url;
        this.depth = depth;
    }

    public String getUrl() {
        return url;
    }

    public int getDepth() {
        return depth;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String toString() {
        return url + "\n" + title + "\n";
    }
}
