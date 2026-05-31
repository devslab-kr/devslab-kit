// Root build — not published. Each publishable artifact lives in its own
// subproject and applies the publishing plugin (see settings.gradle.kts).
//
// Plugin versions are declared here with `apply false` so subprojects can apply
// them without repeating version numbers, keeping module version drift at zero.
// This mirrors the devslab-kr sibling libraries (ssrf-guard, api-log).
plugins {
    id("org.springframework.boot") version "4.0.6" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    id("org.graalvm.buildtools.native") version "1.1.0" apply false
    id("com.vanniktech.maven.publish") version "0.36.0" apply false
}

allprojects {
    group = providers.gradleProperty("GROUP").get()
    // -PVERSION on the release workflow overrides this with the tag version.
    version = providers.gradleProperty("VERSION").get()
}

// Shared config for every library module. The runnable sample-app is excluded —
// it isn't published and applies the Spring Boot application plugin itself.
val nonPublishedModules = setOf("devslab-kit-sample-app")

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "io.spring.dependency-management")

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
            vendor = JvmVendorSpec.GRAAL_VM
        }
    }

    configure<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension> {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:4.0.6")
        }
    }

    dependencies {
        "testImplementation"("org.junit.jupiter:junit-jupiter")
        "testImplementation"("org.assertj:assertj-core")
        "testImplementation"("org.mockito:mockito-core")
        "testImplementation"("org.mockito:mockito-junit-jupiter")
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.compilerArgs.add("-parameters")
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    if (name !in nonPublishedModules) {
        apply(plugin = "com.vanniktech.maven.publish")

        // Library modules publish under the Spring Boot BOM, so their api()/
        // implementation() deps carry no explicit version. Gradle's module-
        // metadata validator rejects version-less deps at publish time;
        // versionMapping { fromResolutionResult() } freezes the BOM-resolved
        // versions into the generated .pom/.module so consumers don't need our
        // BOM. (Same fix as the api-log / ssrf-guard starters.)
        extensions.configure<PublishingExtension>("publishing") {
            publications.withType<MavenPublication>().configureEach {
                versionMapping {
                    allVariants { fromResolutionResult() }
                }
            }
        }
    }
}
