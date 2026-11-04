package com.dfs.corporate.financing;

import com.dfs.corporate.domain.Account;
import com.dfs.corporate.domain.AccountPortalRole;
import com.dfs.corporate.domain.Party;
import com.dfs.corporate.domain.PartyStatus;
import com.dfs.corporate.domain.PartyType;
import com.dfs.corporate.domain.PortalRole;
import com.dfs.corporate.domain.Role;
import com.dfs.corporate.financing.domain.*;
import com.dfs.corporate.financing.repository.*;
import com.dfs.corporate.repository.AccountPortalRoleRepository;
import com.dfs.corporate.repository.AccountRepository;
import com.dfs.corporate.repository.PartyRepository;
import com.dfs.corporate.security.AccountPrincipal;
import com.dfs.corporate.service.FileStorageService;
import com.dfs.corporate.service.PortalUserService;
import com.dfs.corporate.web.error.ApiException;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class FinancingCatalogService {

    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_IMAGE = 8L * 1024 * 1024;

    private final FinancingSettingsRepository settingsRepository;
    private final ProductCategoryRepository categoryRepository;
    private final FinancingProductRepository productRepository;
    private final FinancingProductImageRepository imageRepository;
    private final DealerAuthorizationRepository dealerRepository;
    private final FinancingWhitelistRepository whitelistRepository;
    private final LoanDocumentRuleRepository ruleRepository;
    private final PartyRepository partyRepository;
    private final AccountRepository accountRepository;
    private final AccountPortalRoleRepository portalRoleRepository;
    private final PortalUserService portalUserService;
    private final FileStorageService fileStorageService;
    private final FinancingCalculator calculator;

    public FinancingCatalogService(FinancingSettingsRepository settingsRepository,
                                   ProductCategoryRepository categoryRepository,
                                   FinancingProductRepository productRepository,
                                   FinancingProductImageRepository imageRepository,
                                   DealerAuthorizationRepository dealerRepository,
                                   FinancingWhitelistRepository whitelistRepository,
                                   LoanDocumentRuleRepository ruleRepository,
                                   PartyRepository partyRepository,
                                   AccountRepository accountRepository,
                                   AccountPortalRoleRepository portalRoleRepository,
                                   PortalUserService portalUserService,
                                   FileStorageService fileStorageService,
                                   FinancingCalculator calculator) {
        this.settingsRepository = settingsRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.imageRepository = imageRepository;
        this.dealerRepository = dealerRepository;
        this.whitelistRepository = whitelistRepository;
        this.ruleRepository = ruleRepository;
        this.partyRepository = partyRepository;
        this.accountRepository = accountRepository;
        this.portalRoleRepository = portalRoleRepository;
        this.portalUserService = portalUserService;
        this.fileStorageService = fileStorageService;
        this.calculator = calculator;
    }

    public FinancingSettings requireSettings() {
        return settingsRepository.findById(1L)
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "Financing settings are not configured"));
    }

    public void requireProductOpen() {
        if (!requireSettings().isProductEnabled()) {
            throw new ApiException(HttpStatus.CONFLICT, "Electric bike financing is not available");
        }
    }

    public Map<String, Object> publicConfig() {
        FinancingSettings s = requireSettings();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("productEnabled", s.isProductEnabled());
        out.put("kiborPercent", calculator.effectiveKibor(s));
        out.put("downPaymentMinPercent", s.getDownPaymentMinPercent());
        out.put("downPaymentMaxPercent", s.getDownPaymentMaxPercent());
        out.put("downPaymentOptions", FinancingNumbers.percents(s.getDownPaymentOptions()));
        out.put("tenureMonths", FinancingNumbers.months(s.getTenureMonths()));
        out.put("quotationMaxAgeDays", s.getQuotationMaxAgeDays());
        out.put("latePaymentPercent", s.getLatePaymentPercent());
        out.put("customerRateNote", "Indicative plans use the KIBOR rate only. The final rate is confirmed after review.");
        return out;
    }

    public Map<String, Object> adminSettings() {
        FinancingSettings s = requireSettings();
        Map<String, Object> out = publicConfig();
        out.put("kiborFloorPercent", s.getKiborFloorPercent());
        out.put("configuredKiborPercent", s.getKiborPercent());
        out.put("suggestedBankCommissionPercent", s.getSuggestedBankCommissionPercent());
        out.put("bankCommissionMaxPercent", s.getBankCommissionMaxPercent());
        out.put("dbrThresholdPercent", s.getDbrThresholdPercent());
        out.put("cardLimitBasis", s.getCardLimitBasis());
        out.put("cardLimitFactorPercent", s.getCardLimitFactorPercent());
        out.put("formulaCode", s.getFormulaCode());
        out.put("formulaNote", s.getFormulaNote());
        return out;
    }

    @Transactional
    public Map<String, Object> updateSettings(Map<String, Object> body) {
        FinancingSettings s = requireSettings();
        s.setKiborPercent(requiredDecimal(body, "kiborPercent"));
        s.setKiborFloorPercent(requiredDecimal(body, "kiborFloorPercent"));
        s.setSuggestedBankCommissionPercent(requiredDecimal(body, "suggestedBankCommissionPercent"));
        s.setBankCommissionMaxPercent(requiredDecimal(body, "bankCommissionMaxPercent"));
        s.setDownPaymentMinPercent(requiredDecimal(body, "downPaymentMinPercent"));
        s.setDownPaymentMaxPercent(requiredDecimal(body, "downPaymentMaxPercent"));
        s.setDbrThresholdPercent(requiredDecimal(body, "dbrThresholdPercent"));
        s.setCardLimitFactorPercent(requiredDecimal(body, "cardLimitFactorPercent"));
        s.setLatePaymentPercent(requiredDecimal(body, "latePaymentPercent"));
        s.setDownPaymentOptions(requiredText(body, "downPaymentOptions"));
        s.setTenureMonths(requiredText(body, "tenureMonths"));
        s.setQuotationMaxAgeDays(Integer.parseInt(String.valueOf(body.get("quotationMaxAgeDays"))));
        String basis = requiredText(body, "cardLimitBasis").toUpperCase(Locale.ROOT);
        if (!Set.of("UNSET", "UTILIZED", "AVAILABLE").contains(basis)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Card limit basis must be UNSET, UTILIZED, or AVAILABLE");
        }
        s.setCardLimitBasis(basis);
        String formula = body.get("formulaCode") == null ? "" : String.valueOf(body.get("formulaCode")).trim();
        if (formula.isEmpty()) {
            s.setFormulaCode(null);
        } else if (!FinancingCalculator.FORMULA.equals(formula)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Supported formula code is " + FinancingCalculator.FORMULA);
        } else {
            s.setFormulaCode(formula);
        }
        if (s.getKiborPercent().compareTo(BigDecimal.ZERO) < 0 || s.getKiborFloorPercent().compareTo(BigDecimal.ZERO) < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "KIBOR and its floor cannot be negative");
        }
        if (s.getKiborPercent().compareTo(s.getKiborFloorPercent()) < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "KIBOR cannot be set below the floor");
        }
        if (s.getDownPaymentMinPercent().compareTo(s.getDownPaymentMaxPercent()) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Minimum down payment cannot exceed the maximum");
        }
        if (s.getSuggestedBankCommissionPercent().compareTo(BigDecimal.ZERO) < 0
                || s.getSuggestedBankCommissionPercent().compareTo(s.getBankCommissionMaxPercent()) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Suggested bank commission must sit between 0 and the maximum");
        }
        List<BigDecimal> options;
        List<Integer> tenures;
        try {
            options = FinancingNumbers.percents(s.getDownPaymentOptions());
            tenures = FinancingNumbers.months(s.getTenureMonths());
        } catch (NumberFormatException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Down-payment options and tenures must be comma-separated numbers");
        }
        if (options.isEmpty() || tenures.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Provide at least one down-payment option and one tenure");
        }
        for (BigDecimal option : options) {
            if (option.compareTo(s.getDownPaymentMinPercent()) < 0 || option.compareTo(s.getDownPaymentMaxPercent()) > 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Each down-payment option must fall inside the min and max");
            }
        }
        for (Integer months : tenures) {
            if (months <= 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Tenure months must be positive");
            }
        }
        if (body.containsKey("productEnabled")) {
            s.setProductEnabled(Boolean.parseBoolean(String.valueOf(body.get("productEnabled"))));
        }
        s.setUpdatedAt(Instant.now());
        settingsRepository.save(s);
        return adminSettings();
    }

    public List<Map<String, Object>> categories(boolean activeOnly) {
        List<ProductCategory> rows = activeOnly
                ? categoryRepository.findByActiveTrueOrderBySortOrderAscNameAsc()
                : categoryRepository.findAllByOrderBySortOrderAscNameAsc();
        return rows.stream().map(this::categoryMap).toList();
    }

    @Transactional
    public Map<String, Object> saveCategory(Long id, Map<String, String> body) {
        ProductCategory category = id == null ? new ProductCategory() : categoryRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Category not found"));
        String code = requiredText(body, "code").toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_]", "");
        if (code.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Category code is required");
        }
        categoryRepository.findByCodeIgnoreCase(code).ifPresent(existing -> {
            if (id == null || !existing.getId().equals(id)) {
                throw new ApiException(HttpStatus.CONFLICT, "Category code already exists");
            }
        });
        category.setCode(code);
        category.setName(requiredText(body, "name"));
        if (body.containsKey("active")) {
            category.setActive(Boolean.parseBoolean(body.get("active")));
        }
        if (body.get("sortOrder") != null && !body.get("sortOrder").isBlank()) {
            category.setSortOrder(Integer.parseInt(body.get("sortOrder")));
        }
        return categoryMap(categoryRepository.save(category));
    }

    public List<Map<String, Object>> merchantProducts(AccountPrincipal principal) {
        Party party = requireMerchant(principal);
        return productRepository.findByMerchantPartyIdOrderByCreatedAtDesc(party.getId()).stream()
                .map(p -> productMap(p, false))
                .toList();
    }

    @Transactional
    public Map<String, Object> saveProduct(AccountPrincipal principal, Long id, Map<String, String> body) {
        Party party = requireMerchant(principal);
        FinancingProduct product = id == null ? new FinancingProduct() : productRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));
        if (id != null && !party.getId().equals(product.getMerchantPartyId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This product belongs to another merchant");
        }
        ProductCategory category = categoryRepository.findById(Long.valueOf(requiredText(body, "categoryId")))
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Category not found"));
        if (!category.isActive()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "That category is not active");
        }
        BigDecimal price = new BigDecimal(requiredText(body, "price"));
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Price must be greater than zero");
        }
        product.setMerchantPartyId(party.getId());
        product.setCategoryId(category.getId());
        product.setTitle(requiredText(body, "title"));
        product.setDescription(blankToNull(body.get("description")));
        product.setSpecifications(blankToNull(body.get("specifications")));
        product.setPrice(FinancingNumbers.money(price));
        if (id == null) {
            product.setShownToCustomers(false);
        }
        if (body.containsKey("active")) {
            product.setActive(Boolean.parseBoolean(body.get("active")));
        }
        product.setUpdatedAt(Instant.now());
        return productMap(productRepository.save(product), false);
    }

    @Transactional
    public Map<String, Object> addImage(AccountPrincipal principal, Long productId, MultipartFile file) {
        Party party = requireMerchant(principal);
        FinancingProduct product = ownProduct(party.getId(), productId);
        if (imageRepository.countByProductId(product.getId()) >= 6) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A product can have up to 6 images");
        }
        if (file.getSize() > MAX_IMAGE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Image must be 8 MB or smaller");
        }
        String type = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!IMAGE_TYPES.contains(type)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Upload a JPEG, PNG, or WebP image");
        }
        FinancingProductImage image = new FinancingProductImage();
        image.setProductId(product.getId());
        image.setContentType(type);
        image.setOriginalName(file.getOriginalFilename());
        image.setStoredPath(fileStorageService.storeInFolder("financing-products", "img", file));
        imageRepository.save(image);
        return productMap(product, false);
    }

    public List<Map<String, Object>> publicMerchants(Long categoryId) {
        requireProductOpen();
        ProductCategory category = activeCategory(categoryId);
        List<Map<String, Object>> out = new ArrayList<>();
        for (DealerAuthorization auth : dealerRepository.findByCategoryIdAndActiveTrue(category.getId())) {
            Party party = partyRepository.findById(auth.getPartyId()).orElse(null);
            if (party == null || party.getStatus() != PartyStatus.ACTIVE) continue;
            boolean hasProduct = !visibleProducts(party.getId(), category.getId()).isEmpty();
            if (!hasProduct) continue;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("partyId", party.getId());
            row.put("name", merchantName(party));
            row.put("city", party.getCity());
            out.add(row);
        }
        return out;
    }

    public List<Map<String, Object>> publicProducts(Long partyId, Long categoryId) {
        requireProductOpen();
        activeCategory(categoryId);
        if (!isAuthorized(partyId, categoryId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "This dealer is not authorized for the category");
        }
        return visibleProducts(partyId, categoryId).stream().map(p -> productMap(p, true)).toList();
    }

    public Map<String, Object> publicProduct(Long productId) {
        requireProductOpen();
        FinancingProduct product = productRepository.findById(productId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));
        if (!isCustomerVisible(product)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Product not found");
        }
        return productMap(product, true);
    }

    public Map<String, Object> imageFile(Long imageId, boolean merchantCheck, AccountPrincipal principal) {
        FinancingProductImage image = imageRepository.findById(imageId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Image not found"));
        FinancingProduct product = productRepository.findById(image.getProductId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));
        if (merchantCheck) {
            Party party = requireMerchant(principal);
            if (!party.getId().equals(product.getMerchantPartyId())) {
                throw new ApiException(HttpStatus.FORBIDDEN, "This image belongs to another merchant");
            }
        } else if (!isCustomerVisible(product)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Image not found");
        }
        Resource resource = new FileSystemResource(fileStorageService.resolve(image.getStoredPath()));
        Map<String, Object> file = new LinkedHashMap<>();
        file.put("resource", resource);
        file.put("contentType", image.getContentType() == null ? "application/octet-stream" : image.getContentType());
        file.put("filename", image.getOriginalName() == null ? "image" : image.getOriginalName());
        return file;
    }

    public List<FinancingWhitelist> whitelist() {
        return whitelistRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public FinancingWhitelist saveWhitelist(Map<String, String> body) {
        String cnic = FinancingNumbers.cnic(requiredText(body, "cnic"));
        String mobile = FinancingNumbers.mobile(requiredText(body, "mobile"));
        FinancingWhitelist row = whitelistRepository.findByCnic(cnic).orElseGet(FinancingWhitelist::new);
        row.setCnic(cnic);
        row.setMobile(mobile);
        row.setFullName(blankToNull(body.get("fullName")));
        row.setSegmentCode(blankToNull(body.get("segmentCode")));
        row.setActive(!body.containsKey("active") || Boolean.parseBoolean(body.get("active")));
        return whitelistRepository.save(row);
    }

    public List<Map<String, Object>> dealers() {
        return dealerRepository.findAllByOrderByIdDesc().stream().map(auth -> {
            Party party = partyRepository.findById(auth.getPartyId()).orElse(null);
            ProductCategory category = categoryRepository.findById(auth.getCategoryId()).orElse(null);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", auth.getId());
            row.put("partyId", auth.getPartyId());
            row.put("merchantName", party == null ? "" : merchantName(party));
            row.put("categoryId", auth.getCategoryId());
            row.put("categoryName", category == null ? "" : category.getName());
            row.put("active", auth.isActive());
            return row;
        }).toList();
    }

    @Transactional
    public Map<String, Object> authorizeDealer(Map<String, String> body) {
        long partyId = Long.parseLong(requiredText(body, "partyId"));
        long categoryId = Long.parseLong(requiredText(body, "categoryId"));
        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Merchant not found"));
        if (party.getPartyType() != PartyType.MERCHANT) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only a merchant party can be an authorized dealer");
        }
        activeCategory(categoryId);
        DealerAuthorization auth = dealerRepository.findByPartyIdAndCategoryId(partyId, categoryId)
                .orElseGet(DealerAuthorization::new);
        auth.setPartyId(partyId);
        auth.setCategoryId(categoryId);
        auth.setActive(!body.containsKey("active") || Boolean.parseBoolean(body.get("active")));
        dealerRepository.save(auth);
        return dealers().stream().filter(row -> auth.getId().equals(row.get("id"))).findFirst().orElse(Map.of("id", auth.getId()));
    }

    @Transactional
    public Map<String, Object> grantDeskRole(Map<String, String> body) {
        String email = requiredText(body, "email").trim().toLowerCase(Locale.ROOT);
        PortalRole role;
        try {
            role = PortalRole.valueOf(requiredText(body, "role").trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unknown role");
        }
        if (role != PortalRole.FINANCING_OPS && role != PortalRole.FINANCING_CHECKER) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Role must be FINANCING_OPS or FINANCING_CHECKER");
        }
        Account account = accountRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No portal account uses that email"));
        if (!portalRoleRepository.existsByAccountIdAndPortalRole(account.getId(), role)) {
            AccountPortalRole link = new AccountPortalRole();
            link.setAccountId(account.getId());
            link.setPortalRole(role);
            portalRoleRepository.save(link);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("email", account.getEmail());
        out.put("accountId", account.getId());
        out.put("role", account.getRole().name());
        out.put("portalRoles", portalUserService.rolesOf(account.getId()).stream().map(Enum::name).toList());
        return out;
    }

    public List<LoanDocumentRule> documentRules(String employmentType) {
        if (employmentType == null || employmentType.isBlank()) {
            return ruleRepository.findAllByOrderBySortOrderAsc();
        }
        EmploymentType type;
        try {
            type = EmploymentType.valueOf(employmentType.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Employment type must be SALARY, CORPORATE_REFERRED, or SELF_EMPLOYED");
        }
        return ruleRepository.findByApplicabilityInOrderBySortOrderAsc(List.of("ALL", type.name()));
    }

    public List<Map<String, Object>> catalogue() {
        return productRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(product -> {
                    Map<String, Object> row = productMap(product, false);
                    row.put("merchantName", merchantName(product.getMerchantPartyId()));
                    return row;
                }).toList();
    }

    @Transactional
    public Map<String, Object> setShownToCustomers(Long productId, boolean shown) {
        FinancingProduct product = productRepository.findById(productId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));
        product.setShownToCustomers(shown);
        product.setUpdatedAt(Instant.now());
        Map<String, Object> row = productMap(productRepository.save(product), false);
        row.put("merchantName", merchantName(product.getMerchantPartyId()));
        return row;
    }

    public boolean isAuthorized(Long partyId, Long categoryId) {
        return dealerRepository.findByPartyIdAndCategoryId(partyId, categoryId)
                .map(DealerAuthorization::isActive)
                .orElse(false);
    }

    public FinancingProduct requireSellable(Long productId) {
        FinancingProduct product = productRepository.findById(productId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));
        if (!product.isActive() || !product.isShownToCustomers()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This product is not available");
        }
        if (!isAuthorized(product.getMerchantPartyId(), product.getCategoryId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This dealer is not authorized for the product category");
        }
        return product;
    }

    public void requireOps(AccountPrincipal principal) {
        if (!portalUserService.hasRole(principal.getAccountId(), PortalRole.FINANCING_OPS)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FINANCING_OPS role required");
        }
    }

    public void requireChecker(AccountPrincipal principal) {
        if (!portalUserService.hasRole(principal.getAccountId(), PortalRole.FINANCING_CHECKER)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FINANCING_CHECKER role required");
        }
    }

    public boolean canViewAll(AccountPrincipal principal) {
        return principal.getRole() == Role.PLATFORM_ADMIN
                || portalUserService.hasRole(principal.getAccountId(), PortalRole.FINANCING_OPS)
                || portalUserService.hasRole(principal.getAccountId(), PortalRole.FINANCING_CHECKER);
    }

    public Party requireMerchant(AccountPrincipal principal) {
        if (principal.getPartyId() == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "A merchant account is required");
        }
        Party party = partyRepository.findById(principal.getPartyId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Party not found"));
        if (party.getPartyType() != PartyType.MERCHANT || party.getStatus() != PartyStatus.ACTIVE) {
            throw new ApiException(HttpStatus.FORBIDDEN, "An active merchant account is required");
        }
        return party;
    }

    public String merchantName(Long partyId) {
        return partyRepository.findById(partyId).map(this::merchantName).orElse("");
    }

    private FinancingProduct ownProduct(Long partyId, Long productId) {
        FinancingProduct product = productRepository.findById(productId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));
        if (!partyId.equals(product.getMerchantPartyId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This product belongs to another merchant");
        }
        return product;
    }

    private List<FinancingProduct> visibleProducts(Long partyId, Long categoryId) {
        return productRepository.findByMerchantPartyIdAndCategoryIdAndActiveTrueAndShownToCustomersTrueOrderByTitleAsc(partyId, categoryId);
    }

    private boolean isCustomerVisible(FinancingProduct product) {
        return product.isActive()
                && product.isShownToCustomers()
                && isAuthorized(product.getMerchantPartyId(), product.getCategoryId());
    }

    private ProductCategory activeCategory(Long id) {
        ProductCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Category not found"));
        if (!category.isActive()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "That category is not active");
        }
        return category;
    }

    private Map<String, Object> categoryMap(ProductCategory category) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", category.getId());
        row.put("code", category.getCode());
        row.put("name", category.getName());
        row.put("active", category.isActive());
        row.put("sortOrder", category.getSortOrder());
        return row;
    }

    private Map<String, Object> productMap(FinancingProduct product, boolean publicView) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", product.getId());
        row.put("categoryId", product.getCategoryId());
        row.put("title", product.getTitle());
        row.put("description", product.getDescription());
        row.put("specifications", product.getSpecifications());
        row.put("price", product.getPrice());
        row.put("active", product.isActive());
        if (!publicView) {
            row.put("merchantPartyId", product.getMerchantPartyId());
            row.put("shownToCustomers", product.isShownToCustomers());
        } else {
            row.put("merchantName", merchantName(product.getMerchantPartyId()));
        }
        row.put("imageIds", imageRepository.findByProductIdOrderByIdAsc(product.getId()).stream()
                .map(FinancingProductImage::getId).toList());
        return row;
    }

    private String merchantName(Party party) {
        if (party.getBusinessName() != null && !party.getBusinessName().isBlank()) return party.getBusinessName();
        return party.getFullName();
    }

    private static String requiredText(Map<String, ?> body, String key) {
        Object value = body.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, key + " is required");
        }
        return String.valueOf(value).trim();
    }

    private static BigDecimal requiredDecimal(Map<String, Object> body, String key) {
        try {
            return new BigDecimal(requiredText(body, key));
        } catch (NumberFormatException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, key + " must be a number");
        }
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
