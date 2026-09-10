import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.room)
    alias(libs.plugins.ksp)
}

@OptIn(ExperimentalKotlinGradlePluginApi::class)
kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    jvm()

    // Fase 9b: extensión de navegador. Cripto vía WebCrypto SubtleCrypto (AES-GCM, HMAC,
    // SHA-256, EC P-256) + hash-wasm para Argon2id (WebCrypto no lo soporta nativo).
    wasmJs {
        browser()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
        val wasmJsMain by getting {
            dependencies {
                implementation(npm("hash-wasm", "4.12.0"))
            }
        }

        // Android y JVM comparten la MISMA implementación de crypto/ (BouncyCastle + JCA).
        // Sin este source set intermedio habría dos copias del código criptográfico que
        // mantener sincronizadas y auditar por separado — justo lo que queremos evitar.
        // Room no publica variante wasmJs (ver ADR 0006): vive aquí, no en commonMain, junto
        // con la implementación de crypto/ que también comparten Android y JVM.
        val jvmCommonMain by creating {
            dependsOn(commonMain.get())
            dependencies {
                implementation(libs.bouncycastle.bcprov)
                implementation(libs.androidx.room.runtime)
                implementation(libs.androidx.sqlite.bundled)
            }
        }
        androidMain.get().dependsOn(jvmCommonMain)
        jvmMain.get().dependsOn(jvmCommonMain)
    }
}

android {
    namespace = "dev.passwrd.core"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspJvm", libs.androidx.room.compiler)
}

room {
    schemaDirectory("$projectDir/schemas")
}
