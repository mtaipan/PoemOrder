package com.poemorder.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class OrderForm {

    @NotBlank
    @Size(max = 80)
    private String name;

    @NotBlank
    @Size(max = 30)
    private String phone;

    @Size(max = 80)
    private String social;

    @NotBlank
    @Size(max = 4000)
    private String description;

    // honeypot (в форме name="website")
    private String website;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getSocial() { return social; }
    public void setSocial(String social) { this.social = social; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }
}
