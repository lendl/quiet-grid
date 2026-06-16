# AI Documentation

The context files below are general guardrails that apply to any work in this codebase — bug fixes, feature changes, i18n updates, and new games alike. Load the relevant context file for the subsystem you are touching. The workflows and scaffolds are specific to adding a new game.

## Conventions

- `terminology.md` — player-facing copy rules, preferred terms, game title localization, loss screen conventions
- Prefer canonical game-package imports (`gameplay/`, `ui/`, `content/`, `platform/`, `engine/`). Older games may still keep root-level compatibility shims, but new work should treat those as adapters, not the target structure.

## Reusable context

- `context/learning-center.md` — how to play + technique explanation rules and architecture
- `context/techniques.md` — canonical technique definitions, support action distinction, one-file-per-technique rule
- `context/engine.md` — generation pipeline, difficulty classification, balancing guardrails, catalog structure
- `context/difficulties.md`
- `context/mistake-policy.md`
- `context/feedback-effects.md`
- `context/context-maps.md` — per-subsystem file maps and gotchas; load instead of the full docs when editing one area

## Workflows (new game only)

- `workflows/new-game.md`
- `workflows/new-game-blueprint.md`
- `workflows/new-game-checklist.md`

## Scaffolds (new game only)

- `scaffolds/base-game/`
- `scaffolds/engine-addon/`
