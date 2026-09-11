package com.resto.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "cash_sessions")
public class CashSession {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "store_id", nullable = false)
    private UUID storeId;

    @Column(name = "opened_by_user_id", nullable = false)
    private UUID openedByUserId;

    @Column(name = "closed_by_user_id")
    private UUID closedByUserId;

    @Column(nullable = false, length = 20)
    private String status = "OPEN";

    @Column(name = "opened_at", nullable = false, updatable = false)
    private OffsetDateTime openedAt;

    @Column(name = "closed_at")
    private OffsetDateTime closedAt;

    @Column(name = "opening_float", precision = 10, scale = 2)
    private BigDecimal openingFloat;

    @Column(name = "closing_notes")
    private String closingNotes;

    /** Immutable JSON snapshot of the financial report, set on close. */
    @Column(name = "report_snapshot", columnDefinition = "TEXT")
    private String reportSnapshot;

    @PrePersist
    protected void onCreate() {
        if (openedAt == null) {
            openedAt = OffsetDateTime.now();
        }
        if (status == null) {
            status = "OPEN";
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getOrganizationId() { return organizationId; }
    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
    public UUID getStoreId() { return storeId; }
    public void setStoreId(UUID storeId) { this.storeId = storeId; }
    public UUID getOpenedByUserId() { return openedByUserId; }
    public void setOpenedByUserId(UUID openedByUserId) { this.openedByUserId = openedByUserId; }
    public UUID getClosedByUserId() { return closedByUserId; }
    public void setClosedByUserId(UUID closedByUserId) { this.closedByUserId = closedByUserId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public OffsetDateTime getOpenedAt() { return openedAt; }
    public void setOpenedAt(OffsetDateTime openedAt) { this.openedAt = openedAt; }
    public OffsetDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(OffsetDateTime closedAt) { this.closedAt = closedAt; }
    public BigDecimal getOpeningFloat() { return openingFloat; }
    public void setOpeningFloat(BigDecimal openingFloat) { this.openingFloat = openingFloat; }
    public String getClosingNotes() { return closingNotes; }
    public void setClosingNotes(String closingNotes) { this.closingNotes = closingNotes; }
    public String getReportSnapshot() { return reportSnapshot; }
    public void setReportSnapshot(String reportSnapshot) { this.reportSnapshot = reportSnapshot; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private final CashSession session = new CashSession();

        public Builder id(UUID id) { session.setId(id); return this; }
        public Builder organizationId(UUID organizationId) { session.setOrganizationId(organizationId); return this; }
        public Builder storeId(UUID storeId) { session.setStoreId(storeId); return this; }
        public Builder openedByUserId(UUID openedByUserId) { session.setOpenedByUserId(openedByUserId); return this; }
        public Builder closedByUserId(UUID closedByUserId) { session.setClosedByUserId(closedByUserId); return this; }
        public Builder status(String status) { session.setStatus(status); return this; }
        public Builder openedAt(OffsetDateTime openedAt) { session.setOpenedAt(openedAt); return this; }
        public Builder closedAt(OffsetDateTime closedAt) { session.setClosedAt(closedAt); return this; }
        public Builder openingFloat(BigDecimal openingFloat) { session.setOpeningFloat(openingFloat); return this; }
        public Builder closingNotes(String closingNotes) { session.setClosingNotes(closingNotes); return this; }
        public Builder reportSnapshot(String reportSnapshot) { session.setReportSnapshot(reportSnapshot); return this; }
        public CashSession build() { return session; }
    }
}
