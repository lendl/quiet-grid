# Store art

Play Store screenshots are rendered from the real app, not drawn by hand.

## Tablet screenshots (7" and 10")

1. Capture the app with Roborazzi (Robolectric, no emulator needed):

   ```sh
   ./gradlew :app:recordRoborazziDebug --tests "com.quietgrid.app.storeart.TabletStoreArtCaptureTest"
   ```

   `app/src/test/java/com/quietgrid/app/storeart/TabletStoreArtCaptureTest.kt` seeds demo data
   (a 10 day daily streak, Sudoku and Word Search history, a Sudoku Journey in progress, a few
   badges, a half-solved Star Battle board), launches the real `AppNavHost` and writes:

   - `captures/tablet-7/*.png`: 600x960dp portrait at xhdpi (1200x1920 px)
   - `captures/tablet-10/*.png`: 1280x800dp landscape at xhdpi (2560x1600 px)

   The test is skipped by a normal `./gradlew test` run; it only runs under `recordRoborazziDebug`.

2. Frame the captures with a headline and a device outline, matching the phone set:

   ```sh
   cd tools/store-art && npm install playwright && npx playwright install chromium
   node frame-tablet.mjs
   ```

   Output lands in `out/tablet-7/` (1200x1920) and `out/tablet-10/` (2560x1600). Headlines live in
   the `shots` list at the top of `frame-tablet.mjs`; `08-privacy` is a text-only slide.

3. Copy the results into the repo:

   - `assets/tablet/7-inch/` and `assets/tablet/10-inch/`
   - `fastlane/metadata/android/en-US/images/sevenInchScreenshots/01.png` ... `08.png`
   - `fastlane/metadata/android/en-US/images/tenInchScreenshots/01.png` ... `08.png`

`captures/`, `out/` and `node_modules/` are gitignored.

## Notes

- Emoji do not render under Robolectric, so Animal Doku is not used; the themes slide shows Star Battle
  in the Silk theme instead.
- The status bar (9:41, signal, battery) is drawn by the framing script; Robolectric captures have no
  system bars.
