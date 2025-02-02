package personal.investwallet.modules.asset;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;
import personal.investwallet.exceptions.ConflictException;
import personal.investwallet.exceptions.EmptyFileException;
import personal.investwallet.exceptions.FileProcessingException;
import personal.investwallet.exceptions.ResourceNotFoundException;
import personal.investwallet.modules.asset.dto.AssetInfoDto;
import personal.investwallet.modules.asset.dto.ProcessingContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AssetService {

    private static final int BATCH_SIZE = 1000;
    private static final int MIN_LINE_LENGTH = 28;
    private static final Map<String, String> ASSET_TYPE_MAPPING = Map.of(
            "FII", "fundos-imobiliarios",
            "ACOES", "acoes");

    @Autowired
    private AssetRepository assetRepository;

    public String getAssetTypeByAssetName(String assetName) {

        return assetRepository.findByAssetName(assetName)
                .map(AssetEntity::getAssetType)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("O ativo %s informado não existe", assetName)));
    }

    @Transactional
    public String saveAsset(AssetInfoDto payload) {

        if (assetRepository.existsByAssetName(payload.assetName())) {
            throw new ConflictException("O ativo já possui cadastrado");
        }

        AssetEntity newAsset = createAssetEntity(payload.assetName(), payload.assetType());
        assetRepository.save(newAsset);

        return "O ativo foi salvo";
    }

    @Transactional
    public String readTxtFile(MultipartFile file) throws IOException {

        validateFile(file);

        ProcessingContext context = new ProcessingContext(
                new ArrayList<>(BATCH_SIZE),
                ConcurrentHashMap.newKeySet(),
                loadExistingAssetNames());

        try {
            List<String> allLines = readAllLines(file);

            if (!allLines.isEmpty()) {
                log.info("Removendo a última linha do arquivo");
                allLines = allLines.subList(0, allLines.size() - 1);
            }

            if (!allLines.isEmpty()) {
                allLines.remove(0);
            }

            return processLines(allLines, context);

        } catch (IOException e) {
            throw new FileProcessingException("Erro ao ler arquivo");
        }
    }

    private List<String> readAllLines(MultipartFile file) throws IOException {

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.toList());
        }
    }

    private String processLines(List<String> lines, ProcessingContext context) {

        int totalProcessed = 0;

        for (String line : lines) {
            if (processLine(line, context)) {
                totalProcessed += processBatchIfNeeded(context);
            }
        }
        totalProcessed += processFinalBatch(context);

        log.info("Successfully processed {} assets", totalProcessed);

        return String.format("Successfully processed and saved %d assets.", totalProcessed);
    }

    private boolean processLine(String line, ProcessingContext context) {

        if (line.length() < MIN_LINE_LENGTH) {
            return false;
        }

        AssetInfoDto assetInfo = extractAssetInfo(line);
        if (assetInfo == null || !isValidNewAsset(assetInfo, context)) {
            return false;
        }

        context.processedNames().add(assetInfo.assetName());
        context.assetsToSave().add(createAssetEntity(assetInfo.assetName(), assetInfo.assetType()));

        return true;
    }

    private boolean isValidNewAsset(AssetInfoDto assetInfo, ProcessingContext context) {

        return !context.processedNames().contains(assetInfo.assetName())
                && !context.existingNames().contains(assetInfo.assetName());
    }

    private int processBatchIfNeeded(ProcessingContext context) {

        if (context.assetsToSave().size() >= BATCH_SIZE) {
            int size = context.assetsToSave().size();
            saveAssetBatch(context.assetsToSave());
            context.assetsToSave().clear();

            return size;
        }

        return 0;
    }

    private int processFinalBatch(ProcessingContext context) {

        if (!context.assetsToSave().isEmpty()) {
            int size = context.assetsToSave().size();
            saveAssetBatch(context.assetsToSave());
            context.assetsToSave().clear();

            return size;
        }

        return 0;
    }

    private void validateFile(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new EmptyFileException("O arquivo é inválido por estar vazio");
        }

        String contentType = file.getContentType();

        if (contentType == null || !contentType.equals("text/plain")) {
            throw new FileProcessingException("Formato do arquivo inválido");
        }
    }

    private Set<String> loadExistingAssetNames() {

        return assetRepository.findAllAssetNames()
                .collect(Collectors.toCollection(ConcurrentHashMap::newKeySet));
    }

    private AssetInfoDto extractAssetInfo(String line) {

        try {

            int tabIndex = line.indexOf(' ');

            String name = line.substring(12, tabIndex).trim();
            String typeRaw = line.substring(27).trim().split(" ")[0];
            String type = ASSET_TYPE_MAPPING.getOrDefault(typeRaw.toUpperCase(), "acoes");

            return new AssetInfoDto(name, type);

        } catch (IndexOutOfBoundsException e) {

            log.warn("Invalid line format: {}", line);
            return null;
        }
    }

    private AssetEntity createAssetEntity(String assetName, String assetType) {

        AssetEntity asset = new AssetEntity();
        asset.setAssetName(assetName);
        asset.setAssetType(assetType);

        return asset;
    }

    @Transactional
    private void saveAssetBatch(List<AssetEntity> assets) {
        try {

            assetRepository.saveAll(assets);
            log.debug("Saved batch of {} assets", assets.size());
        } catch (Exception e) {

            log.error("Error saving batch of {} assets", assets.size(), e);
            throw new FileProcessingException("Failed to save assets batch");
        }
    }

}
