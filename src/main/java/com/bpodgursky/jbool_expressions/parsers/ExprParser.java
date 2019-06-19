package com.bpodgursky.jbool_expressions.parsers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collector;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenStream;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.Tree;

import com.bpodgursky.jbool_expressions.And;
import com.bpodgursky.jbool_expressions.Expression;
import com.bpodgursky.jbool_expressions.Literal;
import com.bpodgursky.jbool_expressions.Not;
import com.bpodgursky.jbool_expressions.Or;
import com.bpodgursky.jbool_expressions.Variable;

public class ExprParser {

  public static Expression<String> parse(String expression) {
    return parse(expression, new IdentityMap(), null);
  }
  public static  Expression<String> parse(String expression, Set<String> parameterCollector) {
    return parse(expression, new IdentityMap(), parameterCollector);
  }
  public static <T> Expression<T> parse(String expression, TokenMapper<T> mapper) {
    return parse(expression, mapper, null);

  }
  public static <T> Expression<T> parse(String expression, TokenMapper<T> mapper, Set<String> parameterCollector) {
    try {
      //lexer splits input into tokens
      ANTLRStringStream input = new ANTLRStringStream(expression);
      TokenStream tokens = new CommonTokenStream(new BooleanExprLexer(input));

      //parser generates abstract syntax tree
      BooleanExprParser parser = new BooleanExprParser(tokens);
      BooleanExprParser.expression_return ret = parser.expression();

      //acquire parse result
      CommonTree ast = (CommonTree)ret.getTree();
      return parse(ast, mapper, parameterCollector);
    } catch (RecognitionException e) {
      throw new IllegalStateException("Recognition exception is never thrown, only declared.");
    }
  }

  public static <T> Expression<T> parse(Tree tree, TokenMapper<T> mapper,  Set<String> parameterCollector) {
    if (tree.getType() == BooleanExprParser.AND) {
      List<Expression<T>> children = new ArrayList<>();
      for (int i = 0; i < tree.getChildCount(); i++) {
        Tree child = tree.getChild(i);
        Expression<T> parse = parse(child, mapper, parameterCollector);
        if (child.getType() == BooleanExprParser.AND) {
          children.addAll(Arrays.asList(((And<T>)parse).expressions));
        } else {
          children.add(parse);
        }
      }

      return And.of(children);
    } else if (tree.getType() == BooleanExprParser.OR) {
      List<Expression<T>> children = new ArrayList<>();
      for (int i = 0; i < tree.getChildCount(); i++) {
        Tree child = tree.getChild(i);
        Expression<T> parse = parse(child, mapper, parameterCollector);
        if (child.getType() == BooleanExprParser.OR) {
          children.addAll(Arrays.asList(((Or<T>)parse).expressions));
        } else {
          children.add(parse);
        }
      }
      return Or.of(children);
    } else if (tree.getType() == BooleanExprParser.NOT) {
      return Not.of(parse(tree.getChild(0), mapper, parameterCollector));
    } else if (tree.getType() == BooleanExprParser.NAME) {
      if (parameterCollector != null) {
        parameterCollector.add(tree.getText());
      }
      return Variable.of(mapper.getVariable(tree.getText()));
    } else if (tree.getType() == BooleanExprParser.QUOTED_NAME) {
      if (parameterCollector != null) {
        parameterCollector.add(tree.getText());
      }
      return Variable.of(mapper.getVariable(tree.getText()));
    } else if (tree.getType() == BooleanExprParser.DOUBLE_QUOTED_NAME) {
      if (parameterCollector != null) {
        parameterCollector.add(tree.getText());
      }
      return Variable.of(mapper.getVariable(tree.getText()));
    } else if (tree.getType() == BooleanExprParser.TRUE) {
      return Literal.getTrue();
    } else if (tree.getType() == BooleanExprParser.FALSE) {
      return Literal.getFalse();
    } else if (tree.getType() == BooleanExprParser.LPAREN) {
      return parse(tree.getChild(0), mapper, parameterCollector);
    } else {
      throw new RuntimeException("Unrecognized! " + tree.getType() + " " + tree.getText());
    }
  }

}
