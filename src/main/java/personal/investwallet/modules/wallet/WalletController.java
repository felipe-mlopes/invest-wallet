package personal.investwallet.modules.wallet;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import personal.investwallet.modules.wallet.dto.*;

@RestController
@RequestMapping("/wallet")
public class WalletController {

    @Autowired
    private WalletService walletService;

    @PostMapping()
    public ResponseEntity<WallerSuccessResponseDto> create(
            @CookieValue(value = "access_token") String token,
            @Valid @RequestBody CreateAssetRequestDto payload
    ) {

        String result = walletService.addAssetToWallet(token, payload);

        return ResponseEntity.created(null).body(new WallerSuccessResponseDto(result));
    }

    @PostMapping("/purchase")
    public ResponseEntity<WallerSuccessResponseDto> addPurchase(
            @CookieValue(value = "access_token") String token,
            @Valid @RequestBody AddPurchaseRequestDto payload
            ) {

        String result = walletService.addPurchaseToAsset(token, payload);

        return ResponseEntity.ok(new WallerSuccessResponseDto(result));
    }

    @PostMapping("/purchases")
    public ResponseEntity<WallerSuccessResponseDto> addManyPurchasesByCSV(
            @CookieValue(value = "access_token") String token,
            MultipartFile file
    ) {

        String result = walletService.addAllPurchasesToAssetByFile(token, file);

        return ResponseEntity.ok(new WallerSuccessResponseDto(result));
    }

    @PatchMapping("/{assetType}/{assetName}/purchases/{purchaseId}")
    public ResponseEntity<WallerSuccessResponseDto> updatePurchase(
            @CookieValue(value = "access_token") String token,
            @PathVariable String assetName,
            @PathVariable String purchaseId,
            @Valid @RequestBody UpdatePurchaseRequestDto payload
    ) {

        String result = walletService.updatePurchaseToAssetByPurchaseId(
                token,
                assetName,
                purchaseId,
                payload
        );

        return ResponseEntity.ok(new WallerSuccessResponseDto(result));
    }

    @DeleteMapping("/{assetType}/{assetName}/purchases/{purchaseId}")
    public ResponseEntity<WallerSuccessResponseDto> removePurchase(
            @CookieValue(value = "access_token") String token,
            @PathVariable String assetName,
            @PathVariable String purchaseId
    ) {

        String result = walletService.removePurchaseToAssetByPurchaseId(token, assetName, purchaseId);

        return ResponseEntity.ok(new WallerSuccessResponseDto(result));
    }

    @PostMapping("/sale")
    public ResponseEntity<WallerSuccessResponseDto> addSale(
            @CookieValue(value = "access_token") String token,
            @Valid @RequestBody AddSaleRequestDto payload
    ) {

        String result = walletService.addSaleToAsset(token, payload);

        return ResponseEntity.ok(new WallerSuccessResponseDto(result));
    }

    @PostMapping("/sales")
    public ResponseEntity<WallerSuccessResponseDto> addManySalesByCSV(
            @CookieValue(value = "access_token") String token,
            MultipartFile file
    ) {

        String result = walletService.addAllSalesToAssetByFile(token, file);

        return ResponseEntity.ok(new WallerSuccessResponseDto(result));
    }

    @PatchMapping("/{assetType}/{assetName}/sales/{saleId}")
    public ResponseEntity<WallerSuccessResponseDto> updateSale(
            @CookieValue(value = "access_token") String token,
            @PathVariable String assetName,
            @PathVariable String saleId,
            @Valid @RequestBody UpdateSaleRequestDto payload
    ) {

        String result = walletService.updateSaleToAssetBySaleId(
                token,
                assetName,
                saleId,
                payload
        );

        return ResponseEntity.ok(new WallerSuccessResponseDto(result));
    }

    @DeleteMapping("/{assetType}/{assetName}/sales/{saleId}")
    public ResponseEntity<WallerSuccessResponseDto> removeSale(
            @CookieValue(value = "access_token") String token,
            @PathVariable String assetName,
            @PathVariable String saleId
    ) {

        String result = walletService.removeSaleToAssetBySaleId(token, assetName, saleId);

        return ResponseEntity.ok(new WallerSuccessResponseDto(result));
    }
}
