package personal.investwallet.modules.asset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import personal.investwallet.exceptions.EmptyFileException;
import personal.investwallet.exceptions.FileProcessingException;
import personal.investwallet.exceptions.ResourceNotFoundException;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AssetServiceUnitTest {

    @Mock
    private AssetRepository assetRepository;

    @InjectMocks
    private AssetService assetService;

    @Nested
    class readTxt {

        @Test
        @DisplayName("Should be able to read txt file")
        void shouldBeAbleToReadTxtFile() throws IOException {

            String fileContent = """
                    
                    012024100112ABCD11      010FII BARIGUI CI  ER       R$
                    012024100112XYZW11      010FII BARIGUI CI  ER       R$
                    """;

            MultipartFile file = new MockMultipartFile(
                    "file",
                    "assets.txt",
                    "text/plain",
                    fileContent.getBytes());

            String result = assetService.readTxtFile(file);

            verify(assetRepository, times(1)).saveAll(anyList());
            assertEquals("Processed and saved 2 assets.", result);
        }

        @Test
        @DisplayName("Should not be able to read txt file with empty file")
        void shouldNotBeAbleToReadTxtFileWithEmptyFile() {

            MultipartFile invalidFile = new MockMultipartFile("test.txt", new byte[0]);

            EmptyFileException exception = assertThrows(EmptyFileException.class,
                    () -> assetService.readTxtFile(invalidFile)
            );
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
                    fileContent.getBytes()
            );

            FileProcessingException exception = assertThrows(FileProcessingException.class,
                    () -> assetService.readTxtFile(invalidFile)
            );
            assertEquals("Formato do arquivo inválido", exception.getMessage());
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

            when(assetRepository.findByAssetName("ABCD11")).thenReturn(asset);

            String assetType = assetService.getAssetTypeByAssetName("ABCD11");
            assertEquals("fundos-imobiliarios", assetType);
        }

        @Test
        @DisplayName("Should be able to get asset type by asset name when asset does not exist")
        void shouldNotBeAbleToGetAssetTypeByAssetNameWhenAssetDoesNotExist() {

            when(assetRepository.findByAssetName("ABCD11")).thenReturn(null);

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                    () -> assetService.getAssetTypeByAssetName("ABCD11")
            );
            assertEquals("O ativo informado não existe", exception.getMessage());
        }
    }
}
