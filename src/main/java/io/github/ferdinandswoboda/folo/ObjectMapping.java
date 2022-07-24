package io.github.ferdinandswoboda.folo;

import java.util.List;
import java.util.Map;

/**
 * Complete, bidirectional mapping of {@link L} objects to {@link R} objects, representing a {@link
 * Relation relation}.
 *
 * @param <L> The Java class that the left-hand side of the relation is mapped to.
 * @param <R> The Java class that the right-hand side of the relation is mapped to.
 * @param toSuccessors A map of all {@link L} objects to their related {@link R} objects.
 * @param toPredecessors A reverse map of all {@link R} objects to their related {@link L} objects.
 */
// XXX: Workaround for https://github.com/google/error-prone/issues/2321
@SuppressWarnings({"InvalidParam", "ImmutableMemberCollection"})
record ObjectMapping<L, R>(Map<L, List<R>> toSuccessors, Map<R, List<L>> toPredecessors) {}
