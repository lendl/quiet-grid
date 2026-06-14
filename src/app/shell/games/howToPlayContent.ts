export type HowToPlayCellValue = string | number | null;

export interface HowToPlayTip {
  key: string;
  title: string;
  body: string;
  example?: readonly (readonly HowToPlayCellValue[])[];
}

export interface HowToPlayRule {
  num: string;
  title: string;
  body: string;
}

export interface HowToPlayTechnique {
  key: string;
  title: string;
  body: string;
}

export interface HowToPlayContent {
  goal: string;
  controls: string;
  wrongMove: string;
  rules: readonly HowToPlayRule[];
  techniques: readonly HowToPlayTechnique[];
  scoring?: string;
  tips: readonly HowToPlayTip[];
}
