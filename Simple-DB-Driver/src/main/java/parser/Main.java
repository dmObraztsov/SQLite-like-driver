package parser;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;

public class Main {
    public static void main(String[] args) {
        String input = "1 && (0 || (1 && 0))";

        try {
            ExprLexer lexer = new ExprLexer(CharStreams.fromString(input));
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            ExprParser parser = new ExprParser(tokens);

            ParseTree tree = parser.start();

            EvalVisitor eval = new EvalVisitor();
            Integer result = eval.visit(tree);

            System.out.println("Результат: " + result);

        } catch (Exception e) {
            System.err.println("Ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }
}