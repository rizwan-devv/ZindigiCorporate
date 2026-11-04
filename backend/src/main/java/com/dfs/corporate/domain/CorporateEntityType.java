package com.dfs.corporate.domain;

/**
 * Annex-C entity types in scope for Zindigi Corporate (1–4 only).
 */
public enum CorporateEntityType {
    SOLE_PROPRIETORSHIP,
    SMALL_BUSINESS,
    PARTNERSHIP,
    LLP,
    /** 5 — Limited companies / corporations */
    LIMITED_COMPANY,
    /** 6 — Branch of a foreign company. KYC is completed by back office. */
    FOREIGN_BRANCH,
    /** 7 — Trusts, clubs, societies and associations */
    TRUST_SOCIETY,
    /** 8 — INGOs / NGOs / NPOs / charities */
    NGO_NPO
}
