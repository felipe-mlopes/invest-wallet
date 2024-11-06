package personal.investwallet.modules.yield;

import com.opencsv.CSVReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import personal.investwallet.exceptions.*;
import personal.investwallet.modules.asset.AssetService;
import personal.investwallet.modules.webscraper.ScraperService;
import personal.investwallet.modules.webscraper.dto.ScraperResponseDto;
import personal.investwallet.security.TokenService;

import java.io.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class YieldServiceTest {

    @Mock
    private TokenService tokenService;

    @Mock
    private AssetService assetService;

    @Mock
    private ScraperService scraperService;

    @Mock
    private MultipartFile mockFile;

    @Mock
    private YieldRepository yieldRepository;

    @InjectMocks
    private YieldService yieldService;

    public static final String TOKEN = "validToken";
    public static final String USER_ID = "user1234";

    @BeforeEach
    void setUp() {
        when(tokenService.extractUserIdFromToken(TOKEN)).thenReturn(USER_ID);
    }

    @Nested
    class registerAllYieldsReceivedInPreviousMonthsByFile {

        @Test
        @DisplayName("Should be able to register all yields received in previous months by file in new entity")
        void shouldBeAbleToRegisterAllYieldsReceivedInPreviousMonthsByFileInNewEntity() {

            String csvContent = """
            Asset Name;Yield At;Base Date;Payment Date;Base Price;Income Value;Yield Value
            ASSET1;202311;01/11/2023;15/11/2023;100.00;5.00;0.05
            ASSET2;202311;01/11/2023;15/11/2023;200.00;10.00;0.05
            """;

            MultipartFile file = new MockMultipartFile(
                    "test.csv",
                    "test.csv",
                    "text/csv",
                    csvContent.getBytes()
            );

            when(assetService.getAssetTypeByAssetName("ASSET1")).thenReturn("fundos-imobiliarios");
            when(assetService.getAssetTypeByAssetName("ASSET2")).thenReturn("fundos-imobiliarios");
            when(yieldRepository.save(any(YieldEntity.class))).thenAnswer(i -> i.getArguments()[0]);

            ArgumentCaptor<YieldEntity> yieldCaptor = ArgumentCaptor.forClass(YieldEntity.class);

            int result = yieldService.registerAllYieldsReceivedInPreviousMonthsByFile(TOKEN, file);

            verify(yieldRepository).save(yieldCaptor.capture());
            assertEquals(2, result);
        }

        @Test
        @DisplayName("Should be able to register all yields received in previous months by file with existing entity")
        void shouldBeAbleToRegisterAllYieldsReceivedInPreviousMonthsByFileWithExistingEntity() {

            String csvContent = """
                Asset Name;Yield At;Base Date;Payment Date;Base Price;Income Value;Yield Value
                ASSET1;202311;01/11/2023;15/11/2023;100.00;5.00;0.05
                """;

            MultipartFile file = new MockMultipartFile(
                    "test.csv",
                    "test.csv",
                    "text/csv",
                    csvContent.getBytes()
            );

            YieldEntity existingEntity = new YieldEntity();
            existingEntity.setUserId(USER_ID);

            when(assetService.getAssetTypeByAssetName("ASSET1")).thenReturn("fundos-imobiliarios");
            when(yieldRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existingEntity));

            ArgumentCaptor<YieldEntity> yieldCaptor = ArgumentCaptor.forClass(YieldEntity.class);

            int result = yieldService.registerAllYieldsReceivedInPreviousMonthsByFile(TOKEN, file);

            assertEquals(1, result);
            verify(yieldRepository).save(yieldCaptor.capture());
        }

        @Test
        @DisplayName("Should not be able to register all yields received in previous months by file without sending file")
        void shouldNotBeAbleToRegisterAllYieldsReceivedInPreviousMonthsByFileWithoutSendingFile() {

            EmptyFileException exception = assertThrows(EmptyFileException.class, () ->
                    yieldService.registerAllYieldsReceivedInPreviousMonthsByFile(TOKEN, null));

            assertEquals("O arquivo não enviado ou não preenchido", exception.getMessage());
        }

        @Test
        @DisplayName("Should not be able to register all yields received in previous months by file with empty file")
        void shouldNotBeAbleToRegisterAllYieldsReceivedInPreviousMonthsByFileWithEmptyFile() {

            MultipartFile file = new MockMultipartFile(
                    "test.csv",
                    "test.csv",
                    "text/csv",
                    new byte[0]
            );

            EmptyFileException exception = assertThrows(EmptyFileException.class, () ->
                    yieldService.registerAllYieldsReceivedInPreviousMonthsByFile(TOKEN, file));

            assertEquals("O arquivo não enviado ou não preenchido", exception.getMessage());
        }

        @Test
        @DisplayName("Should not be able to register all yields received in previous months by file with unnamed file")
        void shouldNotBeAbleToRegisterAllYieldsReceivedInPreviousMonthsByFileWithUnnamedFile() {

            String csvContent = """
            Asset Name;Yield At;Base Date;Payment Date;Base Price;Income Value;Yield Value
            ASSET1;202311;01/11/2023;15/11/2023;100.00;5.00;0.05
            ASSET2;202311;01/11/2023;15/11/2023;200.00;10.00;0.05
            """;

            MultipartFile file = new MockMultipartFile(
                    "test",
                    "test",
                    "text/csv",
                    csvContent.getBytes()
            );

            InvalidFileFormatException exception = assertThrows(InvalidFileFormatException.class, () ->
                    yieldService.registerAllYieldsReceivedInPreviousMonthsByFile(TOKEN, file));

            assertEquals("O arquivo deve ser um CSV válido", exception.getMessage());
        }

        @Test
        @DisplayName("Should not be able to register all yields received in previous months by file with invalid format file")
        void shouldNotBeAbleToRegisterAllYieldsReceivedInPreviousMonthsByFileWithInvalidFormatFile() {

            String txtContent = "Oi";

            MultipartFile file = new MockMultipartFile(
                    "test.txt",
                    "test.txt",
                    "text/txt",
                    txtContent.getBytes()
            );

            InvalidFileFormatException exception = assertThrows(InvalidFileFormatException.class, () ->
                    yieldService.registerAllYieldsReceivedInPreviousMonthsByFile(TOKEN, file));

            assertEquals("O arquivo deve ser um CSV válido", exception.getMessage());
        }

        @Test
        @DisplayName("Should not be able to register all yields received in previous months by file with invalid header format in file")
        void shouldNotBeAbleToRegisterAllYieldsReceivedInPreviousMonthsByFileWithInvalidHeaderFormatInFile() {

            String invalidCsvHeader = """
            Invalid Header
            ASSET1;202311;01/11/2023;15/11/2023;100.00;5.00;0.05
            """;

            MultipartFile file = new MockMultipartFile(
                    "test.csv",
                    "test.csv",
                    "text/csv",
                    invalidCsvHeader.getBytes()
            );

            InvalidFileFormatException exception = assertThrows(InvalidFileFormatException.class, () ->
                    yieldService.registerAllYieldsReceivedInPreviousMonthsByFile(TOKEN, file));

            assertEquals(
                    "Formato de cabeçalho inválido. Esperado: Asset Name, Yield At, Base Date, Payment Date, Base Price, Income Value, Yield Value",
                    exception.getMessage()
            );
        }

        @Test
        @DisplayName("Should not be able to register all yields received in previous months by file with invalid header column name in file")
        void shouldNotBeAbleToRegisterAllYieldsReceivedInPreviousMonthsByFileWithInvalidHeaderColumnNameInFile() {

            String invalidCsvHeader = """
            Asset Type;Yield At;Base Date;Payment Date;Base Price;Income Value;Yield Value
            ASSET1;202311;01/11/2023;15/11/2023;100.00;5.00;0.05
            """;

            MultipartFile file = new MockMultipartFile(
                    "test.csv",
                    "test.csv",
                    "text/csv",
                    invalidCsvHeader.getBytes()
            );

            InvalidFileFormatException exception = assertThrows(InvalidFileFormatException.class, () ->
                    yieldService.registerAllYieldsReceivedInPreviousMonthsByFile(TOKEN, file));

            assertEquals(
                    "Coluna inválida no cabeçalho. Esperado: 'Asset Name', Encontrado: 'Asset Type'",
                    exception.getMessage()
            );
        }

        @Test
        @DisplayName("Should not be able to register all yields received in previous months by file with invalid content containing only header filled in file")
        void shouldNotBeAbleToRegisterAllYieldsReceivedInPreviousMonthsByFileWithInvalidContentContainingOnlyHeaderFilledInFile() {

            String invalidCsv = """
            Asset Name;Yield At;Base Date;Payment Date;Base Price;Income Value;Yield Value
            """;

            MultipartFile file = new MockMultipartFile(
                    "test.csv",
                    "test.csv",
                    "text/csv",
                    invalidCsv.getBytes()
            );

            EmptyFileException exception = assertThrows(EmptyFileException.class, () ->
                    yieldService.registerAllYieldsReceivedInPreviousMonthsByFile(TOKEN, file));

            assertEquals(
                    "Arquivo é inválido por estar vazio ou com apenas o cabeçalho preenchido",
                    exception.getMessage()
            );
        }

        @Test
        @DisplayName("Should not be able to register all yields received in previous months by file with invalid content containing too many columns in file")
        void shouldNotBeAbleToRegisterAllYieldsReceivedInPreviousMonthsByFileWithInvalidContentContainingTooManyColumnsInFile() {

            String invalidCsvColumnn = """
            Asset Name;Yield At;Base Date;Payment Date;Base Price;Income Value;Yield Value
            ASSET1;202311;01/11/2023;15/11/2023;100.00;5.00;0.05;invalid-column
            """;

            MultipartFile file = new MockMultipartFile(
                    "test.csv",
                    "test.csv",
                    "text/csv",
                    invalidCsvColumnn.getBytes()
            );

            InvalidFileFormatException exception = assertThrows(InvalidFileFormatException.class, () ->
                    yieldService.registerAllYieldsReceivedInPreviousMonthsByFile(TOKEN, file));

            assertEquals(
                    "A linha 2 possui número incorreto de colunas",
                    exception.getMessage()
            );
        }

        @Test
        @DisplayName("Should not be able to register all yields received in previous months by file with invalid content containing an empty column in file")
        void shouldNotBeAbleToRegisterAllYieldsReceivedInPreviousMonthsByFileWithInvalidContentContainingAnEmptyColumnInFile() {

            String invalidCsvColumnn = """
            Asset Name;Yield At;Base Date;Payment Date;Base Price;Income Value;Yield Value
            ASSET1;202311;01/11/2023; ;100.00;5.00;0.05
            """;

            MultipartFile file = new MockMultipartFile(
                    "test.csv",
                    "test.csv",
                    "text/csv",
                    invalidCsvColumnn.getBytes()
            );

            InvalidFileFormatException exception = assertThrows(InvalidFileFormatException.class, () ->
                    yieldService.registerAllYieldsReceivedInPreviousMonthsByFile(TOKEN, file));

            assertEquals(
                    "Na linha 2, a coluna 4 está vazia",
                    exception.getMessage()
            );
        }

        @Test
        @DisplayName("Should not be able to register all yields received in previous months by file with invalid yield at size format in file")
        void shouldNotBeAbleToRegisterAllYieldsReceivedInPreviousMonthsByFileWithInvalidYieldAtSizeFormatInFile() {

            String invalidDateCsvContent = """
                Asset Name;Yield At;Base Date;Payment Date;Base Price;Income Value;Yield Value
                ASSET1;20311;31/10/2023;15/11/2023;100.00;5.00;0.05
                """;

            MultipartFile file = new MockMultipartFile(
                    "test.csv",
                    "test.csv",
                    "text/csv",
                    invalidDateCsvContent.getBytes()
            );

            InvalidStringFormatException exception = assertThrows(InvalidStringFormatException.class, () ->
                    yieldService.registerAllYieldsReceivedInPreviousMonthsByFile(TOKEN, file));

            assertEquals(
                    "O yieldAt deve conter apenas 6 caracteres contendo o ano (yyyy) e o mês (mm)",
                    exception.getMessage()
            );
        }

        @Test
        @DisplayName("Should not be able to register all yields received in previous months by file with invalid yield at year format in file")
        void shouldNotBeAbleToRegisterAllYieldsReceivedInPreviousMonthsByFileWithInvalidYieldAtYearFormatInFile() {

            String invalidDateCsvContent = """
                Asset Name;Yield At;Base Date;Payment Date;Base Price;Income Value;Yield Value
                ASSET1;250011;31/10/2023;15/11/2023;100.00;5.00;0.05
                """;

            MultipartFile file = new MockMultipartFile(
                    "test.csv",
                    "test.csv",
                    "text/csv",
                    invalidDateCsvContent.getBytes()
            );

            InvalidStringFormatException exception = assertThrows(InvalidStringFormatException.class, () ->
                    yieldService.registerAllYieldsReceivedInPreviousMonthsByFile(TOKEN, file));

            assertEquals(
                    "O ano informado no YieldAt deve ter 4 caracteres e ser menor ou igual ao ano corrente",
                    exception.getMessage()
            );
        }

        @Test
        @DisplayName("Should not be able to register all yields received in previous months by file with invalid yield at month format in file")
        void shouldNotBeAbleToRegisterAllYieldsReceivedInPreviousMonthsByFileWithInvalidYieldAtYearMonthInFile() {

            String invalidDateCsvContent = """
                Asset Name;Yield At;Base Date;Payment Date;Base Price;Income Value;Yield Value
                ASSET1;202315;31/10/2023;15/11/2023;100.00;5.00;0.05
                """;

            MultipartFile file = new MockMultipartFile(
                    "test.csv",
                    "test.csv",
                    "text/csv",
                    invalidDateCsvContent.getBytes()
            );

            InvalidStringFormatException exception = assertThrows(InvalidStringFormatException.class, () ->
                    yieldService.registerAllYieldsReceivedInPreviousMonthsByFile(TOKEN, file));

            assertEquals(
                    "O mês informado no YieldAt deve ter 2 caracteres e ser válido",
                    exception.getMessage()
            );
        }

        @Test
        @DisplayName("Should not be able to register all yields received in previous months by file with invalid date format in file")
        void shouldNotBeAbleToRegisterAllYieldsReceivedInPreviousMonthsByFileWithInvalidDateFormatInFile() {

            String invalidDateCsvContent = """
                Asset Name;Yield At;Base Date;Payment Date;Base Price;Income Value;Yield Value
                ASSET1;202311;invalid-date;15/11/2023;100.00;5.00;0.05
                """;

            MultipartFile file = new MockMultipartFile(
                    "test.csv",
                    "test.csv",
                    "text/csv",
                    invalidDateCsvContent.getBytes()
            );

            InvalidDateFormatException exception = assertThrows(InvalidDateFormatException.class, () ->
                    yieldService.registerAllYieldsReceivedInPreviousMonthsByFile(TOKEN, file));

            assertEquals(
                    "Erro na linha 2, Data Base: formato de data inválido. Use dd/MM/yyyy",
                    exception.getMessage()
            );
        }

        @Test
        @DisplayName("Should not be able to register all yields received in previous months by file with base date greater than payment date in file")
        void shouldNotBeAbleToRegisterAllYieldsReceivedInPreviousMonthsByFileWithBaseDateGreaterThanPaymentDateInFile() {

            String invalidDateCsvContent = """
                Asset Name;Yield At;Base Date;Payment Date;Base Price;Income Value;Yield Value
                ASSET1;202311;30/11/2023;15/11/2023;100.00;5.00;0.05
                """;

            MultipartFile file = new MockMultipartFile(
                    "test.csv",
                    "test.csv",
                    "text/csv",
                    invalidDateCsvContent.getBytes()
            );

            InvalidDateFormatException exception = assertThrows(InvalidDateFormatException.class, () ->
                    yieldService.registerAllYieldsReceivedInPreviousMonthsByFile(TOKEN, file));

            assertEquals(
                    "A data de pagamento precisa ser maior que a data base de cálculo do dividendo",
                    exception.getMessage()
            );
        }

        @Test
        @DisplayName("Should not be able to register all yields received in previous months by file with date years greater than current year in file")
        void shouldNotBeAbleToRegisterAllYieldsReceivedInPreviousMonthsByFileWithDateYearsGreaterThanCurrentYearInFile() {

            String invalidDateCsvContent = """
                Asset Name;Yield At;Base Date;Payment Date;Base Price;Income Value;Yield Value
                ASSET1;202311;30/10/2025;15/11/2025;100.00;5.00;0.05
                """;

            MultipartFile file = new MockMultipartFile(
                    "test.csv",
                    "test.csv",
                    "text/csv",
                    invalidDateCsvContent.getBytes()
            );

            InvalidDateFormatException exception = assertThrows(InvalidDateFormatException.class, () ->
                    yieldService.registerAllYieldsReceivedInPreviousMonthsByFile(TOKEN, file));

            assertEquals(
                    "O ano da data base e/ou da data de pagamento precisa ser menor ou igual a ano corrente",
                    exception.getMessage()
            );
        }

        @Test
        @DisplayName("Should not be able to register all yields received in previous months by file with invalid number format in file")
        void shouldNotBeAbleToRegisterAllYieldsReceivedInPreviousMonthsByFileWithInvalidNumberFormatInFile() {

            String invalidNumberCsvContent = """
                Asset Name;Yield At;Base Date;Payment Date;Base Price;Income Value;Yield Value
                ASSET1;202311;01/11/2023;15/11/2023;invalid-number;5.00;0.05
                """;

            MultipartFile file = new MockMultipartFile(
                    "test.csv",
                    "test.csv",
                    "text/csv",
                    invalidNumberCsvContent.getBytes()
            );

            InvalidNumberFormatException exception = assertThrows(InvalidNumberFormatException.class, () ->
                    yieldService.registerAllYieldsReceivedInPreviousMonthsByFile(TOKEN, file));

            assertEquals(
                    "Erro na linha 2, Preço Base: valor numérico inválido",
                    exception.getMessage()
            );
        }

        @Test
        @DisplayName("Should not be able to register all yields received in previous months by file with invalid number format in file")
        void shouldNotBeAbleToRegisterAllYieldsReceivedInPreviousMonthsByFileWithInvalidNumberFormatInYieldAtFile() {

            String invalidNumberCsvContent = """
                Asset Name;Yield At;Base Date;Payment Date;Base Price;Income Value;Yield Value
                ASSET1;20X311;01/11/2023;15/11/2023;100.00;5.00;0.05
                """;

            MultipartFile file = new MockMultipartFile(
                    "test.csv",
                    "test.csv",
                    "text/csv",
                    invalidNumberCsvContent.getBytes()
            );

            InvalidNumberFormatException exception = assertThrows(InvalidNumberFormatException.class, () ->
                    yieldService.registerAllYieldsReceivedInPreviousMonthsByFile(TOKEN, file));

            assertEquals(
                    "Erro na linha 2: For input string: \"20X3\"",
                    exception.getMessage()
            );
        }

        @Test
        @DisplayName("Should not be able to register all yields received in previous months by file with asset not exist")
        void shouldNotBeAbleToRegisterAllYieldsReceivedInPreviousMonthsByFileWithAssetNotExist() {

            String csvContent = """
                Asset Name;Yield At;Base Date;Payment Date;Base Price;Income Value;Yield Value
                ASSET1;202311;01/11/2023;15/11/2023;100.00;5.00;0.05
                """;

            MultipartFile file = new MockMultipartFile(
                    "test.csv",
                    "test.csv",
                    "text/csv",
                    csvContent.getBytes()
            );

            when(tokenService.extractUserIdFromToken(TOKEN)).thenReturn(USER_ID);
            when(assetService.getAssetTypeByAssetName("ASSET1")).thenReturn(null);

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> yieldService.registerAllYieldsReceivedInPreviousMonthsByFile(
                    TOKEN, file
            ));
            assertEquals("O ativo ASSET1 informado não existe.", exception.getMessage());
        }

        @Test
        @DisplayName("Should not be able to register all yields received in previous months by file when file processing error occurs")
        void shouldNotBeAbleToRegisterAllYieldsReceivedInPreviousMontyByFileWhenFileProcessingErrorOccurs() throws IOException {

            lenient().when(mockFile.getOriginalFilename()).thenReturn("test.csv");
            lenient().when(mockFile.getContentType()).thenReturn("text/csv");
            when(mockFile.getInputStream()).thenThrow(IOException.class);

            FileProcessingException exception = assertThrows(FileProcessingException.class, () -> yieldService.registerAllYieldsReceivedInPreviousMonthsByFile(
                    TOKEN, mockFile
            ));
            assertEquals("Erro ao ler o arquivo CSV", exception.getMessage());
        }
    }

    @Nested
    class registerAllYieldsReceivedInTheMonth {

        @Test
        void shouldBeAbleToRegisterAllYieldsReceivedInTheMonth() {

            List<Object> assets = new ArrayList<>();
            Map<String, Object> asset1 = new HashMap<>();
            asset1.put("assetName", "ASSET1");
            asset1.put("assetQuotaAmount", 100);
            assets.add(asset1);

            when(assetService.getAssetTypeByAssetName("ASSET1")).thenReturn("fundos-imobiliarios");

            Instant baseDate = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
            Instant paymentDate = LocalDate.now().plusDays(15).atStartOfDay(ZoneId.systemDefault()).toInstant();

            ScraperResponseDto scraperResponseDto = new ScraperResponseDto(
                    new BigDecimal("100.00"),
                    new BigDecimal("0.05"),
                    baseDate,
                    paymentDate,
                    getYieldAt()
            );

            when(tokenService.extractUserIdFromToken(TOKEN)).thenReturn(USER_ID);
            when(scraperService.yieldScraping("fundos-imobiliarios", "ASSET1")).thenReturn(scraperResponseDto);
            when(yieldRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            yieldService.registerAllYieldsReceivedInTheMonth(TOKEN, assets);

            verify(yieldRepository).save(any(YieldEntity.class));
        }
    }

    private static String getYieldAt() {
        LocalDate today = LocalDate.now();
        return today.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM"));
    }
}
