package personal.investwallet.modules.yield;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import personal.investwallet.modules.yield.dto.YieldAssetNameRequestDto;
import personal.investwallet.modules.yield.dto.YieldInfoByAssetNameResponseDto;
import personal.investwallet.modules.yield.dto.YieldInfoByYieldAtResponseDto;
import personal.investwallet.modules.yield.dto.YieldRequestDto;
import personal.investwallet.modules.yield.dto.YieldSuccessResponseDto;
import personal.investwallet.modules.yield.dto.YieldTimeIntervalRequestDto;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("yield")
public class YieldController {

    @Autowired
    private YieldService yieldService;

    @Operation(summary = "Registrar diversos dividendos", security = @SecurityRequirement(name = "access_token"))
    @PostMapping()
    public ResponseEntity<YieldSuccessResponseDto> createMany(
            @CookieValue(value = "access_token") String token,
            @Valid @RequestBody List<YieldRequestDto> yields) {

        int result = yieldService.registerManyYieldsReceived(token, yields);

        String message;

        if (result == 1) {
            message = "Foi registrado " + result + " dividendo com sucesso.";
        } else {
            message = "Foram registrados " + result + " dividendos com sucesso.";
        }

        return ResponseEntity.created(null)
                .body(new YieldSuccessResponseDto(message));
    }

    @Operation(summary = "Registrar diversos dividendos via arquivo CSV", security = @SecurityRequirement(name = "access_token"))
    @PostMapping("/file")
    public ResponseEntity<YieldSuccessResponseDto> createManyByCsv(
            @CookieValue(value = "access_token") String token,
            MultipartFile file) {

        int result = yieldService.registerManyYieldsReceivedByCsv(token, file);

        String message;

        if (result == 1) {
            message = "O arquivo com " + result + " linha foi registrado com sucesso.";
        } else {
            message = "O arquivo com " + result + " linhas foi registrado com sucesso.";
        }

        return ResponseEntity.created(null)
                .body(new YieldSuccessResponseDto(message));
    }

    @Operation(summary = "Busca diversos dividendos do usuário dentro de um intervalo de tempo", security = @SecurityRequirement(name = "access_token"))
    @GetMapping("/yield-at")
    public ResponseEntity<Map<String, List<YieldInfoByYieldAtResponseDto>>> getManyByUserIdAndYieldAt(
            @CookieValue(value = "access_token") String token,
            @Valid @RequestBody YieldTimeIntervalRequestDto payload) {

        Map<String, List<YieldInfoByYieldAtResponseDto>> response = yieldService.fetchAllYieldsByTimeInterval(token,
                payload);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Busca diversos dividendos do usuário de um ativo específico", security = @SecurityRequirement(name = "access_token"))
    @GetMapping("/asset-name")
    public ResponseEntity<Map<String, List<YieldInfoByAssetNameResponseDto>>> getManyByUserIdAndAssetName(
            @CookieValue(value = "access_token") String token,
            @Valid @RequestBody YieldAssetNameRequestDto payload) {

        Map<String, List<YieldInfoByAssetNameResponseDto>> response = yieldService.fetchAllYieldAtByAssetName(token,
                payload);
        return ResponseEntity.ok(response);
    }
}
