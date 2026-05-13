package shopeasy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Task 1 – Specification-Based Testing
 *
 * Target: PriceCalculator.calculate(basePrice, discountRate, taxRate)
 *
 * Input dimensions:
 * - basePrice: zero, positive, large positive
 * - discountRate: 0%, typical between 0 and 100, 100%
 * - taxRate: 0%, typical between 0 and 100, 100%
 *
 * These tests focus on equivalence partitions and boundary values.
 */
class PriceCalculatorSpecTest {

    private PriceCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new PriceCalculator();
    }

    /**
     * Partition: zero base price.
     * No matter what valid discount and tax are used, the final price should stay zero.
     */
    @ParameterizedTest(name = "base=0, discount={0}%, tax={1}% should return 0")
    @CsvSource({
            "0.0,   0.0",
            "10.0, 20.0",
            "50.0, 50.0",
            "100.0, 100.0"
    })
    void zeroBasePriceAlwaysReturnsZero(double discountRate, double taxRate) {
        double result = calculator.calculate(0.0, discountRate, taxRate);

        assertThat(result).isEqualTo(0.0);
    }

    /**
     * Boundary: discountRate at lower bound 0%.
     * A 0% discount means the base price should not be reduced before tax.
     */
    @Test
    void zeroDiscountDoesNotReduceBasePrice() {
        double result = calculator.calculate(100.0, 0.0, 20.0);

        assertThat(result).isCloseTo(120.0, within(0.0001));
    }

    /**
     * Boundary: discountRate at upper bound 100%.
     * A 100% discount removes the whole price, so tax should also be zero.
     */
    @Test
    void hundredPercentDiscountReturnsZeroEvenWithTax() {
        double result = calculator.calculate(100.0, 100.0, 20.0);

        assertThat(result).isCloseTo(0.0, within(0.0001));
    }

    /**
     * Boundary: taxRate at lower bound 0%.
     * A 0% tax means the result should only include the discount calculation.
     */
    @Test
    void zeroTaxAppliesOnlyDiscount() {
        double result = calculator.calculate(200.0, 25.0, 0.0);

        assertThat(result).isCloseTo(150.0, within(0.0001));
    }

    /**
     * Boundary: taxRate at upper bound 100%.
     * A 100% tax doubles the discounted price.
     */
    @Test
    void hundredPercentTaxDoublesDiscountedPrice() {
        double result = calculator.calculate(100.0, 25.0, 100.0);

        assertThat(result).isCloseTo(150.0, within(0.0001));
    }

    /**
     * Partition: typical valid values.
     * These cases check the normal formula for common base, discount, and tax combinations.
     */
    @ParameterizedTest(name = "base={0}, discount={1}%, tax={2}% => expected={3}")
    @CsvSource({
            "100.0, 10.0, 20.0, 108.0",
            "200.0,  0.0, 10.0, 220.0",
            "80.0,  50.0, 25.0, 50.0",
            "150.0, 20.0, 10.0, 132.0",
            "99.99, 10.0, 10.0, 98.9901"
    })
    void typicalValidInputsCalculateCorrectFinalPrice(
            double basePrice,
            double discountRate,
            double taxRate,
            double expected
    ) {
        double result = calculator.calculate(basePrice, discountRate, taxRate);

        assertThat(result).isCloseTo(expected, within(0.0001));
    }

    /**
     * Partition: very large valid base price.
     * This checks that the formula still works for large prices and does not accidentally overflow
     * or lose the main calculation logic.
     */
    @Test
    void veryLargeBasePriceStillUsesSameFormula() {
        double result = calculator.calculate(1_000_000.0, 10.0, 20.0);

        assertThat(result).isCloseTo(1_080_000.0, within(0.0001));
    }

    /**
     * Convenience method partition: discount-only calculation.
     * This checks that applyDiscountOnly delegates correctly to calculate with 0% tax.
     */
    @Test
    void applyDiscountOnlyUsesZeroTax() {
        double result = calculator.applyDiscountOnly(100.0, 30.0);

        assertThat(result).isCloseTo(70.0, within(0.0001));
    }

    /**
     * Convenience method partition: tax-only calculation.
     * This checks that applyTaxOnly delegates correctly to calculate with 0% discount.
     */
    @Test
    void applyTaxOnlyUsesZeroDiscount() {
        double result = calculator.applyTaxOnly(100.0, 18.0);

        assertThat(result).isCloseTo(118.0, within(0.0001));
    }
    /**
     * Invalid partition: negative base price.
     * The contract says basePrice must be >= 0, so this should fail with AssertionError.
     */
    @Test
    void negativeBasePriceViolatesContract() {
        assertThatThrownBy(() -> calculator.calculate(-1.0, 10.0, 20.0))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("basePrice must be >= 0");
    }

    /**
     * Invalid partition: discountRate below 0 or above 100.
     * These are outside the valid discount domain.
     */
    @ParameterizedTest(name = "invalid discount={0}% should violate contract")
    @CsvSource({
            "-0.1",
            "-1.0",
            "100.1",
            "101.0"
    })
    void invalidDiscountRateViolatesContract(double discountRate) {
        assertThatThrownBy(() -> calculator.calculate(100.0, discountRate, 20.0))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("discountRate must be between 0 and 100");
    }

    /**
     * Invalid partition: taxRate below 0 or above 100.
     * These are outside the valid tax domain.
     */
    @ParameterizedTest(name = "invalid tax={0}% should violate contract")
    @CsvSource({
            "-0.1",
            "-1.0",
            "100.1",
            "101.0"
    })
    void invalidTaxRateViolatesContract(double taxRate) {
        assertThatThrownBy(() -> calculator.calculate(100.0, 10.0, taxRate))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("taxRate must be between 0 and 100");
    }
}