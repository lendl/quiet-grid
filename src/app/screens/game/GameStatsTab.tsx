import { ScrollView, StyleSheet } from 'react-native';
import type { BottomTabScreenProps } from '@react-navigation/bottom-tabs';
import GamePageShell from '../../components/GamePageShell';
import { useLanguage } from '../../context/LanguageContext';
import type { GameTabParamList } from '../../navigation/types';
import StatsOverviewSection from '../../stats/components/StatsOverviewSection';
import { useStatsSnapshot } from '../../stats/hooks/useStatsSnapshot';
import { buildStatsOverview } from '../../stats/model/buildStatsOverview';

type Props = BottomTabScreenProps<GameTabParamList, 'Stats'>;

export default function GameStatsTab({ route }: Props) {
  const { strings } = useLanguage();
  const { stats } = useStatsSnapshot();

  if (!stats) {
    return (
      <GamePageShell
        headerMode="back"
        contentTransitionDirection="forward"
        gameNav={{
          context: 'tabs',
          activeTab: 'Stats',
          gameId: route.params.gameId,
        }}
      />
    );
  }

  const overview = buildStatsOverview(stats, {
    kind: 'game',
    gameId: route.params.gameId,
  });

  return (
    <GamePageShell
      headerMode="back"
      contentTransitionDirection="forward"
      gameNav={{
        context: 'tabs',
        activeTab: 'Stats',
        gameId: route.params.gameId,
      }}
    >
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
    </GamePageShell>
  );
}

const styles = StyleSheet.create({
  scroll: { padding: 20, gap: 0 },
});
