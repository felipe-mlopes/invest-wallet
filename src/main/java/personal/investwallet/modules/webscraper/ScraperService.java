package personal.investwallet.modules.webscraper;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ScraperService {

    @Value("${url.base}")
    private String baseUrl;

    public void scrapeWebsite(String assetType, String assetAcronym) {

        try {

            String path = baseUrl + "/" + assetType + "/" + assetAcronym;
            Document doc = Jsoup.connect(path).get();

            Elements yieldValueTag = doc.select("#dy-info > div > div.d-flex.align-items-center > strong");
            Elements basePriceTag = doc.select("#dy-info > div > div:nth-child(2) > div:nth-child(1) > div:nth-child(2) > div > b");
            Elements basePriceDateTag = doc.select("#dy-info > div > div:nth-child(2) > div:nth-child(3) > div:nth-child(1) > div > b");
            Elements basePaymentDateTag = doc.select("#dy-info > div > div:nth-child(2) > div:nth-child(3) > div:nth-child(2) > div > b");

            String yieldValue = yieldValueTag.text().replace(",", ".");
            String basePrice = basePriceTag.text().replace(",", ".");
            String basePriceDate = basePriceDateTag.text();
            String basePaymentDate = basePaymentDateTag.text();




        } catch (Exception ex) {
           throw new RuntimeException(ex);
        }
    }

    public boolean verifyIfWebsiteIsValid(String assetType, String assetAcronym) {

        try {

            String path = baseUrl + "/" + assetType + "/" + assetAcronym;
            Document doc = Jsoup.connect(path).get();

            String title = doc.title();
            String asset = title.substring(0, 6);

            return assetAcronym.toUpperCase().equals(asset);
        } catch (Exception ex) {
            ex.printStackTrace();

            return false;
        }
    }
}
