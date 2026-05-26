pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

rootProject.name = "devslab-kit"

include(
    "devslab-kit-core",
    "devslab-kit-identity-api",
    "devslab-kit-identity-core",
    "devslab-kit-access-api",
    "devslab-kit-access-core",
    "devslab-kit-tenant-api",
    "devslab-kit-tenant-core",
    "devslab-kit-menu-api",
    "devslab-kit-menu-core",
    "devslab-kit-audit-api",
    "devslab-kit-audit-core",
    "devslab-kit-admin-api",
    "devslab-kit-autoconfigure",
    "devslab-kit-spring-boot-starter",
    "devslab-kit-sample-app",
)
