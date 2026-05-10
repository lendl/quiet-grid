import React from 'react';
import { Text, View } from 'react-native';

export type TutorialLessonTemplate = {
  key: string;
  title: string;
  body: string;
  exampleBoardId: string;
};

export const tutorialLessonsTemplate: readonly TutorialLessonTemplate[] = [
  {
    key: '__LESSON_KEY__',
    title: '__LESSON_TITLE__',
    body: '__LESSON_BODY__',
    exampleBoardId: '__VALID_EXAMPLE_BOARD__',
  },
];

export default function TutorialScreenTemplate() {
  return (
    <View>
      <Text>Replace this scaffold with a valid lesson flow and valid example boards.</Text>
    </View>
  );
}
