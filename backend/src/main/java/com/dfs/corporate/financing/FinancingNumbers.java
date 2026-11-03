package com.dfs.corporate.financing;

import com.dfs.corporate.web.error.ApiException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public final class FinancingNumbers {

    private FinancingNumbers() {}

    public static String cnic(String raw) {
        if (raw == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CNIC is required");
        }
        String digits = raw.replaceAll("\\D", "");
        if (digits.length() != 13) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CNIC must be 13 digits");
        }
        return digits;
    }

    public static String mobile(String raw) {
        if (raw == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Mobile number is required");
        }
        String digits = raw.replaceAll("\\D", "");
        if (digits.startsWith("0") && digits.length() == 11) {
            digits = "92" + digits.substring(1);
        }
        if (!digits.startsWith("92") || digits.length() != 12) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Enter a Pakistan mobile number");
        }
        return digits;
    }

    public static String maskCnic(String cnic) {
        if (cnic == null || cnic.length() < 4) return "****";
        return "*********" + cnic.substring(cnic.length() - 4);
    }

    public static List<BigDecimal> percents(String csv) {
        List<BigDecimal> out = new ArrayList<>();
        if (csv == null || csv.isBlank()) return out;
        for (String part : csv.split(",")) {
            if (part.isBlank()) continue;
            out.add(new BigDecimal(part.trim()).setScale(4, RoundingMode.HALF_UP));
        }
        return out;
    }

    public static List<Integer> months(String csv) {
        List<Integer> out = new ArrayList<>();
        if (csv == null || csv.isBlank()) return out;
        for (String part : csv.split(",")) {
            if (part.isBlank()) continue;
            out.add(Integer.parseInt(part.trim()));
        }
        return out;
    }

    public static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal percent(BigDecimal value) {
        return value.setScale(4, RoundingMode.HALF_UP);
    }

    public static boolean samePercent(BigDecimal a, BigDecimal b) {
        return a.setScale(4, RoundingMode.HALF_UP).compareTo(b.setScale(4, RoundingMode.HALF_UP)) == 0;
    }
}
