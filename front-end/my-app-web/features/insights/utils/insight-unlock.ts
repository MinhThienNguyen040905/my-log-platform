/**
 * Weekly reflections on the Insight page unlock once the user has journaled on this many distinct days within
 * the history window. The home-page hint uses the same rule so both pages always agree.
 */
export const INSIGHT_MIN_JOURNAL_DAYS = 3;
/** Dashboard range the distinct days are counted over (also the react-query key both pages share). */
export const INSIGHT_HISTORY_RANGE = '90d';
