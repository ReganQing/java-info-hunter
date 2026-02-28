# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

JavaInfoHunter is a Spring Boot 4.0.3 application built with Java 17. The project uses Maven for build automation and includes Lombok for reducing boilerplate code.

## Build and Development Commands

### Building the Project
```bash
# Unix/Linux/MacOS
./mvnw clean package

# Windows
mvnw.cmd clean package
```

### Running the Application
```bash
# Unix/Linux/MacOS
./mvnw spring-boot:run

# Windows
mvnw.cmd spring-boot:run

# Run with specific profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=develop
```

### Running Tests
```bash
# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=JavaInfoHunterApplicationTests

# Run with coverage
./mvnw test jacoco:report
```

### Other Useful Commands
```bash
# Clean build artifacts
./mvnw clean

# Verify the project (compile + test)
./mvnw verify

# Skip tests during build
./mvnw clean package -DskipTests

# Generate dependency tree
./mvnw dependency:tree
```

## Project Architecture

### Package Structure
- `com.ron.javainfohunter` - Root package containing the main application class
- Standard Spring Boot layering will be organized as the project grows (controller, service, repository, etc.)

### Configuration Profiles
The application supports multiple Spring profiles for different environments:

- **develop** (`application-develop.yml`) - Development environment with virtual threads enabled
- **production** (`application-production.yml`) - Production environment with virtual threads enabled
- **example** (`application-example.yml`) - Example configuration reference

### Key Technologies
- **Spring Boot 4.0.3** - Core framework
- **Java 17** - Language version
- **Lombok** - Annotation-based code generation
- **Virtual Threads** - Enabled for improved concurrency (Spring Boot 4.0+ feature)

## Configuration Notes

- Virtual threads are explicitly enabled in both `develop` and `production` profiles
- The project uses the Maven Wrapper (`mvnw`/`mvnw.cmd`) for consistent builds across environments
- Lombok is configured as an annotation processor in the Maven compiler plugin
