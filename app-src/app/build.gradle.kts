import java.util.Properties
plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
  id("org.jetbrains.kotlin.plugin.compose")
}
// 签名信息从 ../signing/signing.properties 读取（不进源码、不进 git、不上传）
val signProps = Properties().apply {
  val f = rootProject.file("../signing/signing.properties")
  if (f.exists()) f.inputStream().use { load(it) }
}
android {
  namespace = "com.scarlett.shiye"
  compileSdk = 36
  defaultConfig {
    applicationId = "com.scarlett.shiye"
    minSdk = 29; targetSdk = 35; versionCode = 8; versionName = "2.1.4"
  }
  signingConfigs {
    if (signProps.isNotEmpty()) create("release") {
      storeFile = rootProject.file("../signing/" + signProps.getProperty("storeFile"))
      storePassword = signProps.getProperty("storePassword")
      keyAlias = signProps.getProperty("keyAlias")
      keyPassword = signProps.getProperty("keyPassword")
    }
  }
  buildTypes {
    getByName("release") {
      isMinifyEnabled = true; isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      if (signProps.isNotEmpty()) signingConfig = signingConfigs.getByName("release")
    }
  }
  buildFeatures { compose = true; buildConfig = true }
  compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
  kotlinOptions { jvmTarget = "17" }
  packaging { resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}", "DebugProbesKt.bin", "kotlin-tooling-metadata.json") }
  testOptions { unitTests.isReturnDefaultValues = true; unitTests.isIncludeAndroidResources = true }
}
dependencies {
  val bom = platform("androidx.compose:compose-bom:2025.10.00")
  implementation(bom)
  implementation("androidx.compose.material3:material3")
  implementation("androidx.compose.ui:ui")
  implementation("androidx.compose.foundation:foundation")
  implementation("dev.chrisbanes.haze:haze:1.1.1")
  implementation("androidx.activity:activity-compose:1.12.0")
  implementation("androidx.core:core-ktx:1.17.0")
  implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
  implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
  implementation("androidx.navigation3:navigation3-runtime:1.0.1")
  implementation("androidx.navigation3:navigation3-ui:1.0.1")
  implementation("androidx.webkit:webkit:1.11.0")
  testImplementation("junit:junit:4.13.2")
  testImplementation("org.json:json:20231013")
  testImplementation("org.robolectric:robolectric:4.13")
  testImplementation("androidx.compose.ui:ui-test-junit4")
  debugImplementation("androidx.compose.ui:ui-test-manifest")
  debugImplementation("androidx.compose.ui:ui-tooling")
}
