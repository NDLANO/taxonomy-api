package millbuild

import mill.javalib.*

object Deps {

  val commonsCodec = mvn"commons-codec:commons-codec:1.18.0"
  val commonsCompress = mvn"org.apache.commons:commons-compress:1.28.0"
  val commonsLang3 = mvn"org.apache.commons:commons-lang3:3.17.0"
  val hypersistenceUtilsHibernate63 =
    mvn"io.hypersistence:hypersistence-utils-hibernate-63:3.10.1"
  val jacksonDatatypeJdk8 =
    mvn"com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.19.4"
  val jacksonModuleJakartaXmlbindAnnotations =
    mvn"com.fasterxml.jackson.module:jackson-module-jakarta-xmlbind-annotations:2.19.4"
  val javaJwt = mvn"com.auth0:java-jwt:4.5.0"
  val javamelodySpringBootStarter =
    mvn"net.bull.javamelody:javamelody-spring-boot-starter:2.5.0"
  val junitJupiter = mvn"org.junit.jupiter:junit-jupiter:5.12.0"
  val junitJupiterApi = mvn"org.junit.jupiter:junit-jupiter-api:5.12.0"
  val junitJupiterEngine = mvn"org.junit.jupiter:junit-jupiter-engine:5.12.0"
  val junitJupiterParams = mvn"org.junit.jupiter:junit-jupiter-params:5.12.0"
  val jsoup = mvn"org.jsoup:jsoup:1.21.1"
  val jwksRsa = mvn"com.auth0:jwks-rsa:0.23.0"
  val liquibaseCore = mvn"org.liquibase:liquibase-core:4.31.1"
  val logbackCore = mvn"ch.qos.logback:logback-core:1.5.21"
  val logbackJackson = mvn"ch.qos.logback.contrib:logback-jackson:0.1.5"
  val logbackJsonClassic =
    mvn"ch.qos.logback.contrib:logback-json-classic:0.1.5"
  val micrometerRegistryPrometheus =
    mvn"io.micrometer:micrometer-registry-prometheus:1.15.6"
  val postgresql = mvn"org.postgresql:postgresql:42.7.8"
  val preliquibaseSpringBootStarter =
    mvn"net.lbruun.springboot:preliquibase-spring-boot-starter:1.6.1"
  val springBootConfigurationProcessor =
    mvn"org.springframework.boot:spring-boot-configuration-processor:3.5.8"
  val springBootDevtools =
    mvn"org.springframework.boot:spring-boot-devtools:3.5.8"
  val springBootStarter =
    mvn"org.springframework.boot:spring-boot-starter:3.5.8"
  val springBootStarterActuator =
    mvn"org.springframework.boot:spring-boot-starter-actuator:3.5.8"
  val springBootStarterDataJpa =
    mvn"org.springframework.boot:spring-boot-starter-data-jpa:3.5.8"
  val springBootStarterSecurity =
    mvn"org.springframework.boot:spring-boot-starter-security:3.5.8"
  val springBootStarterValidation =
    mvn"org.springframework.boot:spring-boot-starter-validation:3.5.8"
  val springBootStarterTest =
    mvn"org.springframework.boot:spring-boot-starter-test:3.5.8"
  val springBootStarterWeb =
    mvn"org.springframework.boot:spring-boot-starter-web:3.5.8"
  val springSecurityWeb =
    mvn"org.springframework.security:spring-security-web:6.5.7"
  val springdocOpenapiStarterWebmvcApi =
    mvn"org.springdoc:springdoc-openapi-starter-webmvc-api:2.8.14"
  val testcontainersJunitJupiter =
    mvn"org.testcontainers:testcontainers-junit-jupiter:2.0.2"
  val testcontainersPostgresql =
    mvn"org.testcontainers:testcontainers-postgresql:2.0.2"
}
