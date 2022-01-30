package io.github.kustosz.ponzi;

import java.util.List;
import java.util.Optional;

sealed public interface Ast {
  record Call(Ast function, List<Ast> arguments) implements Ast {
  }

  record Number(int value) implements Ast {
  }

  record Ident(String name) implements Ast {
  }

  record Lambda(List<String> formals, List<Ast> statements, Ast returnExpr) implements Ast {
  }

  record Conditional(Ast test, Ast ifTrue, Optional<Ast> ifFalse) implements Ast {
  }
}
