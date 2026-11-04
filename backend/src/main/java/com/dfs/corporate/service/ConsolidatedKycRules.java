package com.dfs.corporate.service;

import com.dfs.corporate.domain.CorporateEntityType;
import com.dfs.corporate.domain.PartyType;
import com.dfs.corporate.domain.RiskRating;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Annex-C mandatory docs for Sole Prop, Small Business, Partnership, LLP only.
 */
public final class ConsolidatedKycRules {

    private ConsolidatedKycRules() {}

    public static Set<String> mandatoryDocuments(PartyType partyType,
                                                 CorporateEntityType entityType,
                                                 boolean partnershipUnregistered) {
        Set<String> codes = new LinkedHashSet<>();

        if (partyType == PartyType.SUB_MERCHANT) {
            codes.add("AUTH_ID_FRONT");
            codes.add("AUTH_ID_BACK");
            codes.add("AUTH_LIVE_PHOTO");
            codes.add("PARENT_AUTH");
            return codes;
        }
        if (entityType == null) return codes;

        switch (entityType) {
            case SOLE_PROPRIETORSHIP, SMALL_BUSINESS -> {
                codes.add("AUTH_ID_FRONT");
                codes.add("AUTH_ID_BACK");
                codes.add("AUTH_LIVE_PHOTO");
            }
            case PARTNERSHIP -> {
                codes.add("PARTNERSHIP_DEED");
                codes.add("PARTNERSHIP_AUTHORITY");
                if (!partnershipUnregistered) {
                    codes.add("PARTNERSHIP_REG_CERT");
                }
            }
            case LLP -> {
                codes.add("LLP_DEED");
                codes.add("LLP_SECP_CERT");
                codes.add("LLP_AUTHORITY");
            }
            case LIMITED_COMPANY -> {
                codes.add("LC_DIRECTOR_ID");
                codes.add("LC_BOARD_RESOLUTION");
                codes.add("LC_MOA_AOA");
                codes.add("LC_FORM_A");
            }
            case FOREIGN_BRANCH -> {
                codes.add("FB_OFFICIAL_ID");
                codes.add("FB_BOI_PERMISSION");
                codes.add("FB_DIRECTOR_LIST");
                codes.add("FB_FORM_2");
                codes.add("FB_FORM_5");
                codes.add("FB_PRINCIPAL_LETTER");
            }
            case TRUST_SOCIETY -> {
                codes.add("TS_GOVERNING_ID");
                codes.add("TS_SIGNATORY_ID");
                codes.add("TS_TRUST_PARTIES_ID");
                codes.add("TS_CONTROL_DECLARATION");
                codes.add("TS_REG_OR_INSTRUMENT");
                codes.add("TS_BYLAWS");
                codes.add("TS_RESOLUTION");
            }
            case NGO_NPO -> {
                codes.add("NGO_GOVERNING_ID");
                codes.add("NGO_SIGNATORY_ID");
                codes.add("NGO_REGISTRATION");
                codes.add("NGO_MOA_AOA");
                codes.add("NGO_RESOLUTION");
            }
        }
        return codes;
    }

    public static Set<String> optionalDocuments(CorporateEntityType entityType) {
        if (entityType == CorporateEntityType.NGO_NPO) {
            return Set.of("NGO_ANNUAL_ACCOUNTS");
        }
        return Set.of();
    }

    public static String oneOfGap(CorporateEntityType entityType, Set<String> uploaded) {
        if (entityType == null) return null;
        for (Set<String> group : oneOfGroups(entityType)) {
            if (group.stream().noneMatch(uploaded::contains)) {
                return switch (entityType) {
                    case SOLE_PROPRIETORSHIP ->
                            "Sole prop: upload at least one of NTN, trade body, letterhead declaration, or account requisition (Annex-C)";
                    case SMALL_BUSINESS ->
                            "Small business: upload at least one of registration cert, NTN, trade body, or proof of funds (Annex-C)";
                    case LIMITED_COMPANY ->
                            "Limited company: upload Form 1 if newly incorporated, or Form 9 if already incorporated (Annex-C)";
                    case NGO_NPO ->
                            "NGO/NPO: upload Form 1 if newly incorporated, or Form 9 if already incorporated (Annex-C)";
                    default -> "Upload at least one of: " + String.join(", ", group);
                };
            }
        }
        return null;
    }

    public static Set<String> oneOfCodes(CorporateEntityType entityType) {
        Set<String> codes = new LinkedHashSet<>();
        for (Set<String> group : oneOfGroups(entityType)) {
            codes.addAll(group);
        }
        return codes;
    }

    private static java.util.List<Set<String>> oneOfGroups(CorporateEntityType entityType) {
        if (entityType == null) return java.util.List.of();
        return switch (entityType) {
            case SOLE_PROPRIETORSHIP -> java.util.List.of(solePropAlternatives());
            case SMALL_BUSINESS -> java.util.List.of(smallBusinessAlternatives());
            case LIMITED_COMPANY -> java.util.List.of(Set.of("LC_FORM_1", "LC_FORM_9"));
            case NGO_NPO -> java.util.List.of(Set.of("NGO_FORM_1", "NGO_FORM_9"));
            default -> java.util.List.of();
        };
    }

    public static String documentLabel(String code) {
        return switch (code) {
            case "AUTH_ID_FRONT" -> "Authorized / account holder ID — front";
            case "AUTH_ID_BACK" -> "Authorized / account holder ID — back";
            case "AUTH_LIVE_PHOTO" -> "Live / digital photograph";
            case "NTN_OR_TAX" -> "Sales tax registration or NTN certificate";
            case "TRADE_BODY_MEMBERSHIP" -> "Trade body membership certificate";
            case "SOLE_LETTERHEAD_DECL" -> "Sole proprietorship declaration on letterhead";
            case "SOLE_ACCOUNT_REQ" -> "Account opening requisition on letterhead";
            case "REGISTRATION_CERT" -> "Registration certificate";
            case "PROOF_OF_FUNDS" -> "Proof of source of funds / income";
            case "PARTNERSHIP_DEED" -> "Attested partnership deed signed by all partners";
            case "PARTNERSHIP_REG_CERT" -> "Registration certificate with Registrar of Firms";
            case "PARTNERSHIP_AUTHORITY" -> "Authority letter signed by all partners";
            case "LLP_DEED" -> "LLP deed / agreement";
            case "LLP_SECP_CERT" -> "LLP registration certificate (SECP)";
            case "LLP_AUTHORITY" -> "Authority letter by all partners to operate the account";
            default -> code;
        };
    }

    public static boolean backOfficeKycOnly(CorporateEntityType type) {
        return type == CorporateEntityType.FOREIGN_BRANCH;
    }

    public static final String MANUAL_KYC_ID_FRONT = "MANUAL_KYC_ID_FRONT";
    public static final String MANUAL_KYC_ID_BACK = "MANUAL_KYC_ID_BACK";
    public static final String MANUAL_KYC_PHOTO = "MANUAL_KYC_PHOTO";

    public static boolean isManualKycCode(String code) {
        if (code == null) return false;
        String c = code.trim().toUpperCase();
        return MANUAL_KYC_ID_FRONT.equals(c) || MANUAL_KYC_ID_BACK.equals(c) || MANUAL_KYC_PHOTO.equals(c);
    }

    public static java.util.List<String> manualKycCodes() {
        return java.util.List.of(MANUAL_KYC_ID_FRONT, MANUAL_KYC_ID_BACK, MANUAL_KYC_PHOTO);
    }

    public static Set<String> solePropAlternatives() {
        return Set.of("NTN_OR_TAX", "TRADE_BODY_MEMBERSHIP", "SOLE_LETTERHEAD_DECL", "SOLE_ACCOUNT_REQ");
    }

    public static Set<String> smallBusinessAlternatives() {
        return Set.of("REGISTRATION_CERT", "NTN_OR_TAX", "TRADE_BODY_MEMBERSHIP", "PROOF_OF_FUNDS");
    }

    public static boolean needsPartnerInvites(CorporateEntityType type) {
        // Deprecated: portal partner KYC links replaced by mobile app invites
        return false;
    }

    public static boolean needsPartnerRoster(CorporateEntityType type) {
        return type == CorporateEntityType.PARTNERSHIP || type == CorporateEntityType.LLP;
    }

    public static String partnerCnicFront(Long personId) {
        return "PARTNER_" + personId + "_CNIC_FRONT";
    }

    public static String partnerCnicBack(Long personId) {
        return "PARTNER_" + personId + "_CNIC_BACK";
    }

    public static String partnerAgreement(Long personId) {
        return "PARTNER_" + personId + "_AGREEMENT";
    }

    public static boolean isPartnerUploadCode(String code) {
        return code != null && code.toUpperCase().startsWith("PARTNER_")
                && (code.toUpperCase().endsWith("_CNIC_FRONT")
                || code.toUpperCase().endsWith("_CNIC_BACK")
                || code.toUpperCase().endsWith("_AGREEMENT")
                || code.toUpperCase().contains("_ID_FRONT")
                || code.toUpperCase().contains("_ID_BACK")
                || code.toUpperCase().contains("_PHOTO"));
    }

    public static boolean needsEdd(RiskRating rating, boolean flag) {
        return flag || rating == RiskRating.HIGH;
    }

    public static String partnerDocFront(Long personId) {
        return "PARTNER_" + personId + "_ID_FRONT";
    }

    public static String partnerDocBack(Long personId) {
        return "PARTNER_" + personId + "_ID_BACK";
    }

    public static String partnerDocPhoto(Long personId) {
        return "PARTNER_" + personId + "_PHOTO";
    }
}
