package personal.investwallet.modules.wallet;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import personal.investwallet.modules.wallet.dto.*;

import java.util.List;

@RestController
@RequestMapping("/wallet")
public class WalletController {

    @Autowired
    private WalletService walletService;

    @PostMapping()
    public ResponseEntity<CreateWalletResponseDto> create(
            @CookieValue(value = "access_token") String token,
            @Valid @RequestBody CreateAssetRequestDto payload
    ) {

        String result = walletService.addAssetToWallet(token, payload);

        return ResponseEntity.created(null).body(new CreateWalletResponseDto(result));
    }

    @PostMapping("/purchase")
    public ResponseEntity<UpdateWalletResponseDto> addPurchase(
            @CookieValue(value = "access_token") String token,
            @Valid @RequestBody AddPurchaseRequestDto payload
            ) {

        String result = walletService.addPurchaseToAsset(token, payload);

        return ResponseEntity.ok(new UpdateWalletResponseDto(result));
    }

    @PostMapping("/purchases")
    public ResponseEntity<String> addManyPurchasesByCSV(
            @CookieValue(value = "access_token") String token,
            MultipartFile file
    ) {

        String result = walletService.addAllPurchasesToAssetByFile(token, file);

        return ResponseEntity.created(null).body(result);
    }

    @PostMapping("/sale")
    public ResponseEntity<UpdateWalletResponseDto> addSale(
            @CookieValue(value = "access_token") String token,
            @Valid @RequestBody AddSaleRequestDto payload
    ) {

        String result = walletService.addSaleToAsset(token, payload);

        return ResponseEntity.ok(new UpdateWalletResponseDto(result));
    }

    @PostMapping("/sales")
    public ResponseEntity<String> addManySalesByCSV(
            @CookieValue(value = "access_token") String token,
            MultipartFile file
    ) {

        String result = walletService.addAllSalesToAssetByFile(token, file);

        return ResponseEntity.created(null).body(result);
    }

    @PatchMapping("/{assetType}/{assetName}/purchases/{purchaseId}")
    public ResponseEntity<UpdateWalletResponseDto> updatePurchase(
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

        return ResponseEntity.ok(new UpdateWalletResponseDto(result));
    }

    @PatchMapping("/{assetType}/{assetName}/sales/{saleId}")
    public ResponseEntity<UpdateWalletResponseDto> updateSale(
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

        return ResponseEntity.ok(new UpdateWalletResponseDto(result));
    }

    @DeleteMapping("/{assetType}/{assetName}/purchases/{purchaseId}")
    public ResponseEntity<UpdateWalletResponseDto> removePurchase(
            @CookieValue(value = "access_token") String token,
            @PathVariable String assetName,
            @PathVariable String purchaseId
    ) {

        String result = walletService.removePurchaseToAssetByPurchaseId(token, assetName, purchaseId);

        return ResponseEntity.ok(new UpdateWalletResponseDto(result));
    }

    @DeleteMapping("/{assetType}/{assetName}/sales/{saleId}")
    public ResponseEntity<UpdateWalletResponseDto> removeSale(
            @CookieValue(value = "access_token") String token,
            @PathVariable String assetName,
            @PathVariable String saleId
    ) {

        String result = walletService.removeSaleToAssetBySaleId(token, assetName, saleId);

        return ResponseEntity.ok(new UpdateWalletResponseDto(result));
    }
}
