module io.github.ferdinandswoboda.folo {
  requires java.compiler;
  requires transitive org.jooq;
  requires static com.google.errorprone.annotations;
  requires static org.jetbrains.annotations;

  exports io.github.ferdinandswoboda.folo;

  uses org.jooq.Field;
  uses org.jooq.Record;
  uses org.jooq.Table;
  uses org.jooq.TableField;
  uses org.jooq.ForeignKey;
  uses org.jooq.UniqueKey;
}
