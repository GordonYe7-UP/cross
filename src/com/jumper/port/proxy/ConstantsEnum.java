package com.jumper.port.proxy;


public enum ConstantsEnum {
    CLIENT("client","target_"),
    TARGET("target", "client_");

    private String name;
    private String to;

    ConstantsEnum(String from, String to) {
        this.name = from;
        this.to = to;
    }

    public String getName() {
        return name;
    }

    public String getTo() {
        return to;
    }

    public static ConstantsEnum nameOf(String name) {
        for (ConstantsEnum keyEnum : ConstantsEnum.values()) {
            if(keyEnum.getName().equals(name)) {
                return keyEnum;
            }
        }
        return null;
    }
}
