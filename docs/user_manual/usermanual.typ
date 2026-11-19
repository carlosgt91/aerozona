#import "@preview/polylux:0.4.0": *
#import "formatting.typ": *

#show: userguide.with(
  plugin-name: "AeroZona",
  plugin-version: "1.0.6",
  platform: "ATAK",
  platform-version: "5.8.0",
)

#tak-slide[
= Visión general

#toolbox.side-by-side(columns: (6fr, 6fr))[
  #image("az/panel.jpg", width: 100%)
][
  AeroZona muestra en su pantalla la situación aeronáutica sobre el terreno: *qué
  zonas y restricciones coinciden con su posición y qué condiciones aplican*.

  Abra AeroZona desde el menú de herramientas de ATAK. El panel se abre a media
  anchura. La línea superior *«Está en:»* resume las zonas y NOTAM en su
  posición o en el punto de referencia.

  Diseñado para pilotos civiles de drones (UAS). Las zonas deportivas se
  muestran a título de referencia.
]
]

#tak-slide[
= Antes de empezar

- *Carácter exclusivamente consultivo.* AeroZona no es una fuente oficial ni
  constituye una autorización de vuelo. Consulte siempre las fuentes oficiales:
  AIP España, ENAIRE Drones (`drones.enaire.es`), Ícaro XXI para NOTAM y AESA.
- *Derechos sobre los datos.* La información aeronáutica es titularidad de
  ENAIRE; la información meteorológica es © AEMET; los informes METAR y TAF
  proceden de `aviationweather.gov`.
- *Privacidad.* Nada se transmite a un TAK Server ni a otros usuarios. El
  teléfono únicamente consulta a ENAIRE, aviationweather.gov y, con su clave
  personal, a AEMET.
- *Unidades de medida.* Se adoptan las unidades configuradas en ATAK (distancia
  y altitud). Se recomienda configurar ATAK en sistema métrico.
]

#tak-slide[
= El mapa y las capas

#toolbox.side-by-side(columns: (6fr, 6fr))[
  #image("az/mapa.jpg", width: 100%)
][
  Tres capas cartográficas independientes: *Zonas UAS*, *NOTAM UAS* y *Espacio
  aéreo*. Cada una dispone de conmutador propio y opciones bajo la flecha.

  Los accesos *Todas* y *Ninguna* activan o despejan la cartografía con un
  toque.

  *Operación sin cobertura.* Las zonas se descargan por cuadrículas de 0,25° y se
  conservan 7 días (ciclo AIRAC de 28 días). Una zona visualizada previamente
  permanece accesible sin conexión.
]
]

#tak-slide[
= Zonas UAS (RD 517/2024)

Zonas geográficas de UAS publicadas por ENAIRE conforme al Real Decreto 517/2024,
estructuradas en tres agrupaciones: *Infraestructuras*, *Aero* y *Urbano*.

- *Rojo:* Vuelo prohibido.
- *Naranja:* Requiere autorización previa de la autoridad u operador afectado.
- *Amarillo:* Sujeto al cumplimiento de condiciones o requisitos operacionales.
- *Verde:* Sin restricciones específicas para UAS.

Los límites verticales se indican en metros AGL (sobre el terreno) o AMSL (sobre
el nivel del mar) según los publica oficialmente ENAIRE. Se descargan por bloques
de 0,25° y se conservan 7 días en el dispositivo.
]

#tak-slide[
= NOTAM UAS

Avisos a los navegantes específicos para operaciones de UAS procedentes de
ENAIRE, actualizados cada 30 minutos.

- *Magenta:* Actividad de UAS en curso o programada.
- *Rojo:* Restricción de área en vigor.
- *Violeta:* Otros avisos aeronáuticos.

Se representan en el mapa únicamente durante sus fechas de validez general. El
horario diario programado (por ejemplo, LUN-VIE 0600-1400) se indica en el texto
pero no se computa; verifique siempre el horario concreto antes del vuelo.
]

#tak-slide[
= Espacio aéreo

#toolbox.side-by-side(columns: (6fr, 6fr))[
  #image("az/seleccion.jpg", width: 100%)
][
  Espacio aéreo oficial de ENAIRE: CTR, ATZ/FIZ, zonas P, R, D, TRA/TSA, RMZ/TMZ
  y zonas deportivas ENR 5.5 (paracaidismo, veleros, aeromodelos, RPAS).

  Contorno verde azulado (*teal*); solo P y R van rellenas. Se excluyen zonas con
  suelo en nivel de vuelo (FL) o igual o superior a 5.000 ft. Todo el país se
  descarga a la vez y se guarda 7 días.

  Al tocar el mapa, ATAK despliega *«Seleccione un elemento»* con todas las zonas
  coincidentes bajo el dedo.
]
]

#tak-slide[
= Ficha de espacio aéreo

#toolbox.side-by-side(columns: (6fr, 6fr))[
  #image("az/ficha_atz.jpg", width: 100%)
][
  Toque cualquier zona para ver su ficha: clase, límites superior e inferior,
  servicio ATS y frecuencias de radio.

  Cuando se dispone de frecuencia, se detalla la frecuencia de sintonía y su
  correspondiente canal de 8,33 kHz.

  *Importante:* ENAIRE no publica la frecuencia de muchos CTR y ATZ (como
  Zaragoza); consulte en ese caso el AIP AD 2.18 de dicho aeródromo. Cada ficha
  indica la fuente y el titular de los datos.
]
]

#tak-slide[
= Condiciones METAR

#toolbox.side-by-side(columns: (6fr, 6fr))[
  #image("az/metar.jpg", width: 100%)
][
  Distintivo por aeródromo coloreado según la categoría de vuelo aeronáutica:
  *Verde* VFR, *Azul* MVFR, *Rojo* IFR y *Magenta* LIFR.

  Proceden de `aviationweather.gov`. Al tocar la ficha se detalla: visibilidad en
  km, techo y capas de nubes, viento, temperatura, QNH en hPa y hora de
  observación.

  Incluye los textos íntegros originales de los boletines METAR y TAF para
  comprobación directa del piloto.
]
]

#tak-slide[
= Meteorología AEMET

#toolbox.side-by-side(columns: (6fr, 6fr))[
  #image("az/meteo.jpg", width: 100%)
][
  El botón *Meteo* conecta con AEMET OpenData. Requiere una clave API personal y
  gratuita, configurable en *Ajustes de ATAK > Preferencias de herramientas >
  AeroZona > AEMET* (con guía PDF para obtenerla).

  Arriba aparecen los avisos Meteoalerta de AEMET (amarillo, naranja o rojo)
  que cubren el punto de medida, el más grave primero, con inicio, fin y
  descripción. Es el mismo punto que el del resto del panel (*Ajustes > Medir
  desde*): su posición, o el centro del mapa si lo eligió o no hay señal GNSS;
  la página lo indica arriba.

  Muestra la estación AEMET más cercana (hasta 60 km) con viento, rachas,
  temperatura y visibilidad, junto con la predicción horaria municipal a 12 horas
  en hora local (incluidas Canarias).

  Rachas de 10 m/s (36 km/h) o más se marcan con ⚠. Caché: avisos 15 min,
  observaciones 30 min, predicción 60 min.
]
]

#tak-slide[
= Ajustes del panel

#toolbox.side-by-side(columns: (6fr, 6fr))[
  #image("az/ajustes.jpg", width: 100%)
][
  Desde el botón *Ajustes* del panel principal:

  - *Medir desde:* calcula distancias y la línea «Está en:» desde su posición GPS
    o desde un punto designado en el mapa.
  - *Leyenda del mapa:* relación de todos los colores y símbolos activos.
  - *Actualizaciones:* frecuencia con la que se renuevan los datos en el
    dispositivo.
]
]

#tak-slide[
= Plan de vuelo UAS

#toolbox.side-by-side(columns: (6fr, 6fr))[
  #image("az/plan.jpg", width: 100%)
][
  Pantalla propia abierta desde *Plan de vuelo UAS*. Responde a dos cuestiones
  ante la coordinación: *qué techo requiere la misión* y *qué afección causa un
  techo asignado*.

  Punto de despegue por GPS o tocando el mapa; se borra con la papelera de su
  menú radial o con *Borrar todo*, que limpian el plan entero. El área es un círculo o un
  polígono dibujado. Altura sobre el terreno (AGL): preajustes de 30, 60, 90 y
  120 m (límite de categoría abierta según RD 517/2024; una zona UAS puede
  restringir a menos).

  *Calcular techo* suma la cota máxima del terreno y la altura de vuelo,
  redondeando a los siguientes 100 ft. *Techo asignado* evalúa el límite
  concedido. *Islas* colorea en rojo el terreno que excede el techo y en azul el
  admisible. Requiere elevación (DTED).
]
]

#tak-slide[
= Ajustes del planificador

#toolbox.side-by-side(columns: (6fr, 6fr))[
  #image("az/plan_ajustes.jpg", width: 100%)
][
  Configuración específica del cálculo de misión:

  - *Altura sobre el terreno:* define la cota que se mantendrá sobre el relieve en
    todo momento.
  - *Zona:* radio del círculo de operación en torno al punto de despegue.
  - *Leyenda:* simbología cromática de las curvas de análisis de terreno e islas
    críticas.

  Todos los valores configurados se conservan entre sesiones sin necesidad de
  conexión.
]
]

#tak-slide[
= ENAIRE, AESA y tramitación

#toolbox.side-by-side(columns: (6fr, 6fr))[
  #image("az/plan_enaire.jpg", width: 100%)
][
  El botón *ENAIRE / AESA* conecta con las plataformas oficiales:

  - *ENAIRE Drones:* mapa oficial de zonas geográficas de UAS.
  - *ENAIRE Planea:* tramitación formal de solicitudes de coordinación de vuelo.
  - *AESA Drones:* normativa, autorizaciones de operador y requisitos.

  *Resumen de la operación:* genera un texto con coordenadas de despegue,
  superficie del área y alturas para copiar y pegar en ENAIRE Planea. No tramita
  ni concede autorizaciones por sí mismo.
]
]

#tak-slide[
= Operación sin datos o sin red

- *Sin datos de elevación (DTED).* El planificador lee los modelos de elevación
  alojados en ATAK. Si faltan datos en la zona, el complemento lo advierte
  abiertamente y deja el terreno sin pintar, evitando calcular techos irreales
  sin datos. Cargue la cartografía DTED para habilitar el análisis.
- *Sin conexión de red.* Las zonas UAS y el espacio aéreo descargados permanecen
  7 días en memoria. Si una consulta falla o no hay señal, el mapa no se borra.
- *Información explícita.* La barra de estado indica en texto claro qué fuentes
  no han podido contactarse. Un mapa despejado no presupone un cielo libre de
  restricciones.
]

#tak-slide[
= Avisos y fuentes oficiales

- *Carácter consultivo.* Los datos mostrados reflejan la información publicada en
  el momento de su descarga. No sustituyen los avisos aeronáuticos oficiales, el
  AIP ni las autorizaciones de coordinación con proveedores ATS o AESA.
- *Fuentes oficiales:* AIP España, ENAIRE Drones (`drones.enaire.es`), Ícaro XXI
  para NOTAM y portal de AESA (`seguridadaerea.gob.es`).
- *Propiedad.* Datos aeronáuticos propiedad de ENAIRE. Meteorología © AEMET.
  Datos METAR/TAF procedentes de `aviationweather.gov`.
- *Privacidad.* No se comparten trazas ni ubicaciones con ningún TAK Server ni
  con terceros.
]

#tak-slide[
= English

The complete user manual for the AeroZona ATAK plugin in English follows below.

Screenshots illustrate the plugin with its Spanish user interface. Corresponding
English labels and interface options are detailed alongside each feature.
]

#tak-slide[
= Overview

#toolbox.side-by-side(columns: (6fr, 6fr))[
  #image("az/panel.jpg", width: 100%)
][
  AeroZona provides immediate situational awareness for the column of air you are
  operating in: *which geographic zones and NOTAMs apply, and what rules govern
  the airspace*.

  Open AeroZona from the ATAK Tools menu; the pane opens at half width. The top
  *You are in* ("Está en:") line names active zones and NOTAMs at your GPS
  position or chosen reference point.

  Built for civil drone (UAS) pilots; sporting zones (gliders, parachuting, model
  aircraft) are included for reference.
]
]

#tak-slide[
= Before you start

- *Advisory only.* AeroZona is not an official source and is not a flight
  clearance. Always verify current information with official authorities: AIP
  España, ENAIRE Drones (`drones.enaire.es`), Ícaro XXI for NOTAMs, and AESA.
- *Data ownership.* Aeronautical information is owned by ENAIRE; weather data is
  © AEMET; METAR/TAF observations are retrieved from `aviationweather.gov`.
- *Privacy.* Nothing is transmitted to any TAK Server or shared with other
  users. Your device queries only ENAIRE, aviationweather.gov, and (with your key)
  AEMET.
- *Units.* Distances and altitudes follow ATAK's display preferences. Setting
  ATAK to metric units is recommended.
]

#tak-slide[
= The map and layers

#toolbox.side-by-side(columns: (6fr, 6fr))[
  #image("az/mapa.jpg", width: 100%)
][
  Three primary map layers: *UAS zones* ("Zonas UAS"), *UAS NOTAMs* ("NOTAM UAS"),
  and *Airspace* ("Espacio aéreo"). Each can be toggled individually.

  *All on* ("Todas") and *All off* ("Ninguna") buttons show or clear all layers
  with a single tap. The arrow on each row accesses its specific options.

  *Offline caching.* Zones download in 0.25° tiles around the viewed area and are
  stored for 7 days (aligned with the 28-day AIRAC cycle). An area viewed once
  remains fully available without cellular network.
]
]

#tak-slide[
= UAS zones (RD 517/2024)

Spanish UAS geographic zones published by ENAIRE pursuant to Royal Decree
517/2024 (RD 517/2024), classified into three official groups: *Infrastructures*
(Infraestructuras), *Aviation* (Aero), and *Urban* (Urbano).

- *Red:* Prohibited to UAS flights.
- *Orange:* Prior authorization or coordination mandatory.
- *Yellow:* Specific conditions and operational requirements apply.
- *Green:* Free of specific UAS restrictions.

Vertical limits are stated in metres AGL (above ground level) or AMSL (above mean
sea level) as published by ENAIRE. Cached on device for 7 days.
]

#tak-slide[
= UAS NOTAMs

Notices to Airmen for UAS operations published by ENAIRE, refreshed
automatically every 30 minutes.

- *Magenta:* Active or scheduled UAS activity.
- *Red:* Airspace restriction in force.
- *Violet:* Other notices and operational warnings.

Drawn on the map only within their published effective calendar dates. The daily
operating schedule (e.g. MON-FRI 0600-1400) is shown in the text but not
evaluated programmatically; always check the schedule before flying.
]

#tak-slide[
= Airspace

#toolbox.side-by-side(columns: (6fr, 6fr))[
  #image("az/seleccion.jpg", width: 100%)
][
  Comprehensive Spanish controlled airspace: CTR, ATZ/FIZ, Prohibited (P),
  Restricted (R), Danger (D), TRA/TSA, RMZ/TMZ, and ENR 5.5 sporting activities
  (parachuting, gliding, model aircraft, RPAS).

  Outlined in teal; only P and R zones have solid color fill. Volumes with floors
  at a flight level (FL) or at or above 5,000 ft are excluded. Nationwide data
  downloads at once and caches for 7 days.

  Tapping the map opens ATAK's *Select an item* ("Seleccione un elemento")
  listing every zone beneath your finger.
]
]

#tak-slide[
= Airspace details

#toolbox.side-by-side(columns: (6fr, 6fr))[
  #image("az/ficha_atz.jpg", width: 100%)
][
  Tap any listed airspace feature to inspect its details sheet: airspace class,
  upper and lower vertical limits, service provided, and radio frequencies.

  When available, the communication frequency and its 8.33 kHz channel spacing
  are clearly indicated.

  *Important:* ENAIRE does not publish radio frequencies for many CTRs and ATZs
  (such as Zaragoza); consult AIP AD 2.18 for that aerodrome. Every sheet names
  the data source and owner (ENAIRE).
]
]

#tak-slide[
= METAR conditions

#toolbox.side-by-side(columns: (6fr, 6fr))[
  #image("az/metar.jpg", width: 100%)
][
  Each airfield shows a flight-category chip per aviation standards from
  `aviationweather.gov`: *Green* VFR, *Blue* MVFR, *Red* IFR, and *Magenta* LIFR.

  Tap a chip for surface conditions: horizontal visibility in km, cloud ceiling
  and cloud layers, wind direction and velocity, temperature, and QNH in hPa.

  Includes observation timestamps and the raw, unedited METAR and TAF text for
  direct pilot verification.
]
]

#tak-slide[
= AEMET weather

#toolbox.side-by-side(columns: (6fr, 6fr))[
  #image("az/meteo.jpg", width: 100%)
][
  The *Weather* ("Meteo") button queries AEMET OpenData. It requires a free
  personal API key entered in *ATAK Settings > Tool Preferences > AeroZona >
  AEMET* (where a PDF guide explains how to acquire it).

  At the top it lists the AEMET Meteoalerta warnings (yellow, orange or red)
  covering your position, worst first, with start, end and description.

  Reports the nearest AEMET weather station within 60 km: sustained wind, peak
  gusts, temperature, and visibility, alongside 12-hour hourly municipal forecasts
  in local time (Canary Islands included).

  Gusts of 10 m/s (36 km/h) or higher are flagged with ⚠. Warnings cache for
  15 minutes, surface observations for 30 minutes, forecasts for 60 minutes.
]
]

#tak-slide[
= Settings

#toolbox.side-by-side(columns: (6fr, 6fr))[
  #image("az/ajustes.jpg", width: 100%)
][
  Accessed from the *Settings* ("Ajustes") button on the main pane:

  - *Measure from* ("Medir desde"): calculates distances and the "You are in"
    status line from your GPS position or a selected map coordinate.
  - *Map key* ("Leyenda del mapa"): index of all active colors and layer symbols.
  - *Updates* ("Actualizaciones"): set the frequency for background restriction
    and weather data updates.
]
]

#tak-slide[
= UAS flight plan

#toolbox.side-by-side(columns: (6fr, 6fr))[
  #image("az/plan.jpg", width: 100%)
][
  Opened via *UAS Flight Plan* ("Plan de vuelo UAS"). Solves two vital questions
  for air coordination: *what ceiling does the mission require*, and *how does an
  assigned ceiling affect the flight*.

  Set a launch point via GPS or tap the map. Area can be a circular buffer or a
  drawn shape. Height above terrain (AGL) presets: 30, 60, 90, and 120 m (open
  category ceiling under RD 517/2024; local zones may specify less).

  *Calculate ceiling* finds highest terrain in the area + flight height, rounded
  up to next 100 ft. *Assigned ceiling* checks clearance against assigned limits.
  *Islands* highlights ground breaching ceiling in red; safe ground in blue.
  Requires elevation data (DTED).
]
]

#tak-slide[
= Planner settings

#toolbox.side-by-side(columns: (6fr, 6fr))[
  #image("az/plan_ajustes.jpg", width: 100%)
][
  Configure mission analysis variables from planner settings:

  - *Height above terrain* ("Altura sobre el terreno"): select flight clearance
    above the ground profile.
  - *Zone* ("Zona"): adjust radius of circular mission buffer around the launch
    coordinate.
  - *Map key* ("Leyenda"): color definitions for terrain elevation and critical
    relief islands.

  All flight planner preferences persist across app restarts without network.
]
]

#tak-slide[
= ENAIRE, AESA and coordination

#toolbox.side-by-side(columns: (6fr, 6fr))[
  #image("az/plan_enaire.jpg", width: 100%)
][
  The *ENAIRE / AESA* button opens direct links to official flight portals:

  - *ENAIRE Drones:* official interactive map of Spanish UAS geographic zones.
  - *ENAIRE Planea:* official portal for submitting and managing air coordination
    requests.
  - *AESA Drones:* aviation safety agency portal for rules and certifications.

  *Operation summary* ("Resumen de la operación"): formats launch point, mission
  area, and ceilings into text ready to copy into ENAIRE Planea. This function
  does not grant or file flight clearances.
]
]

#tak-slide[
= Missing data and offline operation

- *Missing elevation data (DTED).* The planner depends on DTED elevation loaded
  in ATAK. When missing for an area, the plugin states this clearly and leaves
  terrain unpainted rather than computing erroneous altitudes. Load DTED elevation
  files to enable terrain analysis.
- *Offline operation.* Downloaded UAS zones and airspace stay on the device for
  7 days. If network connectivity drops or a fetch fails, existing map features
  remain intact.
- *Clear status.* The status line explicitly names any data sources that could
  not be refreshed, ensuring you know exactly what information is current.
]

#tak-slide[
= Official sources and legal notices

- *Advisory nature.* All presented data is strictly for situational awareness and
  flight planning assistance. It does not replace official aeronautical
  publications or mandatory air coordination with ATS providers or AESA.
- *Official sources:* AIP España, ENAIRE Drones (`drones.enaire.es`), Ícaro XXI
  for NOTAMs, and AESA (`seguridadaerea.gob.es`).
- *Intellectual property:* Aeronautical and zone data © ENAIRE. Weather data
  © AEMET. METAR/TAF observations courtesy of `aviationweather.gov`.
- *Privacy.* No position, tracking, or device telemetry is ever transmitted to a
  TAK Server or external entities.
]
