# Word Search Corpus Guidelines

Rules for adding words to word search theme seed files (`src/games/wordsearch/puzzles/seeds/<lang>.ts`).

## Purpose

These rules ensure every word is appropriate for a global children's audience, renders correctly in the puzzle grid, and contributes meaningfully to its theme.

## Hard rules

- **No real names of people** — living or historical, celebrated or notorious. This includes athletes, musicians, scientists, politicians, and religious figures.
- **Place names are allowed** — countries, capitals, cities, mountains, rivers, oceans, continents, and well-known landmarks (e.g. `Everest`, `Amazon`, `Paris`, `Sahara`). Avoid names of politically disputed territories.
- **No fictional character names** — no names owned by a franchise or tied to a specific copyright (e.g. no `Simba`, `Elsa`, `Sherlock`). Generic mythological terms used as common nouns are fine (e.g. `Dragon`, `Phoenix`).
- **No brand or product names** — no trademarks or company names (e.g. no `Lego`, `Adidas`, `Google`).
- **No political or religious content** — no political parties, ideologies, movements, or doctrine terms.
- **Child-safe vocabulary only** — no violence, weapons, drugs, alcohol, profanity, or adult themes. When in doubt, ask: *would a primary school teacher use this word in a classroom activity without any parent objecting?*
- **Single words only** — no spaces, no hyphens. Compound concepts must be written as one word (e.g. `Blackhole`, `Raincoat`) or dropped if that looks unnatural.
- **Standard dictionary English** — no abbreviations, acronyms, slang, or internet/text-speak.

## Word length

- **Minimum: 3 characters** — shorter words are too easy to find by accident.
- **Maximum: 14 characters** — longer words exceed the largest planned grid dimension and will never be placed by the generator.
- Prefer words in the **4–10 character** range; they appear most often across all difficulty levels.

## Form and casing

- Use **Title Case**: first letter uppercase, rest lowercase (e.g. `Elephant`, `Raincoat`).
- Use the word's **most natural standalone form** — typically singular for nouns, base form for verbs. Exception: words only natural as plurals (`Scissors`, `Leggings`, `Tights`).
- Do not conjugate verbs or use possessives.

## Theme relevance

- Every word must clearly belong to its theme without explanation. A player reading the theme name should immediately recognise the connection.
- A word may appear in more than one theme if it genuinely fits both contexts. Duplication is fine.
- Prefer the most specific term for the context. For example, in a food theme use the food-specific name rather than the animal it comes from (`Beef`, `Drumstick`) — the animal name belongs in an animals theme.
- Aim for variety within a theme: cover different sub-categories rather than listing near-synonyms.
- **Do not pad a theme past its natural vocabulary.** Quality over quantity — a theme with 60 well-known words is better than one with 100 where 40 are obscure.

## Theme design

Themes should be broad enough that a typical player recognises most of the words. Narrow specialist topics run out of well-known vocabulary quickly and should be absorbed into a broader theme.

Before adding a new theme, verify:

1. It has a natural pool of at least 80 words that a typical primary-school child would recognise.
2. Its words don't belong more naturally in an existing theme.
3. It is not a sub-category of an existing theme (e.g. "reptiles" would be a sub-category of an animals theme).

The seed files are the source of truth for which themes exist. Do not maintain a separate list here.

## Corpus size targets

- **Minimum per theme: 50 words** (enforced by `WORD_SEARCH_MIN_WORDS_PER_THEME` in `engine/constraints.ts`).
- **Target per theme: 80–120 words** — needed to support larger grids (10×10+) without puzzles feeling repetitive across sessions.
