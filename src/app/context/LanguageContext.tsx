import type { ReactNode } from 'react';
import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import {
  getAppStringsFor,
  resolveLanguage,
  setCurrentLanguage,
  type ResolvedLanguage,
} from '../i18n';
import type { LanguageSetting } from '../utils/settingsStorage';
import { loadLanguageSetting, saveLanguageSetting } from '../utils/settingsStorage';

interface LanguageContextValue {
  languageSetting: LanguageSetting | null;
  resolvedLanguage: ResolvedLanguage;
  setLanguageSetting: (setting: LanguageSetting) => void;
  strings: ReturnType<typeof getAppStringsFor>;
}

const LanguageContext = createContext<LanguageContextValue | undefined>(undefined);

export function LanguageProvider({ children }: { children: ReactNode }) {
  const [languageSetting, setLanguageSettingState] = useState<LanguageSetting | null>(null);

  useEffect(() => {
    let mounted = true;

    void loadLanguageSetting().then((setting) => {
      if (mounted) {
        setLanguageSettingState(setting);
      }
    });

    return () => {
      mounted = false;
    };
  }, []);

  const resolvedLanguage = useMemo(
    () => resolveLanguage(languageSetting),
    [languageSetting],
  );
  const strings = useMemo(
    () => getAppStringsFor(resolvedLanguage),
    [resolvedLanguage],
  );

  useEffect(() => {
    setCurrentLanguage(resolvedLanguage);
  }, [resolvedLanguage]);

  const setLanguageSetting = useCallback((setting: LanguageSetting) => {
    setLanguageSettingState(setting);
    void saveLanguageSetting(setting);
  }, []);

  return (
    <LanguageContext.Provider value={{ languageSetting, resolvedLanguage, setLanguageSetting, strings }}>
      {children}
    </LanguageContext.Provider>
  );
}

export function useLanguage(): LanguageContextValue {
  const ctx = useContext(LanguageContext);

  if (!ctx) {
    throw new Error('useLanguage must be used inside LanguageProvider');
  }

  return ctx;
}
