package io.github.ferdinandswoboda.folo;

import static java.util.Collections.emptySet;
import static java.util.Objects.requireNonNull;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import org.jooq.Record;

/**
 * {@link Record} {@link Collector} that loads entity-relation graphs from a data set. To create a
 * {@code Loader}, use {@link Loader#of Loader::of}. The loader can be used as follows:
 *
 * <pre>{@code
 * // In static initialisation code, set up the loader
 * private static final Entity<T> MY_ENTITY = ...;
 * private static final Loader<T> TO_LINKED_ENTITIES =
 *     Loader.of(MY_ENTITY)
 *             .relation(...)
 *             .oneToMany(...)
 *             .setOneLeft(...)
 *             .and()
 *             .relation(...)
 *             ...;
 *
 * // At runtime:
 * Query query = ...;
 * List<T> result = query.collect(TO_LINKED_ENTITIES);
 * }</pre>
 *
 * <p>It is highly recommended to initialise the loader as early as possible, because during
 * initialisation a number of validations are performed. Initialising at application start-up
 * therefore makes it possible to detect any misconfiguration before the query is first executed.
 */
public final class Loader<T> implements Collector<Record, ObjectGraph, List<T>> {
  private final Entity<T, ?> mainEntity;
  private final Set<Entity<?, ?>> entities;
  private final Set<Relation<?, ?>> relations;

  Loader(Entity<T, ?> mainEntity, Set<Entity<?, ?>> entities, List<Relation<?, ?>> relations) {
    this.mainEntity = mainEntity;
    this.entities = Set.copyOf(entities);
    this.relations = Set.copyOf(relations);
  }

  /**
   * Initiate a chained builder of a {@link Loader} that will load objects specified by the given
   * {@link Entity}.
   *
   * @param mainEntity The given class to table mapping.
   * @param <T> The type of objects to be loaded.
   * @return The initial builder stage.
   */
  public static <T> LoaderBuilder<T> of(Entity<T, ?> mainEntity) {
    requireNonNull(mainEntity);
    return new LoaderBuilderImpl<>(mainEntity);
  }

  /**
   * Convenience function for improved readability when calling the loader as a parameter of {@link
   * java.util.stream.Stream#collect(Collector)}. It returns the given loader.
   *
   * @param <T> Type of the objects to be loaded by the given loader.
   * @param loader The given loader to be returned.
   * @return The given loader.
   */
  @CanIgnoreReturnValue
  public static <T> Loader<T> toLinkedObjectsWith(Loader<T> loader) {
    return loader;
  }

  @Override
  @SuppressWarnings("NoFunctionalReturnType")
  public Supplier<ObjectGraph> supplier() {
    return ObjectGraph::new;
  }

  @Override
  @SuppressWarnings("NoFunctionalReturnType")
  public BiConsumer<ObjectGraph, Record> accumulator() {
    return (objectGraph, record) -> {
      for (Entity<?, ?> entity : entities) {
        entity
            .getId(record)
            .ifPresent(id -> objectGraph.add(entity, id, () -> entity.load(record)));
      }
      for (Relation<?, ?> relation : relations) {
        objectGraph.add(relation, relation.getRelationLoader().apply(record));
      }
    };
  }

  @Override
  @SuppressWarnings("NoFunctionalReturnType")
  public BinaryOperator<ObjectGraph> combiner() {
    return (first, second) -> {
      first.merge(second);
      return first;
    };
  }

  @Override
  @SuppressWarnings({"NoFunctionalReturnType", "rawtypes", "unchecked"})
  public Function<ObjectGraph, List<T>> finisher() {
    return objectGraph -> {
      for (Relation relation : relations) {
        relation.link(objectGraph.getObjectMapping(relation));
      }
      return new ArrayList<>(objectGraph.getObjects(mainEntity));
    };
  }

  @Override
  public Set<Characteristics> characteristics() {
    return emptySet();
  }
}
