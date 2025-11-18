import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "1.9.21"
    id("org.jetbrains.compose") version "1.5.11"
}

group = "com.rats"
version = "1.0.0"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)

    // DuckDB Java Client
    implementation("org.duckdb:duckdb_jdbc:1.1.3")

    // Excel support
    implementation("org.apache.poi:poi:5.2.5")
    implementation("org.apache.poi:poi-ooxml:5.2.5")

    // CSV parsing
    implementation("com.opencsv:opencsv:5.9")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.7.3")
}

compose.desktop {
    application {
        mainClass = "com.rats.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "RATS"
            packageVersion = "1.0.0"
            description = "Desktop data analysis application"
            vendor = "RATS"
            licenseFile.set(project.file("LICENSE").takeIf { it.exists() })

            macOS {
                bundleID = "com.rats.desktop"
            }

            windows {
                menuGroup = "RATS"
                // Upgrade UUID for MSI installer
                upgradeUuid = "61DAB35E-17CB-43B4-B698-C1A92CAB0D2B"
            }
        }
    }
}
