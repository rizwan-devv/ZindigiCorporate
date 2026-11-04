package com.dfs.corporate.financing;

import com.dfs.corporate.financing.domain.*;
import com.dfs.corporate.financing.repository.*;
import com.dfs.corporate.integration.dfs.CorporatePortalAppClient;
import com.dfs.corporate.integration.dfs.CorporatePortalTxnClient;
import com.dfs.corporate.security.AccountPrincipal;
import com.dfs.corporate.service.FileStorageService;
import com.dfs.corporate.service.OtpService;
import com.dfs.corporate.util.IdentityFormats;
import com.dfs.corporate.web.dto.DfsPortalTxnResponse;
import com.dfs.corporate.web.error.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class FinancingApplicationService {

    private static final Logger log = LoggerFactory.getLogger(FinancingApplicationService.class);
    private static final String OTP_PURPOSE = "FINANCING";
    private static final Set<String> FILE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "application/pdf");
    private static final long MAX_FILE = 8L * 1024 * 1024;
    private static final Set<LoanApplicationStatus> LOCKED = EnumSet.of(
            LoanApplicationStatus.SUBMITTED,
            LoanApplicationStatus.WITH_CHECKER,
            LoanApplicationStatus.APPROVED,
            LoanApplicationStatus.DOWN_PAYMENT_SELECTED);

    private final FinancingCatalogService catalog;
    private final FinancingCalculator calculator;
    private final FinancingCustomerRepository customerRepository;
    private final FinancingWhitelistRepository whitelistRepository;
    private final LoanApplicationRepository applicationRepository;
    private final LoanEventRepository eventRepository;
    private final LoanDocumentRepository documentRepository;
    private final LoanDocumentRuleRepository ruleRepository;
    private final FinancingNotificationRepository notificationRepository;
    private final OtpService otpService;
    private final FileStorageService fileStorageService;
    private final CorporatePortalTxnClient txnClient;
    private final CorporatePortalAppClient appClient;
    private final ObjectMapper objectMapper;

    public FinancingApplicationService(FinancingCatalogService catalog,
                                       FinancingCalculator calculator,
                                       FinancingCustomerRepository customerRepository,
                                       FinancingWhitelistRepository whitelistRepository,
                                       LoanApplicationRepository applicationRepository,
                                       LoanEventRepository eventRepository,
                                       LoanDocumentRepository documentRepository,
                                       LoanDocumentRuleRepository ruleRepository,
                                       FinancingNotificationRepository notificationRepository,
                                       OtpService otpService,
                                       FileStorageService fileStorageService,
                                       CorporatePortalTxnClient txnClient,
                                       CorporatePortalAppClient appClient,
                                       ObjectMapper objectMapper) {
        this.catalog = catalog;
        this.calculator = calculator;
        this.customerRepository = customerRepository;
        this.whitelistRepository = whitelistRepository;
        this.applicationRepository = applicationRepository;
        this.eventRepository = eventRepository;
        this.documentRepository = documentRepository;
        this.ruleRepository = ruleRepository;
        this.notificationRepository = notificationRepository;
        this.otpService = otpService;
        this.fileStorageService = fileStorageService;
        this.txnClient = txnClient;
        this.appClient = appClient;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Map<String, Object> identify(String fullName, String cnicRaw, String mobileRaw, String walletRaw) {
        catalog.requireProductOpen();
        String name = cleanName(fullName);
        String cnic = FinancingNumbers.cnic(cnicRaw);
        String mobile = FinancingNumbers.mobile(mobileRaw);
        String wallet = walletRaw == null || walletRaw.isBlank() ? null : customerWallet(cnic, walletRaw);
        FinancingWhitelist allowed = whitelistRepository.findByCnic(cnic)
                .filter(FinancingWhitelist::isActive)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN,
                        "This CNIC is not whitelisted for electric bike financing"));
        if (!mobile.equals(allowed.getMobile())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "CNIC and mobile do not match the whitelist");
        }
        FinancingCustomer byMobile = customerRepository.findByMobile(mobile).orElse(null);
        if (byMobile != null && !cnic.equals(byMobile.getCnic())) {
            throw new ApiException(HttpStatus.CONFLICT, "This mobile number is already used with another CNIC");
        }
        FinancingCustomer customer = customerRepository.findByCnic(cnic).orElseGet(FinancingCustomer::new);
        customer.setCnic(cnic);
        customer.setMobile(mobile);
        customer.setFullName(name);
        if (wallet != null) {
            customer.setDfsWallet(wallet);
        }
        customer.setUpdatedAt(Instant.now());
        customerRepository.save(customer);
        String code = otpService.issueMobileOnScreen(mobile, OTP_PURPOSE);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("otpSent", true);
        out.put("mobile", mobile);
        out.put("otp", code);
        return out;
    }

    @Transactional
    public Map<String, Object> verifyOtp(String cnicRaw, String mobileRaw, String code) {
        String cnic = FinancingNumbers.cnic(cnicRaw);
        String mobile = FinancingNumbers.mobile(mobileRaw);
        otpService.verifyMobile(mobile, OTP_PURPOSE, code);
        FinancingCustomer customer = customerRepository.findByCnic(cnic)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Start again with your CNIC and mobile"));
        if (!mobile.equals(customer.getMobile())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Mobile number does not match this CNIC");
        }
        customer.setSessionToken(UUID.randomUUID().toString().replace("-", ""));
        customer.setSessionExpiresAt(Instant.now().plus(12, ChronoUnit.HOURS));
        customer.setOtpVerifiedAt(Instant.now());
        customer.setUpdatedAt(Instant.now());
        customerRepository.save(customer);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("sessionToken", customer.getSessionToken());
        out.put("fullName", customer.getFullName());
        out.put("cnic", customer.getCnic());
        out.put("mobile", customer.getMobile());
        return out;
    }

    public List<Map<String, Object>> quote(long productId, BigDecimal downPaymentPercent, Integer selectedTenure) {
        catalog.requireProductOpen();
        FinancingSettings settings = catalog.requireSettings();
        FinancingProduct product = catalog.requireSellable(productId);
        assertDownPayment(settings, downPaymentPercent);
        BigDecimal kibor = calculator.effectiveKibor(settings);
        List<Integer> tenures = FinancingNumbers.months(settings.getTenureMonths());
        if (selectedTenure != null && !tenures.contains(selectedTenure)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "That tenure is not offered");
        }
        return tenures.stream()
                .filter(months -> selectedTenure == null || selectedTenure.equals(months))
                .map(months -> {
                    FinancingCalculator.Quote quote = calculator.quote(settings, product.getPrice(), downPaymentPercent, months, kibor);
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("tenureMonths", months);
                    row.put("productPrice", product.getPrice());
                    row.put("downPaymentPercent", downPaymentPercent);
                    row.put("downPaymentAmount", quote.downPaymentAmount());
                    row.put("financedAmount", quote.financedAmount());
                    row.put("annualRatePercent", quote.annualRatePercent());
                    row.put("rateBasis", "KIBOR");
                    row.put("monthlyInstallment", quote.monthlyInstallment());
                    row.put("totalPayable", quote.totalPayable());
                    return row;
                }).toList();
    }

    @Transactional
    public Map<String, Object> saveDraft(String sessionToken, Map<String, Object> body) {
        FinancingCustomer customer = requireSession(sessionToken);
        FinancingSettings settings = catalog.requireSettings();
        catalog.requireProductOpen();
        long productId = Long.parseLong(text(body, "productId"));
        FinancingProduct product = catalog.requireSellable(productId);
        EmploymentType employment = employment(text(body, "employmentType"));
        BigDecimal income = money(text(body, "monthlyIncome"), "monthlyIncome");
        if (income.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Monthly income must be greater than zero");
        }
        BigDecimal existing = body.get("existingInstallments") == null || String.valueOf(body.get("existingInstallments")).isBlank()
                ? BigDecimal.ZERO
                : money(String.valueOf(body.get("existingInstallments")), "existingInstallments");
        if (existing.compareTo(BigDecimal.ZERO) < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Existing installments cannot be negative");
        }
        BigDecimal card = body.get("cardLimitAmount") == null || String.valueOf(body.get("cardLimitAmount")).isBlank()
                ? null
                : money(String.valueOf(body.get("cardLimitAmount")), "cardLimitAmount");
        BigDecimal down = new BigDecimal(text(body, "downPaymentPercent"));
        assertDownPayment(settings, down);
        int tenure = Integer.parseInt(text(body, "tenureMonths"));
        if (!FinancingNumbers.months(settings.getTenureMonths()).contains(tenure)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "That tenure is not offered");
        }
        LocalDate quotationDate = LocalDate.parse(text(body, "quotationDate"));
        assertQuotationAge(settings, quotationDate);

        LoanApplication app = openEditable(customer.getId());
        boolean created = app.getId() == null;
        if (created) {
            app.setPublicId(UUID.randomUUID().toString());
            app.setReferenceNo(newReference());
            app.setCustomerId(customer.getId());
            app.setStatus(LoanApplicationStatus.DRAFT);
            app.setPaymentStatus("UNPAID");
            app.setEcibStatus("NOT_INTEGRATED");
            app.setRiskRatingStatus("NOT_CONFIGURED");
        }
        app.setMerchantPartyId(product.getMerchantPartyId());
        app.setProductId(product.getId());
        app.setCategoryId(product.getCategoryId());
        app.setProductTitle(product.getTitle());
        app.setProductPrice(product.getPrice());
        app.setEmploymentType(employment);
        app.setMonthlyIncome(income);
        app.setExistingInstallments(existing);
        app.setCardLimitAmount(card);
        app.setRequestedDownPaymentPercent(FinancingNumbers.percent(down));
        app.setTenureMonths(tenure);
        app.setQuotationDate(quotationDate);
        app.setUpdatedAt(Instant.now());
        applicationRepository.save(app);
        if (created) {
            event(app, "CREATED", null, customerLabel(customer), "Application started");
        }
        return customerView(app, customer);
    }

    @Transactional
    public Map<String, Object> uploadDocument(String sessionToken, String reference, String documentCode, MultipartFile file) {
        FinancingCustomer customer = requireSession(sessionToken);
        LoanApplication app = ownEditable(customer, reference);
        String code = documentCode == null ? "" : documentCode.trim().toUpperCase(Locale.ROOT);
        boolean known = ruleRepository.findByApplicabilityInOrderBySortOrderAsc(
                        List.of("ALL", app.getEmploymentType().name())).stream()
                .anyMatch(rule -> rule.getDocumentCode().equals(code));
        if (!known) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "That document is not required for this application");
        }
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "File is required");
        }
        if (file.getSize() > MAX_FILE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Each document must be 8 MB or smaller");
        }
        String type = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!FILE_TYPES.contains(type)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Upload a PDF or an image");
        }
        LoanDocument doc = documentRepository.findByApplicationIdAndDocumentCode(app.getId(), code)
                .orElseGet(LoanDocument::new);
        doc.setApplicationId(app.getId());
        doc.setDocumentCode(code);
        doc.setOriginalName(file.getOriginalFilename() == null ? code : file.getOriginalFilename());
        doc.setContentType(type);
        doc.setStoredPath(fileStorageService.storeInFolder("financing-docs", code, file));
        doc.setUploadedAt(Instant.now());
        documentRepository.save(doc);
        event(app, "DOCUMENT", null, customerLabel(customer), code);
        return customerView(app, customer);
    }

    @Transactional
    public Map<String, Object> biometric(String sessionToken, String reference, String sourceRaw) {
        FinancingCustomer customer = requireSession(sessionToken);
        LoanApplication app = ownEditable(customer, reference);
        String source = sourceRaw == null ? "" : sourceRaw.trim().toUpperCase(Locale.ROOT);
        if (!source.equals("CONSUMER_APP")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Verification must be completed in the consumer app");
        }
        String ref = "CONS-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        app.setBiometricRef(ref);
        app.setBiometricSource(source);
        app.setBiometricAt(Instant.now());
        app.setUpdatedAt(Instant.now());
        applicationRepository.save(app);
        event(app, "BIOMETRIC", null, customerLabel(customer), source + " " + ref);
        return customerView(app, customer);
    }

    @Transactional
    public Map<String, Object> submit(String sessionToken, String reference) {
        FinancingCustomer customer = requireSession(sessionToken);
        if (customer.getOtpVerifiedAt() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Verify the mobile OTP before submitting");
        }
        LoanApplication app = ownEditable(customer, reference);
        if (app.getBiometricRef() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Complete biometric verification before submitting");
        }
        List<LoanDocumentRule> rules = ruleRepository.findByApplicabilityInOrderBySortOrderAsc(
                List.of("ALL", app.getEmploymentType().name()));
        List<LoanDocument> docs = documentRepository.findByApplicationIdOrderByUploadedAtAsc(app.getId());
        for (LoanDocumentRule rule : rules) {
            if (!rule.isMandatory()) continue;
            boolean present = docs.stream().anyMatch(doc -> doc.getDocumentCode().equals(rule.getDocumentCode()));
            if (!present) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Upload " + rule.getDocumentLabel());
            }
        }
        FinancingSettings settings = catalog.requireSettings();
        assertQuotationAge(settings, app.getQuotationDate());
        BigDecimal kibor = calculator.effectiveKibor(settings);
        FinancingCalculator.Quote indicative = calculator.quote(
                settings, app.getProductPrice(), app.getRequestedDownPaymentPercent(), app.getTenureMonths(), kibor);
        Dbr dbr = dbr(settings, app.getMonthlyIncome(), app.getExistingInstallments(), app.getCardLimitAmount(),
                indicative.monthlyInstallment());
        app.setIndicativeKiborPercent(kibor);
        app.setIndicativeMonthlyInstallment(indicative.monthlyInstallment());
        app.setIndicativeFinancedAmount(indicative.financedAmount());
        app.setEcibStatus("NOT_INTEGRATED");
        app.setRiskRatingStatus("NOT_CONFIGURED");
        app.setDbrPercent(dbr.percent());
        app.setDbrPassed(dbr.passed());
        app.setDbrNote(dbr.note());
        app.setForwardedByAccountId(null);
        app.setForwardedAt(null);
        app.setBankCommissionPercent(FinancingNumbers.percent(settings.getSuggestedBankCommissionPercent()));
        app.setCommissionSetAt(Instant.now());
        app.setUpdatedAt(Instant.now());
        if (dbr.passed()) {
            applyApprovedFigures(settings, app, app.getRequestedDownPaymentPercent());
        }
        if (!dbr.passed()) {
            app.setStatus(LoanApplicationStatus.ELIGIBILITY_FAILED);
            applicationRepository.save(app);
            event(app, "ELIGIBILITY_FAILED", null, customerLabel(customer), dbr.note());
            return customerView(app, customer);
        }
        app.setStatus(LoanApplicationStatus.SUBMITTED);
        applicationRepository.save(app);
        event(app, "SUBMITTED", null, customerLabel(customer), "Submitted for operations review");
        notify(customer, app, "SUBMITTED", "Application submitted",
                "Your financing application " + app.getReferenceNo() + " for " + app.getProductTitle() + " has been submitted.");
        return customerView(app, customer);
    }

    public List<Map<String, Object>> myApplications(String sessionToken) {
        FinancingCustomer customer = requireSession(sessionToken);
        return applicationRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId()).stream()
                .map(app -> customerView(app, customer))
                .toList();
    }

    public List<Map<String, Object>> myNotifications(String sessionToken) {
        FinancingCustomer customer = requireSession(sessionToken);
        return notificationRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId()).stream()
                .map(note -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", note.getId());
                    row.put("statusCode", note.getStatusCode());
                    row.put("title", note.getTitle());
                    row.put("body", note.getBody());
                    row.put("createdAt", note.getCreatedAt());
                    return row;
                }).toList();
    }

    @Transactional
    public Map<String, Object> confirmDownPayment(String sessionToken, String reference, BigDecimal percent) {
        FinancingCustomer customer = requireSession(sessionToken);
        LoanApplication app = own(customer, reference);
        if (app.getStatus() != LoanApplicationStatus.APPROVED) {
            throw new ApiException(HttpStatus.CONFLICT, "Down payment can be confirmed after approval");
        }
        FinancingSettings settings = catalog.requireSettings();
        assertDownPayment(settings, percent);
        if (app.getBankCommissionPercent() == null || app.getIndicativeKiborPercent() == null) {
            throw new ApiException(HttpStatus.CONFLICT, "Approved financing terms are incomplete");
        }
        FinancingCalculator.Quote quote = calculator.quote(
                settings, app.getProductPrice(), percent, app.getTenureMonths(), app.getApprovedAnnualRate());
        Dbr dbr = dbr(settings, app.getMonthlyIncome(), app.getExistingInstallments(), app.getCardLimitAmount(),
                quote.monthlyInstallment());
        if (!dbr.passed()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "This down payment takes the debt burden ratio above " + settings.getDbrThresholdPercent()
                            + "%. Choose a higher down payment.");
        }
        app.setConfirmedDownPaymentPercent(FinancingNumbers.percent(percent));
        app.setApprovedDownPaymentAmount(quote.downPaymentAmount());
        app.setApprovedFinancedAmount(quote.financedAmount());
        app.setApprovedMonthlyInstallment(quote.monthlyInstallment());
        app.setApprovedTotalPayable(quote.totalPayable());
        app.setDbrPercent(dbr.percent());
        app.setDbrPassed(true);
        app.setDbrNote(dbr.note());
        app.setStatus(LoanApplicationStatus.DOWN_PAYMENT_SELECTED);
        app.setPaymentStatus("UNPAID");
        app.setUpdatedAt(Instant.now());
        applicationRepository.save(app);
        event(app, "DOWN_PAYMENT_SELECTED", null, customerLabel(customer),
                percent.toPlainString() + "% selected. Payment is not collected in this step.");
        return customerView(app, customer);
    }

    /**
     * Sends only the approved down payment from the customer's existing DFS wallet
     * to the bank collection wallet. The financed amount is not transferred.
     * The MPIN is passed to DFS and is not stored.
     */
    @Transactional
    public Map<String, Object> payDownPayment(String sessionToken, String reference, String mpinRaw) {
        FinancingCustomer customer = requireSession(sessionToken);
        LoanApplication app = own(customer, reference);
        if ("PAID".equals(app.getPaymentStatus())) {
            return customerView(app, customer);
        }
        if (app.getStatus() != LoanApplicationStatus.DOWN_PAYMENT_SELECTED) {
            throw new ApiException(HttpStatus.CONFLICT, "Confirm the down payment plan before paying");
        }
        if (customer.getDfsWallet() == null || customer.getDfsWallet().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Add your DFS wallet before paying");
        }
        FinancingSettings settings = catalog.requireSettings();
        if (settings.getCollectionWallet() == null || settings.getCollectionWallet().isBlank()) {
            throw new ApiException(HttpStatus.CONFLICT, "The bank collection wallet is not set");
        }
        BigDecimal amount = app.getApprovedDownPaymentAmount();
        if (amount == null || amount.signum() <= 0) {
            throw new ApiException(HttpStatus.CONFLICT, "This application has no down payment to collect");
        }
        String mpin = IdentityFormats.pinPlain(mpinRaw);
        if (mpin == null || mpin.length() < 4) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Enter the DFS wallet MPIN");
        }
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("mobileNumber", localMobile(customer.getDfsWallet()));
        payload.put("nidNo", customer.getCnic());
        payload.put("accountNo", localMobile(settings.getCollectionWallet()));
        payload.put("accountType", "W");
        payload.put("amount", amount.stripTrailingZeros().toPlainString());
        payload.put("mpin", mpin);
        payload.put("transPurposeId", "1");
        payload.put("narration", "EV bike down payment " + app.getReferenceNo());
        JsonNode root;
        try {
            root = txnClient.fundsTransferLocal(payload);
        } catch (IllegalStateException ex) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, ex.getMessage());
        }
        DfsPortalTxnResponse response = DfsPortalTxnResponse.from(root);
        String code = response.getResponsecode();
        if (!"000".equals(code) && !"00".equals(code)) {
            String message = response.getMessages();
            event(app, "DOWN_PAYMENT_FAILED", null, customerLabel(customer),
                    message == null ? "DFS declined the down payment" : message);
            throw new ApiException(HttpStatus.BAD_GATEWAY,
                    message == null || message.isBlank() ? "The bank did not accept the down payment" : message);
        }
        app.setPaymentStatus("PAID");
        app.setPaymentAmount(amount);
        app.setPaymentFromWallet(customer.getDfsWallet());
        app.setPaymentToWallet(settings.getCollectionWallet());
        app.setPaymentReference(receiptReference(root, app.getReferenceNo()));
        app.setPaidAt(Instant.now());
        app.setUpdatedAt(Instant.now());
        applicationRepository.save(app);
        event(app, "DOWN_PAYMENT_PAID", null, customerLabel(customer),
                "Down payment " + amount.toPlainString() + " received. Receipt " + app.getPaymentReference());
        notify(customer, app, "DOWN_PAYMENT_PAID", "Down payment received",
                "Application " + app.getReferenceNo() + " receipt " + app.getPaymentReference()
                        + " for " + amount.toPlainString() + ".");
        return customerView(app, customer);
    }

    public List<Map<String, Object>> merchantApplications(AccountPrincipal principal) {
        var party = catalog.requireMerchant(principal);
        return applicationRepository.findByMerchantPartyIdOrderByCreatedAtDesc(party.getId()).stream()
                .map(this::merchantView)
                .toList();
    }

    public List<Map<String, Object>> deskList(AccountPrincipal principal) {
        if (!catalog.canViewAll(principal)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Financing desk access required");
        }
        return applicationRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(app -> deskView(app, false))
                .toList();
    }

    public Map<String, Object> deskGet(AccountPrincipal principal, String reference) {
        if (!catalog.canViewAll(principal)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Financing desk access required");
        }
        return deskView(byRef(reference), true);
    }

    @Transactional
    public Map<String, Object> setCommission(AccountPrincipal principal, String reference, BigDecimal percent, String remarks) {
        catalog.requireOps(principal);
        LoanApplication app = byRef(reference);
        if (app.getStatus() != LoanApplicationStatus.SUBMITTED) {
            throw new ApiException(HttpStatus.CONFLICT, "Bank commission can be set while the application is with operations");
        }
        FinancingSettings settings = catalog.requireSettings();
        if (percent == null || percent.compareTo(BigDecimal.ZERO) < 0
                || percent.compareTo(settings.getBankCommissionMaxPercent()) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Bank commission must be between 0 and " + settings.getBankCommissionMaxPercent());
        }
        app.setBankCommissionPercent(FinancingNumbers.percent(percent));
        app.setCommissionSetByAccountId(principal.getAccountId());
        app.setCommissionSetAt(Instant.now());
        if (remarks != null && !remarks.isBlank()) {
            app.setOpsRemarks(remarks.trim());
        }
        applyApprovedFigures(settings, app, app.getRequestedDownPaymentPercent());
        app.setUpdatedAt(Instant.now());
        applicationRepository.save(app);
        event(app, "COMMISSION_SET", principal.getAccountId(), principal.getUsername(),
                "KIBOR " + app.getIndicativeKiborPercent() + " + bank commission " + app.getBankCommissionPercent());
        return deskView(app, true);
    }

    @Transactional
    public Map<String, Object> forward(AccountPrincipal principal, String reference, String remarks) {
        catalog.requireOps(principal);
        LoanApplication app = byRef(reference);
        if (app.getStatus() != LoanApplicationStatus.SUBMITTED) {
            throw new ApiException(HttpStatus.CONFLICT, "Only a submitted application can be forwarded");
        }
        FinancingSettings settings = catalog.requireSettings();
        if (app.getBankCommissionPercent() == null && app.getIndicativeKiborPercent() != null) {
            app.setBankCommissionPercent(FinancingNumbers.percent(settings.getSuggestedBankCommissionPercent()));
            app.setCommissionSetAt(Instant.now());
            applyApprovedFigures(settings, app, app.getRequestedDownPaymentPercent());
        }
        if (remarks != null && !remarks.isBlank()) {
            app.setOpsRemarks(remarks.trim());
        }
        app.setForwardedByAccountId(principal.getAccountId());
        app.setForwardedAt(Instant.now());
        app.setStatus(LoanApplicationStatus.WITH_CHECKER);
        app.setUpdatedAt(Instant.now());
        applicationRepository.save(app);
        event(app, "FORWARDED", principal.getAccountId(), principal.getUsername(), app.getOpsRemarks());
        return deskView(app, true);
    }

    @Transactional
    public Map<String, Object> opsDecision(AccountPrincipal principal, String reference, String decision, String remarks) {
        catalog.requireOps(principal);
        if (remarks == null || remarks.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Remarks are required");
        }
        LoanApplication app = byRef(reference);
        if (app.getStatus() != LoanApplicationStatus.SUBMITTED && app.getStatus() != LoanApplicationStatus.WITH_CHECKER) {
            throw new ApiException(HttpStatus.CONFLICT, "This application cannot be sent back or rejected by operations");
        }
        FinancingCustomer customer = customerRepository.findById(app.getCustomerId()).orElseThrow();
        app.setOpsRemarks(remarks.trim());
        app.setUpdatedAt(Instant.now());
        if ("DISCREPANT".equalsIgnoreCase(decision)) {
            app.setStatus(LoanApplicationStatus.DISCREPANT);
            app.setForwardedByAccountId(null);
            app.setForwardedAt(null);
            applicationRepository.save(app);
            event(app, "DISCREPANT", principal.getAccountId(), principal.getUsername(), remarks.trim());
            notify(customer, app, "DISCREPANT", "Application needs correction",
                    "Application " + app.getReferenceNo() + " needs correction: " + remarks.trim());
        } else if ("REJECT".equalsIgnoreCase(decision)) {
            app.setStatus(LoanApplicationStatus.REJECTED);
            applicationRepository.save(app);
            event(app, "REJECTED", principal.getAccountId(), principal.getUsername(), remarks.trim());
            notify(customer, app, "REJECTED", "Application rejected",
                    "Application " + app.getReferenceNo() + " was rejected. " + remarks.trim());
        } else {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Decision must be DISCREPANT or REJECT");
        }
        return deskView(app, true);
    }

    @Transactional
    public Map<String, Object> checkerDecision(AccountPrincipal principal, String reference, String decision, String remarks) {
        catalog.requireChecker(principal);
        LoanApplication app = byRef(reference);
        if (app.getStatus() != LoanApplicationStatus.WITH_CHECKER) {
            throw new ApiException(HttpStatus.CONFLICT, "This application is not waiting for the checker");
        }
        if (principal.getAccountId().equals(app.getForwardedByAccountId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "The operations user who forwarded this case cannot approve it");
        }
        FinancingCustomer customer = customerRepository.findById(app.getCustomerId()).orElseThrow();
        if ("APPROVE".equalsIgnoreCase(decision)) {
            app.setCheckerRemarks(blank(remarks));
            app.setStatus(LoanApplicationStatus.APPROVED);
            app.setUpdatedAt(Instant.now());
            applicationRepository.save(app);
            event(app, "APPROVED", principal.getAccountId(), principal.getUsername(), app.getCheckerRemarks());
            notify(customer, app, "APPROVED", "Financing approved",
                    "Application " + app.getReferenceNo() + " for " + app.getProductTitle()
                            + " is approved. Open the app and confirm your down payment. Payment is a separate step.");
        } else if ("REJECT".equalsIgnoreCase(decision)) {
            if (remarks == null || remarks.isBlank()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Remarks are required to reject");
            }
            app.setCheckerRemarks(remarks.trim());
            app.setStatus(LoanApplicationStatus.REJECTED);
            app.setUpdatedAt(Instant.now());
            applicationRepository.save(app);
            event(app, "REJECTED", principal.getAccountId(), principal.getUsername(), remarks.trim());
            notify(customer, app, "REJECTED", "Application rejected",
                    "Application " + app.getReferenceNo() + " was rejected. " + remarks.trim());
        } else {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Decision must be APPROVE or REJECT");
        }
        return deskView(app, true);
    }

    public Map<String, Object> documentFile(AccountPrincipal principal, Long documentId) {
        if (!catalog.canViewAll(principal)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Financing desk access required");
        }
        LoanDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Document not found"));
        Resource resource = new FileSystemResource(fileStorageService.resolve(doc.getStoredPath()));
        Map<String, Object> file = new LinkedHashMap<>();
        file.put("resource", resource);
        file.put("contentType", doc.getContentType() == null ? "application/octet-stream" : doc.getContentType());
        file.put("filename", doc.getOriginalName());
        return file;
    }

    private void applyApprovedFigures(FinancingSettings settings, LoanApplication app, BigDecimal downPercent) {
        BigDecimal rate = app.getIndicativeKiborPercent().add(app.getBankCommissionPercent());
        FinancingCalculator.Quote quote = calculator.quote(settings, app.getProductPrice(), downPercent, app.getTenureMonths(), rate);
        app.setApprovedAnnualRate(FinancingNumbers.percent(rate));
        app.setApprovedDownPaymentAmount(quote.downPaymentAmount());
        app.setApprovedFinancedAmount(quote.financedAmount());
        app.setApprovedMonthlyInstallment(quote.monthlyInstallment());
        app.setApprovedTotalPayable(quote.totalPayable());
    }

    private Dbr dbr(FinancingSettings settings, BigDecimal income, BigDecimal existing, BigDecimal cardLimit, BigDecimal proposed) {
        BigDecimal obligations = existing.add(proposed);
        String basis = settings.getCardLimitBasis();
        String note;
        if ("UNSET".equals(basis)) {
            note = "DBR uses declared existing installments plus this installment. The 5% card-limit component is not applied because utilized versus available is still unset. eCIB is not connected.";
        } else if (cardLimit == null) {
            note = "Card limit was not declared, so the configured card-limit component was not added. eCIB is not connected.";
        } else {
            BigDecimal extra = cardLimit.multiply(settings.getCardLimitFactorPercent())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            obligations = obligations.add(extra);
            note = "Includes " + settings.getCardLimitFactorPercent().stripTrailingZeros().toPlainString()
                    + "% of the declared card limit (" + basis + "). This is not an eCIB figure. eCIB is not connected.";
        }
        BigDecimal percent = obligations.multiply(new BigDecimal("100"))
                .divide(income, 4, RoundingMode.HALF_UP);
        boolean passed = percent.compareTo(settings.getDbrThresholdPercent()) <= 0;
        if (!passed) {
            note = note + " DBR " + percent + "% exceeds " + settings.getDbrThresholdPercent() + "%.";
        }
        return new Dbr(percent, passed, trim(note, 500));
    }

    private Map<String, Object> customerView(LoanApplication app, FinancingCustomer customer) {
        Map<String, Object> row = base(app);
        row.put("fullName", customer.getFullName());
        row.put("cnic", customer.getCnic());
        row.put("mobile", customer.getMobile());
        row.put("otpVerified", customer.getOtpVerifiedAt() != null);
        boolean finalTerms = app.getStatus() == LoanApplicationStatus.APPROVED
                || app.getStatus() == LoanApplicationStatus.DOWN_PAYMENT_SELECTED;
        if (!finalTerms) {
            row.remove("approvedAnnualRate");
            row.remove("approvedMonthlyInstallment");
            row.remove("approvedDownPaymentAmount");
            row.remove("approvedFinancedAmount");
            row.remove("approvedTotalPayable");
        }
        row.put("documents", documentRepository.findByApplicationIdOrderByUploadedAtAsc(app.getId()).stream()
                .map(doc -> Map.of("id", doc.getId(), "documentCode", doc.getDocumentCode(), "originalName", doc.getOriginalName()))
                .toList());
        row.put("rateNote", finalTerms
                ? "Approved financing rate. Bank commission is included in this rate and cannot be changed here."
                : "Indicative rate is KIBOR only.");
        return row;
    }

    private Map<String, Object> merchantView(LoanApplication app) {
        FinancingCustomer customer = customerRepository.findById(app.getCustomerId()).orElseThrow();
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("referenceNo", app.getReferenceNo());
        row.put("customerName", customer.getFullName());
        row.put("cnicMasked", FinancingNumbers.maskCnic(customer.getCnic()));
        row.put("productTitle", app.getProductTitle());
        row.put("productPrice", app.getProductPrice());
        row.put("tenureMonths", app.getTenureMonths());
        row.put("status", app.getStatus().name());
        row.put("paymentStatus", app.getPaymentStatus());
        row.put("paymentReference", app.getPaymentReference());
        row.put("paymentAmount", app.getPaymentAmount());
        row.put("paidAt", app.getPaidAt());
        row.put("createdAt", app.getCreatedAt());
        return row;
    }

    private Map<String, Object> deskView(LoanApplication app, boolean detail) {
        FinancingCustomer customer = customerRepository.findById(app.getCustomerId()).orElseThrow();
        Map<String, Object> row = base(app);
        row.put("fullName", customer.getFullName());
        row.put("cnic", customer.getCnic());
        row.put("mobile", customer.getMobile());
        row.put("merchantName", catalog.merchantName(app.getMerchantPartyId()));
        row.put("otpVerifiedAt", customer.getOtpVerifiedAt());
        row.put("bankCommissionPercent", app.getBankCommissionPercent());
        row.put("commissionSetByAccountId", app.getCommissionSetByAccountId());
        row.put("commissionSetAt", app.getCommissionSetAt());
        row.put("forwardedByAccountId", app.getForwardedByAccountId());
        row.put("forwardedAt", app.getForwardedAt());
        row.put("opsRemarks", app.getOpsRemarks());
        row.put("checkerRemarks", app.getCheckerRemarks());
        if (detail) {
            row.put("documents", documentRepository.findByApplicationIdOrderByUploadedAtAsc(app.getId()).stream()
                    .map(doc -> {
                        Map<String, Object> file = new LinkedHashMap<>();
                        file.put("id", doc.getId());
                        file.put("documentCode", doc.getDocumentCode());
                        file.put("originalName", doc.getOriginalName());
                        file.put("uploadedAt", doc.getUploadedAt());
                        return file;
                    }).toList());
            row.put("events", eventRepository.findByApplicationIdOrderByCreatedAtAsc(app.getId()).stream()
                    .map(event -> {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("eventType", event.getEventType());
                        item.put("actorLabel", event.getActorLabel());
                        item.put("remark", event.getRemark());
                        item.put("createdAt", event.getCreatedAt());
                        return item;
                    }).toList());
        }
        return row;
    }

    private Map<String, Object> base(LoanApplication app) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("referenceNo", app.getReferenceNo());
        row.put("status", app.getStatus().name());
        row.put("paymentStatus", app.getPaymentStatus());
        row.put("paymentReference", app.getPaymentReference());
        row.put("paymentAmount", app.getPaymentAmount());
        row.put("paidAt", app.getPaidAt());
        row.put("paymentFromWallet", maskWallet(app.getPaymentFromWallet()));
        row.put("paymentToWallet", maskWallet(app.getPaymentToWallet()));
        row.put("employmentType", app.getEmploymentType().name());
        row.put("productId", app.getProductId());
        row.put("productTitle", app.getProductTitle());
        row.put("productPrice", app.getProductPrice());
        row.put("merchantPartyId", app.getMerchantPartyId());
        row.put("categoryId", app.getCategoryId());
        row.put("monthlyIncome", app.getMonthlyIncome());
        row.put("existingInstallments", app.getExistingInstallments());
        row.put("cardLimitAmount", app.getCardLimitAmount());
        row.put("requestedDownPaymentPercent", app.getRequestedDownPaymentPercent());
        row.put("confirmedDownPaymentPercent", app.getConfirmedDownPaymentPercent());
        row.put("tenureMonths", app.getTenureMonths());
        row.put("quotationDate", app.getQuotationDate());
        row.put("indicativeKiborPercent", app.getIndicativeKiborPercent());
        row.put("indicativeMonthlyInstallment", app.getIndicativeMonthlyInstallment());
        row.put("indicativeFinancedAmount", app.getIndicativeFinancedAmount());
        row.put("approvedAnnualRate", app.getApprovedAnnualRate());
        row.put("approvedMonthlyInstallment", app.getApprovedMonthlyInstallment());
        row.put("approvedDownPaymentAmount", app.getApprovedDownPaymentAmount());
        row.put("approvedFinancedAmount", app.getApprovedFinancedAmount());
        row.put("approvedTotalPayable", app.getApprovedTotalPayable());
        row.put("biometricRef", app.getBiometricRef());
        row.put("biometricSource", app.getBiometricSource());
        row.put("biometricAt", app.getBiometricAt());
        row.put("ecibStatus", app.getEcibStatus());
        row.put("riskRatingStatus", app.getRiskRatingStatus());
        row.put("dbrPercent", app.getDbrPercent());
        row.put("dbrPassed", app.getDbrPassed());
        row.put("dbrNote", app.getDbrNote());
        row.put("createdAt", app.getCreatedAt());
        row.put("updatedAt", app.getUpdatedAt());
        return row;
    }

    private LoanApplication openEditable(Long customerId) {
        List<LoanApplication> rows = applicationRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
        for (LoanApplication app : rows) {
            if (app.getStatus() == LoanApplicationStatus.REJECTED) continue;
            if (LOCKED.contains(app.getStatus())) {
                throw new ApiException(HttpStatus.CONFLICT,
                        "You already have application " + app.getReferenceNo() + " (" + app.getStatus() + ")");
            }
            return app;
        }
        return new LoanApplication();
    }

    private LoanApplication ownEditable(FinancingCustomer customer, String reference) {
        LoanApplication app = own(customer, reference);
        if (LOCKED.contains(app.getStatus()) || app.getStatus() == LoanApplicationStatus.REJECTED) {
            throw new ApiException(HttpStatus.CONFLICT, "This application can no longer be edited");
        }
        return app;
    }

    private LoanApplication own(FinancingCustomer customer, String reference) {
        LoanApplication app = byRef(reference);
        if (!customer.getId().equals(app.getCustomerId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This application belongs to another customer");
        }
        return app;
    }

    private LoanApplication byRef(String reference) {
        return applicationRepository.findByReferenceNo(reference == null ? "" : reference.trim())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Application not found"));
    }

    private FinancingCustomer requireSession(String token) {
        if (token == null || token.isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Financing session required");
        }
        FinancingCustomer customer = customerRepository.findBySessionToken(token.trim())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Financing session is not valid"));
        if (customer.getSessionExpiresAt() == null || customer.getSessionExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Financing session expired. Verify OTP again.");
        }
        return customer;
    }

    private void assertDownPayment(FinancingSettings settings, BigDecimal percent) {
        if (percent == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Down payment percent is required");
        }
        boolean allowed = FinancingNumbers.percents(settings.getDownPaymentOptions()).stream()
                .anyMatch(option -> FinancingNumbers.samePercent(option, percent));
        if (!allowed || percent.compareTo(settings.getDownPaymentMinPercent()) < 0
                || percent.compareTo(settings.getDownPaymentMaxPercent()) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Down payment must be one of " + settings.getDownPaymentOptions());
        }
    }

    private void assertQuotationAge(FinancingSettings settings, LocalDate quotationDate) {
        LocalDate today = LocalDate.now();
        if (quotationDate.isAfter(today) || quotationDate.isBefore(today.minusDays(settings.getQuotationMaxAgeDays()))) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Quotation date must be within the last " + settings.getQuotationMaxAgeDays() + " days");
        }
    }

    private String customerWallet(String cnic, String raw) {
        String stored = FinancingNumbers.mobile(raw);
        if (!appClient.isEnabled()) {
            return stored;
        }
        JsonNode root;
        try {
            root = appClient.accountDetails(localMobile(stored));
        } catch (IllegalStateException ex) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, ex.getMessage());
        }
        DfsPortalTxnResponse response = DfsPortalTxnResponse.from(root);
        String code = response.getResponsecode();
        if (code != null && !"000".equals(code) && !"00".equals(code)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "DFS could not find that wallet");
        }
        String nid = findNid(root);
        if (nid == null || !cnic.equals(nid)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "That DFS wallet does not belong to this CNIC");
        }
        return stored;
    }

    private static String localMobile(String stored) {
        if (stored != null && stored.startsWith("92") && stored.length() == 12) {
            return "0" + stored.substring(2);
        }
        return stored;
    }

    private static String maskWallet(String stored) {
        if (stored == null || stored.length() < 4) return stored;
        return "••••" + stored.substring(stored.length() - 4);
    }

    private static String findNid(JsonNode node) {
        if (node == null || node.isNull()) return null;
        if (node.isObject()) {
            var fields = node.fields();
            while (fields.hasNext()) {
                var field = fields.next();
                String key = field.getKey().toLowerCase(Locale.ROOT);
                if (key.contains("nid") || key.contains("cnic")) {
                    String digits = field.getValue().asText("").replaceAll("\\D", "");
                    if (digits.length() == 13) return digits;
                }
                String nested = findNid(field.getValue());
                if (nested != null) return nested;
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                String nested = findNid(child);
                if (nested != null) return nested;
            }
        }
        return null;
    }

    private static String receiptReference(JsonNode root, String applicationRef) {
        JsonNode data = root != null && root.has("data") && root.get("data").isObject() ? root.get("data") : root;
        if (data != null && data.isObject()) {
            for (String key : List.of("rrn", "transactionId", "txnId", "referenceNo", "transactionReference", "stan")) {
                JsonNode value = data.get(key);
                if (value != null && !value.isNull()) {
                    String text = value.asText("").trim();
                    if (!text.isEmpty()) {
                        return text.length() > 64 ? text.substring(0, 64) : text;
                    }
                }
            }
        }
        String fallback = "FIN-" + applicationRef;
        return fallback.length() > 64 ? fallback.substring(0, 64) : fallback;
    }

    private void event(LoanApplication app, String type, Long accountId, String label, String remark) {
        LoanEvent event = new LoanEvent();
        event.setApplicationId(app.getId());
        event.setEventType(type);
        event.setActorAccountId(accountId);
        event.setActorLabel(label);
        event.setRemark(remark);
        eventRepository.save(event);
    }

    private void notify(FinancingCustomer customer, LoanApplication app, String code, String title, String body) {
        FinancingNotification note = new FinancingNotification();
        note.setCustomerId(customer.getId());
        note.setApplicationId(app.getId());
        note.setStatusCode(code);
        note.setTitle(title);
        note.setBody(body);
        notificationRepository.save(note);
        log.info("[FINANCING] cnic={} ref={} title={} body={}", FinancingNumbers.maskCnic(customer.getCnic()),
                app.getReferenceNo(), title, body);
    }

    private String newReference() {
        String ref = "EV" + java.time.format.DateTimeFormatter.ofPattern("yyMMdd").format(LocalDate.now())
                + UUID.randomUUID().toString().substring(0, 4).toUpperCase(Locale.ROOT);
        if (applicationRepository.findByReferenceNo(ref).isPresent()) {
            return newReference();
        }
        return ref;
    }

    private static String customerLabel(FinancingCustomer customer) {
        return "customer " + FinancingNumbers.maskCnic(customer.getCnic());
    }

    private static String cleanName(String name) {
        if (name == null || name.trim().length() < 2) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Enter the customer name");
        }
        return name.trim();
    }

    private static EmploymentType employment(String raw) {
        try {
            return EmploymentType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Employment type must be SALARY, CORPORATE_REFERRED, or SELF_EMPLOYED");
        }
    }

    private static String text(Map<String, Object> body, String key) {
        Object value = body.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, key + " is required");
        }
        return String.valueOf(value).trim();
    }

    private static BigDecimal money(String raw, String field) {
        try {
            return FinancingNumbers.money(new BigDecimal(raw));
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, field + " must be a number");
        }
    }

    private static String blank(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private static String trim(String value, int max) {
        if (value == null || value.length() <= max) return value;
        return value.substring(0, max);
    }

    private record Dbr(BigDecimal percent, boolean passed, String note) {}
}
