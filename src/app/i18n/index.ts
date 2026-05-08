import { getLocales } from 'expo-localization';
import type { LanguageSetting } from '../utils/settingsStorage';

export type ResolvedLanguage = 'en' | 'nl';

let currentLanguage: ResolvedLanguage = 'en';

function startsWithDutch(value: string | null | undefined): boolean {
  return typeof value === 'string' && value.toLowerCase().startsWith('nl');
}

export function detectSystemLanguage(): ResolvedLanguage {
  const [locale] = getLocales();

  if (
    startsWithDutch(locale?.languageCode)
    || startsWithDutch(locale?.languageTag)
    || startsWithDutch(locale?.regionCode)
  ) {
    return 'nl';
  }

  return 'en';
}

export function resolveLanguage(setting: LanguageSetting | null): ResolvedLanguage {
  if (setting === 'en' || setting === 'nl') {
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
    goHome: 'Go to Home',
    goBack: 'Go back',
    back: 'Back',
    home: 'Home',
    play: 'Play',
    playAgain: 'Play Again',
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
    open: 'Open',
    reveal: 'Reveal',
    flag: 'Flag',
    english: 'English',
    dutch: 'Nederlands',
    systemDefault: 'System default',
    endPuzzle: 'End puzzle',
    continuePuzzle: 'Continue Puzzle',
    startNewPuzzle: 'Start New Puzzle',
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
        body: 'Two logic puzzles. No ads, no accounts, no internet required. Everything stays on your device.',
      },
      {
        title: 'Two puzzle types included',
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
    subtitle: 'Choose your theme, language, and whether first-time tutorials open automatically.',
    appearance: 'Appearance',
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
    tutorialsLabel: 'Show first-time tutorials',
    tutorialsDetail: 'Opens each puzzle tutorial the first time you choose that puzzle type.',
  },
  puzzlePicker: {
    heading: 'Choose puzzle type',
  },
  puzzle: {
    chooseDifficulty: 'Choose difficulty',
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
  },
  loss: {
    difficulty: 'Difficulty',
    elapsedTime: 'Elapsed time',
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
    goHome: 'Ga naar home',
    goBack: 'Ga terug',
    back: 'Terug',
    home: 'Home',
    play: 'Spelen',
    playAgain: 'Opnieuw spelen',
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
    open: 'Openen',
    reveal: 'Openen',
    flag: 'Markeren',
    english: 'English',
    dutch: 'Nederlands',
    systemDefault: 'Systeemstandaard',
    endPuzzle: 'Puzzel beëindigen',
    continuePuzzle: 'Puzzel hervatten',
    startNewPuzzle: 'Nieuwe puzzel starten',
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
        body: 'Twee logische puzzels. Geen advertenties, geen accounts, geen internet nodig. Alles blijft op je toestel.',
      },
      {
        title: 'Twee puzzeltypes inbegrepen',
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
    subtitle: 'Kies je thema, taal en of tutorials automatisch openen bij de eerste keer.',
    appearance: 'Weergave',
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
    tutorialsLabel: 'Toon tutorials voor de eerste keer',
    tutorialsDetail: 'Opent de tutorial van elk puzzeltype de eerste keer dat je het kiest.',
  },
  puzzlePicker: {
    heading: 'Kies puzzeltype',
  },
  puzzle: {
    chooseDifficulty: 'Kies moeilijkheid',
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
  },
  loss: {
    difficulty: 'Moeilijkheid',
    elapsedTime: 'Verstreken tijd',
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

const STRINGS: Record<ResolvedLanguage, AppStrings> = {
  en: EN_STRINGS,
  nl: NL_STRINGS,
};

export function getAppStringsFor(language: ResolvedLanguage): AppStrings {
  return STRINGS[language];
}

export function getAppStrings(): AppStrings {
  return getAppStringsFor(currentLanguage);
}

export function getSupportInfoContent() {
  return getAppStrings().supportInfo;
}
