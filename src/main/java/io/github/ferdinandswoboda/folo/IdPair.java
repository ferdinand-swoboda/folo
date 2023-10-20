package io.github.ferdinandswoboda.folo;

/**
 * A pair of related IDs.
 *
 * @param leftId The left-hand ID of the relation.
 * @param rightId The right-hand ID of the relation.
 */
record IdPair(long leftId, long rightId) {}
