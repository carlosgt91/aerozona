#set document(title: "AeroZona: clave de AEMET OpenData")
#set page(paper: "a4", margin: 2cm)
#set text(lang: "es", size: 11pt)
#set heading(numbering: none)
#show heading.where(level: 1): set text(size: 18pt)

= Cómo obtener tu clave de AEMET OpenData

La meteorología de AeroZona (predicción por horas, observaciones de la estación más cercana y avisos) llega de *AEMET OpenData*, el servicio de datos abiertos de la Agencia Estatal de Meteorología. Es *gratuito*, pero AEMET exige que *cada usuario tenga su propia clave*. Sin clave, AeroZona funciona igual para zonas UAS, NOTAM, espacio aéreo y METAR/TAF; solo falta la parte de AEMET.

== 1. Pedir la clave (5 minutos)

+ Abre #link("https://opendata.aemet.es/centrodedescargas/altaUsuario")[*opendata.aemet.es/centrodedescargas/altaUsuario*] en el móvil o en el PC.
+ Escribe tu *correo electrónico* y resuelve la comprobación.
+ Pulsa *Enviar*.
+ AEMET te envía un correo con un enlace de confirmación: ábrelo.
+ Recibirás un segundo correo con la *clave (API key)*: una cadena larga que empieza por `eyJ`.

Si no llega, mira en la carpeta de correo no deseado.

== 2. Ponerla en ATAK

+ En el móvil, abre el correo de AEMET, mantén pulsada la clave y elige *Copiar*. Cópiala entera.
+ En ATAK: *Ajustes → Preferencias de herramientas → AeroZona → Clave de AEMET OpenData*.
+ Pega la clave y acepta.

La clave se guarda solo en tu dispositivo: no se envía al servidor TAK ni a otros usuarios.

== 3. Buenas prácticas

- La clave es *personal*: no la compartas ni la pongas en paquetes de datos o perfiles de MDM comunes.
- Si cambias de móvil, vuelve a pegarla o pide una nueva con el mismo correo.
- Los datos son de AEMET: al compartir capturas o informes, cita a AEMET como fuente.

== ¿Y AEMET AMA?

*AMA* (Autoservicio Meteorológico Aeronáutico, `ama.aemet.es`) es el portal de meteorología aeronáutica de AEMET. Está restringido a usuarios aeronáuticos con licencia de vuelo y no ofrece acceso para aplicaciones, así que AeroZona no lo usa. Si tienes licencia, úsalo aparte para planificar.

#line(length: 100%)

= Getting your AEMET OpenData key (English)

+ Open #link("https://opendata.aemet.es/centrodedescargas/altaUsuario")[opendata.aemet.es/centrodedescargas/altaUsuario], enter your e-mail, solve the check and press *Enviar*.
+ Open the confirmation link in AEMET's first e-mail; the second e-mail carries the key (a long string starting with `eyJ`).
+ In ATAK: *Settings → Tool Preferences → AeroZona → AEMET OpenData API key*, paste it and accept.

The key is personal and stays on the device. Without it AeroZona still shows UAS zones, NOTAMs, airspace and METAR/TAF.
