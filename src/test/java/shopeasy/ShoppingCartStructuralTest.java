package shopeasy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Task 2 – Structural Testing & Code Coverage
 *
 * Target: ShoppingCart
 *
 * These tests are written with code coverage in mind. The goal is not only to
 * test normal behavior, but also to cover important branches such as:
 * - adding a new product vs adding an existing product
 * - updating an existing product vs missing product
 * - valid quantity vs invalid quantity
 * - removing an existing product vs removing a missing product
 * - empty cart vs non-empty cart
 */
class ShoppingCartStructuralTest {

    private ShoppingCart cart;
    private Product apple;
    private Product banana;

    @BeforeEach
    void setUp() {
        cart   = new ShoppingCart();
        apple  = new Product("P001", "Apple",  1.50, 100);
        banana = new Product("P002", "Banana", 0.80, 50);
    }

    /**
     * Branch: empty cart.
     * Before adding anything, the cart should have no item lines and total should be zero.
     */
    @Test
    void newCartStartsEmpty() {
        assertThat(cart.itemCount()).isZero();
        assertThat(cart.total()).isEqualTo(0.0);
        assertThat(cart.getItems()).isEmpty();
    }

    /**
     * Branch: addItem when the product is not already in the cart.
     * This should create a new cart line.
     */
    @Test
    void addNewProductCreatesNewCartLine() {
        cart.addItem(apple, 2);

        assertThat(cart.itemCount()).isEqualTo(1);
        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getProduct()).isEqualTo(apple);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(cart.total()).isCloseTo(3.00, within(0.0001));
    }

    /**
     * Branch: addItem when the product already exists in the cart.
     * Instead of creating a second line, the cart should combine the quantities.
     */
    @Test
    void addExistingProductCombinesQuantities() {
        cart.addItem(apple, 2);
        cart.addItem(apple, 3);

        assertThat(cart.itemCount()).isEqualTo(1);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(5);
        assertThat(cart.total()).isCloseTo(7.50, within(0.0001));
    }

    /**
     * Branch: addItem with two different products.
     * This covers multiple cart lines and checks that total sums all subtotals.
     */
    @Test
    void addDifferentProductsCreatesMultipleCartLines() {
        cart.addItem(apple, 2);   // 2 * 1.50 = 3.00
        cart.addItem(banana, 5);  // 5 * 0.80 = 4.00

        assertThat(cart.itemCount()).isEqualTo(2);
        assertThat(cart.total()).isCloseTo(7.00, within(0.0001));
    }

    /**
     * Branch: removeItem when the product exists.
     * The matching cart line should be removed.
     */
    @Test
    void removeExistingProductDeletesCartLine() {
        cart.addItem(apple, 2);
        cart.addItem(banana, 5);

        cart.removeItem("P001");

        assertThat(cart.itemCount()).isEqualTo(1);
        assertThat(cart.getItems().get(0).getProduct()).isEqualTo(banana);
        assertThat(cart.total()).isCloseTo(4.00, within(0.0001));
    }

    /**
     * Branch: removeItem when the product does not exist.
     * The method should do nothing and leave the cart unchanged.
     */
    @Test
    void removeMissingProductDoesNothing() {
        cart.addItem(apple, 2);

        cart.removeItem("DOES_NOT_EXIST");

        assertThat(cart.itemCount()).isEqualTo(1);
        assertThat(cart.total()).isCloseTo(3.00, within(0.0001));
    }

    /**
     * Branch: updateQuantity with a valid quantity and an existing product.
     * The product line should be found and updated.
     */
    @Test
    void updateQuantityForExistingProductChangesQuantity() {
        cart.addItem(apple, 2);

        cart.updateQuantity("P001", 10);

        assertThat(cart.itemCount()).isEqualTo(1);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(10);
        assertThat(cart.total()).isCloseTo(15.00, within(0.0001));
    }

    /**
     * Branch: updateQuantity with invalid quantity.
     * Quantity 0 or lower should fail before searching the cart.
     */
    @Test
    void updateQuantityRejectsZeroOrNegativeQuantity() {
        cart.addItem(apple, 2);

        assertThatThrownBy(() -> cart.updateQuantity("P001", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Quantity must be > 0");

        assertThatThrownBy(() -> cart.updateQuantity("P001", -3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Quantity must be > 0");
    }

    /**
     * Branch: updateQuantity with valid quantity but missing product.
     * The loop finishes without finding the product, so the method should throw.
     */
    @Test
    void updateQuantityThrowsWhenProductIsNotInCart() {
        cart.addItem(apple, 2);

        assertThatThrownBy(() -> cart.updateQuantity("P999", 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product not found");
    }

    /**
     * Branch: applyDiscount with 0%.
     * A zero discount should return the same value as the raw total.
     */
    @Test
    void zeroDiscountReturnsRawTotal() {
        cart.addItem(apple, 2);
        cart.addItem(banana, 5);

        double discountedTotal = cart.applyDiscount(0.0);

        assertThat(discountedTotal).isCloseTo(cart.total(), within(0.0001));
    }

    /**
     * Branch: applyDiscount with a normal positive discount.
     * The returned value should be lower than the raw total.
     */
    @Test
    void positiveDiscountReducesTotal() {
        cart.addItem(apple, 2);
        cart.addItem(banana, 5);

        double discountedTotal = cart.applyDiscount(10.0);

        assertThat(cart.total()).isCloseTo(7.00, within(0.0001));
        assertThat(discountedTotal).isCloseTo(6.30, within(0.0001));
        assertThat(discountedTotal).isLessThan(cart.total());
    }

    /**
     * Branch: applyDiscount at the upper boundary 100%.
     * A full discount should reduce the returned value to zero.
     */
    @Test
    void hundredPercentDiscountReturnsZero() {
        cart.addItem(apple, 2);

        double discountedTotal = cart.applyDiscount(100.0);

        assertThat(discountedTotal).isCloseTo(0.0, within(0.0001));
    }

    /**
     * Behavior: getItems returns an unmodifiable view.
     * This protects the cart from outside code directly changing its internal list.
     */
    @Test
    void getItemsReturnsUnmodifiableList() {
        cart.addItem(apple, 1);

        assertThatThrownBy(() -> cart.getItems().clear())
                .isInstanceOf(UnsupportedOperationException.class);

        assertThat(cart.itemCount()).isEqualTo(1);
    }

    /**
     * Behavior: clear removes all items.
     * This also returns the cart to the empty-cart branch for total and item count.
     */
    @Test
    void clearRemovesAllItems() {
        cart.addItem(apple, 2);
        cart.addItem(banana, 5);

        cart.clear();

        assertThat(cart.itemCount()).isZero();
        assertThat(cart.total()).isEqualTo(0.0);
        assertThat(cart.getItems()).isEmpty();
    }

    /**
     * Behavior: toString includes useful cart information.
     * This gives simple coverage for the string representation method.
     */
    @Test
    void toStringIncludesItemCountAndTotal() {
        cart.addItem(apple, 2);

        String text = cart.toString();

        assertThat(text).contains("ShoppingCart");
        assertThat(text).contains("items=1");
        assertThat(text).contains("total=3.00");
    }
}