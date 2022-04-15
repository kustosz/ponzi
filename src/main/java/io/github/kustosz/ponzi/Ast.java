package io.github.kustosz.ponzi;

sealed public interface Ast {
  record Call(Ast function, List<Ast> arguments) implements Ast {
  }

  record Number(int value) implements Ast {
  }

  record Ident(String name) implements Ast {
  }

  record Lambda(List<String> formals, List<Ast> statements, Ast returnExpr) implements Ast {
  }

  record Conditional(Ast test, Ast ifTrue, Option<Ast> ifFalse) implements Ast {
  }

  record LetRec(List<BindingSpec> bindings, List<Ast> statements, Ast returnExpr) implements Ast {
  }

  record BindingSpec(String identifier, Ast expr) {
  }
}
