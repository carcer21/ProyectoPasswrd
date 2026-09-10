plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    wasmJs {
        outputModuleName = "passwrd"
        binaries.executable()
        browser {
            commonWebpackConfig {
                outputFileName = "passwrd.js"
            }
        }
    }

    sourceSets {
        wasmJsMain.dependencies {
            implementation(project(":core"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }
    }
}
