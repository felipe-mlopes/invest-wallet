package personal.investwallet.modules.yield;

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
import personal.investwallet.modules.yield.dto.YieldRequestDto;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class YieldServiceUnitTest {

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
        class RegisterManyYieldsReceivedByCsv {

                @Test
                @DisplayName("Should be able to register all yields received in previous months by file in new entity")
                void shouldBeAbleToRegisterManyYieldsReceivedByCsvInNewEntity() {

                        String csvContent = """
                                        Asset Name, Yield At, Base Date, Payment Date, Base Price, Income Value, Yield Value
                                        ASSET1, 202311, 01/11/2023, 15/11/2023, 100.00, 5.00, 0.05
                                        ASSET2, 202311, 01/11/2023, 15/11/2023, 200.00, 10.00, 0.05
                                        """;

                        MultipartFile file = new MockMultipartFile(
                                        "test.csv",
                                        "test.csv",
                                        "text/csv",
                                        csvContent.getBytes());

                        when(assetService.getAssetTypeByAssetName("ASSET1")).thenReturn("fundos-imobiliarios");
                        when(assetService.getAssetTypeByAssetName("ASSET2")).thenReturn("fundos-imobiliarios");

                        @SuppressWarnings("unchecked")
                        ArgumentCaptor<List<YieldEntity>> yieldListCaptor = ArgumentCaptor.forClass(List.class);

                        int result = yieldService.registerManyYieldsReceivedByCsv(TOKEN, file);

                        assertEquals(2, result);
                        verify(yieldRepository).saveAll(yieldListCaptor.capture());
                }

                @Test
                @DisplayName("Should be able to register all yields received in previous months by file with existing entity")
                void shouldBeAbleToRegisterManyYieldsReceivedByCsvWithExistingEntity() {

                        String csvContent = """
                                        Asset Name, Yield At, Base Date, Payment Date, Base Price, Income Value, Yield Value
                                        ASSET1, 202311, 01/11/2023, 15/11/2023, 100.00, 5.00, 0.05
                                        """;

                        MultipartFile file = new MockMultipartFile(
                                        "test.csv",
                                        "test.csv",
                                        "text/csv",
                                        csvContent.getBytes());

                        YieldEntity existingEntity = new YieldEntity();
                        existingEntity.setUserId(USER_ID);

                        when(assetService.getAssetTypeByAssetName("ASSET1")).thenReturn("fundos-imobiliarios");

                        @SuppressWarnings("unchecked")
                        ArgumentCaptor<List<YieldEntity>> yieldListCaptor = ArgumentCaptor.forClass(List.class);

                        int result = yieldService.registerManyYieldsReceivedByCsv(TOKEN, file);

                        assertEquals(1, result);
                        verify(yieldRepository).saveAll(yieldListCaptor.capture());
                }

                @Test
                @DisplayName("Should not be able to register all yields received in previous months by file without sending file")
                void shouldNotBeAbleToRegisterManyYieldsReceivedByCsvWithoutSendingFile() {

                        EmptyFileException exception = assertThrows(EmptyFileException.class,
                                        () -> yieldService.registerManyYieldsReceivedByCsv(TOKEN, null));

                        assertEquals("O arquivo não enviado ou não preenchido", exception.getMessage());
                }

                @Test
                @DisplayName("Should not be able to register all yields received in previous months by file with empty file")
                void shouldNotBeAbleToRegisterManyYieldsReceivedByCsvWithEmptyFile() {

                        MultipartFile file = new MockMultipartFile(
                                        "test.csv",
                                        "test.csv",
                                        "text/csv",
                                        new byte[0]);

                        EmptyFileException exception = assertThrows(EmptyFileException.class,
                                        () -> yieldService.registerManyYieldsReceivedByCsv(TOKEN, file));

                        assertEquals("O arquivo não enviado ou não preenchido", exception.getMessage());
                }

                @Test
                @DisplayName("Should not be able to register all yields received in previous months by file with unnamed file")
                void shouldNotBeAbleToRegisterManyYieldsReceivedByCsvWithUnnamedFile() {

                        String csvContent = """
                                        Asset Name, Yield At, Base Date, Payment Date, Base Price, Income Value, Yield Value
                                        ASSET1, 202311, 01/11/2023, 15/11/2023, 100.00, 5.00, 0.05
                                        ASSET2, 202311, 01/11/2023, 15/11/2023, 200.00, 10.00, 0.05
                                        """;

                        MultipartFile file = new MockMultipartFile(
                                        "test",
                                        "test",
                                        "text/csv",
                                        csvContent.getBytes());

                        InvalidFileFormatException exception = assertThrows(InvalidFileFormatException.class,
                                        () -> yieldService.registerManyYieldsReceivedByCsv(TOKEN, file));

                        assertEquals("O arquivo deve ser um CSV válido", exception.getMessage());
                }

                @Test
                @DisplayName("Should not be able to register all yields received in previous months by file with invalid format file")
                void shouldNotBeAbleToRegisterManyYieldsReceivedByCsvWithInvalidFormatFile() {

                        String txtContent = "Oi";

                        MultipartFile file = new MockMultipartFile(
                                        "test.txt",
                                        "test.txt",
                                        "text/txt",
                                        txtContent.getBytes());

                        InvalidFileFormatException exception = assertThrows(InvalidFileFormatException.class,
                                        () -> yieldService.registerManyYieldsReceivedByCsv(TOKEN, file));

                        assertEquals("O arquivo deve ser um CSV válido", exception.getMessage());
                }

                @Test
                @DisplayName("Should not be able to register all yields received in previous months by file with invalid header format in file")
                void shouldNotBeAbleToRegisterManyYieldsReceivedByCsvWithInvalidHeaderFormatInFile() {

                        String invalidCsvHeader = """
                                        Invalid Header
                                        ASSET1, 202311, 01/11/2023, 15/11/2023, 100.00, 5.00, 0.05
                                        """;

                        MultipartFile file = new MockMultipartFile(
                                        "test.csv",
                                        "test.csv",
                                        "text/csv",
                                        invalidCsvHeader.getBytes());

                        InvalidFileFormatException exception = assertThrows(InvalidFileFormatException.class,
                                        () -> yieldService.registerManyYieldsReceivedByCsv(TOKEN, file));

                        assertEquals(
                                        "Formato de cabeçalho inválido. Esperado: Asset Name, Yield At, Base Date, Payment Date, Base Price, Income Value, Yield Value",
                                        exception.getMessage());
                }

                @Test
                @DisplayName("Should not be able to register all yields received in previous months by file with invalid header column name in file")
                void shouldNotBeAbleToRegisterManyYieldsReceivedByCsvWithInvalidHeaderColumnNameInFile() {

                        String invalidCsvHeader = """
                                        Asset Type, Yield At, Base Date, Payment Date, Base Price, Income Value, Yield Value
                                        ASSET1, 202311, 01/11/2023, 15/11/2023, 100.00, 5.00, 0.05
                                        """;

                        MultipartFile file = new MockMultipartFile(
                                        "test.csv",
                                        "test.csv",
                                        "text/csv",
                                        invalidCsvHeader.getBytes());

                        InvalidFileFormatException exception = assertThrows(InvalidFileFormatException.class,
                                        () -> yieldService.registerManyYieldsReceivedByCsv(TOKEN, file));

                        assertEquals(
                                        "Coluna inválida no cabeçalho. Esperado: 'Asset Name', Encontrado: 'Asset Type'",
                                        exception.getMessage());
                }

                @Test
                @DisplayName("Should not be able to register all yields received in previous months by file with invalid content containing only header filled in file")
                void shouldNotBeAbleToRegisterManyYieldsReceivedByCsvWithInvalidContentContainingOnlyHeaderFilledInFile() {

                        String invalidCsv = """
                                        Asset Name, Yield At, Base Date, Payment Date, Base Price, Income Value, Yield Value
                                        """;

                        MultipartFile file = new MockMultipartFile(
                                        "test.csv",
                                        "test.csv",
                                        "text/csv",
                                        invalidCsv.getBytes());

                        EmptyFileException exception = assertThrows(EmptyFileException.class,
                                        () -> yieldService.registerManyYieldsReceivedByCsv(TOKEN, file));

                        assertEquals(
                                        "Arquivo é inválido por estar vazio ou com apenas o cabeçalho preenchido",
                                        exception.getMessage());
                }

                @Test
                @DisplayName("Should not be able to register all yields received in previous months by file with invalid content containing too many columns in file")
                void shouldNotBeAbleToRegisterManyYieldsReceivedByCsvWithInvalidContentContainingTooManyColumnsInFile() {

                        String invalidCsvColumnn = """
                                        Asset Name, Yield At, Base Date, Payment Date, Base Price, Income Value, Yield Value
                                        ASSET1, 202311, 01/11/2023, 15/11/2023, 100.00, 5.00, 0.05, invalid-column
                                        """;

                        MultipartFile file = new MockMultipartFile(
                                        "test.csv",
                                        "test.csv",
                                        "text/csv",
                                        invalidCsvColumnn.getBytes());

                        InvalidFileFormatException exception = assertThrows(InvalidFileFormatException.class,
                                        () -> yieldService.registerManyYieldsReceivedByCsv(TOKEN, file));

                        assertEquals(
                                        "A linha 2 possui número incorreto de colunas",
                                        exception.getMessage());
                }

                @Test
                @DisplayName("Should not be able to register all yields received in previous months by file with invalid content containing an empty column in file")
                void shouldNotBeAbleToRegisterManyYieldsReceivedByCsvWithInvalidContentContainingAnEmptyColumnInFile() {

                        String invalidCsvColumnn = """
                                        Asset Name, Yield At, Base Date, Payment Date, Base Price, Income Value, Yield Value
                                        ASSET1, 202311, 01/11/2023, , 100.00, 5.00, 0.05
                                        """;

                        MultipartFile file = new MockMultipartFile(
                                        "test.csv",
                                        "test.csv",
                                        "text/csv",
                                        invalidCsvColumnn.getBytes());

                        InvalidFileFormatException exception = assertThrows(InvalidFileFormatException.class,
                                        () -> yieldService.registerManyYieldsReceivedByCsv(TOKEN, file));

                        assertEquals(
                                        "Na linha 2, a coluna 4 está vazia",
                                        exception.getMessage());
                }

                @Test
                @DisplayName("Should not be able to register all yields received in previous months by file with invalid yield at size format in file")
                void shouldNotBeAbleToRegisterManyYieldsReceivedByCsvWithInvalidYieldAtSizeFormatInFile() {

                        String invalidDateCsvContent = """
                                        Asset Name, Yield At, Base Date, Payment Date, Base Price, Income Value, Yield Value
                                        ASSET1, 20311, 31/10/2023, 15/11/2023, 100.00, 5.00, 0.05
                                        """;

                        MultipartFile file = new MockMultipartFile(
                                        "test.csv",
                                        "test.csv",
                                        "text/csv",
                                        invalidDateCsvContent.getBytes());

                        InvalidStringFormatException exception = assertThrows(InvalidStringFormatException.class,
                                        () -> yieldService.registerManyYieldsReceivedByCsv(TOKEN, file));

                        assertEquals(
                                        "O yieldAt deve conter apenas 6 caracteres contendo o ano (yyyy) e o mês (mm)",
                                        exception.getMessage());
                }

                @Test
                @DisplayName("Should not be able to register all yields received in previous months by file with invalid yield at year format in file")
                void shouldNotBeAbleToRegisterManyYieldsReceivedByCsvWithInvalidYieldAtYearFormatInFile() {

                        String invalidDateCsvContent = """
                                        Asset Name, Yield At, Base Date, Payment Date, Base Price, Income Value, Yield Value
                                        ASSET1, 250011, 31/10/2023, 15/11/2023, 100.00, 5.00, 0.05
                                        """;

                        MultipartFile file = new MockMultipartFile(
                                        "test.csv",
                                        "test.csv",
                                        "text/csv",
                                        invalidDateCsvContent.getBytes());

                        InvalidStringFormatException exception = assertThrows(InvalidStringFormatException.class,
                                        () -> yieldService.registerManyYieldsReceivedByCsv(TOKEN, file));

                        assertEquals(
                                        "O ano informado no YieldAt deve ter 4 caracteres e ser menor ou igual ao ano corrente",
                                        exception.getMessage());
                }

                @Test
                @DisplayName("Should not be able to register all yields received in previous months by file with invalid yield at month format in file")
                void shouldNotBeAbleToRegisterManyYieldsReceivedByCsvWithInvalidYieldAtYearMonthInFile() {

                        String invalidDateCsvContent = """
                                        Asset Name, Yield At, Base Date, Payment Date, Base Price, Income Value, Yield Value
                                        ASSET1, 202315, 31/10/2023, 15/11/2023, 100.00, 5.00, 0.05
                                        """;

                        MultipartFile file = new MockMultipartFile(
                                        "test.csv",
                                        "test.csv",
                                        "text/csv",
                                        invalidDateCsvContent.getBytes());

                        InvalidStringFormatException exception = assertThrows(InvalidStringFormatException.class,
                                        () -> yieldService.registerManyYieldsReceivedByCsv(TOKEN, file));

                        assertEquals(
                                        "O mês informado no YieldAt deve ter 2 caracteres e ser válido",
                                        exception.getMessage());
                }

                @Test
                @DisplayName("Should not be able to register all yields received in previous months by file with invalid date format in file")
                void shouldNotBeAbleToRegisterManyYieldsReceivedByCsvWithInvalidDateFormatInFile() {

                        String invalidDateCsvContent = """
                                        Asset Name, Yield At, Base Date, Payment Date, Base Price, Income Value, Yield Value
                                        ASSET1, 202311, invalid-date, 15/11/2023, 100.00, 5.00, 0.05
                                        """;

                        MultipartFile file = new MockMultipartFile(
                                        "test.csv",
                                        "test.csv",
                                        "text/csv",
                                        invalidDateCsvContent.getBytes());

                        InvalidDateFormatException exception = assertThrows(InvalidDateFormatException.class,
                                        () -> yieldService.registerManyYieldsReceivedByCsv(TOKEN, file));

                        assertEquals(
                                        "Erro na linha 2, Data Base: formato de data inválido. Use dd/MM/yyyy",
                                        exception.getMessage());
                }

                @Test
                @DisplayName("Should not be able to register all yields received in previous months by file with base date greater than payment date in file")
                void shouldNotBeAbleToRegisterManyYieldsReceivedByCsvWithBaseDateGreaterThanPaymentDateInFile() {

                        String invalidDateCsvContent = """
                                        Asset Name, Yield At, Base Date, Payment Date, Base Price, Income Value, Yield Value
                                        ASSET1, 202311, 30/11/2023, 15/11/2023, 100.00, 5.00, 0.05
                                        """;

                        MultipartFile file = new MockMultipartFile(
                                        "test.csv",
                                        "test.csv",
                                        "text/csv",
                                        invalidDateCsvContent.getBytes());

                        InvalidDateFormatException exception = assertThrows(InvalidDateFormatException.class,
                                        () -> yieldService.registerManyYieldsReceivedByCsv(TOKEN, file));

                        assertEquals(
                                        "A data de pagamento precisa ser maior que a data base de cálculo do dividendo",
                                        exception.getMessage());
                }

                @Test
                @DisplayName("Should not be able to register all yields received in previous months by file with date years greater than current year in file")
                void shouldNotBeAbleToRegisterManyYieldsReceivedByCsvWithDateYearsGreaterThanCurrentYearInFile() {

                        String invalidDateCsvContent = """
                                        Asset Name, Yield At, Base Date, Payment Date, Base Price, Income Value, Yield Value
                                        ASSET1, 202311, 30/10/2025, 15/11/2025, 100.00, 5.00, 0.05
                                        """;

                        MultipartFile file = new MockMultipartFile(
                                        "test.csv",
                                        "test.csv",
                                        "text/csv",
                                        invalidDateCsvContent.getBytes());

                        InvalidDateFormatException exception = assertThrows(InvalidDateFormatException.class,
                                        () -> yieldService.registerManyYieldsReceivedByCsv(TOKEN, file));

                        assertEquals(
                                        "O ano da data base e/ou da data de pagamento precisa ser menor ou igual a ano corrente",
                                        exception.getMessage());
                }

                @Test
                @DisplayName("Should not be able to register all yields received in previous months by file with invalid number format in file")
                void shouldNotBeAbleToRegisterManyYieldsReceivedByCsvWithInvalidNumberFormatInFile() {

                        String invalidNumberCsvContent = """
                                        Asset Name, Yield At, Base Date, Payment Date, Base Price, Income Value, Yield Value
                                        ASSET1, 202311, 01/11/2023, 15/11/2023, invalid-number, 5.00, 0.05
                                        """;

                        MultipartFile file = new MockMultipartFile(
                                        "test.csv",
                                        "test.csv",
                                        "text/csv",
                                        invalidNumberCsvContent.getBytes());

                        InvalidNumberFormatException exception = assertThrows(InvalidNumberFormatException.class,
                                        () -> yieldService.registerManyYieldsReceivedByCsv(TOKEN, file));

                        assertEquals(
                                        "Erro na linha 2, Preço Base: valor numérico inválido",
                                        exception.getMessage());
                }

                @Test
                @DisplayName("Should not be able to register all yields received in previous months by file with invalid number format in file")
                void shouldNotBeAbleToRegisterManyYieldsReceivedByCsvWithInvalidNumberFormatInYieldAtFile() {

                        String invalidNumberCsvContent = """
                                        Asset Name, Yield At, Base Date, Payment Date, Base Price, Income Value, Yield Value
                                        ASSET1, 20X311, 01/11/2023, 15/11/2023, 100.00, 5.00, 0.05
                                        """;

                        MultipartFile file = new MockMultipartFile(
                                        "test.csv",
                                        "test.csv",
                                        "text/csv",
                                        invalidNumberCsvContent.getBytes());

                        InvalidNumberFormatException exception = assertThrows(InvalidNumberFormatException.class,
                                        () -> yieldService.registerManyYieldsReceivedByCsv(TOKEN, file));

                        assertEquals(
                                        "Erro na linha 2: For input string: \"20X3\"",
                                        exception.getMessage());
                }

                @Test
                @DisplayName("Should not be able to register all yields received in previous months by file with asset not exist")
                void shouldNotBeAbleToRegisterManyYieldsReceivedByCsvWithAssetNotExist() {

                        String csvContent = """
                                        Asset Name, Yield At, Base Date, Payment Date, Base Price, Income Value, Yield Value
                                        ASSET1, 202311, 01/11/2023, 15/11/2023, 100.00, 5.00, 0.05
                                        """;

                        MultipartFile file = new MockMultipartFile(
                                        "test.csv",
                                        "test.csv",
                                        "text/csv",
                                        csvContent.getBytes());

                        when(tokenService.extractUserIdFromToken(TOKEN)).thenReturn(USER_ID);

                        ResourceNotFoundException expectedEx = new ResourceNotFoundException(
                                        "O ativo ASSET1 informado não existe.");
                        when(assetService.getAssetTypeByAssetName("ASSET1")).thenThrow(expectedEx);

                        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                                        () -> yieldService.registerManyYieldsReceivedByCsv(
                                                        TOKEN, file));
                        assertEquals("O ativo ASSET1 informado não existe.", exception.getMessage());
                }

                @Test
                @DisplayName("Should not be able to register all yields received in previous months by file when file processing error occurs")
                void shouldNotBeAbleToRegisterAllYieldsReceivedInPreviousMontyByFileWhenFileProcessingErrorOccurs()
                                throws IOException {

                        lenient().when(mockFile.getOriginalFilename()).thenReturn("test.csv");
                        lenient().when(mockFile.getContentType()).thenReturn("text/csv");
                        when(mockFile.getInputStream()).thenThrow(IOException.class);

                        FileProcessingException exception = assertThrows(FileProcessingException.class,
                                        () -> yieldService.registerManyYieldsReceivedByCsv(
                                                        TOKEN, mockFile));
                        assertEquals("Erro ao ler o arquivo CSV", exception.getMessage());
                }
        }

        @Nested
        class registerAllYieldsReceivedInTheMonth {

                @Test
                void shouldBeAbleToRegisterAllYieldsReceivedInTheMonth() {

                        List<YieldRequestDto> assets = new ArrayList<>();

                        when(assetService.getAssetTypeByAssetName("ASSET1")).thenReturn("fundos-imobiliarios");

                        Instant baseDate = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
                        Instant paymentDate = LocalDate.now().plusDays(15).atStartOfDay(ZoneId.systemDefault())
                                        .toInstant();

                        ScraperResponseDto scraperResponseDto = new ScraperResponseDto(
                                        anyString(),
                                        new BigDecimal("100.00"),
                                        new BigDecimal("0.05"),
                                        baseDate,
                                        paymentDate,
                                        getYieldAt());

                        when(tokenService.extractUserIdFromToken(TOKEN)).thenReturn(USER_ID);
                        when(scraperService.fiiYieldScraping("fundos-imobiliarios", "ASSET1"))
                                        .thenReturn(scraperResponseDto);

                        yieldService.registerManyYieldsReceived(TOKEN, assets);

                        verify(yieldRepository).save(any(YieldEntity.class));
                }
        }

        private static String getYieldAt() {
                LocalDate today = LocalDate.now();
                return today.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM"));
        }
}
