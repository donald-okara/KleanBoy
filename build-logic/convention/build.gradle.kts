plugins {
    `kotlin-dsl`
}

group = "com.example.app.buildlogic" //your module name

gradlePlugin {
    plugins {
        register("kotlinMultiplatformLibrary"){
            id = "com.example.app.kotlinMultiplatformLibrary"
            implementationClass = "com.example.app.KotlinMultiplatformLibrary"
        }
        register("kotlinMultiplatformApplication"){
            id = "com.example.app.kotlinMultiplatformApplication"
            implementationClass = "com.example.app.KotlinMultiplatformApplication"
        }
        register("composeMultiplatformPlugin"){
            id = "com.example.app.composeMultiplatformPlugin"
            implementationClass = "com.example.app.ComposeMultiplatformPlugin"
        }
        register("FeatureConvention"){
            id = "com.example.app.featureConvention"
            implementationClass = "com.example.app.FeatureConvention"
        }
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
}
