package com.poemorder.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ReviewForm {

    @NotBlank(message = "Укажи имя")
    @Size(max = 50, message = "Имя слишком длинное")
    private String name;

    @NotBlank(message = "Напиши текст отзыва")
    @Size(max = 2000, message = "Слишком длинный отзыв")
    private String text;

    @Size(max = 32, message = "Слишком длинный username")
    private String telegramUsername;

    private boolean telegramPublic;

    // honeypot (антибот). В шаблоне есть input name="website"
    private String website;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getTelegramUsername() { return telegramUsername; }
    public void setTelegramUsername(String telegramUsername) { this.telegramUsername = telegramUsername; }

    public boolean isTelegramPublic() { return telegramPublic; }
    public void setTelegramPublic(boolean telegramPublic) { this.telegramPublic = telegramPublic; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }
}
