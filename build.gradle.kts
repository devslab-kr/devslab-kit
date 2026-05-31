import com.vanniktech.maven.publish.MavenPublishBaseExtension

// vanniktech maven-publish on the buildscript classpath so this root script can
// `import` its extension type and configure publishing for the library modules.
// (Declaring it `apply false` in plugins {} does not reliably expose the classes
// to the root script's compilation under Gradle 9.x — buildscript {} does.)
buildscript {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath("com.vanniktech:gradle-maven-publish-plugin:0.36.0")
    }
}

plugins {
    id("org.springframework.boot") version "4.0.6" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    id("org.graalvm.buildtools.native") version "1.1.0" apply false
}

allprojects {
    group = "kr.devslab"
    version = "0.1.0"

    repositories {
        mavenCentral()
    }
}

// Every module ships to Maven Central except the runnable reference app.
val nonPublishedModules = setOf("devslab-kit-sample-app")

subprojects {
    val proj = this

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
        options.compilerArgs.add("-parameters")
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    // --- Maven Central publishing for the library modules (not the sample app) ---
    if (proj.name !in nonPublishedModules) {
        apply(plugin = "com.vanniktech.maven.publish")

        configure<MavenPublishBaseExtension> {
            // SONATYPE_HOST / SONATYPE_AUTOMATIC_RELEASE and the POM_* metadata
            // come from gradle.properties. signAllPublications() enables GPG
            // signing (with the org key) for releases and is a no-op for
            // -SNAPSHOT, so publishToMavenLocal needs no key.
            //
            // Do NOT also set RELEASE_SIGNING_ENABLED in gradle.properties: it
            // configures signing too, and combining the two double-sets the
            // signing property, which Gradle 9 rejects as "value is final".
            signAllPublications()
            coordinates(proj.group.toString(), proj.name, proj.version.toString())
            pom {
                name.set(proj.name)
                // Each module sets its own `description` in build.gradle.kts; read
                // it lazily since module scripts evaluate after this block runs.
                description.set(proj.provider { proj.description ?: "devslab-kit :: ${proj.name}" })
            }
        }
    }
}
