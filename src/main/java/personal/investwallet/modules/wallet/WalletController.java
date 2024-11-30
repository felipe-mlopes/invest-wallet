package personal.investwallet.modules.wallet;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import personal.investwallet.modules.wallet.dto.*;

@RestController
@RequestMapping("/wallet")
public class WalletController {

        @Autowired
        private WalletService walletService;

        @Operation(summary = "Registrar um novo ativo na carteira", security = @SecurityRequirement(name = "access_token"))
        @PostMapping()
        public ResponseEntity<WallerSuccessResponseDto> create(
                        @CookieValue(value = "access_token") String token,
                        @Valid @RequestBody CreateAssetRequestDto payload) {

                String result = walletService.addAssetToWallet(token, payload);

                return ResponseEntity.created(null).body(new WallerSuccessResponseDto(result));
        }

        @Operation(summary = "Registrar uma compra de um ativo na carteira", security = @SecurityRequirement(name = "access_token"))
        @PostMapping("/purchase")
        public ResponseEntity<WallerSuccessResponseDto> addPurchase(
                        @CookieValue(value = "access_token") String token,
                        @Valid @RequestBody AddPurchaseRequestDto payload) {

                String result = walletService.addPurchaseToAsset(token, payload);

                return ResponseEntity.ok(new WallerSuccessResponseDto(result));
        }

        @Operation(summary = "Registrar diversas compras na carteira via arquivo CSV", security = @SecurityRequirement(name = "access_token"))
        @PostMapping("/purchases")
        public ResponseEntity<WallerSuccessResponseDto> addManyPurchasesByCSV(
                        @CookieValue(value = "access_token") String token,
                        MultipartFile file) {

                String result = walletService.addManyPurchasesToAssetByFile(token, file);

                return ResponseEntity.ok(new WallerSuccessResponseDto(result));
        }

        @Operation(summary = "Atualizar uma compra de um ativo na carteira", security = @SecurityRequirement(name = "access_token"))
        @PatchMapping("/{assetType}/{assetName}/purchases/{purchaseId}")
        public ResponseEntity<WallerSuccessResponseDto> updatePurchase(
                        @CookieValue(value = "access_token") String token,
                        @PathVariable String assetName,
                        @PathVariable String purchaseId,
                        @Valid @RequestBody UpdatePurchaseRequestDto payload) {

                String result = walletService.updatePurchaseToAssetByPurchaseId(
                                token,
                                assetName,
                                purchaseId,
                                payload);

                return ResponseEntity.ok(new WallerSuccessResponseDto(result));
        }

        @Operation(summary = "Remover uma compra de um ativo na carteira", security = @SecurityRequirement(name = "access_token"))
        @DeleteMapping("/{assetType}/{assetName}/purchases/{purchaseId}")
        public ResponseEntity<WallerSuccessResponseDto> removePurchase(
                        @CookieValue(value = "access_token") String token,
                        @PathVariable String assetName,
                        @PathVariable String purchaseId) {

                String result = walletService.removePurchaseToAssetByPurchaseId(token, assetName, purchaseId);

                return ResponseEntity.ok(new WallerSuccessResponseDto(result));
        }

        @Operation(summary = "Registrar uma venda de um ativo na carteira", security = @SecurityRequirement(name = "access_token"))
        @PostMapping("/sale")
        public ResponseEntity<WallerSuccessResponseDto> addSale(
                        @CookieValue(value = "access_token") String token,
                        @Valid @RequestBody AddSaleRequestDto payload) {

                String result = walletService.addSaleToAsset(token, payload);

                return ResponseEntity.ok(new WallerSuccessResponseDto(result));
        }

        @Operation(summary = "Registrar diversas vendas na carteira via arquivo CSV", security = @SecurityRequirement(name = "access_token"))
        @PostMapping("/sales")
        public ResponseEntity<WallerSuccessResponseDto> addManySalesByCSV(
                        @CookieValue(value = "access_token") String token,
                        MultipartFile file) {

                String result = walletService.addManySalesToAssetByFile(token, file);

                return ResponseEntity.ok(new WallerSuccessResponseDto(result));
        }

        @Operation(summary = "Atualizar uma venda de um ativo na carteira", security = @SecurityRequirement(name = "access_token"))
        @PatchMapping("/{assetType}/{assetName}/sales/{saleId}")
        public ResponseEntity<WallerSuccessResponseDto> updateSale(
                        @CookieValue(value = "access_token") String token,
                        @PathVariable String assetName,
                        @PathVariable String saleId,
                        @Valid @RequestBody UpdateSaleRequestDto payload) {

                String result = walletService.updateSaleToAssetBySaleId(
                                token,
                                assetName,
                                saleId,
                                payload);

                return ResponseEntity.ok(new WallerSuccessResponseDto(result));
        }

        @Operation(summary = "Remover uma venda de um ativo na carteira", security = @SecurityRequirement(name = "access_token"))
        @DeleteMapping("/{assetType}/{assetName}/sales/{saleId}")
        public ResponseEntity<WallerSuccessResponseDto> removeSale(
                        @CookieValue(value = "access_token") String token,
                        @PathVariable String assetName,
                        @PathVariable String saleId) {

                String result = walletService.removeSaleToAssetBySaleId(token, assetName, saleId);

                return ResponseEntity.ok(new WallerSuccessResponseDto(result));
        }
}
