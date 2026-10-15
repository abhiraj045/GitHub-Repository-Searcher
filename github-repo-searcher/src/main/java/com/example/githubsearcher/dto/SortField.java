package com.example.githubsearcher.dto;

/** Allowed sort options. Maps the public value to the GitHub API value and the entity field. */
public enum SortField {
    STARS("stars", "stars"),
    FORKS("forks", "forks"),
    UPDATED("updated", "lastUpdated");

    private final String apiValue;
    private final String entityField;

    SortField(String apiValue, String entityField) {
        this.apiValue = apiValue;
        this.entityField = entityField;
    }

    public String getApiValue() {
        return apiValue;
    }

    public String getEntityField() {
        return entityField;
    }

    /** Parses user input; null/blank defaults to STARS. */
    public static SortField from(String value) {
        if (value == null || value.isBlank()) {
            return STARS;
        }
        for (SortField field : values()) {
            if (field.apiValue.equalsIgnoreCase(value.trim())) {
                return field;
            }
        }
        throw new IllegalArgumentException(
                "Invalid sort value '" + value + "'. Allowed values: stars, forks, updated");
    }
}
