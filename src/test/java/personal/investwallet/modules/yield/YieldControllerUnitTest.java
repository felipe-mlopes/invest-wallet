package personal.investwallet.modules.yield;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import jakarta.validation.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import personal.investwallet.modules.yield.dto.YieldAssetNameRequestDto;
import personal.investwallet.modules.yield.dto.YieldInfoByAssetNameResponseDto;
import personal.investwallet.modules.yield.dto.YieldInfoByYieldAtResponseDto;
import personal.investwallet.modules.yield.dto.YieldRequestDto;
import personal.investwallet.modules.yield.dto.YieldSuccessResponseDto;
import personal.investwallet.modules.yield.dto.YieldTimeIntervalRequestDto;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
public class YieldControllerUnitTest {

        @Mock
        private YieldService yieldService;

        @InjectMocks
        private YieldController yieldController;

        private Validator validator;

        private final String TOKEN = "valid-token";

        @BeforeEach
        void setUp() {

                ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
                validator = factory.getValidator();
        }

        @Nested
        class CreateMany {

                @Test
                @DisplayName("Should be able to create many yields with valid payload")
                void shouldBeAbleToCreateManyYieldsWithValidPayload() {

                        YieldRequestDto yieldRequest = new YieldRequestDto(
                                        "ABCD11",
                                        Instant.parse("2024-08-31T00:00:00Z"),
                                        Instant.parse("2024-09-15T00:00:00Z"),
                                        new BigDecimal("10.00"),
                                        new BigDecimal("100.00"),
                                        new BigDecimal("0.1"));

                        List<YieldRequestDto> payload = List.of(yieldRequest);

                        ResponseEntity<YieldSuccessResponseDto> response = yieldController.createMany(TOKEN, payload);

                        assertNotNull(response);
                        assertEquals(HttpStatus.CREATED, response.getStatusCode());
                        assertNotNull(response.getBody());
                        verify(yieldService, times(1)).registerManyYieldsReceived(TOKEN, payload);
                }

                @Test
                @DisplayName("Should not be able to create many yields with empty payload")
                void shouldNotBeAbleToCreateManyYieldsWithEmptyPayload() {

                        YieldRequestDto invalidPayload = new YieldRequestDto("", null, null, null, null, null);

                        var violations = validator.validate(invalidPayload);
                        assertTrue(violations.stream()
                                        .anyMatch(v -> v.getMessage().equals("O nome do ativo não pode ser vazio")));
                }

                @Test
                @DisplayName("Should not be able to create wallet and add asset to it with invalid payload")
                void shouldNotBeAbleToCreateWalletAndAddAssetToItWithInvalidPayload() {

                        YieldRequestDto invalidPayload = new YieldRequestDto(
                                        "AD11",
                                        Instant.parse("2124-08-31T00:00:00Z"),
                                        Instant.parse("2124-09-15T00:00:00Z"),
                                        new BigDecimal("0"),
                                        new BigDecimal("0"),
                                        new BigDecimal("0"));

                        var violations = validator.validate(invalidPayload);

                        assertEquals(6, violations.size());
                        assertTrue(violations.stream()
                                        .anyMatch(v -> v.getMessage()
                                                        .equals("O nome do ativo deve conter entre 5 e 6 caracteres")));
                        assertTrue(violations.stream()
                                        .anyMatch(v -> v.getMessage()
                                                        .equals("A data base deve ser anterior a data corrente")));
                        assertTrue(violations.stream()
                                        .anyMatch(v -> v.getMessage().equals(
                                                        "A data de pagamento deve ser anterior a data corrente")));
                        assertTrue(violations.stream()
                                        .anyMatch(v -> v.getMessage()
                                                        .equals("A cotação base deve ser maior que zero")));
                        assertTrue(violations.stream()
                                        .anyMatch(v -> v.getMessage()
                                                        .equals("O valor de rendimento deve ser maior que zero")));
                        assertTrue(violations.stream()
                                        .anyMatch(v -> v.getMessage()
                                                        .equals("O valor do dividendo deve ser maior que zero")));
                }
        }

        @Nested
        class GetManyByUserIdAndYieldAt {

                @Test
                @DisplayName("Should be able to get many by userId and yield at with valid payload")
                void shouldBeAbleToGetManyByUserIdAndYieldAtWithValidPayload() {

                        YieldTimeIntervalRequestDto payload = new YieldTimeIntervalRequestDto(
                                        Instant.parse("2024-08-01T00:00:00Z"),
                                        Instant.parse("2024-11-30T00:00:00Z"));

                        ResponseEntity<Map<String, List<YieldInfoByYieldAtResponseDto>>> response = yieldController
                                        .getManyByUserIdAndYieldAt(TOKEN, payload);

                        assertNotNull(response);
                        assertEquals(HttpStatus.OK, response.getStatusCode());
                        assertNotNull(response.getBody());
                        verify(yieldService, times(1)).fetchAllYieldsByTimeInterval(TOKEN, payload);
                }

                @Test
                @DisplayName("Should not be able to get many by userId and yield at with invalid payload")
                void shouldNotBeAbleToGetManyByUserIdAndYieldAtWithInvalidPayload() {

                        YieldTimeIntervalRequestDto invalidPayload = new YieldTimeIntervalRequestDto(
                                        Instant.parse("2124-08-01T00:00:00Z"),
                                        Instant.parse("2124-11-30T00:00:00Z"));

                        var violations = validator.validate(invalidPayload);

                        assertEquals(2, violations.size());
                        assertTrue(violations.stream()
                                        .anyMatch(v -> v.getMessage()
                                                        .equals("A data de início deve ser anterior a data corrente")));
                        assertTrue(violations.stream()
                                        .anyMatch(v -> v.getMessage().equals(
                                                        "A data de término deve ser anterior a data corrente")));
                }
        }

        @Nested
        class GetManyByUserIdAndAssetName {

                @Test
                @DisplayName("Should be able to get many by userId and asset name with valid payload")
                void shouldBeAbleToGetManyByUserIdAndAssetNameWithValidPayload() {

                        YieldAssetNameRequestDto payload = new YieldAssetNameRequestDto("ABCD11");

                        ResponseEntity<Map<String, List<YieldInfoByAssetNameResponseDto>>> response = yieldController
                                        .getManyByUserIdAndAssetName(TOKEN, payload);

                        assertNotNull(response);
                        assertEquals(HttpStatus.OK, response.getStatusCode());
                        assertNotNull(response.getBody());
                        verify(yieldService, times(1)).fetchAllYieldAtByAssetName(TOKEN, payload);
                }

                @Test
                @DisplayName("Should not be able to get many by userId and asset name with empty payload")
                void shouldNotBeAbleToGetManyByUserIdAndAssetNameWithEmptyPayload() {

                        YieldAssetNameRequestDto invalidPayload = new YieldAssetNameRequestDto("");

                        var violations = validator.validate(invalidPayload);

                        assertTrue(violations.stream()
                                        .anyMatch(v -> v.getMessage().equals("O nome do ativo não pode ser vazio")));
                }

                @Test
                @DisplayName("Should not be able to get many by userId and asset name with invalid payload")
                void shouldNotBeAbleToGetManyByUserIdAndAssetNameWithInvalidPayload() {

                        YieldAssetNameRequestDto invalidPayload = new YieldAssetNameRequestDto("AB11");

                        var violations = validator.validate(invalidPayload);

                        assertTrue(violations.stream()
                                        .anyMatch(v -> v.getMessage()
                                                        .equals("O nome do ativo deve conter entre 5 e 6 caracteres")));
                }
        }
}
