package personal.investwallet.modules.wallet;

import jakarta.validation.Valid;
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
            @RequestBody CreateAssetRequestDto payload
    ) {

        String result = walletService.addAssetToWallet(token, payload);

        return ResponseEntity.created(null).body(new CreateWalletResponseDto(result));
    }

    @PostMapping("/purchase")
    public ResponseEntity<UpdateWalletResponseDto> addPurchase(
            @CookieValue(value = "access_token") String token,
            @RequestBody AddPurchaseRequestDto payload
            ) {

        String result = walletService.addPurchaseToAsset(token, payload);

        return ResponseEntity.ok(new UpdateWalletResponseDto(result));
    }

    @PostMapping("/sale")
    public ResponseEntity<UpdateWalletResponseDto> addSale(
            @CookieValue(value = "access_token") String token,
            @RequestBody AddSaleRequestDto payload
    ) {

        String result = walletService.addSaleToAsset(token, payload);

        return ResponseEntity.ok(new UpdateWalletResponseDto(result));
    }

    @PatchMapping("/{assetType}/{assetName}/purchases/{purchaseId}")
    public ResponseEntity<UpdateWalletResponseDto> updatePurchase(
            @CookieValue(value = "access_token") String token,
            @PathVariable String assetType,
            @PathVariable String assetName,
            @PathVariable String purchaseId,
            @Valid @RequestBody UpdatePurchaseRequestDto payload
    ) {

        String result = walletService.updatePurchaseToAssetByPurchaseId(
                token,
                assetType,
                assetName,
                purchaseId,
                payload
        );

        return ResponseEntity.ok(new UpdateWalletResponseDto(result));
    }

    @PatchMapping("/{assetType}/{assetName}/sales/{saleId}")
    public ResponseEntity<UpdateWalletResponseDto> updateSale(
            @CookieValue(value = "access_token") String token,
            @PathVariable String assetType,
            @PathVariable String assetName,
            @PathVariable String saleId,
            @Valid @RequestBody UpdateSaleRequestDto payload
    ) {

        String result = walletService.updateSaleToAssetBySaleId(
                token,
                assetType,
                assetName,
                saleId,
                payload
        );

        return ResponseEntity.ok(new UpdateWalletResponseDto(result));
    }

    @DeleteMapping("/{assetType}/{assetName}/purchases/{purchaseId}")
    public ResponseEntity<UpdateWalletResponseDto> removePurchase(
            @CookieValue(value = "access_token") String token,
            @PathVariable String assetType,
            @PathVariable String assetName,
            @PathVariable String purchaseId
    ) {

        String result = walletService.removePurchaseToAssetByPurchaseId(token, assetType, assetName, purchaseId);

        return ResponseEntity.ok(new UpdateWalletResponseDto(result));
    }

    @DeleteMapping("/{assetType}/{assetName}/sales/{saleId}")
    public ResponseEntity<UpdateWalletResponseDto> removeSale(
            @CookieValue(value = "access_token") String token,
            @PathVariable String assetType,
            @PathVariable String assetName,
            @PathVariable String saleId
    ) {

        String result = walletService.removeSaleToAssetBySaleId(token, assetType, assetName, saleId);

        return ResponseEntity.ok(new UpdateWalletResponseDto(result));
    }
}
