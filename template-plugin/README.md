# 📦 Template Identity Manager API Plugin

This repository offers a standardized structure and configuration to help you develop Identity Manager API plugins 
efficiently.

---

## 🚀 Getting Started

To create a new plugin based on this template:

1. **Create a new branch**

```bash
git checkout -b feature/add_new_plugin_myplugin
```

2. **Copy the template directory**

```bash
cp -rp template-plugin myplugin
```

3. **Clean up**
   Remove unnecessary example files and customize the project to fit your plugin’s needs.

4. **Push your branch and open a merge request**

```bash
git push -u origin feature/add_new_plugin_myplugin
```

---

## ➕ Add to Parent Module

Once your plugin is created, don’t forget to declare it in the parent `pom.xml`:

```xml
<modules>
  <module>myplugin</module>
</modules>
```

This ensures your plugin is included in the global build lifecycle. Without this, Maven will ignore your module during multi-module builds.

---

## 🧩 Project Structure

This template includes:

* A standard Maven project layout
* Common CI/CD configuration
* Shared plugin conventions and configurations

---

## 🛠 Building the Project

To compile and build the plugin:

```bash
mvn clean install
```

This will compile the source code, execute tests, and validate the build.

If you use the included CI setup, your commits will also be automatically checked for formatting and commit message style.

---

## 🧪 Running Tests

Unit tests are implemented with **JUnit 5** and **Mockito**.

Run tests using:

```bash
./mvnw test
```

Feel free to modify or delete the example test classes (`MyPlugin****PluginTest.java`) as needed.

---

## 🛠 Dependencies

This template relies on:

* Java 21
* Spring Boot 3.x (provided scope)
* JUnit 5
* Mockito

Dependency versions are managed via the Spring Boot BOM (`spring-boot-dependencies`).

---

## 📁 Project Layout

```
.
├── src/main/java/org/linagora/linid/myplugin/...
│   ├── MyPluginAuthorizationPlugin.java       ← Example AuthorizationPlugin
│   ├── MyPluginProviderPlugin.java            ← Example ProviderPlugin
│   ├── MyPluginRoutePlugin.java               ← Example RoutePlugin
│   ├── MyPluginTaskPlugin.java                ← Example TaskPlugin
│   └── MyPluginValidationPlugin.java          ← Example ValidationPlugin
├── src/test/java/org/linagora/linid/myplugin/...
│   ├── MyPluginAuthorizationPluginTest.java  ← AuthorizationPlugin test
│   ├── MyPluginRoutePluginTest.java           ← RoutePlugin test
│   ├── MyPluginTaskPluginTest.java            ← TaskPlugin test
│   └── MyPluginValidationPluginTest.java      ← ValidationPlugin test
├── pom.xml
└── README.md
```

---

## ❌ Safe to Remove

You can safely delete:

* Example plugin classes and their tests (`MyPlugin****Plugin.java`, `MyPlugin****PluginTest.java`)

---

## ✅ Tips

* Don’t forget to update `groupId`, `artifactId`, and `version` in your `pom.xml` to match your plugin’s identity.

---

## 📄 License

This template is released under the **LGPL-3.0** license by default.
You can adjust the license according to your project or client requirements.

---

## 🔄 Version Compatibility

All plugins must follow the versioning scheme of the
[`linid-im-api-corelib`](https://github.com/linagora/linid-im-api-corelib) project to ensure compatibility.
