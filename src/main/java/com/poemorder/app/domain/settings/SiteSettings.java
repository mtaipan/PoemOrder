package com.poemorder.app.domain.settings;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "site_settings")
public class SiteSettings {

    @Id
    private Short id;

    @Column(name = "hero_title", nullable = false, length = 120)
    private String heroTitle;

    @Column(name = "hero_subtitle", nullable = false, length = 500)
    private String heroSubtitle;

    // --- PORTFOLIO TEXTS ---
    @Column(name = "portfolio_title", length = 120)
    private String portfolioTitle;

    @Column(name = "portfolio_subtitle", columnDefinition = "text")
    private String portfolioSubtitle;

    // --- PRICING / TERMS ---
    @Column(name = "pricing_title", length = 160)
    private String pricingTitle;

    @Column(name = "pricing_payment", columnDefinition = "text")
    private String pricingPayment;

    @Column(name = "pricing_delivery", columnDefinition = "text")
    private String pricingDelivery;

    @Column(name = "pricing_refund", columnDefinition = "text")
    private String pricingRefund;

    // (старые контакты можешь оставить, даже если публичка теперь через ContactLink)
    @Column(name = "telegram", length = 80)
    private String telegram;

    @Column(name = "phone", length = 40)
    private String phone;

    @Column(length = 120)
    private String email;

    @Column(name = "social", length = 120)
    private String social;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Short getId() { return id; }
    public void setId(Short id) { this.id = id; }

    public String getHeroTitle() { return heroTitle; }
    public void setHeroTitle(String heroTitle) { this.heroTitle = heroTitle; }

    public String getHeroSubtitle() { return heroSubtitle; }
    public void setHeroSubtitle(String heroSubtitle) { this.heroSubtitle = heroSubtitle; }

    public String getPortfolioTitle() { return portfolioTitle; }
    public void setPortfolioTitle(String portfolioTitle) { this.portfolioTitle = portfolioTitle; }

    public String getPortfolioSubtitle() { return portfolioSubtitle; }
    public void setPortfolioSubtitle(String portfolioSubtitle) { this.portfolioSubtitle = portfolioSubtitle; }

    public String getPricingTitle() { return pricingTitle; }
    public void setPricingTitle(String pricingTitle) { this.pricingTitle = pricingTitle; }

    public String getPricingPayment() { return pricingPayment; }
    public void setPricingPayment(String pricingPayment) { this.pricingPayment = pricingPayment; }

    public String getPricingDelivery() { return pricingDelivery; }
    public void setPricingDelivery(String pricingDelivery) { this.pricingDelivery = pricingDelivery; }

    public String getPricingRefund() { return pricingRefund; }
    public void setPricingRefund(String pricingRefund) { this.pricingRefund = pricingRefund; }

    public String getTelegram() { return telegram; }
    public void setTelegram(String telegram) { this.telegram = telegram; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getSocial() { return social; }
    public void setSocial(String social) { this.social = social; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
