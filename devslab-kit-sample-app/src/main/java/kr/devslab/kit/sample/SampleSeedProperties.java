package kr.devslab.kit.sample;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings for {@link SampleSeedRunner}. Only meaningful in the sample
 * application — real consumers seed their own data however they like.
 */
@ConfigurationProperties(prefix = "sample.seed")
public class SampleSeedProperties {

    /** Disable for production-like environments where seed data is unwanted. */
    private boolean enabled = true;
    private String adminLoginId = "admin";
    private String adminPassword = "admin";
    private String adminEmail = "admin@example.com";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getAdminLoginId() { return adminLoginId; }
    public void setAdminLoginId(String adminLoginId) { this.adminLoginId = adminLoginId; }
    public String getAdminPassword() { return adminPassword; }
    public void setAdminPassword(String adminPassword) { this.adminPassword = adminPassword; }
    public String getAdminEmail() { return adminEmail; }
    public void setAdminEmail(String adminEmail) { this.adminEmail = adminEmail; }
}
