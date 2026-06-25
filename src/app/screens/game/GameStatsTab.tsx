import { ScrollView, StyleSheet } from 'react-native';
import { useLanguage } from '../../context/LanguageContext';
import StatsOverviewSection from '../../stats/components/StatsOverviewSection';
import { useStatsSnapshot } from '../../stats/hooks/useStatsSnapshot';
import { buildStatsOverview } from '../../stats/model/buildStatsOverview';
import type { GameId } from '../../../games/shared/types';

type Props = {
  gameId: GameId;
};

export default function GameStatsTab({ gameId }: Props) {
  const { strings } = useLanguage();
  const { stats } = useStatsSnapshot();

  if (!stats) {
    return null;
  }

  const overview = buildStatsOverview(stats, {
    kind: 'game',
    gameId,
  });

  return (
    <ScrollView contentContainerStyle={styles.scroll}>
      <StatsOverviewSection
        title={strings.stats.byDifficulty}
        solvedLabel={strings.stats.solved}
        streakLabel={strings.stats.streak}
        winRateLabel={strings.stats.winRate}
        bestScoreLabel={strings.stats.bestScore}
        solvedOutOfPlayed={strings.stats.solvedOutOfPlayed}
        winRateDetail={strings.stats.winRateDetail}
        model={overview}
      />
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  scroll: { padding: 20, gap: 0 },
});
