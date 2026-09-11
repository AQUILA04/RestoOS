package com.resto.payment.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class CashSessionDto {

    private UUID id;
    private UUID organizationId;
    private UUID storeId;
    private String storeName;
    private UUID openedByUserId;
    private String openedByName;
    private UUID closedByUserId;
    private String closedByName;
    private String status;
    private OffsetDateTime openedAt;
    private OffsetDateTime closedAt;
    private BigDecimal openingFloat;
    private String closingNotes;
    private CashSessionReportDto report;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getOrganizationId() { return organizationId; }
    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
    public UUID getStoreId() { return storeId; }
    public void setStoreId(UUID storeId) { this.storeId = storeId; }
    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }
    public UUID getOpenedByUserId() { return openedByUserId; }
    public void setOpenedByUserId(UUID openedByUserId) { this.openedByUserId = openedByUserId; }
    public String getOpenedByName() { return openedByName; }
    public void setOpenedByName(String openedByName) { this.openedByName = openedByName; }
    public UUID getClosedByUserId() { return closedByUserId; }
    public void setClosedByUserId(UUID closedByUserId) { this.closedByUserId = closedByUserId; }
    public String getClosedByName() { return closedByName; }
    public void setClosedByName(String closedByName) { this.closedByName = closedByName; }
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
    public CashSessionReportDto getReport() { return report; }
    public void setReport(CashSessionReportDto report) { this.report = report; }
}
