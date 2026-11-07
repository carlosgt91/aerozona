# Licensing notes for AeroZona

AeroZona is licensed under the **[GNU Affero General Public License v3.0 or
later](LICENSE)** (AGPL-3.0-or-later).

AeroZona is a modified version of AirAware (fork, 2026). As permitted by the
exception terms below, this additional permission for the TAK Software is
extended to the modifications in AeroZona. Modified and newly added files carry
the modifications under AGPL-3.0-or-later.

Copyright of modifications: **Copyright (C) 2026 the AeroZona contributors**.

Two things about an ATAK plugin do not follow from the license text alone, and
both are stated here so nobody has to guess: the additional permission that lets
this plugin be combined with ATAK, and which files in this repository were not
written by the authors.

---

## 1. Additional permission for the TAK Software — AGPL-3.0 §7

An ATAK plugin is loaded into ATAK's own process and calls ATAK's API directly.
That is what a plugin is, and the AGPL was not written with it in mind, so the
permission below is granted explicitly.

> **Additional permission under GNU AGPL version 3 section 7**
>
> As a special exception, the copyright holders of AirAware give you permission to
> combine, link with, and distribute AirAware, or a work based on AirAware, together
> with the TAK Software — ATAK-CIV, ATAK-GOV, ATAK-MIL and the TAK Software
> Development Kit, as licensed by the United States Government under the TAK
> Software License Agreement — and to convey the resulting work.
>
> You must still comply with the GNU AGPL in all respects for the portions of the
> resulting work that are AirAware, or a work based on AirAware.
>
> If you modify this file, you may extend this exception to your version of the
> file, but you are not obliged to do so. If you do not wish to do so, delete this
> exception statement from your version.

**In plain English.** You can build this plugin against the TAK SDK, sideload it
into ATAK, and hand the result to whoever you like, without anyone claiming the
AGPL reaches into ATAK. What stays covered is this plugin's own source: modify it
and pass it on, and the people you pass it to are owed that source under the same
license.

**If you only install and use AeroZona, none of this touches you.** Running the
plugin, in any organization, on any number of devices, triggers no obligation at all.

**This exception does not come from the Government, and does not change the TAK
Software License Agreement.** It removes a restriction the AGPL license would
otherwise impose. The TAK Software remains under its own terms, and the SDK's §6
grant is what makes this repository possible in the first place:

> "you are granted a perpetual, non-exclusive, no-charge, royalty-free right to
> use the TAK-SDK and to derive new works or applications based on the TAK-SDK"

The same clause forbids copying, publishing or distributing the SDK, so no SDK
binary is in this repository and none ever will be. `main.jar`, `atak.apk`,
`atak-gradle-takdev.jar` and the shared development keystore are supplied by the
SDK you download yourself, and are reached through a `local.properties` that is
not tracked here.

---

## 2. Provenance — files not written by the project authors

A plugin is scaffolded from the TAK-SDK's `plugintemplate` sample, so parts of
this repository originate with the TAK Product Center rather than with the plugin
authors. Those parts remain subject to the TAK Software License Agreement, and the
AGPL grant covers only the plugin's own work. Where the two meet in one file, the
AGPL applies to the modifications.

**From the TAK-SDK `plugintemplate` sample**, essentially unchanged:

- `app/src/main/java/com/carlosgt/atak/aerozona/plugin/PluginNativeLoader.java`
  (identical to the template but for the package name)
- `build.gradle`, `settings.gradle`, `gradle.properties`, `gradle/versions.gradle`
- `app/build.gradle` and `app/proguard-gradle.txt` — template files with a plugin's
  own version, target and keep rules edited in
- `template.local.properties`
- the `com.atakmap.app.component` declaration in `app/src/main/AndroidManifest.xml`

**Derived from the template and substantially rewritten** — AGPL applies to the
changes:

- `app/src/main/java/com/carlosgt/atak/aerozona/plugin/AirAware.java`

**Third party and official data sources:**

- `gradlew`, `gradlew.bat` and `gradle/wrapper/` — Gradle Inc., Apache License 2.0
- Aeronautical information, UAS geographic zones, and controlled airspace data
  belong to ENAIRE ("ENAIRE es el titular de los derechos de estos datos").
- Meteorological observations and forecasts belong to AEMET (© AEMET).
- METAR and TAF observations are published by `aviationweather.gov`.
- Municipality records are sourced from Wikidata (CC0).
- Agency records and datasets are the property of their respective owners and
  are not covered by the AGPL grant.

Everything else under `app/src/main/java/`, `app/src/main/res/` and `docs/` is
covered by AGPL-3.0-or-later.

---

Copyright (C) 2026 the AeroZona contributors.  
Copyright (C) 2026 Andreas Johansson (TAKWERX).
