# 🔌 linid-im-api-community-plugins

This repository hosts all **official backend plugins** for the `linid-im-api` platform.

Each plugin is maintained in a dedicated subdirectory and follows the same structure as the provided `template-plugin`.

---

## 📦 Structure

```
linid-dm-api-community-plugins/
├── plugin-a/
├── plugin-b/
├── template-plugin/    ← Reference structure for new plugins
├── .github/workflows/release.yml
└── .releaserc.base.js
```

---

## 🚀 Releasing Plugins

This repository uses a **centralized release process** powered by [semantic-release](https://github.com/Zorin95670/semantic-version) and GitHub Actions.

### ✅ How it works

* On every push to the `main` branch, the workflow:

    1. Analyzes recent commits.
    2. Extracts the plugin names based on commit messages (e.g., `feat(plugin-a): ...`).
    3. Triggers a release **only for affected plugins**.

### ✏️ Commit Convention

Commits must follow [Conventional Commits](https://www.conventionalcommits.org), with the plugin name in parentheses:

```bash
# Examples:
feat(plugin-a): add support for LDAP
fix(plugin-b): correct response format
chore(plugin-c): update dependencies
```

### 🏷️ Tagging

Each plugin release is tagged using the format:

```
<plugin-name>-v<version>
e.g. plugin-a-v1.2.3
```

---

## ➕ Creating a New Official Plugin

To add a new official plugin:

1. Copy the `template-plugin/` directory.
2. Rename it and adapt it to your plugin's needs.
3. Push it to `main` with a commit message following the convention:

```bash
feat(plugin-my-new-plugin): initial implementation
```

The release workflow will handle versioning, changelog, and publication.

---

## ⚙️ CI/CD

This repository uses a modular GitHub Actions setup:

### 🔒 Default Pull Request Checks

Every pull request is validated by a common workflow that performs:

* ✅ **Branch name validation**
  Branches must follow predefined naming patterns (e.g., `feature/<desc>`, `bugfix/<desc>`, `release/<version>`).

* ✅ **Commit message validation**
  Commits must follow the [Conventional Commits](https://www.conventionalcommits.org) format, including plugin scoping (`feat(plugin-x): ...`).

* ✅ **Code style checks**
  The repository enforces a common `checkstyle` configuration shared across all plugins.

These checks are run automatically on every pull request.

### 🧪 Plugin-specific Unit Tests

Each plugin **must define its own unit test workflow**.

To do this:

1. Create a workflow file under `.github/workflows/test-<plugin>.yml`
2. Use the Maven test lifecycle:

```yaml
name: Test plugin-a

on:
  pull_request:
    paths:
      - 'plugin-a/**'

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up Java
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
      - name: Run unit tests
        run: cd plugin-a && mvn test
```

You can copy and adapt this per plugin.

---

## 🛠️ Development Tips

* Ensure that each plugin is **independent and buildable with Maven**.
* Use standard Maven conventions inside each plugin.
* Keep plugin-specific logic scoped to its folder.

---

## 🤝 Contributions

Official plugins are maintained by the core team, but community contributions are welcome through pull requests.
If you're building a **custom plugin**, please use [`linid-im-api-plugin-template`](https://github.com/linagora/linid-im-api-plugin-template)
in your own repository.
