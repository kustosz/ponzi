package io.github.kustosz.ponzi;

import java.io.IOException;
import java.nio.file.Path;

public final class App {

  public static void main(String[] args) throws IOException {
    var ast = Parser.parse(Path.of("test.scm"));
    System.out.println(ast);
    System.out.println(NaiveInterpreter.interpret(ast));
  }
}
