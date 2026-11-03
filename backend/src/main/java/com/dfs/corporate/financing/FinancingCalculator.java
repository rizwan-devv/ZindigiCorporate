package com.dfs.corporate.financing;

import com.dfs.corporate.financing.domain.FinancingSettings;
import com.dfs.corporate.web.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Reducing-balance EMI. This is the only implemented formula, and it runs only when
 * settings.formulaCode is REDUCING_BALANCE_EMI. The bank formula is still an open BRD item.
 */
@Component
public class FinancingCalculator {

    public static final String FORMULA = "REDUCING_BALANCE_EMI";

    public BigDecimal effectiveKibor(FinancingSettings settings) {
        BigDecimal kibor = settings.getKiborPercent();
        BigDecimal floor = settings.getKiborFloorPercent();
        if (kibor.compareTo(floor) < 0) {
            return floor;
        }
        return kibor;
    }

    public Quote quote(FinancingSettings settings, BigDecimal price, BigDecimal downPaymentPercent, int tenureMonths, BigDecimal annualRate) {
        if (settings.getFormulaCode() == null || !FORMULA.equals(settings.getFormulaCode())) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Financing formula is not configured. Installments stay blank until the bank formula is set.");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Product price must be greater than zero");
        }
        if (tenureMonths <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Tenure must be at least 1 month");
        }
        if (annualRate == null || annualRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Financing rate is invalid");
        }
        BigDecimal downAmount = FinancingNumbers.money(
                price.multiply(downPaymentPercent).divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP));
        BigDecimal financed = FinancingNumbers.money(price.subtract(downAmount));
        if (financed.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Down payment leaves nothing to finance");
        }
        BigDecimal installment = installment(financed, annualRate, tenureMonths);
        BigDecimal total = FinancingNumbers.money(installment.multiply(BigDecimal.valueOf(tenureMonths)));
        return new Quote(downAmount, financed, FinancingNumbers.percent(annualRate), tenureMonths, installment, total);
    }

    public BigDecimal installment(BigDecimal principal, BigDecimal annualPercent, int months) {
        if (annualPercent.compareTo(BigDecimal.ZERO) == 0) {
            return FinancingNumbers.money(principal.divide(BigDecimal.valueOf(months), 6, RoundingMode.HALF_UP));
        }
        double monthly = annualPercent.doubleValue() / 100.0 / 12.0;
        double factor = Math.pow(1.0 + monthly, months);
        double emi = principal.doubleValue() * monthly * factor / (factor - 1.0);
        return BigDecimal.valueOf(emi).setScale(2, RoundingMode.HALF_UP);
    }

    public record Quote(
            BigDecimal downPaymentAmount,
            BigDecimal financedAmount,
            BigDecimal annualRatePercent,
            int tenureMonths,
            BigDecimal monthlyInstallment,
            BigDecimal totalPayable
    ) {}
}
