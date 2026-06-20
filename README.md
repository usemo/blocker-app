# Shorts Blocker

Aplikacja na Androida do osobistego użytku, która blokuje:

- **YouTube Shorts** — po wejściu w Short natychmiast Cię z niego cofa,
- **wybrane aplikacje** — po wejściu w zablokowaną apkę wraca na ekran główny,
- **wybrane strony WWW** — w obsługiwanych przeglądarkach (Chrome i podobne)
  wykrywa adres i cofa z zablokowanej domeny.

Każdy rodzaj blokowania ma osobny przełącznik (włącz/wyłącz).

Aplikacja działa w pełni lokalnie i **nie ma uprawnienia do internetu** —
żadne dane nie opuszczają telefonu.

## Jak to działa

Aplikacja korzysta z **usługi ułatwień dostępu** (Accessibility Service). Usługa
obserwuje aktywne aplikacje i reaguje na trzy sytuacje:

- w YouTube wykrywa odtwarzacz Shorts (wewnętrznie „reel”) i wykonuje „wstecz”,
- gdy na pierwszy plan wejdzie zablokowana aplikacja — wykonuje „ekran główny”,
- w obsługiwanej przeglądarce odczytuje pasek adresu i jeśli pasuje do
  zablokowanej domeny — wykonuje „wstecz”.

Z aplikacji innych niż YouTube usługa odczytuje wyłącznie nazwę aplikacji
(a w przeglądarce — adres strony), nigdy ich treści. To jedyny sposób, by
blokować bez modyfikowania innych aplikacji i bez dostępu do sieci.

### Ograniczenia blokowania stron

Blokowanie WWW działa przez odczyt paska adresu, więc:

- działa w przeglądarkach opartych na Chromium (Chrome, Brave, Edge, Opera) oraz
  częściowo w Samsung Internet i Firefox — identyfikatory paska adresu są w
  `BROWSER_URL_BARS` w `ShortsBlockerService.kt`,
- nie blokuje stron otwieranych wewnątrz innych aplikacji (WebView).

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
