# SPDX License Header Convention

Every source file in this project must carry a short SPDX header as the first comment block.
This keeps licensing machine-readable and unambiguous without repeating the full license text.

## Standard header (Kotlin / KTS)

```kotlin
// SPDX-FileCopyrightText: 2024 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later
```

## Standard header (XML)

```xml
<!--
  SPDX-FileCopyrightText: 2024 Marcel Petrick <mail@marcelpetrick.it>
  SPDX-License-Identifier: GPL-3.0-or-later
-->
```

## Standard header (shell / scripts)

```bash
# SPDX-FileCopyrightText: 2024 Marcel Petrick <mail@marcelpetrick.it>
# SPDX-License-Identifier: GPL-3.0-or-later
```

## Rules

- Apply the header to **every** hand-authored source file on creation.
- Update the copyright year to match the year the file was first created.
- Do **not** add SPDX headers to generated files (Gradle-generated code, build outputs, etc.).
- The full license text is in [`LICENSE`](../LICENSE) at the repository root.
