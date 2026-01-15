# 📦 Publishing Leafy SDK to JitPack

This guide explains how to publish **Leafy SDK** as a free Gradle dependency using [JitPack](https://jitpack.io).

## Prerequisites

1.  A GitHub account.
2.  This project pushed to a public GitHub repository.

## Step 1: Push Code to GitHub

If you haven't already, push your code to a new GitHub repository:

```bash
git init
git add .
git commit -m "Initial commit"
git branch -M main
git remote add origin https://github.com/Colibri91/LeafyGraph.git
git push -u origin main
```

## Step 2: Create a Release

1.  Go to your repository on GitHub.
2.  Click on **Releases** > **Create a new release**.
3.  Tag version: `1.0.0` (or whatever version you want).
4.  Release title: `v1.0.0`.
5.  Click **Publish release**.

## Step 3: Get the Dependency

1.  Open [jitpack.io](https://jitpack.io).
2.  Paste your repository URL (e.g., `github.com/Colibri91/LeafyGraph`).
3.  Click **Look up**.
4.  You should see your `1.0.0` release. Click **Get it**.

## Step 4: Use it in any Android Project

**1. Add JitPack repository:**

In your `settings.gradle.kts` (or root `build.gradle.kts`):

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") } // <--- Add this
    }
}
```

**2. Add the dependency:**

In your app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.Colibri91:LeafyGraph:1.0.0")
}
```

---

## Configuration Details

The `composetreeview/build.gradle.kts` has been configured with `maven-publish`:

```kotlin
plugins {
    id("maven-publish")
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                artifactId = "leafy-graph"
            }
        }
    }
}
```
