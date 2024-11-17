package personal.investwallet.modules.webscraper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import personal.investwallet.exceptions.ScraperProcessingException;
import personal.investwallet.modules.webscraper.dto.ScraperResponseDto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
public class ScraperService {

    @Value("${url.base.yield}")
    private String yieldBaseUrl;

    @Value("${url.base.assets}")
    private String assetsBaseUrl;

    public ScraperResponseDto fiiYieldScraping(String assetType, String assetAcronym) {

        try {

            String path = yieldBaseUrl + "/" + assetType + "/" + assetAcronym;
            Document doc = Jsoup.connect(path).get();

            Elements incomeValueTag = doc.select("#dy-info > div > div.d-flex.align-items-center > strong");
            Elements basePriceTag = doc.select("#dy-info > div > div:nth-child(2) > div:nth-child(1) > div:nth-child(2) > div > b");
            Elements basePriceDateTag = doc.select("#dy-info > div > div:nth-child(2) > div:nth-child(3) > div:nth-child(1) > div > b");
            Elements basePaymentDateTag = doc.select("#dy-info > div > div:nth-child(2) > div:nth-child(3) > div:nth-child(2) > div > b");

            String incomeValueText = incomeValueTag.text().replace(",", ".");
            String basePriceText = basePriceTag.text().replace(",", ".");
            String basePriceDateText = basePriceDateTag.text();
            String basePaymentDateText = basePaymentDateTag.text();

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            LocalDate basePriceLocalDate = LocalDate.parse(basePriceDateText, formatter);
            LocalDate basePaymentLocalDate = LocalDate.parse(basePaymentDateText, formatter);

            int year = basePaymentLocalDate.getYear();
            int month = basePaymentLocalDate.getMonthValue();

            String yieldAt = String.format("%04d%02d", year, month);
            BigDecimal incomeValue = new BigDecimal(incomeValueText);
            BigDecimal basePrice = new BigDecimal(basePriceText);
            Instant basePriceDate = basePriceLocalDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
            Instant basePaymentDate = basePaymentLocalDate.atStartOfDay(ZoneId.systemDefault()).toInstant();

            return new ScraperResponseDto(
                    assetAcronym,
                    incomeValue,
                    basePrice,
                    basePriceDate,
                    basePaymentDate,
                    yieldAt
            );

        } catch (Exception ex) {
           throw new ScraperProcessingException("Erro ao realizar web scraping, " + ex.getMessage());
        }
    }

}
