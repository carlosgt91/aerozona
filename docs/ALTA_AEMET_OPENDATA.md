# Cómo obtener tu clave de AEMET OpenData

La meteorología de AirAware ES (predicción horaria del municipio y avisos
Meteoalerta) llega de **AEMET OpenData**, el servicio de datos abiertos de la
Agencia Estatal de Meteorología. Es gratuito, pero AEMET exige que **cada usuario
tenga su propia clave** (API key). Sin ella el plugin funciona igual para zonas,
frecuencias y METAR/TAF; solo falta la parte de AEMET.

> **Borrador.** Pasos pendientes de comprobar con un alta real (07-11-2026).

## 1. Pedir la clave (5 minutos, desde el móvil o el PC)

1. Abre **https://opendata.aemet.es/centrodedescargas/altaUsuario**
2. Escribe tu **correo electrónico** y resuelve la comprobación (captcha).
3. Pulsa **Enviar**.
4. Recibirás un correo de AEMET con un enlace de confirmación. Ábrelo.
5. Recibirás un segundo correo con la **API key**: una cadena larga de letras,
   números y puntos que empieza por `eyJ`.

Si no llega, mira en la carpeta de correo no deseado.

## 2. Ponerla en ATAK

1. En el móvil, abre el correo de AEMET, mantén pulsada la clave y **Copiar**.
   Cópiala entera: es larga y no debe faltar ningún carácter.
2. En ATAK: **Ajustes → Preferencias de herramientas → AirAware ES →
   Clave de AEMET OpenData**.
3. Pega la clave y acepta. El plugin la comprueba al momento y te dice si es
   válida.

La clave se guarda solo en tu dispositivo. No se envía al servidor TAK ni a
otros usuarios.

## 3. Buenas prácticas

- La clave es **personal**: no la compartas ni la pongas en paquetes de datos o
  perfiles de MDM comunes.
- Si cambias de móvil, vuelve a pegarla (o pide una nueva con el mismo correo).
- Los datos son de AEMET: al compartir capturas o informes, cita a AEMET como
  fuente.

## ¿Y AEMET AMA?

**AMA** (Autoservicio Meteorológico Aeronáutico, `ama.aemet.es`) es el portal de
meteorología aeronáutica de AEMET: METAR, TAF, SIGMET, GAMET/AIRMET de baja cota
y mapas. Está **restringido a usuarios aeronáuticos con licencia de vuelo**, se
usa por navegador y **no ofrece API**, así que el plugin no puede integrarlo. Si
tienes licencia, úsalo aparte para la planificación. AirAware ES toma los
METAR/TAF de los aeródromos españoles de una fuente pública sin clave.
