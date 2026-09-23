Quiero que desarrolles una pequeña aplicación móvil Android que pueda compilarse como una APK y utilizarse en campo para realizar levantamientos técnicos de tanques de producción destinados a la posterior selección/configuración de cabezales de limpieza Alfa Laval.

OBJETIVO

La aplicación debe permitir registrar información técnica de múltiples tanques, organizados por cliente.

La estructura principal debe ser:

CLIENTES
→ Cliente
→ Tanques del cliente
→ Levantamiento técnico de cada tanque

Ejemplo:

Jaraba Import
└── Cliente: Bepensa
├── Tanque 01 – Jarabe
├── Tanque 02 – Producto terminado
└── Tanque 03 – CIP

Otro cliente:

└── Cliente: Goya
├── Tanque 01 – Mayonesa
└── Tanque 02 – Salsa

La aplicación debe funcionar completamente OFFLINE y guardar los datos localmente en el dispositivo.

No quiero depender de un servidor, Firebase, Supabase ni conexión a Internet para registrar o consultar los levantamientos.

---

1. TECNOLOGÍA

Selecciona una tecnología adecuada para generar una APK Android moderna y estable.

Preferiblemente:

- Kotlin
- Jetpack Compose
- Android Studio
- Room para base de datos local
- Material 3

Si consideras que Flutter es técnicamente más conveniente para facilitar una futura versión iOS, puedes utilizar Flutter, pero debes priorizar:

1. APK Android funcional.
2. Funcionamiento offline.
3. Base de datos local.
4. Código mantenible.
5. Facilidad para agregar nuevas funciones posteriormente.

No utilices tecnologías innecesariamente complejas.

La aplicación debe poder compilarse desde el proyecto generado mediante un comando claro.

---

2. ESTRUCTURA DE DATOS

Crear como mínimo las siguientes entidades:

CLIENTE

Campos:

- id
- nombre
- contacto
- teléfono
- email
- dirección
- ciudad
- notas
- fecha de creación
- fecha de actualización

El nombre del cliente debe ser obligatorio.

---

3. TANQUE

Cada tanque pertenece a un cliente.

Campos:

- id
- clienteId
- nombreIdentificador
- códigoTanque
- área/planta
- proceso
- producto
- descripciónProducto
- residuo
- volumenNominal
- volumenTrabajo
- diámetroInterno
- alturaCilindrica
- alturaTotal
- tipoTecho
- tipoFondo
- material
- acabadoInterno
- ubicaciónCabezal
- alturaCabezalSobreFondo
- conexiónCabezal
- diámetroConexión
- posiciónCabezal
- agitador
- diámetroAgitador
- alturaAgitador
- serpentín
- descripciónSerpentín
- baffles
- cantidadBaffles
- otrosObstaculos
- caudalCIP
- presiónCIP
- temperaturaCIP
- concentraciónNaOH
- concentraciónÁcido
- tiempoCIP
- diámetroTuberíaCIP
- longitudTuberíaCIP
- observaciones
- fechaLevantamiento
- técnicoResponsable

---

4. LOS SIETE DATOS PRINCIPALES

La aplicación debe destacar visualmente los siete datos principales que permitirán realizar posteriormente una primera evaluación de selección del cabezal:

1. Producto procesado
2. Residuo/característica del residuo
3. Diámetro interno del tanque
4. Altura del tanque
5. Presión CIP disponible
6. Caudal CIP disponible
7. Obstáculos internos

IMPORTANTE:

"Producto" y "Residuo" son campos diferentes.

Ejemplo:

Producto:
Mayonesa

Residuo:
Película grasa/viscosa adherida a paredes, fondo y agitador después del vaciado.

El campo Residuo debe permitir una descripción libre.

No asumir automáticamente que el residuo es igual al producto.

---

5. CAMPOS GEOMÉTRICOS

Crear una sección llamada:

GEOMETRÍA DEL TANQUE

Campos:

- Diámetro interno
- Altura cilíndrica
- Altura total
- Volumen nominal
- Volumen de trabajo
- Tipo de techo
- Tipo de fondo
- Ángulo del fondo
- Material
- Acabado interno

Unidades:

- Dimensiones: mm
- Volumen: L o m³
- Ángulo: grados

Permitir cambiar las unidades si resulta sencillo, pero internamente guardar valores normalizados.

---

6. PRODUCTO Y RESIDUO

Crear una sección:

PRODUCTO Y RESIDUO

Campos:

Producto procesado

Campo obligatorio.

Descripción del producto

Campo de texto.

Residuo después del vaciado

Campo de texto multilínea.

Características del residuo

Permitir seleccionar uno o varios:

- Fácil de remover
- Pegajoso
- Graso
- Viscoso
- Seco
- Proteico
- Azucarado
- Con sólidos
- Forma película
- Se adhiere a superficies
- Difícil de remover
- Otro

Observaciones

Campo libre.

No hacer diagnósticos automáticos sobre el producto.

---

7. SISTEMA CIP

Crear sección:

PARÁMETROS CIP

Campos:

Caudal disponible

m³/h

Presión disponible en el cabezal

bar

IMPORTANTE:

El usuario debe introducir preferiblemente la presión medida durante CIP en la entrada del cabezal, no simplemente la presión nominal de la bomba.

Temperatura

°C

Concentración NaOH

%

Concentración ácido

%

Tiempo total CIP

min

Diámetro tubería CIP

pulgadas

Longitud aproximada

m

Otros elementos

- filtros
- válvulas
- codos
- restricciones

---

8. CABEZAL

Crear sección:

INSTALACIÓN DEL CABEZAL

Campos:

- ¿Existe cabezal actualmente? Sí / No
- Modelo actual
- Ubicación
- Altura sobre fondo
- Tipo de conexión
- Diámetro de conexión
- Posición:
  - Central
  - Lateral
  - Superior
  - Otra

No intentar seleccionar automáticamente todavía un modelo Alfa Laval.

La aplicación debe recopilar correctamente los datos necesarios para que posteriormente pueda implementarse un módulo de recomendación.

---

9. OBSTÁCULOS INTERNOS

Crear sección:

INTERNOS DEL TANQUE

Checkboxes:

- Agitador
- Serpentín
- Baffles
- Tubos internos
- Sensores
- Tuberías internas
- Otros

Si se selecciona:

Agitador

Mostrar:

- Diámetro
- Altura
- Descripción

Serpentín

Mostrar:

- Tipo
- Diámetro
- Dimensiones
- Descripción

Baffles

Mostrar:

- Cantidad
- Dimensiones aproximadas

Otros

Mostrar campo:

"Describa el obstáculo"

Debe ser posible escribir:

"Agitador central de 1,200 mm de diámetro con 4 paletas."

---

10. FOTOGRAFÍAS

La aplicación debe permitir tomar fotografías desde el teléfono.

Categorías:

1. Vista exterior
2. Placa del tanque
3. Vista interior
4. Fondo
5. Techo
6. Agitador
7. Serpentín
8. Conexión del cabezal
9. Línea CIP
10. Manómetro durante CIP
11. Otras

Las fotografías deben asociarse al tanque correspondiente.

Guardar las imágenes localmente.

No es necesario implementar nube en esta primera versión.

---

11. PANTALLA PRINCIPAL

Diseñar una interfaz muy sencilla.

Inicio:

"LEVANTAMIENTO DE TANQUES"

Mostrar:

[ + NUEVO CLIENTE ]

[ CLIENTES ]

[ LEVANTAMIENTOS RECIENTES ]

También mostrar estadísticas:

- Clientes registrados
- Tanques registrados
- Levantamientos realizados

---

12. PANTALLA CLIENTES

Lista de clientes.

Cada tarjeta debe mostrar:

Cliente:
Bepensa

Tanques:
5

Último levantamiento:
23/09/2026

Al tocar el cliente:

→ abrir detalle del cliente.

---

13. DETALLE DEL CLIENTE

Mostrar:

Nombre del cliente

Contacto

Dirección

Teléfono

Email

Botones:

[ + NUEVO TANQUE ]

[ EDITAR CLIENTE ]

[ EXPORTAR ]

Debajo:

TANQUES

Cada tanque debe mostrar:

Código:
TK-001

Nombre:
Tanque de mayonesa

Producto:
Mayonesa

Volumen:
1,000 L

Fecha:
23/09/2026

---

14. CREAR NUEVO TANQUE

El flujo debe ser tipo formulario dividido por secciones.

Paso 1:

IDENTIFICACIÓN

- Nombre del tanque
- Código
- Área/planta
- Proceso

Paso 2:

PRODUCTO

- Producto
- Descripción
- Residuo
- Características del residuo

Paso 3:

GEOMETRÍA

- Diámetro
- Alturas
- Volumen
- Techo
- Fondo
- Material

Paso 4:

CIP

- Caudal
- Presión
- Temperatura
- Químicos
- Tiempo
- Tubería

Paso 5:

INTERNOS

- Agitador
- Serpentín
- Baffles
- Obstáculos

Paso 6:

CABEZAL

- Cabezal actual
- Conexión
- Posición
- Altura

Paso 7:

FOTOGRAFÍAS

Paso 8:

OBSERVACIONES Y FIRMA

---

15. VALIDACIÓN DE DATOS

Implementar validaciones.

Ejemplos:

- Cliente obligatorio.
- Nombre del tanque obligatorio.
- Producto obligatorio.
- Diámetro > 0.
- Altura > 0.
- Caudal ≥ 0.
- Presión ≥ 0.
- Volumen ≥ 0.

Mostrar mensajes claros.

No permitir guardar datos evidentemente inválidos.

---

16. CÁLCULOS AUTOMÁTICOS

La aplicación puede calcular automáticamente:

Volumen geométrico aproximado

Si el usuario introduce:

Diámetro interno
+
Altura cilíndrica

calcular:

V = π × D² / 4 × H

Mostrar:

"Volumen cilíndrico estimado"

Pero aclarar visualmente que es una estimación y no sustituye el volumen nominal del fabricante.

También calcular:

Relación altura/diámetro

H/D

Esto puede resultar útil posteriormente para selección del sistema de limpieza.

---

17. RESUMEN TÉCNICO

Después de completar el levantamiento, crear una pantalla:

RESUMEN DEL LEVANTAMIENTO

Mostrar de manera muy visual:

CLIENTE
Bepensa

TANQUE
TK-001

PRODUCTO
Mayonesa

VOLUMEN
1,000 L

DIÁMETRO
1,200 mm

ALTURA
1,000 mm

PRESIÓN CIP
5.5 bar

CAUDAL CIP
8 m³/h

RESIDUO
Película grasa/viscosa

OBSTÁCULOS
Agitador + serpentín

CABEZAL ACTUAL
No disponible

---

18. EXPORTACIÓN

Implementar exportación del levantamiento.

Como mínimo:

PDF

Generar un informe técnico sencillo con:

- Logo/identificación de Jaraba Import
- Cliente
- Tanque
- Fecha
- Técnico
- Datos del tanque
- Producto
- Residuo
- Geometría
- CIP
- Internos
- Cabezal
- Fotografías
- Observaciones

Título:

"LEVANTAMIENTO TÉCNICO – SISTEMA DE LIMPIEZA DE TANQUE"

IMPORTANTE:

No utilizar el logo de Alfa Laval si no se dispone legalmente del archivo correspondiente.

Dejar preparada la aplicación para poder agregar posteriormente el branding autorizado.

---

19. EXPORTACIÓN EXCEL/CSV

Además del PDF, permitir exportar los datos del tanque a CSV.

Esto será importante para posteriormente alimentar una herramienta de selección técnica.

El CSV debe tener columnas claras y consistentes.

---

20. BÚSQUEDA

En la pantalla de clientes implementar búsqueda.

Permitir buscar por:

- Cliente
- Código de tanque
- Nombre de tanque
- Producto

---

21. EDITAR

Todo levantamiento debe poder editarse posteriormente.

También debe existir:

[ DUPLICAR TANQUE ]

Esto es importante porque muchos clientes tienen varios tanques similares.

Ejemplo:

TK-001

→ Duplicar

TK-002

El usuario solamente modifica las diferencias.

---

22. ESTADO DEL LEVANTAMIENTO

Cada tanque debe tener un estado:

- Borrador
- Levantamiento completado
- Pendiente de revisión
- Revisado

Por defecto:

"Borrador"

Cuando todos los campos principales estén completos:

"Levantamiento completado"

No realizar automáticamente recomendaciones de selección.

---

23. DISEÑO

Quiero una aplicación profesional pero extremadamente sencilla.

Debe estar pensada para un vendedor/ingeniero que está parado frente a un tanque usando el teléfono.

Priorizar:

- botones grandes
- pocos elementos por pantalla
- formularios claros
- navegación rápida
- funcionamiento con guantes si es posible
- contraste adecuado
- teclado numérico para medidas
- dropdowns para opciones repetitivas
- autoguardado

Evitar:

- animaciones innecesarias
- pantallas recargadas
- diseños tipo dashboard empresarial excesivamente complejos

---

24. AUTOGUARDADO

El formulario debe guardar automáticamente el progreso.

Si el usuario cierra la aplicación accidentalmente:

Al volver debe aparecer:

"Hay un levantamiento sin terminar. ¿Desea continuar?"

---

25. BASE DE DATOS

Utilizar una base de datos local robusta.

Preferiblemente Room si se utiliza Kotlin.

Crear relaciones:

Cliente 1 → N Tanques

Tanque 1 → N Fotografías

Tanque 1 → N Observaciones, si resulta necesario.

Utilizar IDs únicos.

---

26. ARQUITECTURA

Utilizar una arquitectura limpia y mantenible.

Separar:

- UI
- Modelos
- Base de datos
- Repositorios
- Lógica de negocio
- Exportación PDF
- Exportación CSV

No crear todo el código en un único archivo.

---

27. FUTURA FASE

Dejar la arquitectura preparada para una futura función:

"SELECCIÓN DE CABEZAL ALFA LAVAL"

En esa fase quiero poder introducir reglas técnicas y/o una base de datos de productos Alfa Laval para ayudar a determinar:

- familia de cabezal
- modelo
- diámetro de boquilla
- presión requerida
- caudal requerido
- cobertura
- compatibilidad con geometría
- compatibilidad con obstáculos
- observaciones técnicas

IMPORTANTE:

NO implementar todavía recomendaciones automáticas de modelos Alfa Laval.

Esta primera aplicación solamente debe realizar correctamente el levantamiento y organizar la información.

---

28. DATOS DE EJEMPLO

Crear datos de prueba para comprobar la aplicación.

Cliente:

"Bepensa"

Tanque:

"TK-001"

Proceso:

"Preparación"

Producto:

"Mayonesa"

Residuo:

"Película grasa y viscosa adherida a paredes y agitador después del vaciado."

Diámetro:

1,500 mm

Altura:

2,000 mm

Volumen:

3,500 L

Presión CIP:

5.5 bar

Caudal:

8 m³/h

Agitador:

Sí

Diámetro agitador:

1,000 mm

Serpentín:

Sí

---

29. CRITERIOS DE ACEPTACIÓN

Consideraré terminada la primera versión cuando pueda:

1. Abrir la aplicación en Android.
2. Crear un cliente.
3. Entrar al cliente.
4. Crear varios tanques.
5. Completar un levantamiento.
6. Guardarlo.
7. Cerrar la aplicación.
8. Volver a abrirla.
9. Encontrar los datos guardados.
10. Editar un tanque.
11. Duplicar un tanque.
12. Tomar fotografías.
13. Ver las fotografías asociadas al tanque.
14. Buscar clientes y tanques.
15. Generar un PDF.
16. Exportar CSV.
17. Utilizar todo esto sin Internet.

---

30. ENTREGABLES

Quiero que generes:

1. Código fuente completo.
2. Proyecto Android completo.
3. APK debug funcional.
4. APK release si es posible.
5. README.md con instrucciones.
6. Instrucciones para compilar.
7. Instrucciones para instalar la APK.
8. Estructura de carpetas explicada.
9. Explicación breve de la arquitectura.
10. Datos de prueba.

El proyecto debe quedar listo para copiarlo a otra PC y continuar trabajando.

---

31. IMPORTANTE SOBRE EL DESARROLLO

No quiero solamente una demostración visual o mockup.

Quiero una aplicación funcional.

Antes de finalizar:

- compila el proyecto
- corrige errores
- verifica que la base de datos funcione
- verifica creación/edición de clientes
- verifica creación/edición de tanques
- verifica fotografías
- verifica exportación PDF
- verifica CSV
- verifica que la aplicación funcione sin Internet

Si encuentras decisiones técnicas ambiguas, elige la opción más sencilla, estable y mantenible que permita cumplir los requisitos.

No agregues funcionalidades innecesarias.

Primero consigue una V1 completamente funcional.

Después de completar la V1, proporciona una lista de posibles mejoras para una V2, pero no implementes esas mejoras hasta que sean solicitadas.