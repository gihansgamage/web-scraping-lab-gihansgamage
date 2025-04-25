
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class webScraper2 {

    static class NewsArticle {
        private String title;
        private String date;
        private String author;

        public NewsArticle(String title, String date, String author) {
            this.title = title;
            this.date = date;
            this.author = author;
        }

        @Override
        public String toString() {
            return "Title: " + title + "\nDate: " + date + "\nAuthor: " + author + "\n";
        }
    }

    public static void main(String[] args) {
        String url = "https://www.bbc.com";
        List<NewsArticle> articles = new ArrayList<>();

        try {
            Document doc = Jsoup.connect(url).get();

            Elements items = doc.select("a[href*='/news/']");

            for (Element item : items) {
                String title = item.text();
                String link = item.absUrl("href");


                if (!link.isEmpty() && title.length() > 10) {
                    try {
                        Document articleDoc = Jsoup.connect(link).get();

                        String date = articleDoc.select("time").text();
                        if (date.isEmpty()) date = "Unknown";

                        String author = articleDoc.select("span[data-testid=byline-name]").text();
                        if (author.isEmpty()) author = "Unknown";

                        articles.add(new NewsArticle(title, date, author));

                        if (articles.size() >= 5) break; 

                    } catch (IOException e) {
                        System.out.println("Could not fetch article: " + link);
                    }
                }
            }

            // Print the result
            System.out.println("\n===== Scraped Articles =====");
            for (NewsArticle article : articles) {
                System.out.println(article);
            }

        } catch (IOException e) {
            System.out.println("Error connecting to site: " + e.getMessage());
        }
    }
}
