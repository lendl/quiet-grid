import { getLocales } from 'expo-localization';
import type { LanguageSetting } from '../utils/settingsStorage';

export type ResolvedLanguage = 'en' | 'nl' | 'de' | 'fr' | 'es';

let currentLanguage: ResolvedLanguage = 'en';

function resolveLocaleLanguage(value: string | null | undefined): ResolvedLanguage | null {
  const normalized = typeof value === 'string' ? value.toLowerCase() : '';
  if (normalized.startsWith('nl')) return 'nl';
  if (normalized.startsWith('de')) return 'de';
  if (normalized.startsWith('fr')) return 'fr';
  if (normalized.startsWith('es')) return 'es';
  if (normalized.startsWith('en')) return 'en';
  return null;
}

export function detectSystemLanguage(): ResolvedLanguage {
  const [locale] = getLocales();
  return resolveLocaleLanguage(locale?.languageCode)
    ?? resolveLocaleLanguage(locale?.languageTag)
    ?? resolveLocaleLanguage(locale?.regionCode)
    ?? 'en';
}

export function resolveLanguage(setting: LanguageSetting | null): ResolvedLanguage {
  if (setting !== null) {
    return setting;
  }

  return detectSystemLanguage();
}

export function setCurrentLanguage(language: ResolvedLanguage): void {
  currentLanguage = language;
}

export function getCurrentLanguage(): ResolvedLanguage {
  return currentLanguage;
}

type AppStrings = typeof EN_STRINGS;

type SupportInfoKey = 'privacy' | 'about' | 'licenses';

interface SupportInfoSection {
  heading: string;
  body: string[];
}

interface SupportInfoContent {
  title: string;
  intro: string;
  sections: SupportInfoSection[];
}

const EN_STRINGS = {
  common: {
    goHome: 'Go to Games',
    goBack: 'Go back',
    back: 'Back',
    home: 'Games',
    game: 'Game',
    play: 'Play',
    playAgain: 'Play Again',
    previous: 'Previous',
    next: 'Next',
    getStarted: 'Get Started',
    settings: 'Settings',
    support: 'Support',
    stats: 'Stats',
    rules: 'Rules',
    tutorial: 'Tutorial',
    cancel: 'Cancel',
    clear: 'Clear',
    current: 'Current',
    on: 'On',
    off: 'Off',
    all: 'All',
    open: 'Open',
    reveal: 'Reveal',
    flag: 'Flag',
    english: 'English',
    dutch: 'Nederlands',
    german: 'Deutsch',
    french: 'Français',
    spanish: 'Español',
    systemDefault: 'System default',
    endPuzzle: 'End puzzle',
    continuePuzzle: 'Continue Puzzle',
    startNewPuzzle: 'Start New Puzzle',
    and: 'and',
  },
  tabs: {
    games: 'Games',
    stats: 'Stats',
    settings: 'Settings',
    support: 'Support',
  },
  games: {
    subtitle: 'Choose a puzzle and keep your active run close.',
    comingSoon: 'Coming Soon',
    betaDisclaimer: 'These games are still in testing and may have rough edges.',
  },
  home: {
    openRepo: 'Open the Quiet Grid GitHub repository',
    brandMark: 'Quiet Grid mark',
    changeTheme: 'Change theme',
    subtitle: 'Pure logic. No distractions.',
    activePuzzleWaiting: 'Your puzzle is waiting',
    startPuzzle: 'Start Puzzle',
    trustOffline: 'Open source. Offline by design.',
    trustPrivacy: 'No data ever leaves your device.',
    repoErrorTitle: "Couldn't open the project page",
    repoErrorMessage: "Your device couldn't open the GitHub link right now. Quiet Grid still works fully offline.",
  },
  replaceDialog: {
    title: 'Replace active puzzle?',
    message: 'You already have an active puzzle. You can continue it now or replace it with a new puzzle.',
  },
  welcome: {
    slides: [
      {
        title: 'Welcome to Quiet Grid',
        body: 'Logic puzzles. No ads, no accounts, no internet required. Everything stays on your device.',
      },
      {
        title: 'Puzzle types included',
        body: '{games} — each with four difficulty levels. Play at your own pace.',
      },
      {
        title: 'Offline and open source',
        body: 'Your progress, stats, and streaks live only on this device. No data ever leaves.',
      },
    ],
  },
  settings: {
    title: 'Settings',
    subtitle: 'Choose your theme, language, timer display, and whether first-time tutorials open automatically.',
    appearance: 'Appearance',
    theme: 'Theme',
    language: 'Language',
    tutorials: 'Tutorials',
    languageDropdownDetail: 'Choose the app language.',
    languageAiDisclaimer: 'Language texts were AI-generated and may contain small mistakes.',
    dark: 'Dark',
    darkDetail: 'Keeps the board and menus in a darker palette.',
    light: 'Light',
    lightDetail: 'Uses a brighter palette across the app.',
    pencil: 'Pencil',
    pencilDetail: 'Uses a paper-like grayscale palette across the app.',
    languageSystemDetail: 'Follow your Android language setting.',
    languageEnglishDetail: 'Show the app in English.',
    languageDutchDetail: 'Show the app in Dutch.',
    languageGermanDetail: 'Show the app in German.',
    languageFrenchDetail: 'Show the app in French.',
    languageSpanishDetail: 'Show the app in Spanish.',
    showTimerInPlayLabel: 'Show timer during play',
    showTimerInPlayDetail: 'Shows the elapsed timer while a puzzle is in progress.',
    tutorialsLabel: 'Show first-time tutorials',
    tutorialsDetail: 'Opens each puzzle tutorial the first time you choose that puzzle type.',
    betaGamesLabel: 'Enable beta games',
    betaGamesDetail: 'Unlocks games that are still in testing. These may have rough edges.',
  },
  puzzlePicker: {
    heading: 'Choose puzzle type',
  },
  puzzle: {
    chooseDifficulty: 'Choose difficulty',
  },
  gameSheet: {
    newToGame: (name: string) => `New to ${name}? Start with the tutorial.`,
  },
  howToPlay: {
    rulesTitle: (name: string) => `${name} rules`,
    tipsTitle: 'Tips',
  },
  tutorialHost: {
    unavailableTitle: 'Tutorial not available',
    unavailableBody: 'This puzzle type does not include a tutorial yet.',
  },
  puzzlePlay: {
    loading: 'Loading puzzle…',
    endDialogTitle: 'End this puzzle?',
    endDialogMessage: 'Your progress will not be saved and the puzzle will end.',
    endDialogConfirm: 'End Puzzle',
  },
  completion: {
    newHighScoreEyebrow: 'New high score',
    newHighScoreTitle: 'Amazing solve!',
    newHighScoreBody: 'You set a new best score on this difficulty.',
    firstScoreEyebrow: 'First score',
    firstScoreTitle: 'Great work!',
    firstScoreBody: 'You recorded your first solved score on this difficulty.',
    solvedEyebrow: 'Puzzle solved',
    solvedTitle: 'Puzzle solved!',
    solvedBody: 'Nice job finishing this puzzle.',
    streakBadge: (streak: number) => `${streak} puzzle streak`,
    score: 'Score',
    elapsedTime: 'Elapsed time',
    accuracy: 'Accuracy',
    tryAnotherGame: 'Try Another Game',
    viewStats: 'Your Progress',
  },
  loss: {
    difficulty: 'Difficulty',
    elapsedTime: 'Elapsed time',
    tryAnotherGame: 'Try Another Game',
  },
  analysis: {
    title: 'Analysis',
    back: 'Back to loss',
    analyze: 'Analyze',
    previous: 'Previous',
    next: 'Next',
    fastJump: 'Jump to step',
    step: (current: number, total: number) => `Step ${current} of ${total}`,
  },
  stats: {
    headerSubtitle: 'Statistics for this puzzle type',
    solved: 'Solved',
    streak: 'Streak',
    winRate: 'Win Rate',
    byDifficulty: 'By Difficulty',
    solvedOutOfPlayed: (solved: number, played: number) => `${solved} solved out of ${played} played`,
    winRateDetail: (rate: number) => `${rate}% win rate`,
    bestScore: 'Best score',
    privacy: 'Privacy',
    privacyText: 'All statistics stay on this device. No account required. No data is sent anywhere.',
    clearData: 'Clear Data',
    clearDataTitle: 'Clear Data',
    clearDataMessage: 'Remove all saved statistics and the current active puzzle? This cannot be undone.',
  },
  support: {
    title: 'Support',
    subtitle: 'Small, practical help for questions, feedback, and trust details.',
    supportSection: 'Support',
    trustSection: 'Trust',
    aboutSection: 'About',
    reportBug: 'Report bug',
    requestFeature: 'Request feature',
    contact: 'Contact',
    opensGithubIssues: 'Opens GitHub issues',
    privacy: 'Privacy',
    sourceCode: 'GitHub / source code',
    opensGithub: 'Opens GitHub',
    licenses: 'Open-source licenses',
    aboutQuietGrid: 'About Quiet Grid',
    rateQuietGrid: 'Rate Quiet Grid',
    opensPlayStore: 'Opens Play Store',
    version: (version: string) => `Version ${version}`,
    openErrorTitle: "Couldn't open that link",
    openErrorMessage: "Your device couldn't open that destination right now. Quiet Grid still works fully offline.",
  },
  supportInfoBack: 'Back',
  supportInfo: {
    privacy: {
      title: 'Privacy',
      intro: 'Quiet Grid works fully offline. No data leaves your device.',
      sections: [
        {
          heading: 'What Quiet Grid collects',
          body: [
            'Quiet Grid does not collect, transmit, sell, or share personal data.',
            'The app does not require an account and does not send data to us or to third parties.',
          ],
        },
        {
          heading: 'What stays on this device',
          body: [
            'Quiet Grid stores a small amount of app data locally so the app can work as expected.',
            'This includes theme preference, local statistics, and active puzzle progress.',
          ],
        },
        {
          heading: 'Ads, analytics, and tracking',
          body: [
            'Quiet Grid does not use ads, analytics, tracking, crash reporting services, or third-party online services.',
          ],
        },
      ],
    },
    about: {
      title: 'About Quiet Grid',
      intro: 'Quiet Grid is a privacy-first puzzle app built for calm focus.',
      sections: [
        {
          heading: 'What the app is for',
          body: [
            'Quiet Grid keeps logic puzzles lightweight, fully offline, ad-free, and free to use.',
            'The goal is simple: clear rules, supportive pacing, and no distractions.',
          ],
        },
        {
          heading: 'What Quiet Grid includes',
          body: [
            'Quiet Grid currently includes {games}, with local statistics, dark, light, and pencil themes, and no accounts.',
          ],
        },
      ],
    },
    licenses: {
      title: 'Open-source licenses',
      intro: 'Quiet Grid is open-source software.',
      sections: [
        {
          heading: 'Project license',
          body: [
            'Quiet Grid source code is available under the GNU General Public License v3.0.',
          ],
        },
        {
          heading: 'Third-party packages',
          body: [
            'Quiet Grid also uses open-source packages that keep their own licenses.',
            'You can review the full source code and package list in the project repository.',
          ],
        },
      ],
    },
  } as Record<SupportInfoKey, SupportInfoContent>,
} satisfies Record<string, unknown>;

const NL_STRINGS: AppStrings = {
  common: {
    goHome: 'Ga naar spellen',
    goBack: 'Ga terug',
    back: 'Terug',
    home: 'Spellen',
    game: 'Spel',
    play: 'Spelen',
    playAgain: 'Opnieuw spelen',
    previous: 'Vorige',
    next: 'Volgende',
    getStarted: 'Beginnen',
    settings: 'Instellingen',
    support: 'Ondersteuning',
    stats: 'Statistieken',
    rules: 'Regels',
    tutorial: 'Tutorial',
    cancel: 'Annuleren',
    clear: 'Wissen',
    current: 'Huidig',
    on: 'Aan',
    off: 'Uit',
    all: 'Alles',
    open: 'Openen',
    reveal: 'Openen',
    flag: 'Markeren',
    english: 'English',
    dutch: 'Nederlands',
    german: 'Deutsch',
    french: 'Français',
    spanish: 'Español',
    systemDefault: 'Systeemstandaard',
    endPuzzle: 'Puzzel beëindigen',
    continuePuzzle: 'Puzzel hervatten',
    startNewPuzzle: 'Nieuwe puzzel starten',
    and: 'en',
  },
  tabs: {
    games: 'Spellen',
    stats: 'Statistieken',
    settings: 'Instellingen',
    support: 'Ondersteuning',
  },
  games: {
    subtitle: 'Kies een puzzel en houd je actieve spel dichtbij.',
    comingSoon: 'Binnenkort beschikbaar',
    betaDisclaimer: 'Deze spellen zijn nog in ontwikkeling en kunnen problemen bevatten.',
  },
  home: {
    openRepo: 'Open de Quiet Grid GitHub-repository',
    brandMark: 'Quiet Grid-logo',
    changeTheme: 'Thema wijzigen',
    subtitle: 'Pure logica. Geen afleiding.',
    activePuzzleWaiting: 'Je puzzel wacht op je',
    startPuzzle: 'Puzzel starten',
    trustOffline: 'Open source. Ontworpen voor offline gebruik.',
    trustPrivacy: 'Er verlaat nooit data je toestel.',
    repoErrorTitle: 'Projectpagina kon niet worden geopend',
    repoErrorMessage: 'Je toestel kon de GitHub-link nu niet openen. Quiet Grid werkt nog steeds volledig offline.',
  },
  replaceDialog: {
    title: 'Actieve puzzel vervangen?',
    message: 'Je hebt al een actieve puzzel. Je kunt die nu hervatten of vervangen door een nieuwe puzzel.',
  },
  welcome: {
    slides: [
      {
        title: 'Welkom bij Quiet Grid',
        body: 'Logische puzzels. Geen advertenties, geen accounts, geen internet nodig. Alles blijft op je toestel.',
      },
      {
        title: 'Puzzeltypes inbegrepen',
        body: '{games} — elk met vier moeilijkheidsgraden. Speel op je eigen tempo.',
      },
      {
        title: 'Offline en open source',
        body: 'Je voortgang, statistieken en streaks blijven alleen op dit toestel. Er verlaat nooit data je toestel.',
      },
    ],
  },
  settings: {
    title: 'Instellingen',
    subtitle: 'Kies je thema, taal, timerweergave en of tutorials automatisch openen bij de eerste keer.',
    appearance: 'Weergave',
    theme: 'Thema',
    language: 'Taal',
    tutorials: 'Tutorials',
    languageDropdownDetail: 'Kies de taal van de app.',
    languageAiDisclaimer: 'De taalteksten zijn met AI gegenereerd en kunnen kleine fouten bevatten.',
    dark: 'Donker',
    darkDetail: 'Houdt bord en menu\'s in een donker palet.',
    light: 'Licht',
    lightDetail: 'Gebruikt een lichter palet in de hele app.',
    pencil: 'Potlood',
    pencilDetail: 'Gebruikt een papierachtige grijsschaal in de hele app.',
    languageSystemDetail: 'Volg je Android-taalinstelling.',
    languageEnglishDetail: 'Toon de app in het Engels.',
    languageDutchDetail: 'Toon de app in het Nederlands.',
    languageGermanDetail: 'Toon de app in het Duits.',
    languageFrenchDetail: 'Toon de app in het Frans.',
    languageSpanishDetail: 'Toon de app in het Spaans.',
    showTimerInPlayLabel: 'Timer tonen tijdens spelen',
    showTimerInPlayDetail: 'Toont de verstreken tijd terwijl een puzzel bezig is.',
    tutorialsLabel: 'Toon tutorials voor de eerste keer',
    tutorialsDetail: 'Opent de tutorial van elk puzzeltype de eerste keer dat je het kiest.',
    betaGamesLabel: 'Bètaspellen inschakelen',
    betaGamesDetail: 'Ontgrendelt spellen die nog in ontwikkeling zijn. Deze kunnen problemen bevatten.',
  },
  puzzlePicker: {
    heading: 'Kies puzzeltype',
  },
  puzzle: {
    chooseDifficulty: 'Kies moeilijkheid',
  },
  gameSheet: {
    newToGame: (name: string) => `Nieuw in ${name}? Begin met de tutorial.`,
  },
  howToPlay: {
    rulesTitle: (name: string) => `${name}-regels`,
    tipsTitle: 'Tips',
  },
  tutorialHost: {
    unavailableTitle: 'Tutorial niet beschikbaar',
    unavailableBody: 'Dit puzzeltype heeft nog geen tutorial.',
  },
  puzzlePlay: {
    loading: 'Puzzel laden…',
    endDialogTitle: 'Deze puzzel beëindigen?',
    endDialogMessage: 'Je voortgang wordt niet bewaard en de puzzel eindigt.',
    endDialogConfirm: 'Puzzel beëindigen',
  },
  completion: {
    newHighScoreEyebrow: 'Nieuwe topscore',
    newHighScoreTitle: 'Geweldig opgelost!',
    newHighScoreBody: 'Je hebt een nieuwe beste score op deze moeilijkheid gezet.',
    firstScoreEyebrow: 'Eerste score',
    firstScoreTitle: 'Goed gedaan!',
    firstScoreBody: 'Je hebt je eerste opgeloste score op deze moeilijkheid vastgelegd.',
    solvedEyebrow: 'Puzzel opgelost',
    solvedTitle: 'Puzzel opgelost!',
    solvedBody: 'Mooi werk met het afronden van deze puzzel.',
    streakBadge: (streak: number) => `${streak} puzzels op rij`,
    score: 'Score',
    elapsedTime: 'Verstreken tijd',
    accuracy: 'Nauwkeurigheid',
    tryAnotherGame: 'Ander spel proberen',
    viewStats: 'Jouw voortgang',
  },
  loss: {
    difficulty: 'Moeilijkheid',
    elapsedTime: 'Verstreken tijd',
    tryAnotherGame: 'Ander spel proberen',
  },
  analysis: {
    title: 'Analyse',
    back: 'Terug naar verlies',
    analyze: 'Analyseren',
    previous: 'Vorige',
    next: 'Volgende',
    fastJump: 'Spring naar stap',
    step: (current: number, total: number) => `Stap ${current} van ${total}`,
  },
  stats: {
    headerSubtitle: 'Statistieken voor dit puzzeltype',
    solved: 'Opgelost',
    streak: 'Streak',
    winRate: 'Winstpercentage',
    byDifficulty: 'Per moeilijkheid',
    solvedOutOfPlayed: (solved: number, played: number) => `${solved} opgelost van ${played} gespeeld`,
    winRateDetail: (rate: number) => `${rate}% winstpercentage`,
    bestScore: 'Beste score',
    privacy: 'Privacy',
    privacyText: 'Alle statistieken blijven op dit toestel. Geen account nodig. Er wordt geen data verstuurd.',
    clearData: 'Gegevens wissen',
    clearDataTitle: 'Gegevens wissen',
    clearDataMessage: 'Alle opgeslagen statistieken en de huidige actieve puzzel verwijderen? Dit kan niet ongedaan worden gemaakt.',
  },
  support: {
    title: 'Ondersteuning',
    subtitle: 'Kleine, praktische hulp voor vragen, feedback en vertrouwensinformatie.',
    supportSection: 'Ondersteuning',
    trustSection: 'Vertrouwen',
    aboutSection: 'Over',
    reportBug: 'Bug melden',
    requestFeature: 'Functie aanvragen',
    contact: 'Contact',
    opensGithubIssues: 'Opent GitHub-issues',
    privacy: 'Privacy',
    sourceCode: 'GitHub / broncode',
    opensGithub: 'Opent GitHub',
    licenses: 'Open-sourcelicenties',
    aboutQuietGrid: 'Over Quiet Grid',
    rateQuietGrid: 'Beoordeel Quiet Grid',
    opensPlayStore: 'Opent Play Store',
    version: (version: string) => `Versie ${version}`,
    openErrorTitle: 'Die link kon niet worden geopend',
    openErrorMessage: 'Je toestel kon die bestemming nu niet openen. Quiet Grid werkt nog steeds volledig offline.',
  },
  supportInfoBack: 'Terug',
  supportInfo: {
    privacy: {
      title: 'Privacy',
      intro: 'Quiet Grid werkt volledig offline. Er verlaat geen data je toestel.',
      sections: [
        {
          heading: 'Wat Quiet Grid verzamelt',
          body: [
            'Quiet Grid verzamelt, verstuurt, verkoopt of deelt geen persoonlijke gegevens.',
            'De app vraagt geen account en stuurt geen data naar ons of naar derden.',
          ],
        },
        {
          heading: 'Wat op dit toestel blijft',
          body: [
            'Quiet Grid bewaart een kleine hoeveelheid appdata lokaal zodat de app goed kan werken.',
            'Dit omvat themavoorkeur, lokale statistieken en voortgang van actieve puzzels.',
          ],
        },
        {
          heading: 'Advertenties, analytics en tracking',
          body: [
            'Quiet Grid gebruikt geen advertenties, analytics, tracking, crashrapportage of online diensten van derden.',
          ],
        },
      ],
    },
    about: {
      title: 'Over Quiet Grid',
      intro: 'Quiet Grid is een privacyvriendelijke puzzelapp gebouwd voor rustige focus.',
      sections: [
        {
          heading: 'Waar de app voor is',
          body: [
            'Quiet Grid houdt logische puzzels licht, volledig offline, advertentievrij en gratis te gebruiken.',
            'Doel is eenvoudig: heldere regels, ondersteunend tempo en geen afleiding.',
          ],
        },
        {
          heading: 'Wat Quiet Grid bevat',
          body: [
            'Quiet Grid bevat momenteel {games}, met lokale statistieken, donkere en lichte thema\'s en geen accounts.',
          ],
        },
      ],
    },
    licenses: {
      title: 'Open-sourcelicenties',
      intro: 'Quiet Grid is opensourcesoftware.',
      sections: [
        {
          heading: 'Projectlicentie',
          body: [
            'De broncode van Quiet Grid is beschikbaar onder de GNU General Public License v3.0.',
          ],
        },
        {
          heading: 'Pakketten van derden',
          body: [
            'Quiet Grid gebruikt ook opensourcepakketten met hun eigen licenties.',
            'Je kunt de volledige broncode en pakketlijst bekijken in de projectrepository.',
          ],
        },
      ],
    },
  },
};

const DE_STRINGS: AppStrings = {
  common: {
    goHome: 'Zu Spielen',
    goBack: 'Zurück',
    back: 'Zurück',
    home: 'Spiele',
    game: 'Spiel',
    play: 'Spielen',
    playAgain: 'Nochmals spielen',
    previous: 'Zurück',
    next: 'Weiter',
    getStarted: 'Loslegen',
    settings: 'Einstellungen',
    support: 'Hilfe',
    stats: 'Statistiken',
    rules: 'Regeln',
    tutorial: 'Tutorial',
    cancel: 'Abbrechen',
    clear: 'Löschen',
    current: 'Aktuell',
    on: 'Ein',
    off: 'Aus',
    all: 'Alle',
    open: 'Öffnen',
    reveal: 'Aufdecken',
    flag: 'Markieren',
    english: 'English',
    dutch: 'Nederlands',
    german: 'Deutsch',
    french: 'Français',
    spanish: 'Español',
    systemDefault: 'Systemstandard',
    endPuzzle: 'Rätsel beenden',
    continuePuzzle: 'Rätsel fortsetzen',
    startNewPuzzle: 'Neues Rätsel starten',
    and: 'und',
  },
  tabs: {
    games: 'Spiele',
    stats: 'Statistiken',
    settings: 'Einstellungen',
    support: 'Hilfe',
  },
  games: {
    subtitle: 'Wähle ein Rätsel und behalte dein aktives Spiel im Blick.',
    comingSoon: 'Demnächst verfügbar',
    betaDisclaimer: 'Diese Spiele befinden sich noch in der Entwicklung und können Fehler enthalten.',
  },
  home: {
    openRepo: 'Quiet Grid GitHub-Repository öffnen',
    brandMark: 'Quiet Grid-Logo',
    changeTheme: 'Design ändern',
    subtitle: 'Reine Logik. Keine Ablenkung.',
    activePuzzleWaiting: 'Dein Rätsel wartet',
    startPuzzle: 'Rätsel starten',
    trustOffline: 'Open Source. Für Offline-Nutzung entwickelt.',
    trustPrivacy: 'Keine Daten verlassen dein Gerät.',
    repoErrorTitle: 'Projektseite konnte nicht geöffnet werden',
    repoErrorMessage: 'Dein Gerät konnte den GitHub-Link gerade nicht öffnen. Quiet Grid funktioniert weiterhin vollständig offline.',
  },
  replaceDialog: {
    title: 'Aktives Rätsel ersetzen?',
    message: 'Du hast bereits ein aktives Rätsel. Du kannst es jetzt fortsetzen oder durch ein neues Rätsel ersetzen.',
  },
  welcome: {
    slides: [
      {
        title: 'Willkommen bei Quiet Grid',
        body: 'Logikrätsel. Keine Werbung, keine Konten, kein Internet erforderlich. Alles bleibt auf deinem Gerät.',
      },
      {
        title: 'Rätseltypen enthalten',
        body: '{games} — jeweils mit vier Schwierigkeitsgraden. Spiele in deinem eigenen Tempo.',
      },
      {
        title: 'Offline und Open Source',
        body: 'Dein Fortschritt, Statistiken und Streaks bleiben nur auf diesem Gerät. Keine Daten verlassen dein Gerät.',
      },
    ],
  },
  settings: {
    title: 'Einstellungen',
    subtitle: 'Wähle Design, Sprache, Timer-Anzeige und ob Tutorials beim ersten Mal automatisch geöffnet werden.',
    appearance: 'Darstellung',
    theme: 'Thema',
    language: 'Sprache',
    tutorials: 'Tutorials',
    languageDropdownDetail: 'Wähle die App-Sprache.',
    languageAiDisclaimer: 'Sprachtexte wurden mit KI erstellt und können kleine Fehler enthalten.',
    dark: 'Dunkel',
    darkDetail: 'Hält Brett und Menüs in einer dunkleren Palette.',
    light: 'Hell',
    lightDetail: 'Verwendet eine hellere Palette in der gesamten App.',
    pencil: 'Bleistift',
    pencilDetail: 'Verwendet eine papierähnliche Grauskalenpalette in der gesamten App.',
    languageSystemDetail: 'Folgt deiner Android-Spracheinstellung.',
    languageEnglishDetail: 'App auf Englisch anzeigen.',
    languageDutchDetail: 'App auf Niederländisch anzeigen.',
    languageGermanDetail: 'App auf Deutsch anzeigen.',
    languageFrenchDetail: 'App auf Französisch anzeigen.',
    languageSpanishDetail: 'App auf Spanisch anzeigen.',
    showTimerInPlayLabel: 'Timer während des Spielens anzeigen',
    showTimerInPlayDetail: 'Zeigt den verstrichenen Timer während eines laufenden Rätsels an.',
    tutorialsLabel: 'Tutorials beim ersten Mal anzeigen',
    tutorialsDetail: 'Öffnet das Tutorial jedes Rätseltyps beim ersten Mal.',
    betaGamesLabel: 'Beta-Spiele aktivieren',
    betaGamesDetail: 'Schaltet Spiele frei, die sich noch in der Entwicklung befinden. Diese können Fehler enthalten.',
  },
  puzzlePicker: {
    heading: 'Rätseltyp wählen',
  },
  puzzle: {
    chooseDifficulty: 'Schwierigkeitsgrad wählen',
  },
  gameSheet: {
    newToGame: (name: string) => `Neu bei ${name}? Starte mit dem Tutorial.`,
  },
  howToPlay: {
    rulesTitle: (name: string) => `${name}-Regeln`,
    tipsTitle: 'Tipps',
  },
  tutorialHost: {
    unavailableTitle: 'Tutorial nicht verfügbar',
    unavailableBody: 'Dieser Rätseltyp enthält noch kein Tutorial.',
  },
  puzzlePlay: {
    loading: 'Rätsel wird geladen…',
    endDialogTitle: 'Dieses Rätsel beenden?',
    endDialogMessage: 'Dein Fortschritt wird nicht gespeichert und das Rätsel endet.',
    endDialogConfirm: 'Rätsel beenden',
  },
  completion: {
    newHighScoreEyebrow: 'Neuer Highscore',
    newHighScoreTitle: 'Fantastische Lösung!',
    newHighScoreBody: 'Du hast einen neuen Bestwert auf diesem Schwierigkeitsgrad erzielt.',
    firstScoreEyebrow: 'Erste Wertung',
    firstScoreTitle: 'Gut gemacht!',
    firstScoreBody: 'Du hast deine erste gelöste Wertung auf diesem Schwierigkeitsgrad eingetragen.',
    solvedEyebrow: 'Rätsel gelöst',
    solvedTitle: 'Rätsel gelöst!',
    solvedBody: 'Schöne Arbeit beim Lösen dieses Rätsels.',
    streakBadge: (streak: number) => `${streak} Rätsel in Folge`,
    score: 'Punkte',
    elapsedTime: 'Verstrichene Zeit',
    accuracy: 'Genauigkeit',
    tryAnotherGame: 'Anderes Spiel versuchen',
    viewStats: 'Dein Fortschritt',
  },
  loss: {
    difficulty: 'Schwierigkeitsgrad',
    elapsedTime: 'Verstrichene Zeit',
    tryAnotherGame: 'Anderes Spiel versuchen',
  },
  analysis: {
    title: 'Analyse',
    back: 'Zurück zur Niederlage',
    analyze: 'Analysieren',
    previous: 'Zurück',
    next: 'Weiter',
    fastJump: 'Zu Schritt springen',
    step: (current: number, total: number) => `Schritt ${current} von ${total}`,
  },
  stats: {
    headerSubtitle: 'Statistiken für diesen Rätseltyp',
    solved: 'Gelöst',
    streak: 'Streak',
    winRate: 'Gewinnrate',
    byDifficulty: 'Nach Schwierigkeitsgrad',
    solvedOutOfPlayed: (solved: number, played: number) => `${solved} von ${played} gespielt gelöst`,
    winRateDetail: (rate: number) => `${rate}% Gewinnrate`,
    bestScore: 'Bester Wert',
    privacy: 'Datenschutz',
    privacyText: 'Alle Statistiken bleiben auf diesem Gerät. Kein Konto erforderlich. Es werden keine Daten gesendet.',
    clearData: 'Daten löschen',
    clearDataTitle: 'Daten löschen',
    clearDataMessage: 'Alle gespeicherten Statistiken und das aktuelle aktive Rätsel entfernen? Dies kann nicht rückgängig gemacht werden.',
  },
  support: {
    title: 'Hilfe',
    subtitle: 'Praktische Hilfe für Fragen, Feedback und Vertrauensdetails.',
    supportSection: 'Hilfe',
    trustSection: 'Vertrauen',
    aboutSection: 'Info',
    reportBug: 'Fehler melden',
    requestFeature: 'Funktion anfragen',
    contact: 'Kontakt',
    opensGithubIssues: 'Öffnet GitHub-Issues',
    privacy: 'Datenschutz',
    sourceCode: 'GitHub / Quellcode',
    opensGithub: 'Öffnet GitHub',
    licenses: 'Open-Source-Lizenzen',
    aboutQuietGrid: 'Über Quiet Grid',
    rateQuietGrid: 'Quiet Grid bewerten',
    opensPlayStore: 'Öffnet Play Store',
    version: (version: string) => `Version ${version}`,
    openErrorTitle: 'Link konnte nicht geöffnet werden',
    openErrorMessage: 'Dein Gerät konnte dieses Ziel gerade nicht öffnen. Quiet Grid funktioniert weiterhin vollständig offline.',
  },
  supportInfoBack: 'Zurück',
  supportInfo: {
    privacy: {
      title: 'Datenschutz',
      intro: 'Quiet Grid funktioniert vollständig offline. Keine Daten verlassen dein Gerät.',
      sections: [
        {
          heading: 'Was Quiet Grid sammelt',
          body: [
            'Quiet Grid sammelt, überträgt, verkauft oder teilt keine persönlichen Daten.',
            'Die App benötigt kein Konto und sendet keine Daten an uns oder Dritte.',
          ],
        },
        {
          heading: 'Was auf diesem Gerät bleibt',
          body: [
            'Quiet Grid speichert lokal eine kleine Menge App-Daten, damit die App wie erwartet funktioniert.',
            'Dazu gehören Design-Einstellung, lokale Statistiken und aktiver Rätselfortschritt.',
          ],
        },
        {
          heading: 'Werbung, Analytics und Tracking',
          body: [
            'Quiet Grid verwendet keine Werbung, Analytics, Tracking, Absturzberichterstattung oder Online-Dienste Dritter.',
          ],
        },
      ],
    },
    about: {
      title: 'Über Quiet Grid',
      intro: 'Quiet Grid ist eine datenschutzorientierte Rätsel-App für ruhige Konzentration.',
      sections: [
        {
          heading: 'Wofür die App ist',
          body: [
            'Quiet Grid hält Logikrätsel leichtgewichtig, vollständig offline, werbefrei und kostenlos nutzbar.',
            'Das Ziel ist einfach: klare Regeln, unterstützendes Tempo und keine Ablenkungen.',
          ],
        },
        {
          heading: 'Was Quiet Grid enthält',
          body: [
            'Quiet Grid enthält derzeit {games}, mit lokalen Statistiken, dunklem, hellem und Bleistift-Design und ohne Konten.',
          ],
        },
      ],
    },
    licenses: {
      title: 'Open-Source-Lizenzen',
      intro: 'Quiet Grid ist Open-Source-Software.',
      sections: [
        {
          heading: 'Projektlizenz',
          body: [
            'Der Quiet Grid-Quellcode ist unter der GNU General Public License v3.0 verfügbar.',
          ],
        },
        {
          heading: 'Pakete von Drittanbietern',
          body: [
            'Quiet Grid verwendet auch Open-Source-Pakete, die ihre eigenen Lizenzen haben.',
            'Du kannst den vollständigen Quellcode und die Paketliste im Projekt-Repository einsehen.',
          ],
        },
      ],
    },
  },
};

const FR_STRINGS: AppStrings = {
  common: {
    goHome: 'Aller aux jeux',
    goBack: 'Retour',
    back: 'Retour',
    home: 'Jeux',
    game: 'Jeu',
    play: 'Jouer',
    playAgain: 'Rejouer',
    previous: 'Précédent',
    next: 'Suivant',
    getStarted: 'Commencer',
    settings: 'Paramètres',
    support: 'Aide',
    stats: 'Statistiques',
    rules: 'Règles',
    tutorial: 'Tutoriel',
    cancel: 'Annuler',
    clear: 'Effacer',
    current: 'En cours',
    on: 'Activé',
    off: 'Désactivé',
    all: 'Tous',
    open: 'Ouvrir',
    reveal: 'Révéler',
    flag: 'Signaler',
    english: 'English',
    dutch: 'Nederlands',
    german: 'Deutsch',
    french: 'Français',
    spanish: 'Español',
    systemDefault: 'Par défaut du système',
    endPuzzle: 'Terminer le puzzle',
    continuePuzzle: 'Continuer le puzzle',
    startNewPuzzle: 'Commencer un nouveau puzzle',
    and: 'et',
  },
  tabs: {
    games: 'Jeux',
    stats: 'Statistiques',
    settings: 'Paramètres',
    support: 'Aide',
  },
  games: {
    subtitle: 'Choisissez un puzzle et gardez votre partie active à portée de main.',
    comingSoon: 'Bientôt disponible',
    betaDisclaimer: 'Ces jeux sont encore en cours de test et peuvent avoir des imperfections.',
  },
  home: {
    openRepo: 'Ouvrir le dépôt GitHub de Quiet Grid',
    brandMark: 'Logo Quiet Grid',
    changeTheme: 'Changer le thème',
    subtitle: 'Logique pure. Sans distractions.',
    activePuzzleWaiting: 'Votre puzzle vous attend',
    startPuzzle: 'Commencer le puzzle',
    trustOffline: 'Open source. Conçu pour fonctionner hors ligne.',
    trustPrivacy: 'Aucune donnée ne quitte votre appareil.',
    repoErrorTitle: "Impossible d'ouvrir la page du projet",
    repoErrorMessage: "Votre appareil n'a pas pu ouvrir le lien GitHub pour l'instant. Quiet Grid fonctionne toujours entièrement hors ligne.",
  },
  replaceDialog: {
    title: 'Remplacer le puzzle actif ?',
    message: 'Vous avez déjà un puzzle actif. Vous pouvez le continuer maintenant ou le remplacer par un nouveau puzzle.',
  },
  welcome: {
    slides: [
      {
        title: 'Bienvenue dans Quiet Grid',
        body: "Puzzles logiques. Pas de publicités, pas de comptes, pas d'internet requis. Tout reste sur votre appareil.",
      },
      {
        title: 'Types de puzzles inclus',
        body: '{games} — chacun avec quatre niveaux de difficulté. Jouez à votre propre rythme.',
      },
      {
        title: 'Hors ligne et open source',
        body: "Vos progrès, statistiques et séries restent uniquement sur cet appareil. Aucune donnée ne quitte votre appareil.",
      },
    ],
  },
  settings: {
    title: 'Paramètres',
    subtitle: "Choisissez votre thème, langue, affichage de la minuterie et si les tutoriels s'ouvrent automatiquement la première fois.",
    appearance: 'Apparence',
    theme: 'Thème',
    language: 'Langue',
    tutorials: 'Tutoriels',
    languageDropdownDetail: "Choisissez la langue de l'application.",
    languageAiDisclaimer: 'Les textes de langue ont été générés par IA et peuvent contenir de petites erreurs.',
    dark: 'Sombre',
    darkDetail: 'Maintient le plateau et les menus dans une palette sombre.',
    light: 'Clair',
    lightDetail: "Utilise une palette plus claire dans toute l'application.",
    pencil: 'Crayon',
    pencilDetail: "Utilise une palette en niveaux de gris façon papier dans toute l'application.",
    languageSystemDetail: 'Suit votre paramètre de langue Android.',
    languageEnglishDetail: "Afficher l'application en anglais.",
    languageDutchDetail: "Afficher l'application en néerlandais.",
    languageGermanDetail: "Afficher l'application en allemand.",
    languageFrenchDetail: "Afficher l'application en français.",
    languageSpanishDetail: "Afficher l'application en espagnol.",
    showTimerInPlayLabel: 'Afficher la minuterie pendant le jeu',
    showTimerInPlayDetail: 'Affiche la minuterie écoulée pendant un puzzle en cours.',
    tutorialsLabel: 'Afficher les tutoriels la première fois',
    tutorialsDetail: 'Ouvre le tutoriel de chaque type de puzzle la première fois que vous le choisissez.',
    betaGamesLabel: 'Activer les jeux bêta',
    betaGamesDetail: 'Déverrouille les jeux encore en cours de test. Ceux-ci peuvent avoir des imperfections.',
  },
  puzzlePicker: {
    heading: 'Choisir le type de puzzle',
  },
  puzzle: {
    chooseDifficulty: 'Choisir la difficulté',
  },
  gameSheet: {
    newToGame: (name: string) => `Nouveau dans ${name} ? Commencez par le tutoriel.`,
  },
  howToPlay: {
    rulesTitle: (name: string) => `Règles de ${name}`,
    tipsTitle: 'Conseils',
  },
  tutorialHost: {
    unavailableTitle: 'Tutoriel non disponible',
    unavailableBody: "Ce type de puzzle ne comprend pas encore de tutoriel.",
  },
  puzzlePlay: {
    loading: 'Chargement du puzzle…',
    endDialogTitle: 'Terminer ce puzzle ?',
    endDialogMessage: 'Votre progression ne sera pas sauvegardée et le puzzle se terminera.',
    endDialogConfirm: 'Terminer le puzzle',
  },
  completion: {
    newHighScoreEyebrow: 'Nouveau meilleur score',
    newHighScoreTitle: 'Résolution incroyable !',
    newHighScoreBody: 'Vous avez établi un nouveau meilleur score sur cette difficulté.',
    firstScoreEyebrow: 'Premier score',
    firstScoreTitle: 'Excellent travail !',
    firstScoreBody: 'Vous avez enregistré votre premier score résolu sur cette difficulté.',
    solvedEyebrow: 'Puzzle résolu',
    solvedTitle: 'Puzzle résolu !',
    solvedBody: 'Bravo pour avoir terminé ce puzzle.',
    streakBadge: (streak: number) => `Série de ${streak} puzzles`,
    score: 'Score',
    elapsedTime: 'Temps écoulé',
    accuracy: 'Précision',
    tryAnotherGame: 'Essayer un autre jeu',
    viewStats: 'Votre progression',
  },
  loss: {
    difficulty: 'Difficulté',
    elapsedTime: 'Temps écoulé',
    tryAnotherGame: 'Essayer un autre jeu',
  },
  analysis: {
    title: 'Analyse',
    back: 'Retour à la défaite',
    analyze: 'Analyser',
    previous: 'Précédent',
    next: 'Suivant',
    fastJump: "Aller à l'étape",
    step: (current: number, total: number) => `Étape ${current} sur ${total}`,
  },
  stats: {
    headerSubtitle: 'Statistiques pour ce type de puzzle',
    solved: 'Résolus',
    streak: 'Série',
    winRate: 'Taux de victoire',
    byDifficulty: 'Par difficulté',
    solvedOutOfPlayed: (solved: number, played: number) => `${solved} résolu sur ${played} joué`,
    winRateDetail: (rate: number) => `${rate}% de taux de victoire`,
    bestScore: 'Meilleur score',
    privacy: 'Confidentialité',
    privacyText: "Toutes les statistiques restent sur cet appareil. Aucun compte requis. Aucune donnée n'est envoyée.",
    clearData: 'Effacer les données',
    clearDataTitle: 'Effacer les données',
    clearDataMessage: 'Supprimer toutes les statistiques sauvegardées et le puzzle actif actuel ? Cette action ne peut pas être annulée.',
  },
  support: {
    title: 'Aide',
    subtitle: 'Aide pratique pour les questions, les commentaires et les détails de confiance.',
    supportSection: 'Aide',
    trustSection: 'Confiance',
    aboutSection: 'À propos',
    reportBug: 'Signaler un bug',
    requestFeature: 'Demander une fonctionnalité',
    contact: 'Contact',
    opensGithubIssues: 'Ouvre les issues GitHub',
    privacy: 'Confidentialité',
    sourceCode: 'GitHub / code source',
    opensGithub: 'Ouvre GitHub',
    licenses: 'Licences open source',
    aboutQuietGrid: 'À propos de Quiet Grid',
    rateQuietGrid: 'Évaluer Quiet Grid',
    opensPlayStore: 'Ouvre le Play Store',
    version: (version: string) => `Version ${version}`,
    openErrorTitle: "Impossible d'ouvrir ce lien",
    openErrorMessage: "Votre appareil n'a pas pu ouvrir cette destination pour l'instant. Quiet Grid fonctionne toujours entièrement hors ligne.",
  },
  supportInfoBack: 'Retour',
  supportInfo: {
    privacy: {
      title: 'Confidentialité',
      intro: "Quiet Grid fonctionne entièrement hors ligne. Aucune donnée ne quitte votre appareil.",
      sections: [
        {
          heading: 'Ce que Quiet Grid collecte',
          body: [
            "Quiet Grid ne collecte, ne transmet, ne vend ni ne partage de données personnelles.",
            "L'application ne nécessite pas de compte et n'envoie pas de données à nous ou à des tiers.",
          ],
        },
        {
          heading: 'Ce qui reste sur cet appareil',
          body: [
            "Quiet Grid stocke localement une petite quantité de données d'application pour que l'application fonctionne correctement.",
            "Cela inclut la préférence de thème, les statistiques locales et la progression du puzzle actif.",
          ],
        },
        {
          heading: 'Publicités, analytiques et suivi',
          body: [
            "Quiet Grid n'utilise pas de publicités, d'analytiques, de suivi, de services de rapport de plantage ou de services en ligne tiers.",
          ],
        },
      ],
    },
    about: {
      title: 'À propos de Quiet Grid',
      intro: 'Quiet Grid est une application de puzzles axée sur la confidentialité, conçue pour une concentration calme.',
      sections: [
        {
          heading: "À quoi sert l'application",
          body: [
            "Quiet Grid garde les puzzles logiques légers, entièrement hors ligne, sans publicités et gratuits à utiliser.",
            "L'objectif est simple : règles claires, rythme encourageant et aucune distraction.",
          ],
        },
        {
          heading: 'Ce que Quiet Grid inclut',
          body: [
            "Quiet Grid inclut actuellement {games}, avec des statistiques locales, des thèmes sombre, clair et crayon, et sans comptes.",
          ],
        },
      ],
    },
    licenses: {
      title: 'Licences open source',
      intro: 'Quiet Grid est un logiciel open source.',
      sections: [
        {
          heading: 'Licence du projet',
          body: [
            "Le code source de Quiet Grid est disponible sous la licence GNU General Public License v3.0.",
          ],
        },
        {
          heading: 'Packages tiers',
          body: [
            "Quiet Grid utilise également des packages open source qui conservent leurs propres licences.",
            "Vous pouvez consulter le code source complet et la liste des packages dans le dépôt du projet.",
          ],
        },
      ],
    },
  },
};

const ES_STRINGS: AppStrings = {
  common: {
    goHome: 'Ir a Juegos',
    goBack: 'Volver',
    back: 'Volver',
    home: 'Juegos',
    game: 'Juego',
    play: 'Jugar',
    playAgain: 'Jugar de nuevo',
    previous: 'Anterior',
    next: 'Siguiente',
    getStarted: 'Comenzar',
    settings: 'Ajustes',
    support: 'Soporte',
    stats: 'Estadísticas',
    rules: 'Reglas',
    tutorial: 'Tutorial',
    cancel: 'Cancelar',
    clear: 'Borrar',
    current: 'Actual',
    on: 'Activado',
    off: 'Desactivado',
    all: 'Todo',
    open: 'Abrir',
    reveal: 'Revelar',
    flag: 'Marcar',
    english: 'English',
    dutch: 'Nederlands',
    german: 'Deutsch',
    french: 'Français',
    spanish: 'Español',
    systemDefault: 'Predeterminado del sistema',
    endPuzzle: 'Terminar puzzle',
    continuePuzzle: 'Continuar puzzle',
    startNewPuzzle: 'Comenzar nuevo puzzle',
    and: 'y',
  },
  tabs: {
    games: 'Juegos',
    stats: 'Estadísticas',
    settings: 'Ajustes',
    support: 'Soporte',
  },
  games: {
    subtitle: 'Elige un puzzle y mantén tu partida activa cerca.',
    comingSoon: 'Próximamente',
    betaDisclaimer: 'Estos juegos aún están en pruebas y pueden tener problemas.',
  },
  home: {
    openRepo: 'Abrir el repositorio GitHub de Quiet Grid',
    brandMark: 'Logo de Quiet Grid',
    changeTheme: 'Cambiar tema',
    subtitle: 'Lógica pura. Sin distracciones.',
    activePuzzleWaiting: 'Tu puzzle te espera',
    startPuzzle: 'Comenzar puzzle',
    trustOffline: 'Código abierto. Diseñado para uso sin conexión.',
    trustPrivacy: 'Ningún dato sale de tu dispositivo.',
    repoErrorTitle: 'No se pudo abrir la página del proyecto',
    repoErrorMessage: 'Tu dispositivo no pudo abrir el enlace de GitHub en este momento. Quiet Grid sigue funcionando completamente sin conexión.',
  },
  replaceDialog: {
    title: '¿Reemplazar puzzle activo?',
    message: 'Ya tienes un puzzle activo. Puedes continuar ahora o reemplazarlo con un nuevo puzzle.',
  },
  welcome: {
    slides: [
      {
        title: 'Bienvenido a Quiet Grid',
        body: 'Puzzles lógicos. Sin anuncios, sin cuentas, sin internet requerido. Todo queda en tu dispositivo.',
      },
      {
        title: 'Tipos de puzzles incluidos',
        body: '{games} — cada uno con cuatro niveles de dificultad. Juega a tu propio ritmo.',
      },
      {
        title: 'Sin conexión y código abierto',
        body: 'Tu progreso, estadísticas y rachas solo viven en este dispositivo. Ningún dato sale de tu dispositivo.',
      },
    ],
  },
  settings: {
    title: 'Ajustes',
    subtitle: 'Elige tu tema, idioma, visualización del temporizador y si los tutoriales se abren automáticamente la primera vez.',
    appearance: 'Apariencia',
    theme: 'Tema',
    language: 'Idioma',
    tutorials: 'Tutoriales',
    languageDropdownDetail: 'Elige el idioma de la aplicación.',
    languageAiDisclaimer: 'Los textos del idioma fueron generados por IA y pueden contener pequeños errores.',
    dark: 'Oscuro',
    darkDetail: 'Mantiene el tablero y los menús en una paleta oscura.',
    light: 'Claro',
    lightDetail: 'Usa una paleta más brillante en toda la aplicación.',
    pencil: 'Lápiz',
    pencilDetail: 'Usa una paleta en escala de grises tipo papel en toda la aplicación.',
    languageSystemDetail: 'Sigue tu configuración de idioma de Android.',
    languageEnglishDetail: 'Mostrar la aplicación en inglés.',
    languageDutchDetail: 'Mostrar la aplicación en neerlandés.',
    languageGermanDetail: 'Mostrar la aplicación en alemán.',
    languageFrenchDetail: 'Mostrar la aplicación en francés.',
    languageSpanishDetail: 'Mostrar la aplicación en español.',
    showTimerInPlayLabel: 'Mostrar temporizador durante el juego',
    showTimerInPlayDetail: 'Muestra el temporizador transcurrido mientras un puzzle está en progreso.',
    tutorialsLabel: 'Mostrar tutoriales la primera vez',
    tutorialsDetail: 'Abre el tutorial de cada tipo de puzzle la primera vez que lo elijas.',
    betaGamesLabel: 'Activar juegos beta',
    betaGamesDetail: 'Desbloquea juegos que aún están en pruebas. Estos pueden tener problemas.',
  },
  puzzlePicker: {
    heading: 'Elegir tipo de puzzle',
  },
  puzzle: {
    chooseDifficulty: 'Elegir dificultad',
  },
  gameSheet: {
    newToGame: (name: string) => `¿Nuevo en ${name}? Empieza con el tutorial.`,
  },
  howToPlay: {
    rulesTitle: (name: string) => `Reglas de ${name}`,
    tipsTitle: 'Consejos',
  },
  tutorialHost: {
    unavailableTitle: 'Tutorial no disponible',
    unavailableBody: 'Este tipo de puzzle aún no incluye un tutorial.',
  },
  puzzlePlay: {
    loading: 'Cargando puzzle…',
    endDialogTitle: '¿Terminar este puzzle?',
    endDialogMessage: 'Tu progreso no se guardará y el puzzle terminará.',
    endDialogConfirm: 'Terminar puzzle',
  },
  completion: {
    newHighScoreEyebrow: 'Nueva puntuación máxima',
    newHighScoreTitle: '¡Resolución increíble!',
    newHighScoreBody: 'Has establecido una nueva mejor puntuación en esta dificultad.',
    firstScoreEyebrow: 'Primera puntuación',
    firstScoreTitle: '¡Buen trabajo!',
    firstScoreBody: 'Has registrado tu primera puntuación resuelta en esta dificultad.',
    solvedEyebrow: 'Puzzle resuelto',
    solvedTitle: '¡Puzzle resuelto!',
    solvedBody: 'Buen trabajo al terminar este puzzle.',
    streakBadge: (streak: number) => `Racha de ${streak} puzzles`,
    score: 'Puntuación',
    elapsedTime: 'Tiempo transcurrido',
    accuracy: 'Precisión',
    tryAnotherGame: 'Probar otro juego',
    viewStats: 'Tu progreso',
  },
  loss: {
    difficulty: 'Dificultad',
    elapsedTime: 'Tiempo transcurrido',
    tryAnotherGame: 'Probar otro juego',
  },
  analysis: {
    title: 'Análisis',
    back: 'Volver a la derrota',
    analyze: 'Analizar',
    previous: 'Anterior',
    next: 'Siguiente',
    fastJump: 'Ir al paso',
    step: (current: number, total: number) => `Paso ${current} de ${total}`,
  },
  stats: {
    headerSubtitle: 'Estadísticas para este tipo de puzzle',
    solved: 'Resueltos',
    streak: 'Racha',
    winRate: 'Tasa de victoria',
    byDifficulty: 'Por dificultad',
    solvedOutOfPlayed: (solved: number, played: number) => `${solved} resueltos de ${played} jugados`,
    winRateDetail: (rate: number) => `${rate}% de tasa de victoria`,
    bestScore: 'Mejor puntuación',
    privacy: 'Privacidad',
    privacyText: 'Todas las estadísticas se quedan en este dispositivo. No se requiere cuenta. No se envían datos.',
    clearData: 'Borrar datos',
    clearDataTitle: 'Borrar datos',
    clearDataMessage: '¿Eliminar todas las estadísticas guardadas y el puzzle activo actual? Esta acción no se puede deshacer.',
  },
  support: {
    title: 'Soporte',
    subtitle: 'Ayuda práctica para preguntas, comentarios y detalles de confianza.',
    supportSection: 'Soporte',
    trustSection: 'Confianza',
    aboutSection: 'Acerca de',
    reportBug: 'Reportar error',
    requestFeature: 'Solicitar función',
    contact: 'Contacto',
    opensGithubIssues: 'Abre issues de GitHub',
    privacy: 'Privacidad',
    sourceCode: 'GitHub / código fuente',
    opensGithub: 'Abre GitHub',
    licenses: 'Licencias de código abierto',
    aboutQuietGrid: 'Acerca de Quiet Grid',
    rateQuietGrid: 'Valorar Quiet Grid',
    opensPlayStore: 'Abre Play Store',
    version: (version: string) => `Versión ${version}`,
    openErrorTitle: 'No se pudo abrir ese enlace',
    openErrorMessage: 'Tu dispositivo no pudo abrir ese destino en este momento. Quiet Grid sigue funcionando completamente sin conexión.',
  },
  supportInfoBack: 'Volver',
  supportInfo: {
    privacy: {
      title: 'Privacidad',
      intro: 'Quiet Grid funciona completamente sin conexión. Ningún dato sale de tu dispositivo.',
      sections: [
        {
          heading: 'Lo que Quiet Grid recopila',
          body: [
            'Quiet Grid no recopila, transmite, vende ni comparte datos personales.',
            'La aplicación no requiere una cuenta y no envía datos a nosotros ni a terceros.',
          ],
        },
        {
          heading: 'Lo que se queda en este dispositivo',
          body: [
            'Quiet Grid almacena localmente una pequeña cantidad de datos de la aplicación para que funcione correctamente.',
            'Esto incluye preferencia de tema, estadísticas locales y progreso del puzzle activo.',
          ],
        },
        {
          heading: 'Anuncios, análisis y seguimiento',
          body: [
            'Quiet Grid no utiliza anuncios, análisis, seguimiento, servicios de informes de fallos ni servicios en línea de terceros.',
          ],
        },
      ],
    },
    about: {
      title: 'Acerca de Quiet Grid',
      intro: 'Quiet Grid es una aplicación de puzzles centrada en la privacidad, creada para la concentración tranquila.',
      sections: [
        {
          heading: 'Para qué es la aplicación',
          body: [
            'Quiet Grid mantiene los puzzles lógicos ligeros, completamente sin conexión, sin anuncios y gratuitos.',
            'El objetivo es simple: reglas claras, ritmo de apoyo y sin distracciones.',
          ],
        },
        {
          heading: 'Lo que incluye Quiet Grid',
          body: [
            'Quiet Grid actualmente incluye {games}, con estadísticas locales, temas oscuro, claro y lápiz, y sin cuentas.',
          ],
        },
      ],
    },
    licenses: {
      title: 'Licencias de código abierto',
      intro: 'Quiet Grid es software de código abierto.',
      sections: [
        {
          heading: 'Licencia del proyecto',
          body: [
            'El código fuente de Quiet Grid está disponible bajo la Licencia Pública General GNU v3.0.',
          ],
        },
        {
          heading: 'Paquetes de terceros',
          body: [
            'Quiet Grid también usa paquetes de código abierto que mantienen sus propias licencias.',
            'Puedes revisar el código fuente completo y la lista de paquetes en el repositorio del proyecto.',
          ],
        },
      ],
    },
  },
};

export function getAppStringsFor(language: ResolvedLanguage): AppStrings {
  switch (language) {
    case 'nl': return NL_STRINGS;
    case 'de': return DE_STRINGS;
    case 'fr': return FR_STRINGS;
    case 'es': return ES_STRINGS;
    case 'en':
    default:
      return EN_STRINGS;
  }
}

export function getAppStrings(): AppStrings {
  return getAppStringsFor(currentLanguage);
}

export function getSupportInfoContent() {
  return getAppStrings().supportInfo;
}
