# Qubiszonnn TPA Helper

Mod kliencki Fabric dla Minecraft Java **1.21.11**.

- wykrywa graczy piszących na czacie (czat podpisany oraz czat serwerowy wysyłany jako wiadomość systemowa),
- przygotowuje komendę `/tpahere NICK`,
- ciemne GUI z animacjami: lista graczy, przyciski **Przygotuj** / **Kopiuj**, a w karcie komendy **Kopiuj** i **Wyślij**,
- stopka: **Create by qubiszonnn**.

Otwieranie GUI: klawisz **G** (do zmiany w Ustawienia → Sterowanie → Qubiszonnn TPA Helper).

## Budowanie
Wymagane: JDK 21 oraz Gradle 9.2+ (albo IntelliJ IDEA, który pobierze Gradle sam).

    gradle build

Gotowy plik: `build/libs/qubiszonnn-tpa-helper-1.0.0.jar` (nie używaj pliku `-sources`).
Alternatywnie wrzuć projekt na GitHub – workflow `.github/workflows/build.yml` zbuduje JAR automatycznie (zakładka Actions → Artifacts).

Instalacja: JAR do folderu `mods` razem z Fabric Loader (0.18.1+) i Fabric API dla 1.21.11.
