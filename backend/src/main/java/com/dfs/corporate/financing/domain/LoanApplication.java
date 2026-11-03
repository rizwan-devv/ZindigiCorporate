package com.dfs.corporate.financing.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "loan_applications")
public class LoanApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, length = 36)
    private String publicId;

    @Column(name = "reference_no", nullable = false, length = 32)
    private String referenceNo;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "merchant_party_id", nullable = false)
    private Long merchantPartyId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", nullable = false, length = 40)
    private EmploymentType employmentType;

    @Column(name = "monthly_income", nullable = false, precision = 18, scale = 2)
    private BigDecimal monthlyIncome;

    @Column(name = "existing_installments", nullable = false, precision = 18, scale = 2)
    private BigDecimal existingInstallments;

    @Column(name = "card_limit_amount", precision = 18, scale = 2)
    private BigDecimal cardLimitAmount;

    @Column(name = "product_title", nullable = false, length = 200)
    private String productTitle;

    @Column(name = "product_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal productPrice;

    @Column(name = "requested_down_payment_percent", nullable = false, precision = 8, scale = 4)
    private BigDecimal requestedDownPaymentPercent;

    @Column(name = "confirmed_down_payment_percent", precision = 8, scale = 4)
    private BigDecimal confirmedDownPaymentPercent;

    @Column(name = "tenure_months", nullable = false)
    private int tenureMonths;

    @Column(name = "quotation_date", nullable = false)
    private LocalDate quotationDate;

    @Column(name = "indicative_kibor_percent", precision = 8, scale = 4)
    private BigDecimal indicativeKiborPercent;

    @Column(name = "indicative_monthly_installment", precision = 18, scale = 2)
    private BigDecimal indicativeMonthlyInstallment;

    @Column(name = "indicative_financed_amount", precision = 18, scale = 2)
    private BigDecimal indicativeFinancedAmount;

    @Column(name = "bank_commission_percent", precision = 8, scale = 4)
    private BigDecimal bankCommissionPercent;

    @Column(name = "approved_annual_rate", precision = 8, scale = 4)
    private BigDecimal approvedAnnualRate;

    @Column(name = "approved_monthly_installment", precision = 18, scale = 2)
    private BigDecimal approvedMonthlyInstallment;

    @Column(name = "approved_down_payment_amount", precision = 18, scale = 2)
    private BigDecimal approvedDownPaymentAmount;

    @Column(name = "approved_financed_amount", precision = 18, scale = 2)
    private BigDecimal approvedFinancedAmount;

    @Column(name = "approved_total_payable", precision = 18, scale = 2)
    private BigDecimal approvedTotalPayable;

    @Column(name = "commission_set_by_account_id")
    private Long commissionSetByAccountId;

    @Column(name = "commission_set_at")
    private Instant commissionSetAt;

    @Column(name = "forwarded_by_account_id")
    private Long forwardedByAccountId;

    @Column(name = "forwarded_at")
    private Instant forwardedAt;

    @Column(name = "ops_remarks", length = 1000)
    private String opsRemarks;

    @Column(name = "checker_remarks", length = 1000)
    private String checkerRemarks;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private LoanApplicationStatus status;

    @Column(name = "payment_status", nullable = false, length = 32)
    private String paymentStatus = "UNPAID";

    @Column(name = "biometric_ref", length = 80)
    private String biometricRef;

    @Column(name = "biometric_source", length = 40)
    private String biometricSource;

    @Column(name = "biometric_at")
    private Instant biometricAt;

    @Column(name = "ecib_status", nullable = false, length = 40)
    private String ecibStatus = "NOT_INTEGRATED";

    @Column(name = "risk_rating_status", nullable = false, length = 40)
    private String riskRatingStatus = "NOT_CONFIGURED";

    @Column(name = "dbr_percent", precision = 8, scale = 4)
    private BigDecimal dbrPercent;

    @Column(name = "dbr_passed")
    private Boolean dbrPassed;

    @Column(name = "dbr_note", length = 500)
    private String dbrNote;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPublicId() { return publicId; }
    public void setPublicId(String publicId) { this.publicId = publicId; }
    public String getReferenceNo() { return referenceNo; }
    public void setReferenceNo(String referenceNo) { this.referenceNo = referenceNo; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public Long getMerchantPartyId() { return merchantPartyId; }
    public void setMerchantPartyId(Long merchantPartyId) { this.merchantPartyId = merchantPartyId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public EmploymentType getEmploymentType() { return employmentType; }
    public void setEmploymentType(EmploymentType employmentType) { this.employmentType = employmentType; }
    public BigDecimal getMonthlyIncome() { return monthlyIncome; }
    public void setMonthlyIncome(BigDecimal monthlyIncome) { this.monthlyIncome = monthlyIncome; }
    public BigDecimal getExistingInstallments() { return existingInstallments; }
    public void setExistingInstallments(BigDecimal existingInstallments) { this.existingInstallments = existingInstallments; }
    public BigDecimal getCardLimitAmount() { return cardLimitAmount; }
    public void setCardLimitAmount(BigDecimal cardLimitAmount) { this.cardLimitAmount = cardLimitAmount; }
    public String getProductTitle() { return productTitle; }
    public void setProductTitle(String productTitle) { this.productTitle = productTitle; }
    public BigDecimal getProductPrice() { return productPrice; }
    public void setProductPrice(BigDecimal productPrice) { this.productPrice = productPrice; }
    public BigDecimal getRequestedDownPaymentPercent() { return requestedDownPaymentPercent; }
    public void setRequestedDownPaymentPercent(BigDecimal requestedDownPaymentPercent) { this.requestedDownPaymentPercent = requestedDownPaymentPercent; }
    public BigDecimal getConfirmedDownPaymentPercent() { return confirmedDownPaymentPercent; }
    public void setConfirmedDownPaymentPercent(BigDecimal confirmedDownPaymentPercent) { this.confirmedDownPaymentPercent = confirmedDownPaymentPercent; }
    public int getTenureMonths() { return tenureMonths; }
    public void setTenureMonths(int tenureMonths) { this.tenureMonths = tenureMonths; }
    public LocalDate getQuotationDate() { return quotationDate; }
    public void setQuotationDate(LocalDate quotationDate) { this.quotationDate = quotationDate; }
    public BigDecimal getIndicativeKiborPercent() { return indicativeKiborPercent; }
    public void setIndicativeKiborPercent(BigDecimal indicativeKiborPercent) { this.indicativeKiborPercent = indicativeKiborPercent; }
    public BigDecimal getIndicativeMonthlyInstallment() { return indicativeMonthlyInstallment; }
    public void setIndicativeMonthlyInstallment(BigDecimal indicativeMonthlyInstallment) { this.indicativeMonthlyInstallment = indicativeMonthlyInstallment; }
    public BigDecimal getIndicativeFinancedAmount() { return indicativeFinancedAmount; }
    public void setIndicativeFinancedAmount(BigDecimal indicativeFinancedAmount) { this.indicativeFinancedAmount = indicativeFinancedAmount; }
    public BigDecimal getBankCommissionPercent() { return bankCommissionPercent; }
    public void setBankCommissionPercent(BigDecimal bankCommissionPercent) { this.bankCommissionPercent = bankCommissionPercent; }
    public BigDecimal getApprovedAnnualRate() { return approvedAnnualRate; }
    public void setApprovedAnnualRate(BigDecimal approvedAnnualRate) { this.approvedAnnualRate = approvedAnnualRate; }
    public BigDecimal getApprovedMonthlyInstallment() { return approvedMonthlyInstallment; }
    public void setApprovedMonthlyInstallment(BigDecimal approvedMonthlyInstallment) { this.approvedMonthlyInstallment = approvedMonthlyInstallment; }
    public BigDecimal getApprovedDownPaymentAmount() { return approvedDownPaymentAmount; }
    public void setApprovedDownPaymentAmount(BigDecimal approvedDownPaymentAmount) { this.approvedDownPaymentAmount = approvedDownPaymentAmount; }
    public BigDecimal getApprovedFinancedAmount() { return approvedFinancedAmount; }
    public void setApprovedFinancedAmount(BigDecimal approvedFinancedAmount) { this.approvedFinancedAmount = approvedFinancedAmount; }
    public BigDecimal getApprovedTotalPayable() { return approvedTotalPayable; }
    public void setApprovedTotalPayable(BigDecimal approvedTotalPayable) { this.approvedTotalPayable = approvedTotalPayable; }
    public Long getCommissionSetByAccountId() { return commissionSetByAccountId; }
    public void setCommissionSetByAccountId(Long commissionSetByAccountId) { this.commissionSetByAccountId = commissionSetByAccountId; }
    public Instant getCommissionSetAt() { return commissionSetAt; }
    public void setCommissionSetAt(Instant commissionSetAt) { this.commissionSetAt = commissionSetAt; }
    public Long getForwardedByAccountId() { return forwardedByAccountId; }
    public void setForwardedByAccountId(Long forwardedByAccountId) { this.forwardedByAccountId = forwardedByAccountId; }
    public Instant getForwardedAt() { return forwardedAt; }
    public void setForwardedAt(Instant forwardedAt) { this.forwardedAt = forwardedAt; }
    public String getOpsRemarks() { return opsRemarks; }
    public void setOpsRemarks(String opsRemarks) { this.opsRemarks = opsRemarks; }
    public String getCheckerRemarks() { return checkerRemarks; }
    public void setCheckerRemarks(String checkerRemarks) { this.checkerRemarks = checkerRemarks; }
    public LoanApplicationStatus getStatus() { return status; }
    public void setStatus(LoanApplicationStatus status) { this.status = status; }
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public String getBiometricRef() { return biometricRef; }
    public void setBiometricRef(String biometricRef) { this.biometricRef = biometricRef; }
    public String getBiometricSource() { return biometricSource; }
    public void setBiometricSource(String biometricSource) { this.biometricSource = biometricSource; }
    public Instant getBiometricAt() { return biometricAt; }
    public void setBiometricAt(Instant biometricAt) { this.biometricAt = biometricAt; }
    public String getEcibStatus() { return ecibStatus; }
    public void setEcibStatus(String ecibStatus) { this.ecibStatus = ecibStatus; }
    public String getRiskRatingStatus() { return riskRatingStatus; }
    public void setRiskRatingStatus(String riskRatingStatus) { this.riskRatingStatus = riskRatingStatus; }
    public BigDecimal getDbrPercent() { return dbrPercent; }
    public void setDbrPercent(BigDecimal dbrPercent) { this.dbrPercent = dbrPercent; }
    public Boolean getDbrPassed() { return dbrPassed; }
    public void setDbrPassed(Boolean dbrPassed) { this.dbrPassed = dbrPassed; }
    public String getDbrNote() { return dbrNote; }
    public void setDbrNote(String dbrNote) { this.dbrNote = dbrNote; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
