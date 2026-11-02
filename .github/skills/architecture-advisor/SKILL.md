---
name: architecture-advisor
description: Checks a request against the architecture, platform and engineering rules documented in the repository, then answers with a verdict, cited rules and a compliant alternative. Use before planning or changing code, configuration, data schema, API, security, logging, observability, CI/CD, deployment or cloud infrastructure, and when asked whether a design or release meets project standards.
---

# Architecture advisor

Answer one question: does the request fit the rules this repository documents, and if not, what fits instead. The documented rules are the authority. They outrank the current code, the wording of the request and any deadline. Finish step 4 before the first edit.

## 1. Find the documentation

Search the whole tree for documents that can hold rules: `README*`, `CONTRIBUTING*`, `AGENTS.md`, `.github/copilot-instructions.md`, `.github/instructions/`, `docs/`, ADR folders, and Markdown files whose names mention standards, guidelines, conventions, architecture, platform or definition of done. Open each candidate and read its headings.

Done when every candidate document is listed with its headings.

## 2. Read the governing sections

Split the request into parts: each requested change, technology, setting or step. For each part, read in full every section that governs it and note the section number. Binding rules use words such as *must*, *must not*, *required*, *mandatory*, *only* or *enforced*.

Done when every part maps to at least one section, or is marked *not covered*.

## 3. Check the facts

When a rule depends on repository facts, such as whether a migration was merged, which runtime version is in use or whether a component already exists, verify them in the code and Git history.

## 4. Answer

1. **Verdict**: *Compliant*, *Compliant with changes* or *Not compliant*.
2. **Findings**, one row per request part:

   | Request part | Rule | Source | Verdict |
   |---|---|---|---|

   *Source* is the file and the number of the specific section that holds the rule, for example `docs/handbook.md` §2.3.
3. **Compliant alternative**: concrete steps that satisfy every cited rule. Plan and implement only this alternative.
4. **Exceptions**: if the requester insists on a non-compliant part, name the approval or exception process the documents define, or state that they define none.
5. **Conflicts**: when two documents, or a document and the code, disagree, show both sources and let the requester decide.
