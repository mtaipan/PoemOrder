package com.poemorder.app.domain.poem;

public enum PoemStatus {
    DRAFT("Черновик"),
    PUBLISHED("Опубликовано");

    private final String label;

    PoemStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
