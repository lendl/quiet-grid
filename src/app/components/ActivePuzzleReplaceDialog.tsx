import React from 'react';
import { useLanguage } from '../context/LanguageContext';
import AppDialog from './AppDialog';

interface ActivePuzzleReplaceDialogProps {
  visible: boolean;
  onContinue: () => void;
  onStartNew: () => Promise<void> | void;
  onDismiss: () => void;
}

export default function ActivePuzzleReplaceDialog({
  visible,
  onContinue,
  onStartNew,
  onDismiss,
}: ActivePuzzleReplaceDialogProps) {
  const { strings } = useLanguage();

  return (
    <AppDialog
      visible={visible}
        title={strings.replaceDialog.title}
        message={strings.replaceDialog.message}
        buttons={[
          { text: strings.common.continuePuzzle, onPress: onContinue },
          {
            text: strings.common.startNewPuzzle,
            onPress: onStartNew,
          },
        ]}
        onDismiss={onDismiss}
      />
  );
}
