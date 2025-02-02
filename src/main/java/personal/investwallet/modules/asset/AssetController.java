package personal.investwallet.modules.asset;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Hidden;

@RestController
@RequestMapping("/asset")
public class AssetController {

    @Autowired
    private AssetService assetService;

    @Hidden
    @PostMapping("/upload")
    public ResponseEntity<String> create(
            @CookieValue(value = "access_token") String token,
            MultipartFile file) throws IOException {

        String result = assetService.readTxtFile(file);

        return ResponseEntity.created(null).body(result);
    }
}
