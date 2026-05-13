package shopeasy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * Task 4 – Property-Based Testing
 *
 * Instead of checking only fixed examples, these tests check general rules
 * that should hold for many generated inputs.
 */
class ShopEasyPropertyTest {

    private final PriceCalculator calculator = new PriceCalculator();

    /**
     * Custom provider for valid money/base price values.
     * It avoids negative values because PriceCalculator's contract only allows basePrice >= 0.
     */
    @Provide
    Arbitrary<Double> validMoneyValues() {
        return Arbitraries.doubles().between(0.0, 10_000.0);
    }

    /**
     * Custom provider for valid percentage rates.
     * It matches the contract range used by discountRate and taxRate: 0 to 100.
     */
    @Provide
    Arbitrary<Double> validRates() {
        return Arbitraries.doubles().between(0.0, 100.0);
    }

    /**
     * Custom provider for positive item quantities.
     * ShoppingCart.addItem requires quantity > 0.
     */
    @Provide
    Arbitrary<Integer> positiveQuantities() {
        return Arbitraries.integers().between(1, 100);
    }

    /**
     * Property: Identity.
     * Meaning: For any valid base price, 0% discount and 0% tax should return the base price.
     * Bug it catches: accidental changes where the calculator changes the price even when
     * no discount or tax should be applied.
     */
    @Property(tries = 100)
    void zeroDiscountAndZeroTaxReturnsBasePrice(
            @ForAll("validMoneyValues") double basePrice
    ) {
        double result = calculator.calculate(basePrice, 0.0, 0.0);

        assertThat(result).isCloseTo(basePrice, within(0.0001));
    }

    /**
     * Property: Monotonicity.
     * Meaning: For the same base price and tax rate, using a higher discount should never
     * create a higher final price.
     * Bug it catches: reversed discount logic, adding the discount instead of subtracting it,
     * or applying discount in the wrong direction.
     */
    @Property(tries = 100)
    void increasingDiscountNeverIncreasesFinalPrice(
            @ForAll("validMoneyValues") double basePrice,
            @ForAll("validRates") double taxRate,
            @ForAll("validRates") double firstDiscount,
            @ForAll("validRates") double secondDiscount
    ) {
        double lowerDiscount = Math.min(firstDiscount, secondDiscount);
        double higherDiscount = Math.max(firstDiscount, secondDiscount);

        double priceWithLowerDiscount = calculator.calculate(basePrice, lowerDiscount, taxRate);
        double priceWithHigherDiscount = calculator.calculate(basePrice, higherDiscount, taxRate);

        assertThat(priceWithHigherDiscount)
                .isLessThanOrEqualTo(priceWithLowerDiscount + 0.0001);
    }

    /**
     * Property: Boundedness.
     * Meaning: For valid inputs, the final price should never be negative and should never be
     * more than basePrice doubled, because the maximum tax rate is 100%.
     * Bug it catches: negative prices, tax applied more than once, or discount calculation errors.
     */
    @Property(tries = 100)
    void finalPriceForValidInputStaysWithinExpectedBounds(
            @ForAll("validMoneyValues") double basePrice,
            @ForAll("validRates") double discountRate,
            @ForAll("validRates") double taxRate
    ) {
        double result = calculator.calculate(basePrice, discountRate, taxRate);

        assertThat(result).isGreaterThanOrEqualTo(-0.0001);
        assertThat(result).isLessThanOrEqualTo((basePrice * 2.0) + 0.0001);
    }

    /**
     * Property: Cart commutativity.
     * Meaning: Adding product A then product B should give the same total as adding
     * product B then product A.
     * Bug it catches: order-dependent cart total calculation or incorrect item accumulation.
     */
    @Property(tries = 100)
    void addingDifferentProductsInDifferentOrdersKeepsSameTotal(
            @ForAll("positiveQuantities") int appleQuantity,
            @ForAll("positiveQuantities") int bananaQuantity
    ) {
        Product apple = new Product("P001", "Apple", 1.50, 1000);
        Product banana = new Product("P002", "Banana", 0.80, 1000);

        ShoppingCart firstCart = new ShoppingCart();
        firstCart.addItem(apple, appleQuantity);
        firstCart.addItem(banana, bananaQuantity);

        ShoppingCart secondCart = new ShoppingCart();
        secondCart.addItem(banana, bananaQuantity);
        secondCart.addItem(apple, appleQuantity);

        assertThat(firstCart.total()).isCloseTo(secondCart.total(), within(0.0001));
        assertThat(firstCart.itemCount()).isEqualTo(secondCart.itemCount());
    }

    /**
     * Property: Combining quantities.
     * Meaning: Adding the same product twice should be equivalent to adding it once with
     * the combined quantity.
     * Bug it catches: duplicate product lines, lost quantities, or incorrect merging behavior.
     */
    @Property(tries = 100)
    void addingSameProductTwiceEqualsAddingCombinedQuantity(
            @ForAll("positiveQuantities") int firstQuantity,
            @ForAll("positiveQuantities") int secondQuantity
    ) {
        Product apple = new Product("P001", "Apple", 1.50, 1000);

        ShoppingCart splitCart = new ShoppingCart();
        splitCart.addItem(apple, firstQuantity);
        splitCart.addItem(apple, secondQuantity);

        ShoppingCart combinedCart = new ShoppingCart();
        combinedCart.addItem(apple, firstQuantity + secondQuantity);

        assertThat(splitCart.itemCount()).isEqualTo(1);
        assertThat(splitCart.total()).isCloseTo(combinedCart.total(), within(0.0001));
    }
}