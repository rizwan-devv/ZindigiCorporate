package com.dfs.corporate.financing.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "loan_document_rules")
public class LoanDocumentRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_code", nullable = false, length = 64)
    private String documentCode;

    @Column(name = "document_label", nullable = false, length = 200)
    private String documentLabel;

    @Column(nullable = false, length = 40)
    private String applicability;

    @Column(nullable = false)
    private boolean mandatory = true;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public Long getId() { return id; }
    public String getDocumentCode() { return documentCode; }
    public String getDocumentLabel() { return documentLabel; }
    public String getApplicability() { return applicability; }
    public boolean isMandatory() { return mandatory; }
    public int getSortOrder() { return sortOrder; }
}
