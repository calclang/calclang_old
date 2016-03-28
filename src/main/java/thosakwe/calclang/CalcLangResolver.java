package thosakwe.calclang;

import thosakwe.calclang.antlr.CalcLangParser;

public class CalcLangResolver {

    public static Double numberFromExpr(CalcLangParser.ExprContext ctx) {
        if (ctx.HEX() != null)
            return (double) Integer.parseInt(ctx.HEX().getText().replaceAll("0x", ""), 16);
        return Double.parseDouble(ctx.getText());
    }

    public static Double operateOnExpr(CalcLangParser.ExprContext ctx, CalcLangParser.OperatorContext operator, Double first, Double second) {
        if (operator.CARET() != null) return Math.pow(first, second);
        else if (operator.TIMES() != null) return first * second;
        else if (operator.SLASH() != null) return first / second;
        else if (operator.PLUS() != null) return first + second;
        else if (operator.MINUS() != null) return first - second;
        else if (operator.MODULO() != null) return first % second;
        return 0.0;
    }
}
