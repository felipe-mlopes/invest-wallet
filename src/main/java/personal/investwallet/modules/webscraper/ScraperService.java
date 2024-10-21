package personal.investwallet.modules.webscraper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import personal.investwallet.modules.webscraper.dto.ScraperResponseDto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
public class ScraperService {

    @Value("${url.base}")
    private String baseUrl;

    public ScraperResponseDto scrapeWebsite(String assetType, String assetAcronym) {

        try {

            String path = baseUrl + "/" + assetType + "/" + assetAcronym;
            Document doc = Jsoup.connect(path).get();

            Elements yieldValueTag = doc.select("#dy-info > div > div.d-flex.align-items-center > strong");
            Elements basePriceTag = doc.select("#dy-info > div > div:nth-child(2) > div:nth-child(1) > div:nth-child(2) > div > b");
            Elements basePriceDateTag = doc.select("#dy-info > div > div:nth-child(2) > div:nth-child(3) > div:nth-child(1) > div > b");
            Elements basePaymentDateTag = doc.select("#dy-info > div > div:nth-child(2) > div:nth-child(3) > div:nth-child(2) > div > b");

            String yieldValueText = yieldValueTag.text().replace(",", ".");
            String basePriceText = basePriceTag.text().replace(",", ".");
            String basePriceDateText = basePriceDateTag.text();
            String basePaymentDateText = basePaymentDateTag.text();

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            LocalDate basePriceLocalDate = LocalDate.parse(basePriceDateText, formatter);
            LocalDate basePaymentLocalDate = LocalDate.parse(basePaymentDateText, formatter);

            int year = basePaymentLocalDate.getYear();
            int month = basePaymentLocalDate.getMonthValue();

            String yieldAt = String.format("%04d%02d", year, month);
            BigDecimal yieldValue = new BigDecimal(yieldValueText);
            BigDecimal basePrice = new BigDecimal(basePriceText);
            Instant basePriceDate = basePriceLocalDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
            Instant basePaymentDate = basePaymentLocalDate.atStartOfDay(ZoneId.systemDefault()).toInstant();

            return new ScraperResponseDto(
                    yieldValue,
                    basePrice,
                    basePriceDate,
                    basePaymentDate,
                    yieldAt
            );

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
