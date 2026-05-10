export type AnalyzerFindingTemplate = {
  title: string;
  body: string;
  moveKey: string;
};

export type AnalyzerSummaryTemplate = {
  mode: 'engine-solution' | 'loss-state';
  findings: readonly AnalyzerFindingTemplate[];
};

export function analyzePuzzleStateTemplate(): AnalyzerSummaryTemplate {
  return {
    mode: 'loss-state',
    findings: [
      {
        title: '__ANALYZER_TITLE__',
        body: '__ANALYZER_BODY__',
        moveKey: '__MOVE_KEY__',
      },
    ],
  };
}
