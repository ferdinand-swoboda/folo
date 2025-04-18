package io.github.ferdinandswoboda.folo;

import static io.github.ferdinandswoboda.folo.Util.validate;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.jooq.Field;
import org.jooq.Record;

/**
 * Represents a relation between two {@link Entity entities}. This class is used to extract links
 * between a data set's rows and set their corresponding objects' references.
 *
 * @param <L> The Java class that the left-hand side of the relation is mapped to.
 * @param <R> The Java class that the right-hand side of the relation is mapped to.
 */
final class Relation<L, R> {
  enum Arity {
    ZERO_OR_ONE,
    ONE,
    MANY
  }

  private final Entity<L, ?> left;
  private final Entity<R, ?> right;
  private final Field<Long> leftKey;
  private final Field<Long> rightKey;
  private final Arity leftArity;
  private final Arity rightArity;
  private final Optional<BiConsumer<L, ?>> leftSetter;
  private final Optional<BiConsumer<R, ?>> rightSetter;
  private final Function<Record, Set<IdPair>> relationLoader;

  @SuppressWarnings("ConstructorLeaksThis")
  Relation(
      Entity<L, ?> left,
      Entity<R, ?> right,
      Field<Long> leftKey,
      Field<Long> rightKey,
      Arity leftArity,
      Arity rightArity,
      Optional<BiConsumer<L, ?>> leftSetter,
      Optional<BiConsumer<R, ?>> rightSetter,
      Optional<Function<Record, Set<IdPair>>> relationLoader) {
    this.left = left;
    this.right = right;
    this.leftKey = leftKey;
    this.rightKey = rightKey;
    this.leftArity = leftArity;
    this.rightArity = rightArity;
    this.leftSetter = leftSetter;
    this.rightSetter = rightSetter;
    this.relationLoader = relationLoader.orElse(this::foreignKeyRelationLoader);
  }

  @Override
  public String toString() {
    return "%s-to-%s Relation<%s (%s), %s (%s)>"
        .formatted(
            leftArity,
            rightArity,
            left.getTable().getName(),
            leftKey.getName(),
            right.getTable().getName(),
            rightKey.getName());
  }

  @SuppressWarnings("NoFunctionalReturnType")
  Function<Record, Set<IdPair>> getRelationLoader() {
    return relationLoader;
  }

  Entity<L, ?> getLeft() {
    return left;
  }

  Entity<R, ?> getRight() {
    return right;
  }

  /** Given an {@link ObjectMapping object mapping}, setters are called on the objects. */
  void link(ObjectMapping<L, R> objectMappping) {
    leftSetter.ifPresent(setter -> link(setter, objectMappping.toSuccessors(), rightArity));
    rightSetter.ifPresent(setter -> link(setter, objectMappping.toPredecessors(), leftArity));
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private <T, U> void link(BiConsumer setter, Map<T, List<U>> objectMapping, Arity arity) {
    switch (arity) {
      case MANY -> linkMany(objectMapping, setter);
      case ONE -> linkOne(objectMapping, setter);
      case ZERO_OR_ONE -> linkOptional(objectMapping, setter);
    }
  }

  private <T, U> void linkOne(Map<T, List<U>> objectMapping, BiConsumer<T, U> setter) {
    objectMapping.forEach(
        (object, successors) -> {
          validate(
              successors.size() <= 1,
              """
                  N-to-1 relation between %s (%s) and %s (%s) contains conflicting tuples.
                  Encountered more than one entity candidate for a given entity on the left side.
                  Please verify that the query result's rows abide by the loader definition.
                  """,
              left,
              leftArity,
              right,
              rightArity);
          validate(
              !successors.isEmpty(),
              """
                  N-to-1 relation between %s (%s) and %s (%s) contains conflicting tuples.
                  Encountered no entity candidate for a given entity on the left side.
                  Please verify that the query result's rows abide by the loader definition.
                  """,
              left,
              leftArity,
              right,
              rightArity);
          setter.accept(object, successors.get(0));
        });
  }

  private <T, U> void linkOptional(
      Map<T, List<U>> objectMapping, BiConsumer<T, Optional<U>> setter) {
    objectMapping.forEach(
        (object, successors) -> {
          validate(
              successors.size() <= 1,
              """
                  N-to-0..1 relation between %s (%s) and %s (%s) contains conflicting tuples.
                  Encountered more than one entity candidate for a given entity on the left side.
                  Please verify that the query result's rows abide by the loader definition.
                  """,
              left,
              leftArity,
              right,
              rightArity);
          setter.accept(object, successors.stream().findFirst());
        });
  }

  private static <T, U> void linkMany(
      Map<T, List<U>> objectMapping, BiConsumer<T, Collection<U>> setter) {
    objectMapping.forEach(setter);
  }

  /**
   * Attempts to load a single pair from this relation from the given record. This is done by
   * looking up the two key fields associated with this record (either a primary key and a foreign
   * key, or two foreign keys in case of a many-to-may relation), and if both can be found, the two
   * values are considered to represent a pair that is part of the relation.
   */
  private Set<IdPair> foreignKeyRelationLoader(Record record) {
    Long leftId = record.get(leftKey);
    Long rightId = record.get(rightKey);
    return (leftId != null && rightId != null) ? Set.of(new IdPair(leftId, rightId)) : Set.of();
  }
}
