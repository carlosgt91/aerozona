<div align="center">

# ATAK Plugin — AeroZona

[![ATAK-CIV](https://img.shields.io/badge/ATAK--CIV-5.8-blue.svg)](https://tak.gov)
[![Normativa](https://img.shields.io/badge/Normativa-RD%20517%2F2024-success.svg)](#1-zonas-geográficas-de-uas-rd-5172024)
[![Fuentes](https://img.shields.io/badge/Fuentes-ENAIRE%20%7C%20AEMET%20%7C%20METAR%2FTAF-orange.svg)](#-fuentes-de-datos)
[![Versión](https://img.shields.io/badge/Versión-1.0.6-brightgreen.svg)](#-distribución)
[![Licencia](https://img.shields.io/badge/Licencia-AGPL--3.0-lightgrey.svg)](LICENSE)

**Conciencia del espacio aéreo para pilotos de UAS en España, dentro de ATAK.**  
*Zonas geográficas de UAS de ENAIRE, NOTAM UAS, espacio aéreo con frecuencias, METAR/TAF y meteorología de AEMET, con planificador de vuelo.*

</div>

---

## 📌 Qué es

**AeroZona** responde en el propio mapa de ATAK a las dos preguntas del piloto de UAS antes de despegar: **qué zonas y restricciones hay donde voy a volar** y **qué condiciones aplican a esa columna de aire**.

Todo se procesa en el dispositivo: no necesita TAK Server ni misiones Data Sync. La línea **«Está en:»** resume al instante las zonas y NOTAM activos en la posición GPS o en el punto elegido.

```
   ENAIRE servAIS        aviationweather.gov        AEMET OpenData
 (zonas UAS, NOTAM,        (METAR / TAF)          (avisos, estación,
   espacio aéreo)                                  predicción horaria)
         │                       │                        │
         └──────────── HTTPS saliente (443) ──────────────┘
                                 ▼
                   ┌──────────────────────────┐
                   │     Plugin AeroZona      │ ◄── Posición / centro del mapa
                   │  caché local de 7 días   │ ◄── Elevación DTED de ATAK
                   └──────────────────────────┘
                                 ▼
             Mapa de ATAK · «Está en:» · Plan de vuelo UAS
```

---

## 📦 Distribución

AeroZona se distribuye **solo como APK firmado por tak.gov** (firma de plugins de terceros), por canal oficial. Carga en el ATAK-CIV 5.8 oficial de Play Store o de tak.gov.

| Versión | Fichero | ATAK |
| :--- | :--- | :--- |
| **1.0.6** | `ATAK-Plugin-AeroZona-1.0.6--5.8.0-civ-release.apk` | ATAK-CIV 5.8 |

Instalación: instale el APK y cárguelo en el **Administrador de complementos** de ATAK. El manual de usuario va dentro del plugin: *Ajustes → Preferencias de herramientas → AeroZona*.

---

## 🎯 Capacidades

### 1. Zonas geográficas de UAS (RD 517/2024)
* Las zonas publicadas por **ENAIRE**, en sus tres agrupaciones: Infraestructuras, Aero y Urbano.
* Colores: **rojo** (prohibido), **naranja** (autorización o coordinación previa), **amarillo** (condiciones operacionales) y **verde** (sin restricciones específicas).
* Límites verticales en metros AGL o AMSL, tal como se publican. Una sola etiqueta por zona aunque ENAIRE la publique en varios trozos.

### 2. NOTAM UAS
* Avisos de ENAIRE para operaciones de UAS, actualizados cada 30 minutos.
* **Magenta** (actividad UAS), **rojo** (restricción de área) y **violeta** (otros avisos). Los horarios diarios se detallan en el texto del aviso.

### 3. Espacio aéreo con frecuencias
* CTR, ATZ/FIZ, zonas P, R y D, TRA/TSA, RMZ/TMZ y zonas deportivas ENR 5.5.
* Ficha de cada zona con clase, límites verticales, servicio ATS y **frecuencias de radio** (canal de 8,33 kHz cuando existe).

### 4. Meteorología aeronáutica y AEMET
* **METAR/TAF** con distintivos por categoría de vuelo: VFR, MVFR, IFR y LIFR; visibilidad en km y QNH en hPa.
* **AEMET OpenData** (botón «Meteo», con clave personal y gratuita): avisos **Meteoalerta** de la posición, estación más cercana (hasta 60 km) y predicción horaria municipal a 12 horas. Rachas de 36 km/h o más, señaladas con ⚠.

### 5. Plan de vuelo UAS
* Despegue por GPS o sobre el mapa, área en círculo o polígono, alturas de 30, 60, 90 y 120 m AGL.
* **«Calcular techo»**: cota máxima del terreno + altura de vuelo, redondeado a los 100 ft siguientes. **«Techo asignado»**: margen disponible. **«Islas»**: relieve que supera el techo, en rojo (requiere DTED; sin elevación, lo dice y no calcula).
* Accesos a ENAIRE Drones, ENAIRE Planea y AESA, y **resumen de la operación** listo para pegar en ENAIRE Planea.

### 6. Funciona sin cobertura
* Todo lo descargado se conserva **7 días** en el dispositivo. Sin red, trabaja con la caché y avisa de qué fuentes no ha podido actualizar.

---

## 🔒 Seguridad y comunicaciones

| Destino | Puerto | Sentido | Uso |
| :--- | :--- | :--- | :--- |
| `servais.enaire.es` | `443` (HTTPS) | Solo saliente | Zonas UAS, NOTAM UAS y espacio aéreo |
| `aviationweather.gov` | `443` (HTTPS) | Solo saliente | METAR y TAF |
| `opendata.aemet.es` | `443` (HTTPS) | Solo saliente | AEMET, solo con la clave del usuario |

* **Sin puertos a la escucha** ni conexiones entrantes; no envía nada al TAK Server.
* **Sin telemetría:** no transmite la posición ni identificadores del dispositivo. Solo envía la zona de mapa consultada (ENAIRE, aviationweather.gov) y el código del municipio más cercano (AEMET).

---

## 🗂️ Fuentes de datos

| Fuente | Datos | Titularidad |
| :--- | :--- | :--- |
| **ENAIRE servAIS** | Zonas UAS, NOTAM UAS, espacio aéreo | ENAIRE es el titular de los derechos de estos datos |
| **aviationweather.gov** | METAR y TAF | NOAA / NWS |
| **AEMET OpenData** | Avisos, observación y predicción | Información elaborada por la Agencia Estatal de Meteorología (© AEMET) |
| **Wikidata** | Municipios y códigos geográficos | CC0 |

> ⚠️ **Uso consultivo.** AeroZona no es una fuente oficial, no constituye una autorización de vuelo ni sustituye la comprobación reglamentaria. Antes de volar, consulte siempre **AIP España**, **ENAIRE Drones**, **Ícaro XXI** (NOTAM oficiales) y **AESA**.

---

## 🗺️ Próximamente (1.1)

- Frecuencias de aeródromo del **AIP AD 2.18** donde ENAIRE no las publica (p. ej. Zaragoza).
- **Filtro por misión** (RD 517/2024): ver solo las zonas que afectan a la operación prevista.
- **Geovallas** sobre zonas UAS, espacio aéreo y NOTAM que sobreviven al reinicio.

---

## 🛠️ Desarrollo

### Requisitos
* Java 17 (OpenJDK / Temurin)
* Android SDK (API 34–36, Build Tools 34.0.0+)
* SDK de ATAK-CIV 5.8.0 (de tak.gov; nunca se incluye en el repositorio)

### Compilación local
1. Indique el SDK en `local.properties` (véase `template.local.properties`):
   ```properties
   sdk.path=/ruta/a/ATAK-CIV-5.8.0.7-SDK
   ```
2. Compile:
   ```bash
   ./gradlew assembleCivDebug
   ./gradlew assembleCivRelease -PbuildManual
   ```

La compilación local sirve **solo para desarrollar y probar**. Toda versión distribuida se compila y firma en **tak.gov**.

---

## 📄 Licencia y créditos

* **[AGPL-3.0-or-later](LICENSE)**, con un permiso adicional para el software TAK ([LICENSE-EXCEPTION.md](LICENSE-EXCEPTION.md)).
* Basado en [AirAware](https://github.com/takwerx/air-aware) de TAKWERX (Copyright © 2026 Andreas Johansson), del que conserva la estructura (paneles, planificador y línea «Está en:»). Todo lo que depende del país se ha rehecho para España, y las capas de EE. UU. (FAA, LAANC, TFR) están desactivadas. AeroZona no está afiliado a TAKWERX ni avalado por TAKWERX.
* Los archivos del plugintemplate del SDK siguen bajo la licencia del TAK Product Center.
* Los datos aeronáuticos y meteorológicos pertenecen a sus organismos titulares (ENAIRE, AEMET).

---

<div align="center">
  <sub>Mantenido por <b><a href="https://github.com/carlosgt91">carlosgt91</a></b> · ATAK-CIV 5.8</sub>
</div>
