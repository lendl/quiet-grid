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

export interface HowToPlayContent {
  rules: readonly HowToPlayRule[];
  tips: readonly HowToPlayTip[];
}
