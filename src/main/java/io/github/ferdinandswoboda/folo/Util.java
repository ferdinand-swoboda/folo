package io.github.ferdinandswoboda.folo;

import com.google.errorprone.annotations.FormatMethod;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;
import org.jooq.ForeignKey;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.UniqueKey;

final class Util {
  private Util() {}

  static <R extends Record> TableField<R, UniqueKey<R>> getPrimaryKey(Table<? extends R> table) {
    return table.getPrimaryKey();
  }

  static <L extends Record, R extends Record> TableField<?, Long> getForeignKey(
      Table<L> from, Table<R> into) {
    return getOptionalForeignKey(from, into)
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    String.format("Table %s has no foreign key into %s", from, into)));
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
        "One-to-* relationship between %s and %s is ambiguous, "
            + "please specify the foreign key explicitly",
        fromTable.getName(),
        intoTable.getName());
    return Optional.of(getKey(from, keys.get(0).getFields(), "foreign"));
  }

  @FormatMethod
  static void validate(boolean condition, String message, @Nullable Object... args) {
    if (!condition) {
      throw new ValidationException(String.format(message, args));
    }
  }

  private static <R extends Record> TableField<R, Long> getKey(
          Table<? extends R> table, List<? extends TableField<? extends R, Long>> fields, String keyType) {
    validate(fields.size() == 1, "Compound %s keys are not supported", keyType);
    validate(
        Long.class.equals(fields.get(0).getType()),
        "Only %s keys of type Long are supported",
        keyType);
      return (TableField<R, Long>) table.field(fields.get(0));
  }

  private static <R extends Record> Table<R> unalias(Table<R> table) {
    UniqueKey<R> primaryKey = table.getPrimaryKey();
    return primaryKey == null ? table : primaryKey.getTable();
  }
}
