import type { WordSearchI18n } from './index';
import en from './en';

const locale: WordSearchI18n = {
  ...en,
  strings: {
    ...en.strings,
    title: 'Woordzoeker',
    shortTitle: 'Woorden',
    tagline: 'Trek woorden in rechte lijnen en los het verborgen bonuswoord op in het raster.',
    tutorial: {
      ...en.strings.tutorial,
      exitLabel: {
        skip: 'Tutorial overslaan',
        end: 'Woordzoeker openen',
      },
      checkpoint: {
        prompt: 'Je wilt STAR selecteren. Na het tikken op S, wat tik je dan?',
        validOption: 'R — de laatste letter',
        invalidOption: 'T — de volgende letter',
        correctFeedback: 'Correct — tik de eerste letter, dan de laatste. Het pad wordt automatisch getraceerd.',
        wrongFeedback: 'Niet helemaal. Tik alleen op de eerste en laatste letter — niet op elke letter ertussen.',
      },
    },
    play: {
      ...en.strings.play,
      noPuzzlesDialog: {
        title: 'Woordzoeker niet beschikbaar',
        message: (difficultyLabel: string) => `Nog geen Woordzoeker-puzzel klaar voor ${difficultyLabel}.`,
      },
      hiddenWord: {
        ...en.strings.play.hiddenWord,
        locked: 'Los het verborgen woord in het raster op zodra je er klaar voor bent.',
        revealed: (clue: string, word: string) => `${clue}: ${word}`,
        solvedTitle: 'Verborgen woord gevonden!',
        enterMode: 'Verborgen woord oplossen',
        exitMode: 'Verborgen-woordmodus verlaten',
        instructions: 'Tik de letters van het verborgen woord in volgorde aan op het raster.',
        resetOnMistake: 'Een verkeerde tik op het verborgen woord zet je voortgang terug.',
        nextLetterTitle: (clue: string) => `Verborgen woord: ${clue}`,
        nextLetterBody: 'De gemarkeerde cel is de volgende letter van het verborgen woord.',
      },
    },
  },
  howToPlay: {
    ...en.howToPlay,
    rules: [
      en.howToPlay.rules[0],
      en.howToPlay.rules[1],
      {
        num: '3',
        title: 'Los het verborgen woord op',
        body: 'Gebruik tijdens het spelen de verborgen-woordmodus en tik daarna de letters in volgorde aan op het raster.',
      },
    ],
  },
  loss: {
    ...en.loss,
    abandoned: {
      ...en.loss.abandoned,
      title: 'Woordzoeker-sessie beëindigd',
      body: 'Je hebt deze Woordzoeker-sessie beëindigd voordat je alle woorden en het verborgen woord had opgelost.',
    },
    'rule-failure': {
      ...en.loss['rule-failure'],
      title: 'Woordzoeker heeft geen regelovertreding',
      body: 'Ongeldige selecties worden genegeerd in Woordzoeker. Blijf scannen en ga door met de puzzel.',
    },
  },
  tutorialLessons: {
    ...en.tutorialLessons,
    'win-condition': {
      title: 'Vind het verborgen woord om te winnen',
      body: 'De woorden in de lijst zijn thema-aanwijzingen — ze helpen je het verborgen bonuswoord te identificeren. Het verborgen woord vinden is de enige manier om te winnen.',
      summary: 'Normale woorden zijn aanwijzingen. Het verborgen woord is het doel.',
      continueLabel: 'Volgende les',
    },
    'selection': {
      title: 'Tik begin, dan einde',
      body: 'Tik op de eerste letter van een woord, dan op de laatste. Het spel trekt het pad — horizontaal, verticaal, diagonaal of met één hoek.',
      summary: 'Twee tikken selecteren een woord.',
      continueLabel: 'Volgende les',
    },
    'no-penalty': {
      title: 'Foute gokken verdwijnen gewoon',
      body: 'Als er geen woord gevonden wordt, verdwijnt de selectie — geen straf. Scan vrij en probeer elke combinatie van begin en einde.',
      summary: 'Niets te verliezen — blijf scannen.',
      continueLabel: 'Volgende les',
    },
    'hidden-word': {
      title: 'Vind het verborgen woord',
      body: 'Tik op het sleutelicoontje om de verborgen-woordmodus in te schakelen. De letters staan verspreid door het raster. De balk onderaan toont elke letter terwijl je tikt — vind ze allemaal om te winnen.',
      summary: 'Voortgang onderaan begeleidt je letter voor letter.',
      continueLabel: 'Start puzzel',
    },
  },
};

export default locale;
