package com.resto.unit;

import com.resto.order.domain.OrderStateMachine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderStateMachineTest {

    @Test
    @DisplayName("Allows happy-path kitchen transitions")
    void happyPath() {
        assertTrue(OrderStateMachine.canTransition("CREATED", "SENT_TO_KITCHEN"));
        assertTrue(OrderStateMachine.canTransition("SENT_TO_KITCHEN", "PREPARING"));
        assertTrue(OrderStateMachine.canTransition("PREPARING", "READY"));
        assertTrue(OrderStateMachine.canTransition("READY", "DELIVERED"));
        assertTrue(OrderStateMachine.canTransition("DELIVERED", "CLOSED"));
    }

    @Test
    @DisplayName("Rejects illegal transitions and READY cancel")
    void rejectsIllegal() {
        assertFalse(OrderStateMachine.canTransition("READY", "PREPARING"));
        assertFalse(OrderStateMachine.canTransition("CLOSED", "CANCELLED"));
        assertFalse(OrderStateMachine.canTransition("CREATED", "READY"));
        assertFalse(OrderStateMachine.canTransition("CREATED", "CLOSED"));
        assertFalse(OrderStateMachine.canTransition("SENT_TO_KITCHEN", "READY"));
        assertFalse(OrderStateMachine.canTransition("PREPARING", "DELIVERED"));
        assertFalse(OrderStateMachine.canTransition("DELIVERED", "CANCELLED"));
        assertFalse(OrderStateMachine.canTransition("CANCELLED", "CREATED"));
        assertFalse(OrderStateMachine.canTransition(null, "CREATED"));
        assertFalse(OrderStateMachine.canCancel("READY"));
        assertFalse(OrderStateMachine.canCancel("DELIVERED"));
        assertFalse(OrderStateMachine.canCancel("CLOSED"));
        assertThrows(IllegalStateException.class,
                () -> OrderStateMachine.assertTransition("READY", "PREPARING"));
        assertThrows(IllegalStateException.class,
                () -> OrderStateMachine.assertTransition("CLOSED", "CREATED"));
        assertThrows(IllegalStateException.class,
                () -> OrderStateMachine.assertTransition("CREATED", "DELIVERED"));
    }

    @Test
    @DisplayName("Allows cancel only from early states")
    void cancelRules() {
        assertTrue(OrderStateMachine.canCancel("CREATED"));
        assertTrue(OrderStateMachine.canCancel("SENT_TO_KITCHEN"));
        assertTrue(OrderStateMachine.canCancel("PREPARING"));
    }
}
