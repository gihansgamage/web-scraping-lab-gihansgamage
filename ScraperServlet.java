package webScraper;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.util.*;

@WebServlet(urlPatterns = {"/scrape", "/download"})
public class ScrapeServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String path = request.getServletPath();

        if ("/scrape".equals(path)) {
            handleScrape(request, response);
        } else if ("/download".equals(path)) {
            handleDownload(request, response);
        }
    }

    private void handleScrape(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        HttpSession session = request.getSession();
        Integer visitCount = (Integer) session.getAttribute("visitCount");
        if (visitCount == null) visitCount = 0;
        session.setAttribute("visitCount", ++visitCount);

        String url = request.getParameter("url");
        String[] options = request.getParameterValues("option");

        Map<String, List<String>> resultMap = new HashMap<>();

        try {
            Document doc = Jsoup.connect(url).get();

            for (String option : options) {
                switch (option) {
                    case "title":
                        resultMap.put("Title", List.of(doc.title()));
                        break;

                    case "links":
                        Elements links = doc.select("a[href]");
                        List<String> linkTexts = new ArrayList<>();
                        for (Element link : links) {
                            linkTexts.add(link.text() + " => " + link.absUrl("href"));
                        }
                        resultMap.put("Links", linkTexts);
                        break;

                    case "images":
                        Elements imgs = doc.select("img");
                        List<String> imgUrls = new ArrayList<>();
                        for (Element img : imgs) {
                            imgUrls.add(img.absUrl("src"));
                        }
                        resultMap.put("Images", imgUrls);
                        break;
                }
            }

            Gson gson = new Gson();
            String json = gson.toJson(resultMap);

            response.setContentType("text/html");
            PrintWriter out = response.getWriter();

            out.println("<html><head><title>Scrape Results</title></head><body>");
            out.println("<h3>You have visited this page " + visitCount + " times.</h3>");
            out.println("<h2>Scraped Data:</h2>");

            for (String key : resultMap.keySet()) {
                out.println("<h3>" + key + "</h3>");
                out.println("<table border='1'>");
                for (String val : resultMap.get(key)) {
                    out.println("<tr><td>" + val + "</td></tr>");
                }
                out.println("</table><br>");
            }

            out.println("<h3>JSON Output:</h3><pre>" + json + "</pre>");

            out.println("<form method='post' action='download'>");
            out.println("<input type='hidden' name='json' value='" + json.replace("\"", "&quot;") + "'/>");
            out.println("<input type='submit' value='Download as CSV'/>");
            out.println("</form>");

            out.println("</body></html>");
            out.close();

        } catch (Exception e) {
            response.getWriter().println("Error: " + e.getMessage());
        }
    }

    private void handleDownload(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String json = request.getParameter("json");

        Gson gson = new Gson();
        Type mapType = new TypeToken<Map<String, List<String>>>() {}.getType();
        Map<String, List<String>> dataMap = gson.fromJson(json, mapType);

        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment;filename=results.csv");

        PrintWriter writer = response.getWriter();

        for (Map.Entry<String, List<String>> entry : dataMap.entrySet()) {
            writer.println(entry.getKey());
            for (String val : entry.getValue()) {
                writer.println("\"" + val.replace("\"", "\"\"") + "\"");
            }
            writer.println(); // blank line
        }

        writer.flush();
        writer.close();
    }
}
