// Wiring snippet — not a standalone file. Apply these edits to the real
// app/src/main/java/com/quietgrid/app/core/GameCatalog.kt.

// 1. Add to the GameId enum:
//    __GAME_ID_UPPER__("__game_id__"),

// 2. Add a GameMeta entry to GameCatalog.games:
//    GameMeta(GameId.__GAME_ID_UPPER__, R.string.__game_id___title, R.string.__game_id___tagline),
//    // add `beta = true` while the game is still rough

// 3. Add title/tagline (and any other new strings) to res/values/strings.xml first, then every
//    res/values-*/strings.xml locale it should ship in.
