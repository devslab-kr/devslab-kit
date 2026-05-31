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
    // Root-level jacoco so we can register an aggregate JacocoReport task (below)
    // that merges every module's coverage into one XML for Codecov.
    jacoco
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
    apply(plugin = "jacoco")

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

// Root-level aggregate coverage report for Codecov. Merges every module's JaCoCo
// exec data — including the sample-app, whose Testcontainers integration tests are
// where most of the -core code is actually exercised — into one XML, so the badge
// reflects real cross-module coverage rather than the thin per-module unit tests.
// Mirrors the ssrf-guard / easy-paging aggregate pattern.
val testCodeCoverageReport = tasks.register<JacocoReport>("testCodeCoverageReport") {
    group = "verification"
    description = "Aggregates JaCoCo coverage from every subproject into one report."
    dependsOn(subprojects.map { "${it.path}:test" })
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

// Wire sources/classes/exec after all subprojects are evaluated (so their main
// source sets exist). Exec data is referenced as a lazy fileTree — resolved when
// the report task runs, by which point each :test has produced its test.exec
// (a config-time .filter { exists() } would see nothing and yield an empty report).
gradle.projectsEvaluated {
    testCodeCoverageReport.configure {
        sourceDirectories.setFrom(files(subprojects.map {
            it.extensions.getByType<SourceSetContainer>()["main"].allSource.srcDirs
        }))
        classDirectories.setFrom(files(subprojects.map {
            it.extensions.getByType<SourceSetContainer>()["main"].output
        }))
        executionData.setFrom(files(subprojects.map {
            it.fileTree(it.layout.buildDirectory.dir("jacoco")) { include("*.exec") }
        }))
    }
}
