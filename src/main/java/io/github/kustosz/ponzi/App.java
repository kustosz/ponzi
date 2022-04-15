package io.github.kustosz.ponzi;

import java.io.IOException;
import java.nio.file.Path;

public final class App {

  public static void main(String[] args) throws IOException {
    var ast = Parser.parse(Path.of("test2.scm"));
    System.out.println(ast);

    var compiled = Continuations.Converter.compile(ast);
    System.out.println(compiled);
    System.out.println("cps: " + Continuations.Interpreter.interpret(compiled));
    System.out.println("naive: " + NaiveInterpreter.interpret(ast));

  }
}
