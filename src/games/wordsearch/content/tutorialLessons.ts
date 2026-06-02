import {
  getWordSearchTutorialLessonCopies,
  type WordSearchTutorialLessonCopy,
  type WordSearchTutorialLessonKey,
} from './i18n';

export type { WordSearchTutorialLessonCopy, WordSearchTutorialLessonKey };

export function getWordSearchTutorialLessons(): Record<WordSearchTutorialLessonKey, WordSearchTutorialLessonCopy> {
  return getWordSearchTutorialLessonCopies();
}
