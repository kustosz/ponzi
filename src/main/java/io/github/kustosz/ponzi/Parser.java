package io.github.kustosz.ponzi;

import io.github.kustosz.ponzi.parser.PonziBaseVisitor;
import io.github.kustosz.ponzi.parser.PonziLexer;
import io.github.kustosz.ponzi.parser.PonziParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.IOException;
import java.nio.file.Path;

public class Parser {
  private static class Visitor extends PonziBaseVisitor<Ast> {
    @Override
    public Ast visitProcedureCall(PonziParser.ProcedureCallContext ctx) {
      var exprs = ctx.expression().stream().map(this::visit).toList();
      return new Ast.Call(exprs.get(0), List.of(exprs.stream().skip(1).toList()));
    }

    @Override
    public Ast visitIdentifier(PonziParser.IdentifierContext ctx) {
      return new Ast.Ident(ctx.Identifier().getText());
    }

    @Override
    public Ast visitNum_10(PonziParser.Num_10Context ctx) {
      return new Ast.Number(Integer.valueOf(ctx.Number_10().getText(), 10));
    }

    @Override
    public Ast visitLambdaExpression(PonziParser.LambdaExpressionContext ctx) {
      var body = ctx.body().expression().stream().map(this::visit).toList();
      var stmts = List.of(body.stream().limit(body.size() - 1).toList());
      var retVal = body.get(body.size() - 1);
      var formals = List.of(ctx.formals().Identifier().stream().map(ParseTree::getText).toList());
      return new Ast.Lambda(formals, stmts, retVal);
    }

    @Override
    public Ast visitConditional(PonziParser.ConditionalContext ctx) {
      var test = visit(ctx.test);
      var ifTrue = visit(ctx.ifTrue);
      var ifFalse = Option.of(ctx.ifFalse).map(this::visit);
      return new Ast.Conditional(test, ifTrue, ifFalse);
    }

    @Override
    public Ast visitLetRec(PonziParser.LetRecContext ctx) {
      var bindings = List.of(ctx.bindingSpec()
          .stream()
          .map(binding -> new Ast.BindingSpec(binding.var.getText(), visit(binding.expr)))
          .toList());
      var body = ctx.body().expression().stream().map(this::visit).toList();
      var stmts = List.of(body.stream().limit(body.size() - 1).toList());
      var retVal = body.get(body.size() - 1);
      return new Ast.LetRec(bindings, stmts, retVal);
    }
  }

  public static Ast parse(Path path) throws IOException {
    var input = CharStreams.fromPath(path);
    var lexer = new PonziLexer(input);
    var tokenStream = new CommonTokenStream(lexer);
    var parser = new PonziParser(tokenStream);
    var tree = parser.init();
    var ast = new Visitor().visit(tree);
    return ast;
  }
}
