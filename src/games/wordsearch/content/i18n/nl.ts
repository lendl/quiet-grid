import type { WordSearchI18n } from './index';
import en from './en';

const locale: WordSearchI18n = {
  ...en,
  strings: {
    ...en.strings,
    tagline: 'Trek woorden in rechte lijnen en los het verborgen bonuswoord op in het raster.',
    play: {
      ...en.strings.play,
      hiddenWord: {
        ...en.strings.play.hiddenWord,
        locked: 'Los het verborgen woord in het raster op zodra je er klaar voor bent.',
        revealed: (clue: string, word: string) => `${clue}: ${word}`,
        enterMode: 'Verborgen woord oplossen',
        exitMode: 'Verborgen-woordmodus verlaten',
        progress: (current: number, total: number) => `Verborgen woord ${current}/${total}`,
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
      body: 'Je hebt deze Woordzoeker-sessie beëindigd voordat je alle woorden en het verborgen woord had opgelost.',
    },
  },
  tutorialLessons: {
    ...en.tutorialLessons,
    'hidden-word': {
      title: 'Schakel naar verborgen-woordmodus',
      body: 'Gebruik tijdens het spelen de actie voor het verborgen woord en tik daarna de letters in volgorde aan op het live raster.',
      summary: 'Het verborgen woord los je direct tijdens het spelen op.',
      continueLabel: 'Start puzzel',
    },
  },
};

export default locale;
