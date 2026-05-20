import type { FailureReason } from '../../loss/types';

export interface LossContentEntry {
  eyebrow: string;
  title: string;
  body: string;
  icon: string;
}

export type LossContent = Record<FailureReason, LossContentEntry>;
