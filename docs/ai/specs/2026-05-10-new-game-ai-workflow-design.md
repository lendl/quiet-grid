# New Game AI Workflow Design

## Problem

Quiet Grid needs a better AI-facing system for adding new games. Future AI sessions should not need to rediscover architecture, product intent, file ownership, or validation rules from scratch.

The goal is to make AI-led game additions:

- low-error
- low-context-loss
- consistent with repo architecture
- consistent with code style and conventions
- consistent with product intent
- easy to update over time

This design uses **small, role-based files** instead of one large instruction document.

## Design goals

1. Keep AI context small and focused.
2. Make game architecture explicit.
3. Make product intent explicit, not only technical ownership.
4. Reduce missed wiring when adding a new game.
5. Give AI a real scaffold so it copies good structure instead of inventing it.
6. Make puzzle **moves** a first-class concept shared across gameplay, education, analysis, and engine logic.
7. Make the same docs reusable for non-game features, especially feature work inside existing subsystems like Learning Center.

## New game eligibility rules

Not every game idea should fit Quiet Grid.

A new game should:

- be a **logic grid puzzle**
- work fully **offline**
- not require any internet connection
- not require a bot or external opponent

These rules should appear early in the AI workflow so future sessions reject mismatched game ideas before implementation starts.

## Core idea

Future AI sessions should use a **two-layer AI doc system**:

1. **reusable context docs**
   - explain shared product and architecture concepts
   - usable for both new games and feature work inside existing systems
2. **task-specific workflow docs**
   - explain the ordered process for a specific type of work, like adding a new game

Then task-specific artifacts can build on top:

3. a base game scaffold
4. an optional engine add-on scaffold
5. a concrete implementation and review checklist

Each context file should be loaded by **role**, not all at once.

## Context model

Every major subsystem doc should use the same 3-layer format:

1. **User goal**  
   What this part is trying to achieve for the player.

2. **Architecture goal**  
   Where this responsibility belongs in Quiet Grid code.

3. **File map**  
   Which files to inspect, which files own behavior, and which reference files to compare against.

This keeps future AI from making changes that are technically valid but wrong for the player experience.

## Reuse rule

Some of these concepts are broader than “add a new game”.

For example:

- Learning Center context is needed when adding a tutorial improvement
- difficulty context is needed when tuning progression rules
- move-system context is needed when adding a new hint or analyzer feature
- mistake policy context is needed when changing scoring or validation feedback

So the docs should not all live under a “new game only” mental model. Shared concepts should exist as reusable context docs, and new-game docs should point to them.

## AI suggestion rule

If the AI already knows the puzzle type well, it may propose sensible defaults to help the user move faster.

Examples:

- core rules
- canonical moves
- support actions
- mistake policy
- difficulty ladder ideas
- tutorial direction
- Learning Center structure

But these should always be treated as **proposals**, not decisions.

### Approval rule

The user should always approve the important game-design choices before AI treats them as final.

That includes approval of:

- puzzle rules
- move vocabulary
- mistake policy
- difficulty approach
- tutorial direction
- analyzer mode

So for a familiar puzzle like Sudoku, AI may suggest a strong starting model, but the user remains the authority on what Quiet Grid should actually ship.

## Puzzle move system

Every puzzle game should define a **selection of canonical moves** that represent how expert reasoning works in that game.

Examples:

- Takuzu:
  - find pairs
  - avoid trios
  - complete lines
- Nonogram:
  - overlap fill
  - forced empty
  - complete line

### Moves vs support actions

New games should also distinguish between:

- **canonical moves** — core logical reasoning steps required to solve puzzles well
- **support actions** — optional player-style actions that may help some players but are not part of the core reasoning vocabulary

Example:

- Minesweeper
  - hitting a bomb and reasoning about safe cells belong to core puzzle rules
  - flagging is a **support action**

Support actions can be useful, but they should not automatically be treated as equally fundamental teaching targets.

### Move system purpose

Moves are not only hint labels. They should be part of the **core design of each game package**.

### Move system responsibilities

For every game, canonical moves should drive:

- **Learning Center**
  - tutorial should introduce key moves
  - next move should explain one valid move
  - analyzer should explain missed or available moves
- **Gameplay analysis**
  - move detection and explanation should come from the game’s move logic
- **Engine solving and generation**
  - engine should use move logic to solve puzzles, classify difficulty, and support generation constraints

Support actions may be documented and surfaced in UI, but they should remain secondary to the canonical move system.

## Mistake policy

New games should define a clear **mistake policy**.

Mistake policy means:

- whether the game detects mistakes during play
- when mistakes are checked
- what feedback the user gets
- whether mistakes affect score
- whether mistakes only give feedback, affect score, or block progress

Mistakes should **not** be treated as loss conditions.

A game can have a separate **loss condition**, but that is different from mistake policy.

Example:

- Takuzu can detect a completed line mistake and penalize score
- Minesweeper can have a loss condition when a mine is triggered
- triggering a mine is not the same thing as a “mistake policy” in this model

Example:

- Takuzu checks a row or column when it becomes complete
- if that completed line contains a rule violation, the game recognizes it as a mistake
- that mistake affects score

### Mistake policy rule

When adding a new game, AI should make mistake policy explicit instead of leaving it implicit in gameplay code.

That means a new game should define:

- **mistakes allowed or not**
- **mistake detection trigger**
  - e.g. per move, on line completion, on board completion, never
- **player feedback**
  - e.g. highlight, message, penalty only
- **score impact**
  - e.g. no impact, penalty
- **interaction impact**
  - e.g. feedback only, temporary block, correction required

Loss conditions should be defined separately from mistake policy.

### Mistake policy and Learning Center

Learning Center should explain mistake policy only when it matters to player understanding.

It should help answer:

- what kind of errors the game notices
- when the game will call something a mistake
- whether mistakes are part of score pressure or only feedback

It should keep mistake policy separate from loss conditions.

It should not overload the player with scoring internals unless that affects play decisions.

## Feedback effects

New games should use a **pre-selected feedback effect set**.

Feedback effects means the reusable visual or interaction responses the app uses to show game state changes, such as:

- success feedback
- mistake feedback
- hint emphasis
- completion emphasis
- line or cell highlight effects

### Feedback effects rule

When adding a new game, AI should not invent a brand-new feedback effect inline unless that effect already exists and has been chosen up front.

If the desired effect does not already exist, that should be treated as a **separate change**:

1. create or approve the reusable effect first
2. merge that effect as its own isolated change
3. only then add the new game using that pre-selected effect

Adding a new reusable effect and a new game in one go is too large and too error-prone a change.

### Feedback effects and Learning Center

Learning Center can rely on existing feedback effects, but it should not introduce new effect systems as part of game addition work.

That keeps teaching behavior consistent and keeps new-game changes focused on the game itself.

### Move system rule

Future AI sessions should not design tutorial, next move, analyzer, and engine as separate reasoning systems. They should all derive from the same move vocabulary.

That means a new game should define:

- move names
- move meaning
- move detection logic
- move explanation copy
- how move complexity relates to difficulty
- support actions separately from canonical moves when they exist

## Difficulty system

Every new game should ship with the four standard Quiet Grid difficulty levels as playable:

- easy
- medium
- hard
- expert

### Difficulty purpose

Difficulty should not be a cosmetic label. It should reflect meaningful differences in the move complexity, solve path, and learning demands of the puzzle set.

### Difficulty validation rules

When adding a new game, AI should validate that the difficulty model is credible:

- each of the four levels should make sense for that game
- the separation between levels should be meaningful
- generated or selected puzzles should not collapse into one bucket
- a level should not be technically declared but practically unreachable

Examples of invalid outcomes:

- easy is almost impossible to generate
- less than 5% of generated puzzles fit medium
- hard and expert are functionally the same
- difficulty depends on arbitrary size alone without move complexity differences

### Difficulty engine rule

If a game has engine support, the engine should help validate whether current difficulty rules are working in practice. That includes:

- classifying puzzles into easy, medium, hard, expert
- checking whether bucket distribution is reasonable
- allowing existing catalogs to be re-evaluated if difficulty heuristics change

## Learning Center definition

Every new game context pack should include an overarching **Learning Center** definition.

### Learning Center purpose

Quiet Grid is not only trying to let users finish puzzles. It wants users to **learn the game, improve their reasoning, and become expert players over time**.

### Learning Center scope

The Learning Center is the umbrella for:

- **Tutorial** — first-time teaching flow for the game’s interaction model and core moves
- **Next Move** — in-play hinting that helps users see one valid move
- **Analyzer** — post-failure or review flow that explains what the player could have learned from the board state

### Tutorial

Tutorial should explain how the user can interact with the puzzle and how the core move language works.

When tutorial is applicable, it should use an **example grid** for each lesson.

Tutorial example grids should be:

- valid for the game
- correct for the lesson being taught
- consistent with the game rules
- small and focused enough to highlight one lesson concept at a time

AI should not invent tutorial boards loosely. The example grid should match the real puzzle rules and the exact move or interaction the lesson is trying to teach.

### Next Move

Next Move should explain one valid move in the current puzzle state.

It should:

- point to a real valid move from the game’s move logic
- explain why that move is valid
- support learning without solving everything for the player at once

Next Move belongs to Learning Center because it helps the player improve while still actively playing.

### Analyzer modes

Analyzer behavior depends on whether the game uses engine-backed puzzles.

#### Engine-backed games

If a game uses the engine and has a solvable generated puzzle definition, the analyzer should be able to teach the player **how to solve the puzzle from start to finish**.

That means analyzer can use the canonical move system to walk through the whole solve path, not only the player’s last mistake.

#### Non-engine games

If a game does not use the engine and does not rely on a predefined solvable puzzle path in the same way, the analyzer should instead focus on the **loss moment**.

That means it should inspect the board state at the point of loss and explain:

- what the user could have inferred at that moment
- which move(s) were available
- what the user missed or misunderstood

So Analyzer has two valid modes:

- **full-solution teaching** for engine-backed games
- **loss-state reflection** for non-engine games

### Learning Center rule

These should be treated as connected parts of one learning mission:

- tutorial teaches foundations
- next move supports active improvement during play
- analyzer supports reflection after mistakes or loss

Learning Center should focus primarily on **canonical moves**, not overload the user with every support action.

### Learning Center and support actions

Support actions should be grouped and handled carefully.

Rules:

- Learning Center should not assume all players use the same support actions
- Learning Center should not over-teach optional support actions
- support actions should be explained as optional style tools when appropriate
- core teaching should still center on canonical moves

Example:

- in Minesweeper, some players use flags heavily
- other players avoid flags because they prefer faster play
- Learning Center should not assume flagging is mandatory for skill growth
- it can mention flagging as an optional support action, but should not build the whole learning path around it

### Learning Center and difficulty

Learning Center should also explain the difficulty ladder.

It should help the player understand:

- what kind of reasoning is expected at the current difficulty
- which move patterns define the next difficulty
- what the player should master before moving up

That means future AI should treat difficulty as part of education, not only puzzle sorting.

This does **not** mean tutorial should compare difficulty levels. Tutorial should focus on how to interact with the puzzle and understand the core move language. Difficulty guidance belongs elsewhere in Learning Center.

## Artifact layout

### 1. Short always-on instructions

Keep `.github/copilot-instructions.md` short. It should:

- state that new games must follow the game-package architecture
- point to the modular AI docs
- state that AI should load only the docs relevant to the subsystem being edited
- state that product intent context is required, not only file ownership
- state that canonical puzzle moves are required for new games

It should **not** become the full new-game manual.

### 2. Small focused docs under `docs/ai/`

Recommended split:

**Reusable context docs**
- `docs/ai/context/learning-center.md`
  - product intent and architecture rules for tutorial, next move, analyzer, and difficulty progression
- `docs/ai/context/moves.md`
  - canonical move system rules, support action rules, and expectations
- `docs/ai/context/difficulties.md`
  - four-tier difficulty rules, validation expectations, and separation criteria
- `docs/ai/context/mistake-policy.md`
  - mistake policy and loss-condition separation
- `docs/ai/context/feedback-effects.md`
  - reusable effect selection and split-change rule
- `docs/ai/context/context-maps.md`
  - subsystem-by-subsystem context packets using user goal + architecture goal + file map

**Task-specific workflow docs**
- `docs/ai/workflows/new-game.md`
  - end-to-end process for adding a new game
- `docs/ai/workflows/new-game-blueprint.md`
  - expected package structure and file responsibilities
- `docs/ai/workflows/new-game-checklist.md`
  - implementation and review checklist with exact wiring points

Each file should stay focused enough that AI can load it alone.

### 3. Base scaffold

Add a reusable base scaffold folder for new games. It should model the default package shape without assuming generator support.

Recommended base scaffold contents:

- `definition.ts`
- `types.ts`
- `gameplay/activePuzzle.ts`
- `gameplay/actions.ts`
- `gameplay/playContract.ts`
- `gameplay/moves.ts`
- `ui/play/adapter.tsx`
- `content/strings.ts`
- `content/i18n/index.ts`
- `content/i18n/en.ts`
- `content/i18n/nl.ts`
- minimal starter folders for `ui/tutorial`, `ui/learning`, and `platform` when supported by the game

The scaffold should be structural and instructional, not a fake finished game.

### 4. Optional engine add-on

Add a separate engine add-on scaffold for games with generated puzzle support.

Recommended contents:

- `engine/definition.ts`
- `engine/generator.ts`
- `engine/difficulty.ts`
- `puzzles/all.ts`

Engine should remain optional and separate from the base scaffold so non-generator games do not inherit irrelevant files.

### Non-engine rule

A new game does not need a predefined puzzle catalog or engine support. Games like Minesweeper can still fit Quiet Grid without using the engine.

That means the AI workflow must explicitly branch between:

- **engine-backed games**
- **non-engine games**

### Engine rules

When a game has engine support, the engine guidance must require that it:

- generates puzzles with **exactly one solution**
- determines puzzle difficulty
- can **re-evaluate or regenerate existing puzzles** if difficulty rules are updated later
- exposes enough move-based solving information for analyzer to teach the player from start to finish

This means engine support is not only “make random puzzles”. It must support:

- uniqueness checking
- difficulty classification
- catalog maintenance when difficulty heuristics change

## Workflow for future AI sessions

The workflow doc should instruct AI to follow this order:

1. gather game rules and product requirements first
2. check that the game fits Quiet Grid eligibility rules:
   - logic grid puzzle
   - offline only
   - no bot requirement
   - no internet requirement
3. identify canonical move set for the game
4. identify support actions separately from canonical moves
5. define mistake policy
6. choose pre-existing feedback effects the game will use
7. if needed feedback effect does not exist yet, stop and split work into:
   - add reusable effect first
   - add game second
8. define how moves map across all four difficulty levels:
   - easy
   - medium
   - hard
   - expert
9. validate that the four tiers are meaningful and separable
10. decide whether the game is:
   - engine-backed
   - non-engine
11. decide whether the game needs Learning Center surfaces:
   - tutorial
   - next move
   - analyzer
12. choose the base scaffold
13. add engine scaffold only if generator support exists
14. fill package files under `src/games/<id>/`
15. wire `src/app/shell/games/gameRegistry.ts`
16. wire `src/engine/gameRegistry.ts` only if engine support exists
17. update persistence and normalization only where needed
18. keep all user-visible copy in `content/i18n/`
19. choose analyzer mode:
   - full-solution teaching for engine-backed games
   - loss-state reflection for non-engine games
20. ensure Learning Center focuses on canonical moves and treats support actions as optional style tools
21. ensure Learning Center explains mistake policy where it affects play understanding
22. if engine exists, ensure it supports:
   - single-solution generation
   - difficulty determination
   - puzzle reclassification or regeneration when difficulty settings change
   - validation that the difficulty buckets are actually usable and meaningfully separated
   - move-based solve data sufficient for start-to-finish analyzer teaching
23. ensure Learning Center explains current difficulty expectations and next-step growth
24. validate with:
   - `npm run lint`
   - `npm run typecheck:app`
   - `npm run typecheck:engine` when engine files changed
25. review against checklist before stopping

## Context maps

The context-maps doc should give future AI focused “zoom-in packets” for each major subsystem.

Each packet should answer:

- what is this subsystem for the player?
- what is this subsystem for the architecture?
- which files own it?
- which reference files should AI inspect before editing?
- which mistakes should AI avoid?

### Required packets

- game definition
- gameplay core
- canonical moves
- support actions
- mistake policy
- feedback effects
- difficulty system
- play adapter
- play board/components
- engine vs non-engine mode
- Learning Center
  - tutorial
  - next move
  - analyzer
  - difficulty progression
- content/i18n
- persistence/platform
- engine generation

### Example: tutorial packet

The tutorial packet should explicitly say:

**User goal**
- teach how the user can interact with the puzzle
- teach core mental model of the game in small steps
- reduce first-play confusion
- explain why a move is valid
- introduce the game’s core moves in a usable order

**Architecture goal**
- tutorial flow state belongs in `ui/tutorial/screen.tsx`
- tutorial board helpers belong in `ui/tutorial/components/`
- lesson configuration belongs in `content/tutorialLessons.ts`
- all game-facing copy belongs in `content/i18n/`, including tutorial copy
- shared shell layout should be reused when possible

**File map**
- inspect same-game tutorial files first
- inspect Takuzu and Minesweeper tutorial implementations when behavior or layout questions exist
- inspect shared scaffold only when the layout contract is relevant

**Mistakes to avoid**
- do not put game-facing copy inline in screens or components
- do not move lesson data into UI files
- do not create one-off layout when the shared scaffold fits
- do not teach random facts that are disconnected from canonical moves
- do not turn tutorial into a comparison between difficulty levels

## Checklist design

The checklist doc should be concrete and file-specific. It should include items like:

- definition added at `src/games/<id>/definition.ts`
- app registry updated
- engine registry updated if applicable
- game passes Quiet Grid eligibility rules
- persisted active puzzle shape defined
- storage normalization updated if needed
- all user text in `content/i18n`
- canonical move set defined
- support actions identified separately from canonical moves
- mistake policy defined explicitly
- pre-selected feedback effects chosen from existing system
- if desired feedback effect is missing, effect work split out before game addition
- all four difficulty levels defined as playable: easy, medium, hard, expert
- separation between difficulty levels validated as meaningful
- difficulty distribution checked so tiers are not theoretical or vanishingly rare
- if engine exists, unique-solution guarantee defined
- if engine exists, difficulty classification defined
- if engine exists, reclassification/regeneration path for existing puzzles defined
- if engine exists, difficulty bucket quality checked against actual generation output
- analyzer mode chosen explicitly:
  - engine-backed = start-to-finish solve teaching
  - non-engine = loss-state analysis
- Learning Center decision made explicitly:
  - tutorial yes/no
  - next move yes/no
  - analyzer yes/no
- Learning Center centers canonical moves, not optional support actions
- support actions explained only where helpful and without assuming one player style
- Learning Center explains mistake policy when it changes how the user should play
- Learning Center explains current difficulty and what is needed for the next difficulty
- validation commands run

The checklist should be usable both during implementation and during review.

## Error-prevention rules

The system should reduce these common failure modes:

1. **Architecture drift**
   - solved by scaffold + blueprint doc

2. **Missing wiring**
   - solved by checklist + workflow doc

3. **Product-intent drift**
   - solved by user goal sections and Learning Center doc

4. **Move-system fragmentation**
   - solved by canonical moves doc and move-first workflow

5. **Support-action overreach**
   - solved by explicit canonical-move vs support-action split

6. **Hidden mistake behavior**
   - solved by explicit mistake policy definition

7. **Too much change at once**
   - solved by splitting reusable feedback-effect creation from game addition

8. **Bad difficulty ladder**
   - solved by mandatory four-tier difficulty doc and validation rules

9. **Context overload**
   - solved by multiple small docs instead of one large doc

10. **Bad copying between games**
   - solved by reference-file guidance inside context maps

## Naming rules

New-game docs should use the same repo language consistently:

- **Learning Center** is the umbrella for tutorial, next move, and analyzer
- **canonical moves** means the game’s core logical move types
- **support actions** means optional play-style actions that may help some players but are not the core logical move vocabulary
- **mistake policy** means whether the game detects player mistakes, when it checks them, what feedback it gives, and how mistakes affect score or run state
- **loss condition** means the rule that ends a puzzle attempt, and it is separate from mistake policy
- **feedback effects** means reusable visual or interaction feedback patterns already available in the app
- **difficulty ladder** means easy, medium, hard, expert progression
- **game package** means `src/games/<id>/`
- **app shell** means shared app-side flow under `src/app/`
- **engine add-on** means optional generator support files

## Expected result

After this system exists, a future AI session adding a game should be able to:

- load a short new-game workflow
- load only the subsystem packets it needs
- copy the correct scaffold
- define canonical moves early
- identify support actions without confusing them with core moves
- define mistake policy early
- choose existing feedback effects early
- define a real four-level difficulty ladder early
- keep Learning Center and engine logic aligned with those moves
- keep Learning Center aligned with difficulty progression
- follow repo architecture and styling more reliably
- understand both technical ownership and player-facing purpose

That should make new game additions faster, more consistent, and less error-prone.
