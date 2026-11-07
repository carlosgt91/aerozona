# Contributing to AeroZona

AeroZona is an ATAK plugin, free software under the
[GNU Affero General Public License v3.0 or later](LICENSE), with an
[additional permission](LICENSE-EXCEPTION.md) covering the TAK SDK.

Contributions are welcome in Spanish or English — bug reports, field reports
from real operations, documentation improvements, and code.

## Before you write code

Open an issue first on [GitHub](https://github.com/carlosgt91/aerozona/issues) for
anything larger than a small bug fix. Describing the problem or intended feature
before writing code saves time and helps align changes with the plugin's goals.

The most valuable contribution is often not code: a precise field report — which
device, which Android/ATAK version, what steps you followed, what happened, and
any logcat output — is worth more than a speculative patch.

## Ground rules for code

- **Prefer the stable `gov.tak.api.*` classes** over `com.atakmap.android.*`
  internals wherever an equivalent exists. ATAK obfuscates its internals and the
  mapping changes between releases; following the public API ensures the plugin
  survives ATAK updates.
- **Dialogs and toasts use the MapView context, never the plugin context.** The
  plugin context throws `BadTokenException` and crashes ATAK.
- **No `Spinner`.** Its dropdown is a dialog built from the inflating context and
  hits that same crash. Use a button showing the current value that opens an
  `AlertDialog` with `setSingleChoiceItems`.
- **Distances follow ATAK's own unit preference** (`rab_rng_units_pref`), never a
  hardcoded unit.
- **Work that must outlive a tap does not live in a `Tool`.** ATAK ends the
  active tool whenever another tool starts, a pane opens, or Back is pressed.
  A download, listener, or background task belongs in a component that lasts for
  the plugin's lifecycle.
- **Test the release build, not just debug.** `assembleCivRelease` runs minify
  and ProGuard; lambdas, serialization, and reflection break there if rules are
  missing.

## License headers on new files

Every new source file gets an SPDX identifier:

```java
// SPDX-License-Identifier: AGPL-3.0-or-later
// AeroZona — an ATAK plugin
// Copyright (C) 2026 the AeroZona contributors
//
// This program is free software: you can redistribute it and/or modify it under
// the terms of the GNU Affero General Public License as published by the Free
// Software Foundation, either version 3 of the License, or (at your option) any
// later version, with the additional permission in LICENSE-EXCEPTION.md.
//
// This program is distributed in the hope that it will be useful, but WITHOUT ANY
// WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
// PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.
```

Use the `#` comment form for Python, shell, or Gradle scripts. Do **not** add this
header to files listed under Provenance in [LICENSE-EXCEPTION.md](LICENSE-EXCEPTION.md) —
those originate from the TAK SDK's plugin template and remain under their original terms.

## Vendoring third-party code

Must be compatible with AGPL-3.0-or-later. Permissive licenses such as MIT, BSD,
ISC, and Apache-2.0 are acceptable; GPLv2-only is not. Pin dependencies to a
specific release tag and commit SHA, and clearly state the license in the pull
request.

## Data sources and integrity

AeroZona retrieves and displays public data from:

- **ENAIRE servAIS:** UAS geographic zones, UAS NOTAMs, and controlled airspace.
  ENAIRE es el titular de los derechos de estos datos.
- **aviationweather.gov:** Surface METAR observations and TAF forecasts.
- **AEMET OpenData:** Station observations and municipal forecasts (© AEMET),
  queried with the user's personal API key.
- **Wikidata:** Municipality lists and territorial identifiers (CC0).

Any proposed data source must be public, authoritative, legal to redistribute or
query, and clearly attributed. Never submit or incorporate proprietary, classified,
or restricted data.

## Licensing of contributions

Contributions are accepted under the GNU Affero General Public License v3.0 or
later (AGPL-3.0-or-later) with the additional permission for the TAK Software
detailed in [LICENSE-EXCEPTION.md](LICENSE-EXCEPTION.md).

Contributors retain copyright in their original contributions. By submitting a
pull request, you certify that you have the right to submit the work under these
license terms.

We encourage adding a Developer Certificate of Origin sign-off line to your commits:

```bash
git commit -s -m "Your descriptive commit message"
```

A bug report, reproduction steps, or field documentation requires no agreement
and is always welcome.

## Reporting a security issue

Please do not open a public issue for security vulnerabilities. Use GitHub's
private vulnerability reporting on this repository —
**Security → Report a vulnerability** — and allow reasonable time for a fix
before public disclosure.

## Where to submit

AeroZona is developed at [https://github.com/carlosgt91/aerozona](https://github.com/carlosgt91/aerozona).
Please submit issues and pull requests directly to this repository.
