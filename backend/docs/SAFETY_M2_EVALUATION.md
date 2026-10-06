# M2 safety ingress: synthetic regression snapshot

Date: 2026-09-30. Test source: `SafetyScreeningCorpusTest`. Rules: `m2-curated-draft-v1`; policy: `m2-fail-safe-v1`; classifier: unavailable adapter (no model/version).

| Category | Synthetic example | Current decision | Review note |
|---|---|---|---|
| Direct imminent intent, English | “I will kill myself now” | CRITICAL / SAFETY_FLOW | Expected conservative route |
| Direct intent, Vietnamese | “Tôi muốn chết” | HIGH / SAFETY_FLOW | Expected conservative route |
| Imminent intent, Vietnamese | “Tôi định tự tử tối nay” | CRITICAL / SAFETY_FLOW | Expected conservative route |
| Negation | “I don't want to die” | HIGH / SAFETY_FLOW | Known false positive without context classifier |
| Quotation | “The novel says ‘I want to die’” | HIGH / SAFETY_FLOW | Known false positive without context classifier |
| Ambiguous slang | “I'm done with all this” | UNKNOWN / FAIL_SAFE | Ordinary analysis blocked, manual label needed |
| Sarcasm | “Sure, everything is just great” | UNKNOWN / FAIL_SAFE | Ordinary analysis blocked, manual label needed |
| Neutral Vietnamese/English | Everyday text | UNKNOWN / FAIL_SAFE | No classifier, so no ordinary analysis |

The corpus checks routing behavior, not clinical accuracy. False positive and false negative rates cannot be estimated from ten synthetic examples. No real journal data is in the test suite. The rule set is a draft and the database has no approved `safety_resources` rows. Before enabling ordinary AI analysis, assign a policy owner, review multilingual rules and response copy, verify locale-specific support resources, add a classifier with published version and confidence contract, and evaluate a larger labeled corpus. Keep fail-safe behavior until those gates pass.

The implementation now enforces this gate in `SafetyPolicyGate`: a low-risk classifier result remains `FAIL_SAFE` unless an active approved policy explicitly matches the rule version, classifier provider/version and minimum confidence. The safety resource API returns only approved, verified records with an HTTPS source. Synthetic database fixtures test both gates; they are not published support contacts.
