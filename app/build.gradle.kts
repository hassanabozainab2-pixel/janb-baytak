plugins {
 id("com.android.application")
 id("org.jetbrains.kotlin.android")
 id("org.jetbrains.kotlin.plugin.compose")
 id("com.google.gms.google-services")
}
android {
 namespace = "com.janbbaytak.app"
 compileSdk = 35
 defaultConfig { applicationId = "com.janbbaytak.app"; minSdk = 24; targetSdk = 35; versionCode = 4; versionName = "2.2.0" }
 signingConfigs {
  create("release") {
   val ks = System.getenv("ANDROID_KEYSTORE_PATH")
   if (!ks.isNullOrBlank()) {
    storeFile = file(ks)
    storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
    keyAlias = System.getenv("ANDROID_KEY_ALIAS")
    keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
   }
  }
 }
 buildTypes {
  getByName("release") {
   signingConfig = signingConfigs.getByName("release")
   isMinifyEnabled = false
  }
 }
 buildFeatures { compose = true }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
}
dependencies {
 implementation(platform("androidx.compose:compose-bom:2024.12.01"))
 implementation(platform("com.google.firebase:firebase-bom:35.2.0"))
 implementation("com.google.firebase:firebase-messaging")
 implementation("androidx.activity:activity-compose:1.10.0")
 implementation("androidx.fragment:fragment-ktx:1.8.5")
 implementation("androidx.compose.ui:ui")
 implementation("androidx.compose.ui:ui-graphics")
 implementation("androidx.compose.ui:ui-tooling-preview")
 implementation("androidx.compose.material3:material3")
 implementation("androidx.compose.foundation:foundation")
 debugImplementation("androidx.compose.ui:ui-tooling")
}

// Signing certificate continuity diagnostic is performed in GitHub Actions.
