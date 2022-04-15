package io.github.kustosz.ponzi;

import java.util.HashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Continuations {

  sealed interface CValue {
    record Number(int value) implements CValue {
      @Override
      public String toString() {
        return String.valueOf(value);
      }
    }

    record Var(String name) implements CValue {
      @Override
      public String toString() {
        return name;
      }
    }
  }

  sealed interface CExp {
    record App(CValue function, List<CValue> arguments) implements CExp {
      @Override
      public String toString() {
        return new List.Cons<>(function, arguments).stream()
            .map(Object::toString)
            .collect(Collectors.joining(" ", "(", ")"));
      }
    }

    record If(CValue condition, CExp ifTrue, CExp ifFalse) implements CExp {
      @Override
      public String toString() {
        return Stream.of("if", condition, ifTrue, ifFalse)
            .map(Object::toString)
            .collect(Collectors.joining(" ", "(", ")"));
      }
    }

    record LetRec(List<BindingSpec> bindings, CExp cont) implements CExp {
      @Override
      public String toString() {
        return Stream.of(Stream.of("letrec*"), bindings.stream(), Stream.of(cont))
            .flatMap(i -> i)
            .map(Object::toString)
            .collect(Collectors.joining(" ", "(", ")"));
      }
    }

    record BindingSpec(String name, List<String> formals, CExp definition) {
      @Override
      public String toString() {
        var arglist = formals.stream().collect(Collectors.joining(" ", "(", ")"));
        return Stream.of(name, arglist, definition.toString())
            .collect(Collectors.joining(" ", "(", ")"));
      }
    }
  }

  static class Converter {
    private int lastVar = 0;

    private String genVar() {
      return "$$var" + lastVar++;
    }

    private String genCont() {
      return "$$cont" + lastVar++;
    }

    static CExp compile(Ast ast) {
      return new Converter().convert(ast, x -> new CExp.App(new CValue.Var("$$HALT"), List.of(x)));
    }

    CExp convert(Ast ast, Function<CValue, CExp> cont) {
      return switch (ast) {
        case Ast.Number n -> cont.apply(new CValue.Number(n.value()));
        case Ast.Ident i -> cont.apply(new CValue.Var(i.name()));
        case Ast.Lambda l -> {
          var f = genVar();
          var k = genCont();
          var body = convert(l.returnExpr(),
              z -> new CExp.App(new CValue.Var(k), List.of(z))); //todo BODY
          var binding = new CExp.BindingSpec(f, l.formals().append(k), body);
          yield new CExp.LetRec(List.of(binding), cont.apply(new CValue.Var(f)));
        }
        case Ast.Call c -> {
          var r = genCont();
          var x = genCont();
          var app = convert(c.function(),
              f -> convertMany(c.arguments(),
                  args -> new CExp.App(f, args.append(new CValue.Var(r)))));
          var binding = new CExp.BindingSpec(r, List.of(x), cont.apply(new CValue.Var(x)));
          yield new CExp.LetRec(List.of(binding), app);
        }

        case Ast.LetRec lr -> {
          var newBindings = lr.bindings().map(this::convertBinding);
          yield new CExp.LetRec(newBindings, convert(lr.returnExpr(), cont)); // todo BODY
        }

        case Ast.Conditional c -> {
          var k = genCont();
          var x = genVar();
          var ifT = convert(c.ifTrue(), v -> new CExp.App(new CValue.Var(k), List.of(v)));
          var ifF = c.ifFalse()
              .map(iff -> convert(iff, v -> new CExp.App(new CValue.Var(k), List.of(v))))
              .get(); // TODO
          var body = convert(c.test(), v -> {
            var cond = new CExp.If(v, ifT, ifF);
            var bindSpec = new CExp.BindingSpec(k, List.of(x), cont.apply(new CValue.Var(x)));
            return new CExp.LetRec(List.of(bindSpec), cond);
          });
          yield body;
        }
      };
    }

    CExp.BindingSpec convertBinding(Ast.BindingSpec binding) {
      if (binding.expr() instanceof Ast.Lambda l) {
        var w = genCont();
        var body = convert(l.returnExpr(),
            z -> new CExp.App(new CValue.Var(w), List.of(z))); // todo BODY
        return new CExp.BindingSpec(binding.identifier(), l.formals().append(w), body);
      } else {
        throw new RuntimeException("Can't do this (yet?)");
      }
    }

    CExp convertMany(List<Ast> exprs, Function<List<CValue>, CExp> cont) {
      return switch (exprs) {
        case List.Empty e -> cont.apply(List.empty());
        case List.Cons<Ast> cons -> convert(cons.head(),
            hd -> convertMany(cons.tail(), tl -> cont.apply(new List.Cons<>(hd, tl))));
      };
    }
  }

  public static class Interpreter {
    static Value interpretValue(CValue value, Env scope) {
      return switch (value) {
        case CValue.Number n -> new Number(n.value);
        case CValue.Var v -> scope.lookup(v.name);
      };
    }

    static Result interpretUntilCont(CExp expr, Env scope) {
      return switch (expr) {
        case CExp.App a -> {
          var fn = (Callable) interpretValue(a.function, scope);
          var args = a.arguments.map(arg -> interpretValue(arg, scope));
          yield new NextCall(fn, args);
        }
        case CExp.If i -> {
          var cond = interpretValue(i.condition, scope);
          if (cond.equals(new Boolean(false))) {
            yield interpretUntilCont(i.ifFalse, scope);
          } else {
            yield interpretUntilCont(i.ifTrue, scope);
          }
        }
        case CExp.LetRec l -> {
          var childScope = scope.makeChild();
          l.bindings.forEach(spec -> childScope.declare(spec.name,
              new Func(spec.name, spec.formals, spec.definition, childScope)));
          yield interpretUntilCont(l.cont, childScope);
        }
      };
    }

    static Value interpret(CExp expr) {
      var scope = Env.global();
      Result lastResult = interpretUntilCont(expr, scope);
      while (lastResult instanceof NextCall nc) {
        lastResult = nc.fn().call(nc.arguments());
      }
      return ((Final) lastResult).result;
    }

    interface Value {
    }

    @FunctionalInterface
    interface Callable extends Value {
      Result call(List<Value> arguments);
    }

    record Func(String name, List<String> formals, CExp body, Env scope) implements Callable {
      @Override
      public Result call(List<Value> arguments) {
        if (formals.size() != arguments.size()) {
          throw new RuntimeException("Wrong arity when calling " + name);
        }
        var frame = scope.makeChild();
        formals.zip(arguments).forEach(p -> frame.declare(p.fst(), p.snd()));
        return interpretUntilCont(body, frame);
      }
    }

    record Number(long value) implements Value {
    }

    record Boolean(boolean value) implements Value {
    }

    record NoValue() implements Value {
    }

    sealed interface Result {
    }

    record Final(Value result) implements Result {
    }

    record NextCall(Callable fn, List<Value> arguments) implements Result {
    }

    record Env(HashMap<String, Value> vars, Env parent) {
      public static Env global() {
        var ret = new Env(new HashMap<>(), null);
        ret.declareBuiltin("zero?",
            arguments -> new NextCall((Callable) arguments.get(1),
                List.of(new Boolean(arguments.get(0).equals(new Number(0))))));

        ret.declareBuiltin("*",
            arguments -> new NextCall((Callable) arguments.get(2),
                List.of(new Number(((Number) arguments.get(0)).value() * ((Number) arguments.get(1)).value()))));

        ret.declareBuiltin("+",
            arguments -> new NextCall((Callable) arguments.get(2),
                List.of(new Number(((Number) arguments.get(0)).value() + ((Number) arguments.get(1)).value()))));

        ret.declareBuiltin("add",
            arguments -> new NextCall((Callable) arguments.get(2),
                List.of(new Number(((Number) arguments.get(0)).value() + ((Number) arguments.get(1)).value()))));

        ret.declareBuiltin("subtract",
            arguments -> new NextCall((Callable) arguments.get(2),
                List.of(new Number(((Number) arguments.get(0)).value() - ((Number) arguments.get(1)).value()))));

        ret.declareBuiltin("$$HALT", arguments -> new Final(arguments.get(0)));

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

      public Env declareBuiltin(String variable, Callable val) {
        vars.put(variable, val);
        return this;
      }
    }
  }
}
