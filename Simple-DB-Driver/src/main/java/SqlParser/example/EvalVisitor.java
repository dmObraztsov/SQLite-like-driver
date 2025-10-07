package SqlParser.example;

import SqlParser.ExprBaseVisitor;
import SqlParser.ExprParser;

public class EvalVisitor extends ExprBaseVisitor<Integer> {

    @Override
    public Integer visitInt(ExprParser.IntContext ctx) {
        Integer result = Integer.valueOf(ctx.INT().getText());
        System.out.println("Visit INT: " + ctx.INT().getText() + " = " + result);
        return result;
    }
    @Override
    public Integer visitDis(ExprParser.DisContext ctx)
    {
        int left = visit(ctx.expr(0));
        int right = visit(ctx.expr(1));

        return (left == 1 && right == 1) ? 1 : 0;
    }

    @Override
    public Integer visitCon(ExprParser.ConContext ctx)
    {
        int left = visit(ctx.expr(0));
        int right = visit(ctx.expr(1));

        return (left == 1 || right == 1) ? 1 : 0;
    }

    @Override
    public Integer visitParens(ExprParser.ParensContext ctx) {
        System.out.println("Visit Parens");
        Integer result = visit(ctx.expr());
        System.out.println("Parens result: " + result);
        return result;
    }

    @Override
    public Integer visitStart(ExprParser.StartContext ctx) {
        System.out.println("Visit Start");
        Integer result = visit(ctx.expr());
        System.out.println("Start result: " + result);
        return result;
    }
}