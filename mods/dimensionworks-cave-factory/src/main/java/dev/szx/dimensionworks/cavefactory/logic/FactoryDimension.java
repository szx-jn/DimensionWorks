package dev.szx.dimensionworks.cavefactory.logic;

public enum FactoryDimension {
    MAGNETIC("magnetic_caves", "磁场洞穴"),
    PRIMORDIAL("primordial_caves", "原始洞穴"),
    TOXIC("toxic_caves", "毒化洞穴"),
    ABYSSAL("abyssal_chasm", "渊海陷窟"),
    FORLORN("forlorn_hollows", "异寂空谷"),
    CANDY("candy_cavity", "糖果龋洞");

    private final String path;
    private final String displayName;

    FactoryDimension(String path, String displayName) {
        this.path = path;
        this.displayName = displayName;
    }

    public String dimensionId() {
        return "alex_caves_dimensions:" + path;
    }

    public String path() {
        return path;
    }

    public String displayName() {
        return displayName;
    }
}
