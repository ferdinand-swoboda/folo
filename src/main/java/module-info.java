module folo {
  requires java.compiler;
  requires transitive org.jooq;
  requires static jsr305;
  requires static com.google.errorprone.annotations;

  exports io.github.ferdinandswoboda.folo;

  uses org.jooq.Field;
  uses org.jooq.Record;
  uses org.jooq.Table;
  uses org.jooq.TableField;
  uses org.jooq.ForeignKey;
  uses org.jooq.UniqueKey;
}
