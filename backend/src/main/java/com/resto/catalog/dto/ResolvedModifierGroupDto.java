package com.resto.catalog.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ResolvedModifierGroupDto {
    private UUID id;
    private String name;
    private Boolean required;
    private Integer minSelection;
    private Integer maxSelection;
    private List<ResolvedModifierOptionDto> options = new ArrayList<>();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Boolean getRequired() { return required; }
    public void setRequired(Boolean required) { this.required = required; }
    public Integer getMinSelection() { return minSelection; }
    public void setMinSelection(Integer minSelection) { this.minSelection = minSelection; }
    public Integer getMaxSelection() { return maxSelection; }
    public void setMaxSelection(Integer maxSelection) { this.maxSelection = maxSelection; }
    /** Alias for minSelection (JSON-friendly min). */
    public Integer getMin() { return minSelection; }
    /** Alias for maxSelection (JSON-friendly max). */
    public Integer getMax() { return maxSelection; }
    public List<ResolvedModifierOptionDto> getOptions() { return options; }
    public void setOptions(List<ResolvedModifierOptionDto> options) {
        this.options = options != null ? options : new ArrayList<>();
    }

    public static ResolvedModifierGroupDtoBuilder builder() {
        return new ResolvedModifierGroupDtoBuilder();
    }

    public static class ResolvedModifierGroupDtoBuilder {
        private UUID id;
        private String name;
        private Boolean required;
        private Integer minSelection;
        private Integer maxSelection;
        private List<ResolvedModifierOptionDto> options = new ArrayList<>();

        public ResolvedModifierGroupDtoBuilder id(UUID id) { this.id = id; return this; }
        public ResolvedModifierGroupDtoBuilder name(String name) { this.name = name; return this; }
        public ResolvedModifierGroupDtoBuilder required(Boolean required) { this.required = required; return this; }
        public ResolvedModifierGroupDtoBuilder minSelection(Integer minSelection) { this.minSelection = minSelection; return this; }
        public ResolvedModifierGroupDtoBuilder maxSelection(Integer maxSelection) { this.maxSelection = maxSelection; return this; }
        public ResolvedModifierGroupDtoBuilder options(List<ResolvedModifierOptionDto> options) {
            this.options = options != null ? options : new ArrayList<>();
            return this;
        }

        public ResolvedModifierGroupDto build() {
            ResolvedModifierGroupDto dto = new ResolvedModifierGroupDto();
            dto.setId(this.id);
            dto.setName(this.name);
            dto.setRequired(this.required);
            dto.setMinSelection(this.minSelection);
            dto.setMaxSelection(this.maxSelection);
            dto.setOptions(this.options);
            return dto;
        }
    }
}
