import type { ChimpTestI18n } from './index';

const de: ChimpTestI18n = {
  strings: {
    title: 'Chimp Test',
    shortTitle: 'Chimp',
    tagline: 'Tippe die Zahlen der Reihe nach an, bevor sie verschwinden.',
    difficultyLabels: {
      easy: 'Leicht',
      medium: 'Mittel',
      hard: 'Schwer',
      expert: 'Experte',
    },
    difficultyDescriptions: {
      easy: '4×4-Raster. Merke dir bis zu 7 Zahlen.',
      medium: '5×5-Raster. Merke dir bis zu 9 Zahlen.',
      hard: '6×6-Raster. Merke dir bis zu 11 Zahlen.',
      expert: '7×7-Raster. Merke dir bis zu 13 Zahlen.',
    },
    play: {
      metadataLabels: {
        round: 'Runde',
        difficulty: 'Schwierigkeit',
      },
    },
  },
  howToPlayGoal: 'Tippe jede Zahl in aufsteigender Reihenfolge an, bevor die zuvor getippten aus dem Blickfeld verschwinden.',
  howToPlayControls: 'Tippe die nächste Zahl in der Folge an. Jede korrekt angetippte Zahl verschwindet sofort – merke dir also, wo die verbleibenden Zahlen sind.',
  howToPlayWrongMove: 'Wenn du ein Feld antippst, das nicht die nächste Zahl ist, endet das Rätsel sofort.',
  howToPlayRules: [
    {
      num: '1',
      title: 'Raster erkunden',
      body: 'Die Zahlen erscheinen in jedem Durchlauf an zufälligen Positionen. Scanne das gesamte Raster, bevor du tippst.',
    },
    {
      num: '2',
      title: 'Der Reihe nach tippen',
      body: 'Tippe immer zuerst auf 1, dann auf 2, 3 und so weiter. Solange die Zahlen sichtbar sind, gibt es keinen Zeitdruck.',
    },
    {
      num: '3',
      title: 'Zahlen verschwinden beim Tippen',
      body: 'Jedes korrekte Antippen entfernt die Zahl aus dem Raster. Merke dir die Positionen der verbleibenden Zahlen.',
    },
    {
      num: '4',
      title: 'Durchläufe werden schwieriger',
      body: 'Jeder erfolgreiche Durchlauf fügt eine weitere Zahl hinzu. Erreiche die maximale Anzahl, um das Rätsel zu lösen.',
    },
  ],
  howToPlayTechniques: [],
  howToPlayTips: [
    {
      key: 'scan-first',
      title: 'Erst scannen, dann tippen',
      body: 'Nimm dir einen Moment, um vor dem Antippen der 1 einen Pfad durch alle Zahlen zu planen. Eine mentale Route ist schneller als das Suchen mitten in der Folge.',
    },
    {
      key: 'group',
      title: 'Benachbarte Zahlen gruppieren',
      body: 'Achte auf Gruppen aufeinanderfolgender Zahlen. Benachbarte Zahlen nacheinander anzutippen ist schneller, als quer über das Raster zu springen.',
    },
  ],
  howToPlayScoring: 'Die Punktzahl basiert auf der Gesamtzeit über alle Durchläufe. Schnelleres Tippen ergibt eine höhere Punktzahl.',
  loss: {
    abandoned: {
      eyebrow: 'Rätsel beendet',
      title: 'Rätsel nicht abgeschlossen',
      body: 'Du hast dieses Rätsel beendet, bevor es gelöst war.',
      icon: '🏁',
    },
    'rule-failure': {
      eyebrow: 'Rätsel verloren',
      title: 'Rätsel verloren',
      body: 'Du hast ein falsches Feld angetippt.',
      icon: '⚠️',
    },
  },
};

export default de;
