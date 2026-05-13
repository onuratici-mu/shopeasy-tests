package shopeasy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Task 3 – Design by Contract
 *
 * These tests check that the assert-based contracts in ShoppingCart and
 * PriceCalculator work for both valid and invalid inputs.
 */
class ContractTest {

    private ShoppingCart cart;
    private PriceCalculator calculator;
    private Product product;

    @BeforeEach
    void setUp() {
        cart       = new ShoppingCart();
        calculator = new PriceCalculator();
        product    = new Product("P001", "Widget", 10.0, 50);
    }

    /**
     * Valid contract case: addItem should accept a non-null product and positive quantity.
     * It should also update the cart state correctly.
     */
    @Test
    void addItemValidInputShouldSatisfyContract() {
        assertThatCode(() -> cart.addItem(product, 3))
                .doesNotThrowAnyException();

        assertThat(cart.itemCount()).isEqualTo(1);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(3);
        assertThat(cart.total()).isCloseTo(30.0, within(0.0001));
    }

    /**
     * Pre-condition violation: addItem should reject a null product.
     */
    @Test
    void addItemNullProductShouldViolatePreCondition() {
        assertThatThrownBy(() -> cart.addItem(null, 1))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("product must not be null");
    }

    /**
     * Pre-condition violation: addItem should reject zero or negative quantity.
     */
    @Test
    void addItemNonPositiveQuantityShouldViolatePreCondition() {
        assertThatThrownBy(() -> cart.addItem(product, 0))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("quantity must be > 0");

        assertThatThrownBy(() -> cart.addItem(product, -2))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("quantity must be > 0");
    }

    /**
     * Valid contract case: adding the same product twice should keep one cart line
     * and increase the quantity.
     */
    @Test
    void addItemExistingProductShouldSatisfyPostCondition() {
        cart.addItem(product, 2);

        assertThatCode(() -> cart.addItem(product, 3))
                .doesNotThrowAnyException();

        assertThat(cart.itemCount()).isEqualTo(1);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(5);
        assertThat(cart.total()).isCloseTo(50.0, within(0.0001));
    }

    /**
     * Valid contract case: applyDiscount should accept values from 0 to 100.
     */
    @Test
    void applyDiscountValidRatesShouldSatisfyContract() {
        cart.addItem(product, 5); // raw total = 50

        assertThat(cart.applyDiscount(0.0)).isCloseTo(50.0, within(0.0001));
        assertThat(cart.applyDiscount(20.0)).isCloseTo(40.0, within(0.0001));
        assertThat(cart.applyDiscount(100.0)).isCloseTo(0.0, within(0.0001));
    }

    /**
     * Pre-condition violation: applyDiscount should reject rates below 0 or above 100.
     */
    @Test
    void applyDiscountInvalidRateShouldViolatePreCondition() {
        cart.addItem(product, 5);

        assertThatThrownBy(() -> cart.applyDiscount(-1.0))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("discountRate must be between 0 and 100");

        assertThatThrownBy(() -> cart.applyDiscount(101.0))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("discountRate must be between 0 and 100");
    }

    /**
     * Invariant case: after normal cart operations, total should stay non-negative.
     */
    @Test
    void shoppingCartTotalInvariantShouldHoldAfterOperations() {
        cart.addItem(product, 4);
        cart.updateQuantity("P001", 2);
        cart.removeItem("DOES_NOT_EXIST");
        cart.clear();

        assertThat(cart.total()).isGreaterThanOrEqualTo(0.0);
    }

    /**
     * Valid contract case: PriceCalculator should accept valid values and return
     * a non-negative result.
     */
    @Test
    void priceCalculatorValidInputShouldSatisfyContract() {
        double result = calculator.calculate(100.0, 10.0, 20.0);

        assertThat(result).isCloseTo(108.0, within(0.0001));
        assertThat(result).isGreaterThanOrEqualTo(0.0);
    }

    /**
     * Pre-condition violation: PriceCalculator should reject a negative base price.
     */
    @Test
    void priceCalculatorNegativeBaseShouldViolatePreCondition() {
        assertThatThrownBy(() -> calculator.calculate(-1.0, 10.0, 20.0))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("basePrice must be >= 0");
    }

    /**
     * Pre-condition violation: PriceCalculator should reject discount outside 0 to 100.
     */
    @Test
    void priceCalculatorInvalidDiscountShouldViolatePreCondition() {
        assertThatThrownBy(() -> calculator.calculate(100.0, -1.0, 20.0))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("discountRate must be between 0 and 100");

        assertThatThrownBy(() -> calculator.calculate(100.0, 101.0, 20.0))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("discountRate must be between 0 and 100");
    }

    /**
     * Pre-condition violation: PriceCalculator should reject tax outside 0 to 100.
     */
    @Test
    void priceCalculatorInvalidTaxShouldViolatePreCondition() {
        assertThatThrownBy(() -> calculator.calculate(100.0, 10.0, -1.0))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("taxRate must be between 0 and 100");

        assertThatThrownBy(() -> calculator.calculate(100.0, 10.0, 101.0))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("taxRate must be between 0 and 100");
    }
}