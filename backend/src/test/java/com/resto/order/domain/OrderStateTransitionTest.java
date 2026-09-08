package com.resto.order.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderStateTransitionTest {

    @Test
    @DisplayName("Happy path kitchen transitions")
    void happyPath() {
        assertTrue(OrderStateMachine.canTransition("CREATED", "SENT_TO_KITCHEN"));
        assertTrue(OrderStateMachine.canTransition("SENT_TO_KITCHEN", "PREPARING"));
        assertTrue(OrderStateMachine.canTransition("PREPARING", "READY"));
        assertTrue(OrderStateMachine.canTransition("READY", "DELIVERED"));
        assertTrue(OrderStateMachine.canTransition("DELIVERED", "CLOSED"));
    }

    @Test
    @DisplayName("Cancel allowed only from early states")
    void cancelRules() {
        assertTrue(OrderStateMachine.canCancel("CREATED"));
        assertTrue(OrderStateMachine.canCancel("SENT_TO_KITCHEN"));
        assertTrue(OrderStateMachine.canCancel("PREPARING"));
        assertFalse(OrderStateMachine.canCancel("READY"));
        assertFalse(OrderStateMachine.canCancel("DELIVERED"));
        assertFalse(OrderStateMachine.canCancel("CLOSED"));
    }

    @Test
    @DisplayName("Invalid transitions throw")
    void invalidThrows() {
        assertThrows(IllegalStateException.class,
                () -> OrderStateMachine.assertTransition("CREATED", "READY"));
        assertThrows(IllegalStateException.class,
                () -> OrderStateMachine.assertTransition("READY", "PREPARING"));
        assertThrows(IllegalStateException.class,
                () -> OrderStateMachine.assertTransition("CLOSED", "CREATED"));
    }

    @Test
    @DisplayName("Kitchen active filter")
    void kitchenActive() {
        assertTrue(OrderStateMachine.isKitchenActive("SENT_TO_KITCHEN"));
        assertTrue(OrderStateMachine.isKitchenActive("PREPARING"));
        assertTrue(OrderStateMachine.isKitchenActive("READY"));
        assertFalse(OrderStateMachine.isKitchenActive("CREATED"));
        assertFalse(OrderStateMachine.isKitchenActive("CLOSED"));
    }
}
