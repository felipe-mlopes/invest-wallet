package personal.investwallet.modules.asset;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import personal.investwallet.exceptions.EmptyFileException;
import personal.investwallet.exceptions.FileProcessingException;
import personal.investwallet.exceptions.ResourceNotFoundException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

@Service
public class AssetService {

    @Autowired
    private AssetRepository assetRepository;

    public String readTxtFile(MultipartFile file) throws IOException {

        if (file.isEmpty())
            throw new EmptyFileException("O arquivo é inválido por estar vazio");

        if (!Objects.requireNonNull(file.getContentType()).equals("text/plain"))
            throw new FileProcessingException("Formato do arquivo inválido");

        List<AssetEntity> assets = new ArrayList<>();
        Set<String> assetNamesSet = new HashSet<>();

        BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
        reader.readLine();

        String line;
        while ((line = reader.readLine()) != null) {
            int tabIndex = line.indexOf(' ');

            if (tabIndex != -1) {
                String firstColumn = line.substring(0, tabIndex).trim();
                String assetName = firstColumn.substring(12);

                String secondColumn = line.substring(27).trim();
                int tabIndex2 = secondColumn.indexOf(' ');

                if (tabIndex2 != -1) {
                    String type = secondColumn.substring(0, tabIndex2);
                    String assetType = type.equals("FII") ? "fundos-imobiliarios" : "acoes";

                    if (!assetNamesSet.contains(assetName) && assetRepository.findByAssetName(assetName) == null) {
                        assetNamesSet.add(assetName);

                        AssetEntity asset = new AssetEntity();
                        asset.setAssetName(assetName);
                        asset.setAssetType(assetType);

                        assets.add(asset);
                    }
                }
            }
        }

        assetRepository.saveAll(assets);

        return "Processed and saved " + assets.size() + " assets.";
    }

    public String getAssetTypeByAssetName(String assetName) {

        AssetEntity asset = assetRepository.findByAssetName(assetName);

        if (asset == null)
            throw new ResourceNotFoundException("O ativo " + assetName + " informado não existe");

        return asset.getAssetType();
    }
}
