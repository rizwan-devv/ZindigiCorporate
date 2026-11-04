package com.dfs.corporate.financing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "financing_settings")
public class FinancingSettings {

    @Id
    private Long id;

    @Column(name = "kibor_percent", nullable = false, precision = 8, scale = 4)
    private BigDecimal kiborPercent;

    @Column(name = "kibor_floor_percent", nullable = false, precision = 8, scale = 4)
    private BigDecimal kiborFloorPercent;

    @Column(name = "suggested_bank_commission_percent", nullable = false, precision = 8, scale = 4)
    private BigDecimal suggestedBankCommissionPercent;

    @Column(name = "bank_commission_max_percent", nullable = false, precision = 8, scale = 4)
    private BigDecimal bankCommissionMaxPercent;

    @Column(name = "down_payment_min_percent", nullable = false, precision = 8, scale = 4)
    private BigDecimal downPaymentMinPercent;

    @Column(name = "down_payment_max_percent", nullable = false, precision = 8, scale = 4)
    private BigDecimal downPaymentMaxPercent;

    @Column(name = "down_payment_options", nullable = false, length = 200)
    private String downPaymentOptions;

    @Column(name = "tenure_months", nullable = false, length = 100)
    private String tenureMonths;

    @Column(name = "dbr_threshold_percent", nullable = false, precision = 8, scale = 4)
    private BigDecimal dbrThresholdPercent;

    @Column(name = "card_limit_basis", nullable = false, length = 32)
    private String cardLimitBasis;

    @Column(name = "card_limit_factor_percent", nullable = false, precision = 8, scale = 4)
    private BigDecimal cardLimitFactorPercent;

    @Column(name = "late_payment_percent", nullable = false, precision = 8, scale = 4)
    private BigDecimal latePaymentPercent;

    @Column(name = "quotation_max_age_days", nullable = false)
    private int quotationMaxAgeDays;

    @Column(name = "formula_code", length = 64)
    private String formulaCode;

    @Column(name = "formula_note", length = 500)
    private String formulaNote;

    @Column(name = "product_enabled", nullable = false)
    private boolean productEnabled;

    /** DFS wallet that receives customer down payments. */
    @Column(name = "collection_wallet", length = 20)
    private String collectionWallet;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public BigDecimal getKiborPercent() { return kiborPercent; }
    public void setKiborPercent(BigDecimal kiborPercent) { this.kiborPercent = kiborPercent; }
    public BigDecimal getKiborFloorPercent() { return kiborFloorPercent; }
    public void setKiborFloorPercent(BigDecimal kiborFloorPercent) { this.kiborFloorPercent = kiborFloorPercent; }
    public BigDecimal getSuggestedBankCommissionPercent() { return suggestedBankCommissionPercent; }
    public void setSuggestedBankCommissionPercent(BigDecimal suggestedBankCommissionPercent) { this.suggestedBankCommissionPercent = suggestedBankCommissionPercent; }
    public BigDecimal getBankCommissionMaxPercent() { return bankCommissionMaxPercent; }
    public void setBankCommissionMaxPercent(BigDecimal bankCommissionMaxPercent) { this.bankCommissionMaxPercent = bankCommissionMaxPercent; }
    public BigDecimal getDownPaymentMinPercent() { return downPaymentMinPercent; }
    public void setDownPaymentMinPercent(BigDecimal downPaymentMinPercent) { this.downPaymentMinPercent = downPaymentMinPercent; }
    public BigDecimal getDownPaymentMaxPercent() { return downPaymentMaxPercent; }
    public void setDownPaymentMaxPercent(BigDecimal downPaymentMaxPercent) { this.downPaymentMaxPercent = downPaymentMaxPercent; }
    public String getDownPaymentOptions() { return downPaymentOptions; }
    public void setDownPaymentOptions(String downPaymentOptions) { this.downPaymentOptions = downPaymentOptions; }
    public String getTenureMonths() { return tenureMonths; }
    public void setTenureMonths(String tenureMonths) { this.tenureMonths = tenureMonths; }
    public BigDecimal getDbrThresholdPercent() { return dbrThresholdPercent; }
    public void setDbrThresholdPercent(BigDecimal dbrThresholdPercent) { this.dbrThresholdPercent = dbrThresholdPercent; }
    public String getCardLimitBasis() { return cardLimitBasis; }
    public void setCardLimitBasis(String cardLimitBasis) { this.cardLimitBasis = cardLimitBasis; }
    public BigDecimal getCardLimitFactorPercent() { return cardLimitFactorPercent; }
    public void setCardLimitFactorPercent(BigDecimal cardLimitFactorPercent) { this.cardLimitFactorPercent = cardLimitFactorPercent; }
    public BigDecimal getLatePaymentPercent() { return latePaymentPercent; }
    public void setLatePaymentPercent(BigDecimal latePaymentPercent) { this.latePaymentPercent = latePaymentPercent; }
    public int getQuotationMaxAgeDays() { return quotationMaxAgeDays; }
    public void setQuotationMaxAgeDays(int quotationMaxAgeDays) { this.quotationMaxAgeDays = quotationMaxAgeDays; }
    public String getFormulaCode() { return formulaCode; }
    public void setFormulaCode(String formulaCode) { this.formulaCode = formulaCode; }
    public String getFormulaNote() { return formulaNote; }
    public void setFormulaNote(String formulaNote) { this.formulaNote = formulaNote; }
    public boolean isProductEnabled() { return productEnabled; }
    public void setProductEnabled(boolean productEnabled) { this.productEnabled = productEnabled; }
    public String getCollectionWallet() { return collectionWallet; }
    public void setCollectionWallet(String collectionWallet) { this.collectionWallet = collectionWallet; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
