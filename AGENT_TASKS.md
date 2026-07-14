# SchedulerFiles — attività agente

## Priorità alta

- [x] Eseguire build e test completi con JDK 8+ (`javac` disponibile).
- [x] Spostare tutti gli aggiornamenti Swing di `MoveClass` sull’EDT.
- [x] Verificare race condition tra annullamento, chiusura finestra e worker thread.
- [x] Aggiungere test per cache `Updater` con URL diversi e accesso concorrente.
- [x] Aggiungere test per errori di copia/move e comportamento CLI con exit code corretto.

## Priorità media

- [x] Separare trasferimento file, confronto, logging e scansione directory da `MoveClass`.
- [x] Rendere confronto versioni compatibile con prerelease (`1.2.0-beta`).
- [x] Migliorare gestione symlink, junction e cicli durante `walkFileTree`.
- [x] Rendere percorsi source/destination normalizzati e confrontabili in modo robusto.
- [x] Definire gestione esplicita per errori del transaction log dopo trasferimento riuscito.

## Priorità bassa

- [x] Sostituire concatenazioni HTML in `infoJFrame` con risorsa separata/template.
- [x] Migliorare accessibilità Swing: nomi accessibili, focus, contrasto e feedback errori.
- [x] Aggiornare README con procedura Windows/Linux e requisito JDK per build.
- [x] Aggiungere controllo statico/lint Java e integrazione CI.
- [x] Aggiornare o aggiungere test per progresso `0/0`, cancellazione e directory vuote.

## Stato recupero (2026-07-14)

- Ultima suite: **115 test passati, 0 falliti**.
- Build verificata con JDK 17 `--release 8`, `-Xlint:all` e `-Werror`.
- `ant` non disponibile; usare compilazione manuale finché non installato.
- Warning noti: cleanup Windows può segnalare file mappati ancora aperti dopo confronti byte.

### Build/test manuale da Git Bash

```bash
JDK="/c/Program Files (x86)/Android/openjdk/jdk-17.0.14/bin"
rm -rf /tmp/schedulerfiles-classes
mkdir -p /tmp/schedulerfiles-classes
"$JDK/javac.exe" --release 8 -encoding UTF-8 -Xlint:all -Werror \
  -cp 'dist/lib/AbsoluteLayout.jar' \
  -d /tmp/schedulerfiles-classes src/*.java tests/*.java
cp src/about.html /tmp/schedulerfiles-classes/about.html
"$JDK/java.exe" -Djava.awt.headless=true \
  -cp 'C:/Users/dntpp/AppData/Local/Temp/schedulerfiles-classes;D:/repos/SchedulerFiles/dist/lib/AbsoluteLayout.jar' \
  TestRunner
```

Nota: `javac -cp` deve usare `dist/lib/AbsoluteLayout.jar`, non `src/lib`.

## Replica doppia destinazione

- [x] Aggiungere `--dest2` opzionale in CLI.
- [x] Replicare file in seconda destinazione solo dopo prima copia riuscita.
- [x] Rimuovere file obsoleti da seconda destinazione per mantenerla gemella.
- [x] Bloccare combinazione `--dest2` + `--move`.
- [x] Aggiungere test replica e validazione opzioni.
- [x] Aggiungere seconda destinazione opzionale nella GUI.
- [x] Aggiornare guida CLI/GUI per replica twin.
- [x] Evolvere replica a sincronizzazione mirror con rimozione stale e verifica parity SHA-256.

## Evoluzione RAID-1 e affidabilità

- [x] Copia atomica per file: temp, hash, rename.
- [x] Manifest mirror con percorso, dimensione, SHA-256 e stato.
- [x] Recovery mirror da manifest dopo interruzione.
- [x] Gestione conflitti durante `--mirror-import`.
- [x] Lock esclusivo sulle destinazioni.
- [x] Test recovery, conflitti e copia atomica.

## Ottimizzazioni performance

- [x] Indicizzare destinazione una volta per operazione, evitando `listFiles()` per ogni file.
- [x] Cache hash destinazione con size/lastModified.
- [x] Sostituire `MappedByteBuffer` con buffer riutilizzabile per confronto byte.
- [x] Accorpare flush transaction log per ridurre I/O.
- [x] Valutare hashing parallelo opzionale su storage veloce.

## Prossimo lavoro consigliato

Nessun TODO tecnico urgente. Possibili evoluzioni future: test GUI reali, installer Windows `.exe` con icona, benchmark dataset grandi, recovery manifest avanzato.

## Stato corrente

- GitHub Actions rimosso su richiesta: build/release eseguiti localmente.
- JAR versionato aggiornato direttamente dopo build locale.
- Ultimo risultato: **144 test passati, 0 falliti**.
- Nessun TODO tecnico urgente residuo.
- Target bytecode: Java 8, major version 52.
- `dist/SchedulerFiles.jar` self-contained con AbsoluteLayout, guide e icone.
- Push automatico richiesto dall'utente dopo modifiche valide.
- Aggiornare sempre questo file dopo ogni attività, test, decisione o modifica di workflow.

## Regole operative

- Modifiche piccole e isolate.
- Prima di ogni commit: `git diff --check`, build e suite test.
- Non modificare `dist/SchedulerFiles.jar` salvo richiesta esplicita o build di rilascio.
- Non creare commit automatici senza richiesta dell’utente.
