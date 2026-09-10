package com.resto.tenant.dto;

import java.util.List;
import java.util.UUID;

/** Org member row for owner admin console. */
public class OrgMemberDto {
    private UUID membershipId;
    private UUID userId;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private Boolean active;
    private Boolean hasPin;
    private List<UUID> storeIds;

    public UUID getMembershipId() { return membershipId; }
    public void setMembershipId(UUID membershipId) { this.membershipId = membershipId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public Boolean getHasPin() { return hasPin; }
    public void setHasPin(Boolean hasPin) { this.hasPin = hasPin; }
    public List<UUID> getStoreIds() { return storeIds; }
    public void setStoreIds(List<UUID> storeIds) { this.storeIds = storeIds; }
}
