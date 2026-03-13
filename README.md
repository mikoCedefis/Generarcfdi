<<<<<<< HEAD
# Generarcfdi
=======
Proyecto generar_cfdi

Instrucciones rápidas para subir a GitHub:

1) Revisa que los archivos sensibles no se añadan al repositorio.
   - Este proyecto contiene claves en `src/main/resources/csd/`.
   - El `.gitignore` ya excluye `*.key` en esa carpeta, pero si quieres conservar una copia segura, muévela fuera del árbol del proyecto.

2) Inicializar el repositorio local y hacer el primer commit (ya ejecutado por este asistente):
   git init
   git add .
   git commit -m "Initial commit"

3) Crear un repositorio remoto en GitHub:
   - Desde la web: https://github.com/new -> crea un repo (por ejemplo: generar_cfdi).
   - O con la CLI de GitHub (si la tienes): gh repo create <usuario>/generar_cfdi --public --source=. --remote=origin

4) Añadir el remoto y subir:
   git remote add origin https://github.com/<usuario>/generar_cfdi.git
   git branch -M main
   git push -u origin main

Notas y buenas prácticas:
- Nunca comiences un repositorio público con claves privadas; si ya las subiste, rota las claves y elimina el historial con herramientas como `git filter-repo` o `bfg`.
- Considera añadir un archivo `LICENSE` si quieres especificar la licencia.
- Puedes añadir un `.github/workflows/ci.yml` más adelante para CI.
# IntelliJ
.idea/
*.iml

# Maven / Java
target/
*.class
*.log

# OS
.DS_Store

# Sensitive keys and keystores
src/main/resources/csd/*.key
src/main/resources/csd/*.jks
src/main/resources/csd/*.keystore

# Eclipse
.classpath
.project

# Generated sources
generated-sources/

# Others
out/

>>>>>>> bc98c9c (Initial commit)
