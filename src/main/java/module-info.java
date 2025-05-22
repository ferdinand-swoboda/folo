module folo {
  requires java.compiler;
  requires transitive org.jooq;
  requires static org.jspecify;
  requires static com.google.errorprone.annotations;

  exports io.github.ferdinandswoboda.folo;
}
