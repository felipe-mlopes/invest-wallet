package personal.investwallet.modules.wallet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import personal.investwallet.modules.wallet.dto.AssetCreateRequestDto;
import personal.investwallet.modules.wallet.dto.CreateWalletResponseDto;
import personal.investwallet.modules.wallet.dto.PurchasesInfoRequestDto;
import personal.investwallet.modules.wallet.dto.UpdateWalletRespondeDto;

@RestController
@RequestMapping("/wallet")
public class WalletController {

    @Autowired
    private WalletService walletService;

    @PostMapping()
    public ResponseEntity<CreateWalletResponseDto> create(
            @CookieValue(value = "access_token") String token,
            @RequestBody AssetCreateRequestDto payload
    ) {

        String result = walletService.addAssetToWallet(token, payload);

        return ResponseEntity.created(null).body(new CreateWalletResponseDto(result));
    }

    @PatchMapping("/purchase")
    public ResponseEntity<UpdateWalletRespondeDto> registerPurchaseOfExistingAsset(
            @CookieValue(value = "access_token") String token,
            @RequestBody PurchasesInfoRequestDto payload
            ) {

        String result = walletService.registerPurchaseOfAnExistingAsset(token, payload);

        return ResponseEntity.created(null).body(new UpdateWalletRespondeDto(result));
    }
}
