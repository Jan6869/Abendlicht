# Abendlicht 🌙

Eine kleine, ruhige Android-App, um gute Gewohnheiten am Abend einzuführen.
Eine einzige Seite: liebevoll illustrierte Karten, ein Würfel für den Zufall, ein Mini-Kalender und deine Serie.

## Was die App kann

- **Karten antippen** – die Karte wandert ein kleines Stück auf dich zu, die anderen treten zurück.
  Unten erscheint ein Panel: *Erledigt* oder *Timer starten*.
- **Zufall** – Karten antippen, die heute nicht ins Los sollen (sie verblassen), dann *Würfeln*.
  Die Auswahl läuft wie ein auslaufendes Glücksrad mit leichtem haptischem Ticken.
- **Timer (optional)** – mit gewellter, wandernder Fortschrittslinie im Stil von Material 3 Expressive.
  Bei Pause glättet sich die Welle. Ist die Zeit um, wird die Gewohnheit automatisch eingetragen.
- **Kalender & Serie** – farbige Tage in der Farbe der Gewohnheit. Tippe auf einen Tag, um etwas nachzutragen.
  Ein neuer Abend beginnt um 4 Uhr morgens.
- **Einstellungen (Zahnrad)** – Gewohnheiten hinzufügen, umbenennen, Bild und Dauer wählen, ausblenden,
  sortieren; sanfte Abend-Erinnerung (nur wenn du noch nichts gemacht hast).
- Heller und dunkler Modus folgen dem System. Wer Systemanimationen ausschaltet, bekommt ruhige, stehende Bilder.

## Auf GitHub laden und APK bekommen

1. Auf github.com ein neues Repository anlegen (am besten **privat**, siehe unten), z. B. `abendlicht`.
2. Den Inhalt dieses Ordners hochladen (per „Add file → Upload files“ oder mit git):
   ```bash
   git init
   git add .
   git commit -m "Abendlicht"
   git branch -M main
   git remote add origin https://github.com/DEIN-NAME/abendlicht.git
   git push -u origin main
   ```
3. GitHub baut die App automatisch (Tab **Actions**, dauert ca. 5 Minuten).
4. Fertige APK: Tab **Releases** → `Abendlicht.apk` – direkt auf dem Pixel öffnen.
5. Beim ersten Mal fragt Android, ob Chrome/Dateien Apps installieren darf → erlauben.

Jeder neue Push erzeugt eine neue Version, die sich einfach über die alte installiert (deine Daten bleiben).

## Zum Signaturschlüssel

Im Ordner `keystore/` liegt ein fester Schlüssel, damit Updates über die alte Version installiert werden können.
Er ist nur für dich persönlich gedacht – lass das Repository deshalb am besten **privat**.

## Selbst bauen (optional)

Mit Android Studio den Ordner öffnen und auf ▶ drücken, oder im Terminal:
```bash
./gradlew assembleRelease
```
Die APK liegt dann unter `app/build/outputs/apk/release/`.

## Technik

Kotlin · Jetpack Compose · Material 3 · eigene Canvas-Illustrationen · Federn statt starrer Animationen ·
Schriften Fredoka & Nunito (SIL Open Font License) · Daten bleiben lokal auf dem Gerät.
