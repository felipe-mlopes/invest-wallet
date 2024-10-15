package personal.investwallet.modules.wallet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import personal.investwallet.modules.wallet.dto.*;

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

    @PostMapping("/purchase")
    public ResponseEntity<UpdateWalletRespondeDto> addPurchase(
            @CookieValue(value = "access_token") String token,
            @RequestBody PurchasesInfoRequestDto payload
            ) {

        String result = walletService.addPurchaseToAsset(token, payload);

        return ResponseEntity.ok(new UpdateWalletRespondeDto(result));
    }

    @PostMapping("/sale")
    public ResponseEntity<UpdateWalletRespondeDto> addSale(
            @CookieValue(value = "access_token") String token,
            @RequestBody SalesInfoRequestDto payload
    ) {

        String result = walletService.addSaleToAsset(token, payload);

        return ResponseEntity.ok(new UpdateWalletRespondeDto(result));
    }

    @DeleteMapping("/{assetType}/{assetName}/purchases/{purchaseId}")
    public ResponseEntity<UpdateWalletRespondeDto> removePurchase(
            @CookieValue(value = "access_token") String token,
            @PathVariable String assetType,
            @PathVariable String assetName,
            @PathVariable String purchaseId
    ) {

        String result = walletService.removePurchaseToAssetByPurchaseId(token, assetType, assetName, purchaseId);

        return ResponseEntity.ok(new UpdateWalletRespondeDto(result));
    }

    @DeleteMapping("/{assetType}/{assetName}/sales/{saleId}")
    public ResponseEntity<UpdateWalletRespondeDto> removeSale(
            @CookieValue(value = "access_token") String token,
            @PathVariable String assetType,
            @PathVariable String assetName,
            @PathVariable String saleId
    ) {

        String result = walletService.removeSaleToAssetBySaleId(token, assetType, assetName, saleId);

        return ResponseEntity.ok(new UpdateWalletRespondeDto(result));
    }
}
