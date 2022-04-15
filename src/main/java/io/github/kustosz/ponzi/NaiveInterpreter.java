package io.github.kustosz.ponzi;

import java.util.HashMap;
import java.util.stream.IntStream;

public class NaiveInterpreter {
  interface Value {
  }

  record Func(Ast.Lambda ast, Env scope) implements Value {
  }

  record Number(long value) implements Value {
  }

  record Boolean(boolean value) implements Value {
  }

  @FunctionalInterface
  interface BuiltinFunc extends Value {
    Value call(List<Value> arguments);
  }

  record NoValue() implements Value {
  }

  record Env(HashMap<String, Value> vars, Env parent) {
    public static Env global() {
      var ret = new Env(new HashMap<>(), null);
      ret.declareBuiltin("zero?", arguments -> new Boolean(arguments.get(0).equals(new Number(0))));

      ret.declareBuiltin("*",
          arguments -> new Number(((Number) arguments.get(0)).value() * ((Number) arguments.get(1)).value()));

      ret.declareBuiltin("+",
          arguments -> new Number(((Number) arguments.get(0)).value() + ((Number) arguments.get(1)).value()));

      ret.declareBuiltin("add",
          arguments -> new Number(((Number) arguments.get(0)).value() + ((Number) arguments.get(1)).value()));

      ret.declareBuiltin("subtract",
          arguments -> new Number(((Number) arguments.get(0)).value() - ((Number) arguments.get(1)).value()));

      return ret;
    }

    private Value lookupNoThrow(String variable) {
      if (vars.containsKey(variable)) {
        return vars.get(variable);
      } else if (parent != null) {
        return parent.lookupNoThrow(variable);
      } else {
        return null;
      }
    }

    public Value lookup(String variable) {
      var res = lookupNoThrow(variable);
      if (res != null) {
        return res;
      } else {
        System.out.println("Env is " + this);
        throw new RuntimeException("No such variable %s.".formatted(variable));
      }
    }

    public Env makeChild() {
      return new Env(new HashMap<>(), this);
    }

    public Env declare(String variable, Value val) {
      vars.put(variable, val);
      return this;
    }

    public Env declareBuiltin(String variable, BuiltinFunc val) {
      vars.put(variable, val);
      return this;
    }
  }

  public static Value interpret(Ast ast) {
    return interpret(ast, Env.global());
  }

  public static Value interpret(Ast ast, Env env) {
    return switch (ast) {
      case Ast.Number n -> new Number(n.value());
      case Ast.Ident i -> env.lookup(i.name());
      case Ast.Lambda l -> new Func(l, env);
      case Ast.Conditional c -> {
        var test = interpret(c.test(), env);
        if (test.equals(new Boolean(false))) {
          if (c.ifFalse() instanceof Option.Some<Ast> s) {
            yield interpret(s.value(), env);
          } else {
            yield new NoValue();
          }
        } else {
          yield (interpret(c.ifTrue(), env));
        }
      }

      case Ast.LetRec block -> {
        var newEnv = env.makeChild();
        block.bindings()
            .forEach(bind -> newEnv.declare(bind.identifier(), interpret(bind.expr(), newEnv)));
        block.statements().forEach(bind -> interpret(bind, newEnv));
        yield interpret(block.returnExpr(), newEnv);
      }

      case Ast.Call c -> {
        var func = interpret(c.function(), env);
        yield switch (func) {
          case Func f -> {
            var newEnv = f.scope().makeChild();
            if (c.arguments().size() != f.ast().formals().size()) {
              throw new RuntimeException("Wrong arity for a lambda call.");
            }
            IntStream.range(0, c.arguments().size())
                .forEach(i -> newEnv.declare(f.ast().formals().get(i),
                    interpret(c.arguments().get(i), env)));
            f.ast().statements().forEach(stmt -> interpret(stmt, newEnv));
            yield interpret(f.ast().returnExpr(), newEnv);
          }
          case BuiltinFunc f -> f.call(c.arguments().map(arg -> interpret(arg, env)));
          case default -> throw new RuntimeException("Expected a function but got " + func);
        };
      }
    };
  }

}
