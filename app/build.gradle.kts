import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Firma de release: se lee de keystore.properties (raíz del proyecto) si existe.
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}

android {
    namespace = "com.jarabaimport.levantamiento"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.jarabaimport.levantamiento"
        minSdk = 26
        targetSdk = 34
        versionCode = 3
        versionName = "1.0.2"
    }

    signingConfigs {
        if (keystorePropsFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (keystorePropsFile.exists()) signingConfig = signingConfigs.getByName("release")
        }
        debug {
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    // Las pruebas de lógica están en src/test y se ejecutan con ./build.sh test (sin JUnit).
    sourceSets["test"].java.setSrcDirs(emptyList<String>())
}

dependencies {
    // Sin dependencias externas (a propósito): máxima estabilidad y compilación offline.
}
