import type { WordSearchI18n } from './index';
import en from './en';

const locale: WordSearchI18n = {
  ...en,
  strings: {
    ...en.strings,
    title: 'Woordzoeker',
    shortTitle: 'Woorden',
    tagline: 'Trek woorden in rechte lijnen en los het verborgen bonuswoord op in het raster.',
    play: {
      ...en.strings.play,
      noPuzzlesDialog: {
        title: 'Woordzoeker niet beschikbaar',
        message: (difficultyLabel: string) => `Nog geen Woordzoeker-puzzel klaar voor ${difficultyLabel}.`,
      },
      hiddenWord: {
        ...en.strings.play.hiddenWord,
        locked: 'Vind alle woorden om het verborgen woord te ontgrendelen.',
        revealed: (clue: string, word: string) => `${clue}: ${word}`,
        solvedTitle: 'Verborgen woord gevonden!',
        enterMode: 'Verborgen woord oplossen',
        exitMode: 'Verborgen-woordmodus verlaten',
        instructions: 'De letters van het verborgen woord staan in de lege cellen, van links naar rechts en van boven naar beneden. Tik ze op volgorde aan.',
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
        body: 'De letters van het verborgen woord vullen de lege cellen van links naar rechts en van boven naar beneden. Zodra alle woorden gevonden zijn, tik je de letters op volgorde aan om te winnen.',
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
};

export default locale;
