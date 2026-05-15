pluginManagement {
    repositories {

        maven { url = uri("https://maven.aliyun.com/repository/google") }        // ← 新增
        maven { url = uri("https://maven.aliyun.com/repository/public") }        // ← 新增
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") } // ← 新增
        maven { url = uri("https://maven.aliyun.com/repository/central") }       // ← 新增
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {

        maven { url = uri("https://maven.aliyun.com/repository/google") }    // ← 新增
        maven { url = uri("https://maven.aliyun.com/repository/public") }    // ← 新增
        maven { url = uri("https://maven.aliyun.com/repository/central") }   // ← 新增
        google()
        mavenCentral()
    }
}

rootProject.name = "AI Answerer"
include(":app")
 