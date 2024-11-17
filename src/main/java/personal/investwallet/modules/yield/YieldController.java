package personal.investwallet.modules.yield;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import personal.investwallet.modules.yield.dto.YieldInfoResponse;
import personal.investwallet.modules.yield.dto.YieldRequestDto;
import personal.investwallet.modules.yield.dto.YieldTimeIntervalRequestDto;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("yield")
public class YieldController {

    @Autowired
    private YieldService yieldService;

    @PostMapping()
    public ResponseEntity<String> createMany(
            @CookieValue(value = "access_token") String token,
            @Valid @RequestBody List<YieldRequestDto> yields
    ) {

        int result = yieldService.registerManyYieldsReceived(token, yields);
        return ResponseEntity.created(null).body("Foram registrados " + result + " dividendos com sucesso.");
    }

    @PostMapping("/file")
    public ResponseEntity<String> createManyByCsv(
            @CookieValue(value = "access_token") String token,
            MultipartFile file
    ) {

        int result = yieldService.registerManyYieldsReceivedByCsv(token, file);
        return ResponseEntity.created(null).body("O arquivo com " + result + " linhas foi registrado com sucesso.");
    }

    @GetMapping()
    public ResponseEntity<Map<String, List<YieldInfoResponse>>> getManyByUserIdAndYieldAt(
            @CookieValue(value = "access_token") String token,
            @RequestBody YieldTimeIntervalRequestDto payload
    ) {

        Map<String, List<YieldInfoResponse>> response = yieldService.fetchAllYieldsByTimeInterval(token, payload);
        return ResponseEntity.ok(response);
    }
}
