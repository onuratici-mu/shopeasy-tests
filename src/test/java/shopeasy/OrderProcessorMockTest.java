package shopeasy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Task 5 – Mocks & Stubs
 *
 * These tests isolate OrderProcessor from its two external dependencies:
 * InventoryService and PaymentGateway.
 */
@ExtendWith(MockitoExtension.class)
class OrderProcessorMockTest {

    @Mock
    private InventoryService inventoryService;

    @Mock
    private PaymentGateway paymentGateway;

    @InjectMocks
    private OrderProcessor orderProcessor;

    private ShoppingCart cart;
    private Product widget;
    private Product keyboard;

    @BeforeEach
    void setUp() {
        cart = new ShoppingCart();
        widget = new Product("P001", "Widget", 25.0, 100);
        keyboard = new Product("P002", "Keyboard", 40.0, 50);
    }

    /**
     * Scenario 1: Happy path.
     * Inventory is available and payment succeeds, so an Order should be created.
     */
    @Test
    void processInventoryAvailableAndPaymentSucceedsReturnsOrder() {
        cart.addItem(widget, 2); // total = 50

        when(inventoryService.isAvailable(widget, 2)).thenReturn(true);
        when(paymentGateway.charge("customer-1", 50.0)).thenReturn(true);

        Order order = orderProcessor.process("customer-1", cart);

        assertThat(order).isNotNull();
        assertThat(order.getCustomerId()).isEqualTo("customer-1");
        assertThat(order.getItems()).hasSize(1);
        assertThat(order.getTotal()).isCloseTo(50.0, within(0.0001));

        verify(inventoryService).isAvailable(widget, 2);
        verify(paymentGateway).charge("customer-1", 50.0);
    }

    /**
     * Scenario 2: Inventory failure.
     * If inventory is unavailable, checkout should stop and payment should never be attempted.
     */
    @Test
    void processInventoryUnavailableReturnsNullAndDoesNotChargePayment() {
        cart.addItem(widget, 2);

        when(inventoryService.isAvailable(widget, 2)).thenReturn(false);

        Order order = orderProcessor.process("customer-1", cart);

        assertThat(order).isNull();

        verify(inventoryService).isAvailable(widget, 2);
        verify(paymentGateway, never()).charge(anyString(), anyDouble());
    }

    /**
     * Scenario 3: Payment failure.
     * Inventory is available, but payment fails, so no Order should be created.
     */
    @Test
    void processPaymentFailsReturnsNull() {
        cart.addItem(widget, 2); // total = 50

        when(inventoryService.isAvailable(widget, 2)).thenReturn(true);
        when(paymentGateway.charge("customer-1", 50.0)).thenReturn(false);

        Order order = orderProcessor.process("customer-1", cart);

        assertThat(order).isNull();

        verify(inventoryService).isAvailable(widget, 2);
        verify(paymentGateway).charge("customer-1", 50.0);
    }

    /**
     * Scenario 4: Partial quantity / insufficient requested quantity.
     * Expected behavior: if the requested quantity cannot be fully supplied,
     * InventoryService returns false, the order is not created, and payment is not charged.
     */
    @Test
    void processPartialQuantityUnavailableReturnsNullAndDoesNotCharge() {
        cart.addItem(widget, 5);

        when(inventoryService.isAvailable(widget, 5)).thenReturn(false);

        Order order = orderProcessor.process("customer-1", cart);

        assertThat(order).isNull();

        verify(inventoryService).isAvailable(widget, 5);
        verify(paymentGateway, never()).charge(anyString(), anyDouble());
    }

    /**
     * Scenario 5: Multiple items where one item is unavailable.
     * This checks that OrderProcessor stops when any cart line fails inventory.
     */
    @Test
    void processOneUnavailableItemInMultiItemCartReturnsNullAndDoesNotCharge() {
        cart.addItem(widget, 2);    // 50
        cart.addItem(keyboard, 1);  // 40

        when(inventoryService.isAvailable(widget, 2)).thenReturn(true);
        when(inventoryService.isAvailable(keyboard, 1)).thenReturn(false);

        Order order = orderProcessor.process("customer-1", cart);

        assertThat(order).isNull();

        verify(inventoryService).isAvailable(widget, 2);
        verify(inventoryService).isAvailable(keyboard, 1);
        verify(paymentGateway, never()).charge(anyString(), anyDouble());
    }

    /**
     * Validation case: empty cart.
     * Empty carts are rejected before inventory or payment services are called.
     */
    @Test
    void processEmptyCartThrowsExceptionAndDoesNotCallDependencies() {
        assertThatThrownBy(() -> orderProcessor.process("customer-1", cart))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cart must not be empty");

        verifyNoInteractions(inventoryService);
        verifyNoInteractions(paymentGateway);
    }

    /**
     * Validation case: invalid customer id.
     * A blank customer id is rejected before external services are called.
     */
    @Test
    void processBlankCustomerIdThrowsExceptionAndDoesNotCallDependencies() {
        cart.addItem(widget, 1);

        assertThatThrownBy(() -> orderProcessor.process("   ", cart))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("customerId must not be null or blank");

        verifyNoInteractions(inventoryService);
        verifyNoInteractions(paymentGateway);
    }
}