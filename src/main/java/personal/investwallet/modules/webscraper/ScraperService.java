package personal.investwallet.modules.webscraper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URL;

@Service
public class ScraperService implements ScraperRepository {

    @Value("${url.base}")
    private String baseUrl;

    @Override
    public ScraperEntity scrapeWebSite(ScraperEntity scraper) {

        try {

            String path = baseUrl + "/fundos-imobiliarios" + "/BARI11";
            Document doc = Jsoup.connect(path).get();

            Elements yieldValue = doc.selectXpath("//*[@id=\"dy-info\"]/div/div[1]/Strong");



        } catch (Exception ex) {
           throw new RuntimeException(ex);
        }

        return scraper;
    }
}
