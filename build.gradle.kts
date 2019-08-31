plugins {
    kotlin("jvm") version "1.3.41"
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

repositories {
    mavenCentral()
    jcenter()
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "11"
    }
    jar {
        manifest {
            attributes.put("Main-Class", "org.rootbr.BpmnApplicationKt")
        }
        from(Callable { configurations["runtimeClasspath"].map { if (it.isDirectory) it else zipTree(it) } })
    }
}

dependencies {
    implementation("io.javalin:javalin:3.1.0")
    implementation("ch.qos.logback:logback-classic:1.2.3")


    implementation(kotlin("stdlib-jdk8"))
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.9.9")


    implementation("org.camunda.bpm:camunda-engine:7.11.0")
    implementation("com.h2database:h2:1.4.199")

    // camunda spin json
    implementation("org.camunda.bpm:camunda-engine-plugin-spin:7.11.0")
    implementation("org.camunda.spin:camunda-spin-core:1.6.7")
    implementation("org.camunda.spin:camunda-spin-dataformat-json-jackson:1.6.7")

    testImplementation("khttp:khttp:1.0.0")

    testCompile("org.junit.jupiter:junit-jupiter-api:5.4.2")
    testCompile("org.junit.jupiter:junit-jupiter-params:5.4.2")
    testRuntime("org.junit.jupiter:junit-jupiter-engine:5.4.2")
    testImplementation("org.assertj:assertj-core:3.9.1")
    testImplementation("org.mockito:mockito-core:2.27.0")
    testImplementation("org.mockito:mockito-all:1.9.5")
    testImplementation("org.mockito:junit-jupiter:2.20.0")
}
