# Mistake Policy

## User goal

- Make it clear whether the game notices mistakes, when it notices them, and what that means for feedback or score.

## Architecture goal

- Mistake policy should be defined explicitly in game behavior, not left implicit in random UI or reducer branches.
- Loss condition is separate from mistake policy.

## Rules

- Define whether mistakes exist.
- Define when mistake checks happen.
- Define what feedback the player gets.
- Define whether score is affected.
- Define whether mistakes only give feedback or temporarily block progress.
- Do not treat mistakes as loss conditions.

## Examples

- Takuzu can check completed lines and penalize score.
- Minesweeper can have a loss condition when a mine is triggered, which is not the same thing as mistake policy.

## File map

- `src/games/<id>/gameplay/`
- `src/games/<id>/ui/play/adapter.tsx`
- `src/games/<id>/content/i18n/`
- scoring helpers under `src/app/utils/` when relevant

## Mistakes to avoid

- Do not mix mistake policy and loss condition into one vague rule.
- Do not hide score penalties inside UI-only code.
