package com.dfs.corporate.financing.web;

import com.dfs.corporate.financing.FinancingApplicationService;
import com.dfs.corporate.financing.FinancingCatalogService;
import com.dfs.corporate.security.AccountPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
public class FinancingController {

    private final FinancingCatalogService catalog;
    private final FinancingApplicationService applications;

    public FinancingController(FinancingCatalogService catalog, FinancingApplicationService applications) {
        this.catalog = catalog;
        this.applications = applications;
    }

    @GetMapping("/api/public/financing/config")
    public Map<String, Object> config() {
        return catalog.publicConfig();
    }

    @GetMapping("/api/public/financing/categories")
    public List<Map<String, Object>> categories() {
        catalog.requireProductOpen();
        return catalog.categories(true);
    }

    @GetMapping("/api/public/financing/categories/{categoryId}/merchants")
    public List<Map<String, Object>> merchants(@PathVariable Long categoryId) {
        return catalog.publicMerchants(categoryId);
    }

    @GetMapping("/api/public/financing/merchants/{partyId}/products")
    public List<Map<String, Object>> products(@PathVariable Long partyId, @RequestParam Long categoryId) {
        return catalog.publicProducts(partyId, categoryId);
    }

    @GetMapping("/api/public/financing/products/{productId}")
    public Map<String, Object> product(@PathVariable Long productId) {
        return catalog.publicProduct(productId);
    }

    @GetMapping("/api/public/financing/images/{imageId}")
    public ResponseEntity<Resource> image(@PathVariable Long imageId) {
        return file(catalog.imageFile(imageId, false, null));
    }

    @GetMapping("/api/public/financing/document-rules")
    public Object documentRules(@RequestParam(required = false) String employmentType) {
        return catalog.documentRules(employmentType);
    }

    @PostMapping("/api/public/financing/identify")
    public Map<String, Object> identify(@RequestBody Map<String, String> body) {
        return applications.identify(body.get("fullName"), body.get("cnic"), body.get("mobile"));
    }

    @PostMapping("/api/public/financing/otp/verify")
    public Map<String, Object> verifyOtp(@RequestBody Map<String, String> body) {
        return applications.verifyOtp(body.get("cnic"), body.get("mobile"), body.get("code"));
    }

    @PostMapping("/api/public/financing/quote")
    public List<Map<String, Object>> quote(@RequestBody Map<String, Object> body) {
        Integer tenure = body.get("tenureMonths") == null || String.valueOf(body.get("tenureMonths")).isBlank()
                ? null
                : Integer.valueOf(String.valueOf(body.get("tenureMonths")));
        return applications.quote(
                Long.parseLong(String.valueOf(body.get("productId"))),
                new BigDecimal(String.valueOf(body.get("downPaymentPercent"))),
                tenure);
    }

    @PostMapping("/api/public/financing/applications")
    public Map<String, Object> saveDraft(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        return applications.saveDraft(session(request), body);
    }

    @GetMapping("/api/public/financing/applications")
    public List<Map<String, Object>> mine(HttpServletRequest request) {
        return applications.myApplications(session(request));
    }

    @GetMapping("/api/public/financing/notifications")
    public List<Map<String, Object>> notifications(HttpServletRequest request) {
        return applications.myNotifications(session(request));
    }

    @PostMapping(value = "/api/public/financing/applications/{reference}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> upload(HttpServletRequest request,
                                      @PathVariable String reference,
                                      @RequestParam String documentCode,
                                      @RequestParam("file") MultipartFile file) {
        return applications.uploadDocument(session(request), reference, documentCode, file);
    }

    @PostMapping("/api/public/financing/applications/{reference}/biometric")
    public Map<String, Object> biometric(HttpServletRequest request,
                                         @PathVariable String reference,
                                         @RequestBody Map<String, String> body) {
        return applications.biometric(session(request), reference, body.get("source"));
    }

    @PostMapping("/api/public/financing/applications/{reference}/submit")
    public Map<String, Object> submit(HttpServletRequest request, @PathVariable String reference) {
        return applications.submit(session(request), reference);
    }

    @PostMapping("/api/public/financing/applications/{reference}/pay")
    public Map<String, Object> payDownPayment(HttpServletRequest request,
                                              @PathVariable String reference,
                                              @RequestBody Map<String, String> body) {
        return applications.payDownPayment(session(request), reference, body.get("wallet"), body.get("mpin"));
    }

    @PostMapping("/api/public/financing/applications/{reference}/down-payment")
    public Map<String, Object> downPayment(HttpServletRequest request,
                                           @PathVariable String reference,
                                           @RequestBody Map<String, Object> body) {
        return applications.confirmDownPayment(session(request), reference,
                new BigDecimal(String.valueOf(body.get("percent"))));
    }

    @GetMapping("/api/financing/categories")
    public List<Map<String, Object>> merchantCategories() {
        return catalog.categories(true);
    }

    @GetMapping("/api/financing/products")
    public List<Map<String, Object>> myProducts(@AuthenticationPrincipal AccountPrincipal principal) {
        return catalog.merchantProducts(principal);
    }

    @PostMapping("/api/financing/products")
    public Map<String, Object> createProduct(@AuthenticationPrincipal AccountPrincipal principal,
                                             @RequestBody Map<String, String> body) {
        return catalog.saveProduct(principal, null, body);
    }

    @PutMapping("/api/financing/products/{id}")
    public Map<String, Object> updateProduct(@AuthenticationPrincipal AccountPrincipal principal,
                                             @PathVariable Long id,
                                             @RequestBody Map<String, String> body) {
        return catalog.saveProduct(principal, id, body);
    }

    @PostMapping(value = "/api/financing/products/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> addImage(@AuthenticationPrincipal AccountPrincipal principal,
                                        @PathVariable Long id,
                                        @RequestParam("file") MultipartFile file) {
        return catalog.addImage(principal, id, file);
    }

    @GetMapping("/api/financing/applications")
    public List<Map<String, Object>> merchantLoans(@AuthenticationPrincipal AccountPrincipal principal) {
        return applications.merchantApplications(principal);
    }

    @GetMapping("/api/financing/desk/config")
    public Map<String, Object> deskConfig(@AuthenticationPrincipal AccountPrincipal principal) {
        if (!catalog.canViewAll(principal)) {
            throw new com.dfs.corporate.web.error.ApiException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "Financing desk access required");
        }
        return catalog.adminSettings();
    }

    @GetMapping("/api/financing/desk/applications")
    public List<Map<String, Object>> desk(@AuthenticationPrincipal AccountPrincipal principal) {
        return applications.deskList(principal);
    }

    @GetMapping("/api/financing/desk/applications/{reference}")
    public Map<String, Object> deskOne(@AuthenticationPrincipal AccountPrincipal principal, @PathVariable String reference) {
        return applications.deskGet(principal, reference);
    }

    @PostMapping("/api/financing/desk/applications/{reference}/commission")
    public Map<String, Object> commission(@AuthenticationPrincipal AccountPrincipal principal,
                                          @PathVariable String reference,
                                          @RequestBody Map<String, Object> body) {
        String remarks = body.get("remarks") == null ? null : String.valueOf(body.get("remarks"));
        return applications.setCommission(principal, reference, new BigDecimal(String.valueOf(body.get("percent"))), remarks);
    }

    @PostMapping("/api/financing/desk/applications/{reference}/forward")
    public Map<String, Object> forward(@AuthenticationPrincipal AccountPrincipal principal,
                                       @PathVariable String reference,
                                       @RequestBody(required = false) Map<String, String> body) {
        return applications.forward(principal, reference, body == null ? null : body.get("remarks"));
    }

    @PostMapping("/api/financing/desk/applications/{reference}/ops")
    public Map<String, Object> ops(@AuthenticationPrincipal AccountPrincipal principal,
                                   @PathVariable String reference,
                                   @RequestBody Map<String, String> body) {
        return applications.opsDecision(principal, reference, body.get("decision"), body.get("remarks"));
    }

    @PostMapping("/api/financing/desk/applications/{reference}/checker")
    public Map<String, Object> checker(@AuthenticationPrincipal AccountPrincipal principal,
                                       @PathVariable String reference,
                                       @RequestBody Map<String, String> body) {
        return applications.checkerDecision(principal, reference, body.get("decision"), body.get("remarks"));
    }

    @GetMapping("/api/financing/desk/documents/{id}/file")
    public ResponseEntity<Resource> document(@AuthenticationPrincipal AccountPrincipal principal, @PathVariable Long id) {
        return file(applications.documentFile(principal, id));
    }

    @GetMapping("/api/admin/financing/products")
    public List<Map<String, Object>> catalogue() {
        return catalog.catalogue();
    }

    @PostMapping("/api/admin/financing/products/{id}/visibility")
    public Map<String, Object> visibility(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return catalog.setShownToCustomers(id, Boolean.parseBoolean(String.valueOf(body.get("shown"))));
    }

    @GetMapping("/api/admin/financing/settings")
    public Map<String, Object> adminSettings() {
        return catalog.adminSettings();
    }

    @PutMapping("/api/admin/financing/settings")
    public Map<String, Object> updateSettings(@RequestBody Map<String, Object> body) {
        return catalog.updateSettings(body);
    }

    @GetMapping("/api/admin/financing/categories")
    public List<Map<String, Object>> allCategories() {
        return catalog.categories(false);
    }

    @PostMapping("/api/admin/financing/categories")
    public Map<String, Object> createCategory(@RequestBody Map<String, String> body) {
        return catalog.saveCategory(null, body);
    }

    @PutMapping("/api/admin/financing/categories/{id}")
    public Map<String, Object> updateCategory(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return catalog.saveCategory(id, body);
    }

    @GetMapping("/api/admin/financing/whitelist")
    public Object whitelist() {
        return catalog.whitelist();
    }

    @PostMapping("/api/admin/financing/whitelist")
    public Object saveWhitelist(@RequestBody Map<String, String> body) {
        return catalog.saveWhitelist(body);
    }

    @GetMapping("/api/admin/financing/dealers")
    public List<Map<String, Object>> dealers() {
        return catalog.dealers();
    }

    @PostMapping("/api/admin/financing/dealers")
    public Map<String, Object> authorize(@RequestBody Map<String, String> body) {
        return catalog.authorizeDealer(body);
    }

    @PostMapping("/api/admin/financing/roles")
    public Map<String, Object> grant(@RequestBody Map<String, String> body) {
        return catalog.grantDeskRole(body);
    }

    @GetMapping("/api/admin/financing/document-rules")
    public Object adminRules() {
        return catalog.documentRules(null);
    }

    private static String session(HttpServletRequest request) {
        return request.getHeader("X-Financing-Session");
    }

    private static ResponseEntity<Resource> file(Map<String, Object> file) {
        Resource resource = (Resource) file.get("resource");
        String contentType = (String) file.get("contentType");
        String filename = String.valueOf(file.get("filename")).replace("\"", "");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }
}
