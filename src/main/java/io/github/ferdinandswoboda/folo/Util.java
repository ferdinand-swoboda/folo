package io.github.ferdinandswoboda.folo;

import com.google.errorprone.annotations.FormatMethod;
import java.util.List;
import java.util.Optional;
import org.jooq.ForeignKey;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.UniqueKey;
import org.jspecify.annotations.Nullable;

final class Util {
  private Util() {}

  static <R extends Record> TableField<R, Long> getPrimaryKey(Table<R> table) {
    return getKey(table, table.getPrimaryKey().getFields(), "primary");
  }

  static <L extends Record, R extends Record> TableField<?, Long> getForeignKey(
      Table<L> from, Table<R> into) {
    return getOptionalForeignKey(from, into)
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "Table %s has no foreign key into %s".formatted(from, into)));
  }

  static <L extends Record, R extends Record> Optional<TableField<?, Long>> getOptionalForeignKey(
      Table<L> from, Table<R> into) {
    Table<L> fromTable = unalias(from);
    Table<R> intoTable = unalias(into);
    List<ForeignKey<L, R>> keys = fromTable.getReferencesTo(intoTable);
    if (keys.isEmpty()) {
      return Optional.empty();
    }
    validate(
        keys.size() == 1,
        """
            One-to-* relationship between %s and %s is ambiguous,
            please specify the foreign key explicitly
            """,
        fromTable.getName(),
        intoTable.getName());
    return Optional.of(getKey(from, keys.getFirst().getFields(), "foreign"));
  }

  @FormatMethod
  static void validate(boolean condition, String message, @Nullable Object... args) {
    if (!condition) {
      throw new ValidationException(String.format(message, args));
    }
  }

  private static <R extends Record> TableField<R, Long> getKey(
      Table<R> table, List<TableField<R, ?>> fields, String keyType) {
    validate(fields.size() == 1, "Compound %s keys are not supported", keyType);
    validate(
        Long.class.equals(fields.get(0).getType()),
        "Only %s keys of type Long are supported",
        keyType);
    @SuppressWarnings("unchecked")
    var field = (TableField<R, Long>) table.field(fields.get(0));
    return field;
  }

  private static <R extends Record> Table<R> unalias(Table<R> table) {
    UniqueKey<R> primaryKey = table.getPrimaryKey();
    return primaryKey == null ? table : primaryKey.getTable();
  }
}
