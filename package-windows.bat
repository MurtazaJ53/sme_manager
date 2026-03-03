@echo off
set VERSION=2.2
set MAIN_CLASS=com.lax.sme_manager.LaxSmeManagerStarter
set APP_NAME="Lax Yard & SME Manager"
set VENDOR="RASolutions"

echo [1/3] Cleaning and Building Project...
call gradlew clean build copyDependencies

echo [2/3] Creating Runtime Image...
rmdir /s /q build\runtime
jlink --module-path "%JAVA_HOME%\jmods;build\libs\libs;build\libs" ^
      --add-modules javafx.controls,javafx.fxml,javafx.graphics,java.sql,java.naming ^
      --output build\runtime ^
      --strip-debug ^
      --no-header-files ^
      --no-man-pages

echo [3/3] Packaging MSI...

rem --- CODE SIGNING (UNCOMMENT AND FILL TO REMOVE UNKNOWN PUBLISHER WARNING) ---
rem set SIGN_OPTS=--win-signing --win-signing-keystore "YOUR_KEYSTORE_PATH" --win-signing-key-alias "YOUR_ALIAS" --win-signing-store-password "YOUR_PASSWORD"
rem ------------------------------------------------------------------------------

jpackage --name %APP_NAME% ^
         --vendor %VENDOR% ^
         --app-version %VERSION% ^
         --input build\libs ^
         --dest build\dist ^
         --main-jar sme-mngr.jar ^
         --main-class %MAIN_CLASS% ^
         --runtime-image build\runtime ^
         --type msi ^
         --win-header --win-menu --win-menu-group "Lax SME Manager" ^
         --win-shortcut ^
         --win-dir-chooser ^
         --win-upgrade-uuid "9D26D684-065C-4E15-998C-82142279140B" ^
         %SIGN_OPTS%

echo Done! MSI is in build/dist
pause
