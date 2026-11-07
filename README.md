ATAK Plugin — AeroZona

**Download AeroZona 1.0** (compatible con ATAK-CIV 5.8; instale manualmente y active en el Administrador de complementos de ATAK / sideload, then load in ATAK's Plugins manager):

- **ATAK-CIV 5.8:** https://github.com/carlosgt91/aerozona/releases/download/v1.0/ATAK-Plugin-AeroZona-1.0--5.8.0-civ-release.apk

All releases: https://github.com/carlosgt91/aerozona/releases

**Guía de usuario con capturas / User guide with screenshots: [docs/USER_GUIDE.md](docs/USER_GUIDE.md)**
(https://github.com/carlosgt91/aerozona/blob/main/docs/USER_GUIDE.md)

_________________________________________________________________
PROPÓSITO Y FUNCIONALIDADES

AeroZona proporciona conciencia situacional aeronáutica inmediata para operadores y pilotos civiles de drones (UAS) en España: qué zonas y restricciones coinciden con su posición y qué condiciones normativas y operacionales aplican a la columna de aire en la que opera.

Diseñado específicamente para operaciones de UAS; las zonas deportivas de vuelo (paracaidismo, vuelo a vela, aeromodelismo, RPAS) se representan a título de referencia aeronáutica.

Todo se procesa y representa directamente en el dispositivo del operador, sin depender de un TAK Server ni de misiones Data Sync. La línea de estado superior «Está en:» resume al instante las zonas y NOTAMs activos en la posición GPS o en el punto de referencia designado.

Tres capas cartográficas principales, conmutables de forma independiente:

  Zonas UAS (RD 517/2024)   Zonas geográficas de UAS publicadas oficialmente por
                            ENAIRE conforme al Real Decreto 517/2024, estructuradas
                            en tres agrupaciones: Infraestructuras, Aero y Urbano.
                            Código cromático: Rojo (vuelo prohibido), Naranja
                            (requiere autorización o coordinación previa), Amarillo
                            (sujeto a condiciones operacionales) y Verde (sin
                            restricciones específicas para UAS). Límites verticales
                            en metros AGL o AMSL según la publicación oficial.
                            Se descargan por cuadrículas de 0,25° y se conservan 7
                            días en memoria local.

  NOTAM UAS                 Avisos a los navegantes para operaciones de UAS
                            procedentes de ENAIRE, actualizados cada 30 minutos.
                            Colores: Magenta (actividad UAS en curso o programada),
                            Rojo (restricción de área en vigor) y Violeta (otros
                            avisos operacionales). Se representan en el mapa según
                            sus fechas de validez general; los horarios diarios
                            programados se detallan en el texto para comprobación
                            directa del piloto.

  Espacio aéreo             Espacio aéreo controlado oficial publicado por ENAIRE:
                            CTR, ATZ/FIZ, zonas Prohibidas (P), Restringidas (R),
                            Peligrosas (D), TRA/TSA, RMZ/TMZ y zonas deportivas
                            ENR 5.5. Contornos en verde azulado (teal); únicamente
                            las zonas P y R presentan relleno sólido. Se excluyen
                            volúmenes con suelo en nivel de vuelo (FL) o igual o
                            superior a 5.000 ft. Todo el territorio nacional se
                            descarga en un único bloque y se almacena 7 días.
                            Al tocar el mapa se despliega la selección de zonas bajo
                            el dedo; al pulsar una zona se abre su ficha con clase,
                            límites verticales, servicio ATS y frecuencias de radio
                            (con canal de 8,33 kHz cuando están disponibles; en
                            aeródromos donde ENAIRE omite frecuencias, como Zaragoza,
                            debe consultarse el AIP AD 2.18).

Junto a las capas cartográficas, AeroZona incluye:

  Condiciones METAR         Observaciones meteorológicas de aeródromos procedentes
                            de aviationweather.gov con distintivos coloreados por
                            categoría de vuelo estándar: Verde (VFR), Azul (MVFR),
                            Rojo (IFR) y Magenta (LIFR). La ficha desglosa
                            visibilidad horizontal en km, techo y capas de nubes,
                            viento, temperatura, QNH en hPa y los textos originales
                            completos de los boletines METAR y TAF.

  Geovallas de alerta       Creación de geovallas en ATAK directamente desde la
                            ficha de cualquier zona o restricción para recibir
                            avisos perimetrales. Permite alertar según las
                            altitudes publicadas o sobre la proyección en el suelo.
                            Constituyen copias estáticas en el instante de su
                            creación y se gestionan desde Ajustes > Geovallas.

  Meteorología AEMET        Conexión directa con AEMET OpenData a través del botón
                            «Meteo». Requiere una clave API personal y gratuita
                            configurable en Ajustes de ATAK > Preferencias de
                            herramientas > AeroZona > AEMET (con guía PDF paso a
                            paso). Muestra la estación AEMET más cercana (hasta
                            60 km) con viento sostenido, rachas, temperatura y
                            visibilidad, junto con la predicción horaria municipal a
                            12 horas en hora local (incluidas las Islas Canarias).
                            Las rachas de 10 m/s (36 km/h) o más se señalan con ⚠.
                            Caché local: observaciones 30 min, predicción 60 min.

  Plan de vuelo UAS         Pantalla propia accesible desde «Plan de vuelo UAS».
                            Resuelve dos preguntas operativas clave: qué techo
                            requiere la misión y qué afección causa un techo
                            asignado. Permite fijar el despegue por GPS o sobre el
                            mapa, definiendo el área como círculo o polígono
                            dibujado. Preajustes de altura sobre el terreno (AGL) de
                            30, 60, 90 y 120 m (límite de categoría abierta según
                            RD 517/2024). «Calcular techo» suma la cota máxima del
                            terreno y la altura de vuelo, redondeando a los
                            siguientes 100 ft. «Techo asignado» evalúa el margen
                            disponible. «Islas» resalta en rojo el relieve que
                            supera el techo y en azul el admisible (requiere
                            cartografía de elevación DTED; si no hay elevación, se
                            indica claramente y no se calcula). El botón «ENAIRE /
                            AESA» ofrece accesos directos a ENAIRE Drones, ENAIRE
                            Planea y AESA Drones, y «Resumen de la operación» genera
                            un texto formateado listo para copiar y pegar en ENAIRE
                            Planea.

Todo lo descargado se conserva localmente en el dispositivo durante 7 días y permanece accesible sin cobertura. Ocultar una capa no borra sus datos.

_________________________________________________________________
FUENTES DE DATOS Y TITULARIDAD

AeroZona consulta y procesa información pública de las siguientes fuentes:

  ENAIRE servAIS            Zonas geográficas de UAS, avisos NOTAM UAS y espacio
                            aéreo controlado. ENAIRE es el titular de los derechos
                            de estos datos.

  aviationweather.gov       Observaciones meteorológicas de superficie METAR y
                            pronósticos terminales de aeródromo TAF.

  AEMET OpenData            Observaciones de estaciones meteorológicas en tiempo
                            real y predicción horaria municipal. Información
                            elaborada por la Agencia Estatal de Meteorología
                            (© AEMET), consultada mediante la clave API personal
                            del propio usuario.

  Wikidata                  Relación de municipios y códigos geográficos para la
                            localización de predicciones municipales (CC0).

_________________________________________________________________
CARÁCTER CONSULTIVO Y FUENTES OFICIALES

La información mostrada por AeroZona tiene carácter estrictamente consultivo y de asistencia al vuelo. AeroZona no es una fuente oficial ni garantiza la exactitud o actualización en tiempo real de los datos. No constituye una autorización de vuelo ni exime de la obligación del operador o piloto a distancia de verificar los avisos y coordinaciones reglamentarias.

Consulte siempre los canales y publicaciones oficiales aeronáuticas de España:

  AIP España                Publicación de Información Aeronáutica oficial.
  ENAIRE Drones             Mapa oficial de zonas geográficas de UAS (drones.enaire.es).
  Ícaro XXI                 Sistema oficial de gestión de planes de vuelo y NOTAMs.
  AESA                      Agencia Estatal de Seguridad Aérea (seguridadaerea.gob.es).

_________________________________________________________________
PRIVACIDAD Y COMUNICACIONES

Conexión saliente exclusivamente por HTTPS (TCP 443). AeroZona no abre ningún puerto a la escucha, no acepta conexiones entrantes y no transmite datos a ningún TAK Server ni a servidores de terceros.

El complemento es un visor y calculador local: no envía telemetría, no transmite la posición del operador ni identificadores del dispositivo. Las únicas consultas de red son peticiones GET directas por HTTPS a los servicios públicos de ENAIRE, aviationweather.gov y (con la clave personal configurada por el usuario) AEMET OpenData.

Si una consulta falla o el dispositivo se encuentra en zona sin cobertura, AeroZona trabaja con la información conservada en su memoria local y avisa explícitamente en su barra de estado de las fuentes que no se han podido actualizar.

_________________________________________________________________
REQUISITOS DE EQUIPO

  Dispositivo               Dispositivo Android compatible con ATAK-CIV 5.8.0.
  Conectividad              Acceso a Internet para la descarga inicial de datos;
                            posteriormente opera sin red durante 7 días con la
                            información en caché.
  Elevación (DTED)          El planificador de vuelo UAS utiliza la elevación
                            cargada en ATAK (DTED2 o superior). Si faltan datos en
                            la zona, el complemento lo indica explícitamente y
                            evita calcular cotas sobre modelos no verificados.

_________________________________________________________________
COMPILACIÓN

Proyecto Gradle independiente compilado contra el SDK de ATAK-CIV 5.8.0, estructurado a partir del plugintemplate del SDK.

  ./gradlew assembleCivDebug
  ./gradlew assembleCivRelease -PbuildManual

El archivo local.properties debe definir sdk.path apuntando a la raíz del SDK de ATAK-CIV compatible con ext.ATAK_VERSION en app/build.gradle. Véase template.local.properties.

El SDK de ATAK-CIV suministrado por tak.gov nunca se incluye ni se versiona en el repositorio (su licencia prohíbe la redistribución de main.jar, atak.apk, atak-gradle-takdev.jar y el keystore de desarrollo).

_________________________________________________________________
CRÉDITOS Y LICENCIA

AeroZona es una versión modificada de AirAware de TAKWERX (https://github.com/takwerx/air-aware).

AeroZona es software libre publicado bajo los términos de la
GNU Affero General Public License v3.0 o posterior (AGPL-3.0-or-later), con un
permiso adicional para el software TAK (LICENSE-EXCEPTION.md) que autoriza su
combinación y distribución junto con ATAK y el SDK de TAK.

Copyright (C) 2026 the AeroZona contributors.
Copyright (C) 2026 Andreas Johansson (TAKWERX).

Los archivos derivados del plugintemplate del TAK-CIV SDK permanecen sujetos a la
licencia del TAK Product Center; véase LICENSE-EXCEPTION.md para el desglose de
procedencia. Los datos aeronáuticos y meteorológicos pertenecen a sus respectivos
organismos titulares (ENAIRE, AEMET).

Para colaborar o enviar informes, véase CONTRIBUTING.md.

_________________________________________________________________
PURPOSE AND CAPABILITIES (ENGLISH)

AeroZona provides immediate aeronautical situational awareness for civil drone (UAS) operators and remote pilots in Spain: what airspace zones and restrictions coincide with your operating position, and what regulatory and operational conditions apply to the column of air you are in.

Specifically designed for civil UAS operations; sporting aviation areas (gliding, parachuting, model aircraft, RPAS) are displayed for aeronautical reference.

Everything is processed and rendered directly on the operator's device, without relying on a TAK Server or Data Sync missions. The top "Está en:" ("You are in:") status line instantly summarizes active zones and NOTAMs at your GPS position or designated reference point.

Three primary cartographic layers, each independently switched:

  UAS zones (RD 517/2024)   Official Spanish UAS geographic zones published by
                            ENAIRE pursuant to Royal Decree 517/2024 (RD 517/2024),
                            categorized into three groups: Infrastructures,
                            Aviation, and Urban. Color coding: Red (flight
                            prohibited), Orange (prior authorization or
                            coordination mandatory), Yellow (operational conditions
                            apply), and Green (free of specific UAS restrictions).
                            Vertical limits are expressed in metres AGL or AMSL as
                            published. Downloaded in 0.25° tiles and cached for 7
                            days on device.

  UAS NOTAMs                Notices to Airmen specifically for UAS operations
                            published by ENAIRE, refreshed every 30 minutes.
                            Colors: Magenta (active or scheduled UAS activity),
                            Red (airspace restriction in effect), and Violet
                            (other operational notices). Rendered during their
                            general validity dates; daily operating hours are shown
                            in text for direct pilot verification.

  Airspace                  Official controlled airspace published by ENAIRE:
                            CTR, ATZ/FIZ, Prohibited (P), Restricted (R),
                            Danger (D), TRA/TSA, RMZ/TMZ, and ENR 5.5 sporting
                            areas. Outlined in teal; only P and R zones carry solid
                            fill. Airspace volumes with floors at a flight level (FL)
                            or at or above 5,000 ft are excluded. Nationwide data
                            downloads in a single pack and caches for 7 days.
                            Tapping the map opens ATAK's item picker; tapping a
                            zone opens its details sheet with class, vertical
                            limits, ATS service, and communication frequencies (with
                            8.33 kHz channel when available; for airfields where
                            ENAIRE does not publish frequencies, such as Zaragoza,
                            consult AIP AD 2.18).

Alongside the map layers, AeroZona includes:

  METAR conditions          Surface weather observations from aviationweather.gov
                            with airfield chips colored by aviation flight
                            category: Green (VFR), Blue (MVFR), Red (IFR), and
                            Magenta (LIFR). The details sheet breaks down
                            horizontal visibility in km, cloud ceiling and layers,
                            wind, temperature, QNH in hPa, and complete raw METAR
                            and TAF bulletins.

  Alert geofences           Create ATAK geofences directly from any zone or
                            restriction sheet to receive proximity alerts. Conforms
                            to published altitude limits or ground footprint.
                            Geofences capture a static snapshot at creation time and
                            are managed under Settings > Geofences.

  AEMET weather             Direct connection to AEMET OpenData via the "Meteo"
                            button. Requires a free personal API key configured in
                            ATAK Settings > Tool Preferences > AeroZona > AEMET
                            (includes a step-by-step PDF guide). Reports the nearest
                            AEMET weather station (within 60 km) with sustained
                            wind, peak gusts, temperature, and visibility, alongside
                            a 12-hour hourly municipal forecast in local time
                            (Canary Islands included). Gusts of 10 m/s (36 km/h) or
                            more are flagged with ⚠. Cache: observations 30 min,
                            forecasts 60 min.

  UAS flight planner        Dedicated screen opened from "Plan de vuelo UAS".
                            Solves two vital coordination questions: what ceiling
                            does the mission require, and what clearance does an
                            assigned ceiling provide. Set launch point by GPS or
                            map tap; mission area can be a circular buffer or drawn
                            polygon. Above ground level (AGL) presets: 30, 60, 90,
                            and 120 m (open category ceiling under RD 517/2024).
                            "Calculate ceiling" sums maximum terrain elevation and
                            flight height, rounding up to the next 100 ft. "Assigned
                            ceiling" evaluates clearance. "Islands" colors terrain
                            breaching ceiling in red and safe ground in blue
                            (requires DTED elevation; when missing, clearly warns
                            and leaves unpainted). The "ENAIRE / AESA" button links
                            to official portals (ENAIRE Drones, ENAIRE Planea, AESA
                            Drones), and "Operation summary" formats coordinates and
                            ceilings ready to copy into ENAIRE Planea.

All downloaded data is retained locally on device for 7 days and remains fully usable offline. Switching a layer off hides it without removing data.

_________________________________________________________________
DATA SOURCES AND OWNERSHIP

AeroZona retrieves and displays public information from the following official sources:

  ENAIRE servAIS            UAS geographic zones, UAS NOTAMs, and controlled
                            airspace. ENAIRE es el titular de los derechos de estos
                            datos (ENAIRE holds intellectual property rights to
                            this data).

  aviationweather.gov       Surface METAR observations and TAF terminal forecasts.

  AEMET OpenData            Real-time weather station observations and hourly
                            municipal forecasts. Produced by the Spanish State
                            Meteorological Agency (© AEMET), queried using the
                            user's personal API key.

  Wikidata                  Municipality records and geographic identifiers for
                            local forecast mapping (CC0).

_________________________________________________________________
ADVISORY DISCLAIMER AND OFFICIAL SOURCES

All information displayed by AeroZona is strictly advisory and intended for situational awareness assistance. AeroZona is not an official aeronautical source and makes no warranty of accuracy or real-time timeliness. It does not constitute a flight clearance or replace mandatory pilot verifications.

Always verify current conditions through Spain's official aeronautical publications and platforms:

  AIP España                Official Aeronautical Information Publication.
  ENAIRE Drones             Official UAS geographic zones portal (drones.enaire.es).
  Ícaro XXI                 Official flight plan and NOTAM management system.
  AESA                      State Aviation Safety Agency (seguridadaerea.gob.es).

_________________________________________________________________
PRIVACY AND NETWORK

Outbound HTTPS (TCP 443) only. AeroZona opens no listening ports, accepts no inbound connections, and transmits no data to any TAK Server or external entities.

The plugin acts strictly as a reader: it transmits no telemetry, no operator location, and no device identifiers. Network activity is limited to direct HTTPS GET requests for public data from ENAIRE, aviationweather.gov, and (with the user's personal key) AEMET OpenData.

When network connectivity is unavailable, AeroZona continues operating from cached data and explicitly indicates in its status line any source that could not be refreshed.

_________________________________________________________________
EQUIPMENT REQUIRED

  Device                    Android device running ATAK-CIV 5.8.0.
  Connectivity              Network access for initial tile and restriction
                            downloads; operates offline for 7 days thereafter.
  Elevation (DTED)          The flight planner uses DTED elevation data loaded in
                            ATAK (DTED2 or finer). If elevation data is missing for
                            an area, the plugin states this clearly and avoids
                            unverified computations.

_________________________________________________________________
COMPILATION

A standalone Gradle project built against the ATAK-CIV 5.8.0 SDK, scaffolded from the SDK's plugintemplate sample.

  ./gradlew assembleCivDebug
  ./gradlew assembleCivRelease -PbuildManual

local.properties must define sdk.path pointing to the ATAK-CIV SDK matching ext.ATAK_VERSION in app/build.gradle (see template.local.properties).

The ATAK-CIV SDK distributed by tak.gov is never committed to git (its license prohibits redistributing main.jar, atak.apk, atak-gradle-takdev.jar, and the debug keystore).

_________________________________________________________________
CREDITS AND LICENSE

AeroZona es una versión modificada de AirAware de TAKWERX (https://github.com/takwerx/air-aware).

AeroZona is free software released under the GNU Affero General Public License v3.0 or later (AGPL-3.0-or-later), with an additional permission for the TAK Software (LICENSE-EXCEPTION.md) allowing it to be linked with and distributed alongside ATAK and the TAK SDK.

Copyright (C) 2026 the AeroZona contributors.
Copyright (C) 2026 Andreas Johansson (TAKWERX).

Files originating from the TAK-CIV SDK plugintemplate remain under the TAK Product Center license; see LICENSE-EXCEPTION.md for details. Aeronautical and meteorological data belong to their respective agencies (ENAIRE, AEMET).

For contribution guidelines and issue reporting, see CONTRIBUTING.md.
