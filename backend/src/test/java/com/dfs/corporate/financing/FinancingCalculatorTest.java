package com.dfs.corporate.financing;

import com.dfs.corporate.financing.domain.FinancingSettings;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinancingCalculatorTest {

    @Test
    void reducingBalanceEmiForOneYearAtTwelvePercent() {
        FinancingCalculator calculator = new FinancingCalculator();
        FinancingSettings settings = new FinancingSettings();
        settings.setFormulaCode(FinancingCalculator.FORMULA);
        settings.setKiborPercent(new BigDecimal("12"));
        settings.setKiborFloorPercent(new BigDecimal("12"));

        FinancingCalculator.Quote quote = calculator.quote(
                settings,
                new BigDecimal("100000"),
                new BigDecimal("0"),
                12,
                new BigDecimal("12"));

        assertEquals(new BigDecimal("100000.00"), quote.financedAmount());
        assertEquals(new BigDecimal("8884.88"), quote.monthlyInstallment());
        assertTrue(quote.totalPayable().compareTo(quote.financedAmount()) > 0);
    }

    @Test
    void kiborDoesNotFallBelowFloor() {
        FinancingCalculator calculator = new FinancingCalculator();
        FinancingSettings settings = new FinancingSettings();
        settings.setKiborPercent(new BigDecimal("9"));
        settings.setKiborFloorPercent(new BigDecimal("12"));
        assertEquals(0, calculator.effectiveKibor(settings).compareTo(new BigDecimal("12")));
    }
}
