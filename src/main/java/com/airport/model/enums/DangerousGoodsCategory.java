package com.airport.model.enums;

/**
 * IATA tehlikeli madde kategorileri.
 * SecurityService bu kategoriyle bagajı filtreler ve Güvenlik Havuzu'na alır.
 */
public enum DangerousGoodsCategory {

    EXPLOSIVE("Patlayıcı Madde"),
    FLAMMABLE_LIQUID("Yanıcı Sıvı"),
    FLAMMABLE_SOLID("Yanıcı Katı"),
    TOXIC("Zehirli / Enfeksiyöz Madde"),
    RADIOACTIVE("Radyoaktif Madde"),
    CORROSIVE("Aşındırıcı Madde"),
    OXIDIZER("Oksitleyici Madde"),
    COMPRESSED_GAS("Basınçlı Gaz"),
    MISCELLANEOUS("Diğer Tehlikeli Maddeler");

    private final String description;

    DangerousGoodsCategory(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return description;
    }
}