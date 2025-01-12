package personal.investwallet.modules.asset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import personal.investwallet.exceptions.ConflictException;
import personal.investwallet.exceptions.EmptyFileException;
import personal.investwallet.exceptions.FileProcessingException;
import personal.investwallet.exceptions.ResourceNotFoundException;
import personal.investwallet.modules.asset.dto.AssetInfoDto;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
public class AssetServiceUnitTest {

    @Mock
    private AssetRepository assetRepository;

    @InjectMocks
    private AssetService assetService;

    private static final String FILE_CONTENT = """
            COTAHIST.2024
            012024010202PETR4      010PETROBRAS   PN      R$  000000000263000000000026300000000002630000000000263000000000026300000000002630000000000263000000000026300000000000000000000010000000000000PETRN
            012024100112ABCD11      010FII BARIGUI CI  ER       R$
            012024100112XYZW11      010FII BARIGUI CI  ER       R$
            TOTAL GERAL
            """;

    @BeforeEach
    void setUp() {
        lenient().when(assetRepository.findAllAssetNames()).thenReturn(Stream.empty());
    }

    @Nested
    class readTxt {

        @Test
        @DisplayName("Should be able to read txt file")
        void shouldBeAbleToReadTxtFile() throws IOException {

            MultipartFile file = new MockMultipartFile(
                    "file",
                    "assets.txt",
                    "text/plain",
                    FILE_CONTENT.getBytes(StandardCharsets.UTF_8));

            String result = assetService.readTxtFile(file);

            verify(assetRepository, times(1)).saveAll(anyList());
            assertEquals("Successfully processed and saved 3 assets.", result);
        }

        @Test
        @DisplayName("Should not be able to read txt file with empty file")
        void shouldNotBeAbleToReadTxtFileWithEmptyFile() {

            MockMultipartFile invalidFile = new MockMultipartFile(
                    "invalidFile",
                    "test.txt",
                    "text/plain",
                    new byte[0]);

            EmptyFileException exception = assertThrows(EmptyFileException.class,
                    () -> assetService.readTxtFile(invalidFile));
            assertEquals("O arquivo é inválido por estar vazio", exception.getMessage());
        }

        @Test
        @DisplayName("Should not be able to read txt file with a different format")
        void shouldNotBeAbleToReadTxtFileWithADifferentFormat() {

            String fileContent = """

                    012024100112ABCD11      010FII BARIGUI CI  ER       R$
                    012024100112XYZW11      010FII BARIGUI CI  ER       R$
                    """;

            MultipartFile invalidFile = new MockMultipartFile(
                    "file",
                    "file.csv",
                    "text/csv",
                    fileContent.getBytes());

            FileProcessingException exception = assertThrows(FileProcessingException.class,
                    () -> assetService.readTxtFile(invalidFile));
            assertEquals("Formato do arquivo inválido", exception.getMessage());
        }

        @Test
        @DisplayName("Should not be able to read txt file when existing assets")
        void shouldNotBeAbleToReadTxtFileWhenExistingAssets() throws IOException {

            when(assetRepository.findAllAssetNames())
                    .thenReturn(Stream.of("PETR4"));

            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "test.txt",
                    "text/plain",
                    FILE_CONTENT.getBytes(StandardCharsets.UTF_8));

            assetService.readTxtFile(file);

            verify(assetRepository).saveAll(argThat(
                    list -> ((Collection<AssetEntity>) list).stream()
                            .noneMatch(asset -> ((AssetEntity) asset).getAssetName().equals("PETR4"))));
        }

        @Test
        @DisplayName("Should not be able to read txt file when database error")
        void shouldNotBeAbleToReadTxtFileWhenDatabaseError() {

            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "test.txt",
                    "text/plain",
                    FILE_CONTENT.getBytes(StandardCharsets.UTF_8));

            when(assetRepository.saveAll(anyList()))
                    .thenThrow(new RuntimeException("Database error"));

            FileProcessingException exception = assertThrows(FileProcessingException.class,
                    () -> assetService.readTxtFile(file));
            assertEquals("Failed to save assets batch", exception.getMessage());
        }

    }

    @Nested
    class saveAsset {

        @Test
        void shouldSaveNewAsset() {

            AssetInfoDto dto = new AssetInfoDto("PETR4", "acoes");

            String result = assetService.saveAsset(dto);

            verify(assetRepository).save(any(AssetEntity.class));
            assertEquals("O ativo foi salvo", result);
        }

        @Test
        void shouldNotSaveDuplicateAsset() {

            AssetInfoDto dto = new AssetInfoDto("PETR4", "acoes");

            when(assetRepository.existsByAssetName("PETR4")).thenReturn(true);

            ConflictException exception = assertThrows(ConflictException.class,
                    () -> assetService.saveAsset(dto));
            assertEquals("O ativo já possui cadastrado", exception.getMessage());

            verify(assetRepository, never()).save(any(AssetEntity.class));
        }
    }

    @Nested
    class getAssetTypeByAssetName {

        @Test
        @DisplayName("Should be able to get asset type by asset name")
        void shouldBeAbleToGetAssetTypeByAssetName() {

            AssetEntity asset = new AssetEntity();
            asset.setAssetName("ABCD11");
            asset.setAssetType("fundos-imobiliarios");

            when(assetRepository.findByAssetName("ABCD11"))
                    .thenReturn(Optional.of(asset));

            String assetType = assetService.getAssetTypeByAssetName("ABCD11");
            assertEquals("fundos-imobiliarios", assetType);
        }

        @Test
        @DisplayName("Should be able to get asset type by asset name when asset does not exist")
        void shouldNotBeAbleToGetAssetTypeByAssetNameWhenAssetDoesNotExist() {

            when(assetRepository.findByAssetName("INVALID")).thenReturn(Optional.empty());

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                    () -> assetService.getAssetTypeByAssetName("INVALID"));
            assertEquals("O ativo INVALID informado não existe", exception.getMessage());
        }
    }
}
