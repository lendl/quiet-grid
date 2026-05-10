# AI Doc System Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a reusable AI documentation system that supports both new-game work and feature work inside existing subsystems, plus scaffolds and linking guidance that keep future AI sessions aligned with Quiet Grid architecture and product intent.

**Architecture:** Split the implementation into reusable context docs, task-specific workflow docs, scaffold files, and a short Copilot entrypoint. Keep reusable concepts under `docs/ai/context/`, workflow steps under `docs/ai/workflows/`, and scaffolds under `docs/ai/scaffolds/` so AI can load only the files relevant to the task at hand. Preserve the spec as the design source, then materialize it into small focused docs and templates.

**Tech Stack:** Markdown docs, TypeScript scaffold stubs, existing Quiet Grid folder conventions, `npm run lint`, `npm run typecheck:app`, `npm run typecheck:engine`

---

### Task 1: Create reusable AI context docs

**Files:**
- Create: `docs\ai\context\learning-center.md`
- Create: `docs\ai\context\moves.md`
- Create: `docs\ai\context\difficulties.md`
- Create: `docs\ai\context\mistake-policy.md`
- Create: `docs\ai\context\feedback-effects.md`
- Create: `docs\ai\context\context-maps.md`
- Modify: `docs\ai\new-game\README.md`
- Reference: `docs\ai\specs\2026-05-10-new-game-ai-workflow-design.md`

- [ ] **Step 1: Write `docs\ai\context\learning-center.md`**

Use this structure:

```md
# Learning Center

## User goal
- Help players learn the game, improve, and become expert over time.
- Keep tutorial focused on interaction + core move language.
- Keep next move focused on one valid move.
- Keep analyzer focused on full-solution teaching for engine-backed games or loss-state reflection for non-engine games.

## Architecture goal
- Tutorial flow belongs in `ui/tutorial/`.
- Next move belongs in gameplay move detection + play adapter/UI.
- Analyzer belongs in learning/analyzer code and should reuse game move logic.

## Rules
- Tutorial should use valid example grids per lesson.
- Tutorial should not compare difficulty levels.
- Learning Center should teach canonical moves before support actions.
- Difficulty guidance belongs in Learning Center, but outside tutorial.
- Mistake policy should be explained only when it affects player understanding.
```

- [ ] **Step 2: Write `docs\ai\context\moves.md`, `difficulties.md`, `mistake-policy.md`, and `feedback-effects.md`**

Use focused single-topic docs. For example, `docs\ai\context\moves.md` should include:

```md
# Canonical Moves

## User goal
- Give players a consistent reasoning vocabulary for the puzzle.

## Architecture goal
- Canonical moves are the source of truth for tutorial, next move, analyzer, and engine solving/classification.

## Rules
- Separate canonical moves from support actions.
- Support actions are optional style tools, not core move vocabulary.
- AI may suggest default moves for known games, but user approval is required before treating them as final.
```

`difficulties.md` should explicitly require `easy`, `medium`, `hard`, `expert` as playable, and `mistake-policy.md` should explicitly separate mistake policy from loss conditions.

- [ ] **Step 3: Write `docs\ai\context\context-maps.md`**

Include packets for:

```md
- game definition
- gameplay core
- canonical moves
- support actions
- mistake policy
- difficulty system
- play adapter
- play board/components
- Learning Center
  - tutorial
  - next move
  - analyzer
- content/i18n
- persistence/platform
- engine generation
```

Each packet should use:

```md
## <Subsystem>
### User goal
### Architecture goal
### File map
### Mistakes to avoid
```

- [ ] **Step 4: Update `docs\ai\new-game\README.md` to point at reusable context docs first**

Replace its current list with something like:

```md
# AI Doc System

## Reusable context
- `..\context\learning-center.md`
- `..\context\moves.md`
- `..\context\difficulties.md`
- `..\context\mistake-policy.md`
- `..\context\feedback-effects.md`
- `..\context\context-maps.md`

## Task workflows
- `..\workflows\new-game.md`
- `..\workflows\new-game-blueprint.md`
- `..\workflows\new-game-checklist.md`
```

### Task 2: Create task-specific workflow docs

**Files:**
- Create: `docs\ai\workflows\new-game.md`
- Create: `docs\ai\workflows\new-game-blueprint.md`
- Create: `docs\ai\workflows\new-game-checklist.md`
- Reference: `docs\ai\specs\2026-05-10-new-game-ai-workflow-design.md`

- [ ] **Step 1: Write `docs\ai\workflows\new-game.md`**

Include ordered steps like:

```md
1. Confirm the puzzle fits Quiet Grid:
   - logic grid puzzle
   - offline only
   - no bot requirement
   - no internet requirement
2. Propose defaults if the AI already knows the game, but require user approval.
3. Define canonical moves.
4. Define support actions separately.
5. Define mistake policy.
6. Choose existing feedback effects.
7. Split effect creation into a separate change if a required effect does not exist.
8. Define the full difficulty ladder: easy, medium, hard, expert.
9. Decide engine-backed vs non-engine.
10. Decide tutorial, next move, and analyzer scope.
11. Apply scaffold.
12. Wire registries and persistence as needed.
13. Validate with lint and typechecks.
```

- [ ] **Step 2: Write `docs\ai\workflows\new-game-blueprint.md`**

Document expected game package layout:

```md
src/games/<id>/
  definition.ts
  types.ts
  gameplay/
  ui/
  content/
  platform/
  engine/        # optional
  puzzles/       # optional, engine-backed only
```

Also describe what each folder owns and explicitly state that all game-facing copy belongs in `content/i18n/`.

- [ ] **Step 3: Write `docs\ai\workflows\new-game-checklist.md`**

Include checklist items like:

```md
- [ ] app registry updated
- [ ] engine registry updated if applicable
- [ ] canonical moves defined
- [ ] support actions separated
- [ ] mistake policy defined
- [ ] loss condition defined separately if applicable
- [ ] existing feedback effects selected
- [ ] four difficulty levels defined as playable
- [ ] analyzer mode chosen
- [ ] all game-facing copy placed in `content/i18n/`
- [ ] lint run
- [ ] app typecheck run
- [ ] engine typecheck run when engine touched
```

### Task 3: Create scaffold kit

**Files:**
- Create: `docs\ai\scaffolds\base-game\definition.ts`
- Create: `docs\ai\scaffolds\base-game\types.ts`
- Create: `docs\ai\scaffolds\base-game\gameplay\activePuzzle.ts`
- Create: `docs\ai\scaffolds\base-game\gameplay\actions.ts`
- Create: `docs\ai\scaffolds\base-game\gameplay\playContract.ts`
- Create: `docs\ai\scaffolds\base-game\gameplay\moves.ts`
- Create: `docs\ai\scaffolds\base-game\ui\play\adapter.tsx`
- Create: `docs\ai\scaffolds\base-game\content\strings.ts`
- Create: `docs\ai\scaffolds\base-game\content\i18n\index.ts`
- Create: `docs\ai\scaffolds\base-game\content\i18n\en.ts`
- Create: `docs\ai\scaffolds\base-game\content\i18n\nl.ts`
- Create: `docs\ai\scaffolds\engine-addon\engine\definition.ts`
- Create: `docs\ai\scaffolds\engine-addon\engine\generator.ts`
- Create: `docs\ai\scaffolds\engine-addon\engine\difficulty.ts`
- Create: `docs\ai\scaffolds\engine-addon\puzzles\all.ts`

- [ ] **Step 1: Create base scaffold files with minimal structural stubs**

For `docs\ai\scaffolds\base-game\definition.ts`, use a placeholder shape like:

```ts
import type { PuzzleDefinition } from '../../../../src/app/shell/games/gameDefinition';

export const __GAME_ID__Definition: PuzzleDefinition = {
  id: '__GAME_ID__',
  get title() {
    throw new Error('Replace with game strings getter.');
  },
  get shortTitle() {
    throw new Error('Replace with game strings getter.');
  },
  emoji: '🧩',
  get tagline() {
    throw new Error('Replace with game strings getter.');
  },
  supports: {
    tutorial: false,
    learning: false,
    scoring: false,
  },
  difficulties: ['easy', 'medium', 'hard', 'expert'],
  createOutcome: () => {
    throw new Error('Wire game outcome adapter.');
  },
  playAdapter: undefined as never,
};
```

Keep the scaffold intentionally incomplete but structurally correct.

- [ ] **Step 2: Add move and i18n-specific stubs**

For `docs\ai\scaffolds\base-game\gameplay\moves.ts`, use:

```ts
export type __GAME_ID__MoveKind =
  | '__MOVE_ONE__'
  | '__MOVE_TWO__';

export interface __GAME_ID__MoveDefinition {
  kind: __GAME_ID__MoveKind;
  label: string;
  description: string;
}
```

For `docs\ai\scaffolds\base-game\content\i18n\index.ts`, include comments that all game-facing copy belongs here and that tutorial text must not be hardcoded in UI.

- [ ] **Step 3: Add engine add-on stubs that reflect spec rules**

`docs\ai\scaffolds\engine-addon\engine\definition.ts` should explicitly remind the implementer that engine-backed games must provide:

```ts
// Required:
// - single-solution generation
// - difficulty determination
// - reclassification/regeneration path if difficulty rules change
// - solve data sufficient for analyzer to teach from start to finish
```

### Task 4: Link Copilot instructions and validate

**Files:**
- Modify: `.github\copilot-instructions.md`
- Create: `docs\ai\README.md`
- Validate: repo root commands

- [ ] **Step 1: Add `docs\ai\README.md` as top-level index**

Use:

```md
# AI Documentation

## Reusable context
- `context/learning-center.md`
- `context/moves.md`
- `context/difficulties.md`
- `context/mistake-policy.md`
- `context/feedback-effects.md`
- `context/context-maps.md`

## Workflows
- `workflows/new-game.md`
- `workflows/new-game-blueprint.md`
- `workflows/new-game-checklist.md`

## Scaffolds
- `scaffolds/base-game/`
- `scaffolds/engine-addon/`
```

- [ ] **Step 2: Update `.github\copilot-instructions.md` with a short AI-doc pointer section**

Add a short section such as:

```md
## AI context docs

- Reusable subsystem context lives under `docs/ai/context/`
- Task workflows live under `docs/ai/workflows/`
- Reusable scaffolds live under `docs/ai/scaffolds/`
- When doing feature work inside an existing subsystem (for example Learning Center), load the relevant context doc first instead of only reading new-game workflow docs
```

Keep this section short; do not duplicate the full docs into Copilot instructions.

- [ ] **Step 3: Run validation commands**

Run:

```bash
npm run lint
npm run typecheck:app
npm run typecheck:engine
```

Expected:

```text
lint exits 0
typecheck:app exits 0
typecheck:engine exits 0
```

- [ ] **Step 4: Review diff scope**

Run:

```bash
git --no-pager diff -- docs/ai .github/copilot-instructions.md
```

Expected: diff contains only AI docs, scaffolds, and Copilot-linking changes.
