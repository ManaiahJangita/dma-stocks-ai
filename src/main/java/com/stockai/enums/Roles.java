package com.stockai.enums;

public enum Roles {
    ADMIN("ADMIN"), USER("USER");

    private String value;

    Roles(String name) {
        this.value = name;
    }

    public String getName() {
        return this.value;
    }

    public static Roles getType(String name) {
        Roles[] values = Roles.values();
        for (Roles entityType : values) {
            if (entityType.name().equalsIgnoreCase(name)) {
                return entityType;
            }
        }

        return null;
    }
}