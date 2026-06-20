# Shorts Blocker

Prosta aplikacja na Androida, która **blokuje YouTube Shorts**. Gdy w aplikacji
YouTube otworzy się Short, ta aplikacja natychmiast Cię z niego cofa — w praktyce
Shorts się nie odtwarzają.

Aplikacja jest na prywatny użytek. Działa w pełni lokalnie i **nie ma uprawnienia
do internetu** — żadne dane o tym, co oglądasz, nie opuszczają telefonu.

## Jak to działa

Aplikacja korzysta z **usługi ułatwień dostępu** (Accessibility Service). Usługa
obserwuje wyłącznie aplikację YouTube i wykrywa moment, w którym na ekranie
pojawia się odtwarzacz Shorts (wewnętrznie nazywany „reel”). Wtedy wykonuje
gest „wstecz”, który wyrzuca Cię z Shortsa z powrotem na poprzedni ekran.

To jedyny sposób, by zablokować Shorts bez modyfikowania samej aplikacji YouTube.

## Instalacja (gotowy APK)

1. Skopiuj plik `ShortsBlocker.apk` na telefon.
2. Otwórz go i zezwól na instalację z nieznanych źródeł (Android o to poprosi).
3. Uruchom aplikację **Shorts Blocker**.
4. Kliknij **„Otwórz ustawienia ułatwień dostępu”** i włącz tam „Shorts Blocker”.
5. Gotowe. Wróć do aplikacji — status powinien być zielony.

> Po aktualizacji aplikacji YouTube usługa może wymagać ponownego włączenia,
> jeśli kiedykolwiek przestanie działać.

## Samodzielne zbudowanie

Wymagania: Android Studio (lub Android SDK + JDK 17).

```bash
./gradlew assembleDebug      # plik: app/build/outputs/apk/debug/app-debug.apk
# lub
./gradlew assembleRelease    # wersja release (wymaga podpisania)
```

## Konfiguracja w aplikacji

- **Blokowanie aktywne** — globalny włącznik/wyłącznik blokowania.
- **Pokazuj powiadomienie po zablokowaniu** — krótki komunikat (toast) po każdym
  zablokowanym Shortcie; można wyłączyć.
- **Licznik** — ile Shortsów zostało dotąd zablokowanych.

## Uwaga techniczna

YouTube co jakiś czas zmienia wewnętrzne identyfikatory widoków. Gdyby blokowanie
kiedyś przestało działać po aktualizacji YouTube, trzeba zaktualizować listę
identyfikatorów `REEL_IDS` w pliku `ShortsBlockerService.kt`.
