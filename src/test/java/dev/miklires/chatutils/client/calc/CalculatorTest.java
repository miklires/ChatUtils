package dev.miklires.chatutils.client.calc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CalculatorTest {
    @Test void respectsPrecedenceAndAssociativity() {
        assertEquals(14, Calculator.evaluate("2 + 3 * 4"));
        assertEquals(512, Calculator.evaluate("2^3^2"));
        assertEquals(-4, Calculator.evaluate("-2^2"));
    }

    @Test void evaluatesConstantsAndFunctions() {
        assertEquals(5, Calculator.evaluate("hypot(3, 4)"));
        assertEquals(1, Calculator.evaluate("sind(90)"), 1e-12);
        assertEquals(3, Calculator.evaluate("log(8, 2)"), 1e-12);
    }

    @Test void rejectsInvalidOrUnsafeResults() {
        assertThrows(Calculator.CalculatorException.class, () -> Calculator.evaluate("1 / 0"));
        assertThrows(Calculator.CalculatorException.class, () -> Calculator.evaluate("sqrt(-1)"));
        assertThrows(Calculator.CalculatorException.class, () -> Calculator.evaluate("2 +"));
    }
}
