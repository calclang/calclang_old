package thosakwe.calclang.vm;

import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.cli.CommandLine;
import thosakwe.calclang.CalcLangResolver;
import thosakwe.calclang.CalcLangTarget;
import thosakwe.calclang.antlr.CalcLangLexer;
import thosakwe.calclang.antlr.CalcLangParser;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CalcLangInterpreter extends CalcLangTarget<Double> {

    public CalcLangInterpreter(PrintStream output, CommandLine options) {
        super(output, options);
    }

    /**
     * Runs a function. Magical!
     *
     * @param name The name of the function to invoke.
     * @param args Arguments to invoke the function with.
     * @return Returns the value of the executed function.
     */
    private Double invokeFunction(String name, CalcLangParser.ExprContext... args) {
        debug("Calling " + name);
        if (stdlib.containsKey(name)) {
            List<Double> arguments = new ArrayList<>();
            for (CalcLangParser.ExprContext arg : args) {
                debug("Passing argument: " + visitExpr(arg));
                arguments.add(visitExpr(arg));
            }
            return stdlib.get(name).apply(arguments.toArray(new Double[arguments.size()]));
        } else if (currentScript.functions.containsKey(name)) {
            return invokeFunction(currentScript.functions.get(name), args);
        } else {
            if (!silent)
                output.println("Function " + name + " does not exist.");
            return 0.0;
        }
    }


    private Double invokeFunction(CalcLangParser.FnblockContext ctx, CalcLangParser.ExprContext... args) {
        CalcLangParser.BlockContext oldBlock = currentBlock;
        String name = ctx.name.getText();
        Double result;

        CalcLangParser.BlockContext blockContext = (CalcLangParser.BlockContext) ctx.parent;
        currentBlock = blockContext;
        List<TerminalNode> parameterNames = ctx.params().ID();

        for (int i = 0; i < args.length && i < parameterNames.size(); i++) {
            String paramName = parameterNames.get(i).getText();
            debug("Loading value into " + paramName);
            blockContext.symbols.put(parameterNames.get(i).getText(), args[i]);
            debug("Setting " + paramName + "=" + visitExpr(args[i]));
            debug("Original expression text: " + args[i].getText());
        }

        // Fuck it, might as well copy all symbols into it too
        if (oldBlock != null)
            oldBlock.symbols.forEach((k, v) -> {
                debug("Injecting " + k + "=" + v.getText() + " into new scope");
                blockContext.symbols.put(k, v);
            });

        blockContext.execute = true;
        visitFnblock(ctx);
        result = visitExpr(currentBlock.returnValue);
        currentBlock = oldBlock;
        blockContext.execute = false;
        if (weAreDebugging() && !silent) {
            output.printf("Result of %s: %f\n", name, result);
        }
        return result;
    }

    private Double resolveSymbol(String symbol) {

        CalcLangParser.BlockContext ctx = currentBlock;
        if (weAreDebugging()) {
            output.printf("Now looking up %s\n", symbol);
            output.printf("Current scope has %d symbols.\n", ctx.symbols.size());
            currentBlock.symbols.forEach((k, v) -> {
                debug("Scope: " + k + "=" + v.getText());
            });
        }
        if (constants.containsKey(symbol)) return constants.get(symbol);
        else if (currentScript.globals.containsKey(symbol)) {
            return visitExpr(currentScript.globals.get(symbol));
        } else if (ctx != null && ctx.symbols != null && ctx.symbols.containsKey(symbol))
            return visitExpr(ctx.symbols.get(symbol));

        if (weAreDebugging()) output.printf("Could not resolve symbol %s.\n", symbol);
        return 0.0;
    }

    @Override
    public Double visitBlock(CalcLangParser.BlockContext ctx) {
        // Inject some friendly constants...
        if (!ctx.execute) {
            return 0.0;
        }
        Double result = super.visitBlock(ctx);
        debug("Block defined these symbols:");
        ctx.symbols.forEach((key, value) -> debug("\t" + key + ": " + visitExpr(value)));
        if (ctx.returnValue != null)
            debug("Block returned: " + visitExpr(ctx.returnValue));

        return result;
    }

    @Override
    public Double visitCallstmt(CalcLangParser.CallstmtContext ctx) {
        return invokeFunction(
                ctx.function.getText(),
                (CalcLangParser.ExprContext[]) ctx.expr().toArray(new CalcLangParser.ExprContext[ctx.expr().size()]));
    }

    @Override
    public Double visitCompilationUnit(CalcLangParser.CompilationUnitContext ctx) {
        Double result = super.visitCompilationUnit(ctx);
        if (ctx.functions.containsKey("main")) {
            CalcLangParser.FnblockContext main = ctx.functions.get("main");
            return invokeFunction(main);
        }
        //System.err.println("CalcLang programs must contain a main function.");
        return result;
    }

    @Override
    public Double visitCondition(CalcLangParser.ConditionContext ctx) {
        List<CalcLangParser.ExprContext> expressions = ctx.expr();

        if (expressions.size() == 1)
            return visitExpr(expressions.get(0));
        else {
            Boolean result = false;
            Double first = visitExpr(expressions.get(0));
            Double second = visitExpr(expressions.get(1));

            if (ctx.EQU() != null) result = first.equals(second);
            else if (ctx.NOT() != null) result = !first.equals(second);
            else if (ctx.AND() != null) result = first.equals(1.0) && second.equals(1.0);
            else if (ctx.OR() != null) result = first.equals(1.0) || second.equals(1.0);
            else if (ctx.LT() != null) result = first < second;
            else if (ctx.LTE() != null) result = first <= second;
            else if (ctx.GT() != null) result = first > second;
            else if (ctx.GTE() != null) result = first >= second;

            return result ? 1.0 : 0.0;
        }
    }

    @Override
    public Double visitExpr(CalcLangParser.ExprContext ctx) {
        if (ctx == null) return 0.0;
        if (ctx.HEX() != null || ctx.NUMBER() != null)
            return CalcLangResolver.numberFromExpr(ctx);

        else if (ctx.FALSE() != null) return 0.0;
        else if (ctx.TRUE() != null) return 1.0;

        else if (ctx.function != null) {
            if (options.getOptionValue("target").toLowerCase().equals("c")) {
                // C compiler will resolve the function itself
                return null;
            }
            return invokeFunction(
                    ctx.function.getText().trim(),
                    (CalcLangParser.ExprContext[]) ctx.expr().toArray(new CalcLangParser.ExprContext[ctx.expr().size()]));
        } else if (ctx.operator() != null)
            return CalcLangResolver.operateOnExpr(ctx, ctx.operator(), visitExpr(ctx.expr(0)), visitExpr(ctx.expr(1)));

        else if (ctx.expr().size() > 0)
            return visitExpr(ctx.expr(0));

        else if (ctx.ID() != null && ctx.expr().size() == 0) {
            Double result = resolveSymbol(ctx.ID().getText().trim());
            if (weAreDebugging()) {
                output.printf("Resolved: %s=%f\n", ctx.ID().getText().trim(), result);
            }
            return result;
        }
        return 0.0;
    }

    @Override
    public Double visitFnblock(CalcLangParser.FnblockContext ctx) {
        CalcLangParser.BlockContext block = (CalcLangParser.BlockContext) ctx.parent;
        if (!block.execute) return 0.0;
        return super.visitFnblock(ctx);
    }

    @Override
    public Double visitIfstmt(CalcLangParser.IfstmtContext ctx) {
        if (visitCondition(ctx.condition()) == 1.0) {
            return super.visitIfstmt(ctx);
        }
        return 0.0;
    }

    @Override
    public Double visitImportstmt(CalcLangParser.ImportstmtContext ctx) {
        CalcLangParser.CompilationUnitContext oldScript = currentScript;
        String path = ctx.STRING().getText().replaceAll("(^\")|(\"$)", "");
        File file = new File(options.getOptionValue("in", "."));
        String absolute = file.toPath().resolveSibling(".").resolve(path).toAbsolutePath().toString();
        try {
            ANTLRInputStream antlrInputStream = new ANTLRFileStream(absolute);
            CalcLangLexer lexer = new CalcLangLexer(antlrInputStream);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            CalcLangParser parser = new CalcLangParser(tokens);
            parser.setBuildParseTree(true);
            CalcLangParser.CompilationUnitContext program = parser.compilationUnit();
            Double result = visitCompilationUnit(program);
            for (String key : program.functions.keySet()) {
                // Import all functions
                oldScript.functions.put(key, program.functions.get(key));
            }
            for (String key : program.globals.keySet()) {
                // ... And globals
                oldScript.globals.put(key, program.globals.get(key));
            }
            currentScript = oldScript;
            return result;
        } catch (IOException e) {
            System.err.println("Could not open file \"" + absolute + "\".");
            return 0.0;
        }
    }

    @Override
    public Double visitLoopstmt(CalcLangParser.LoopstmtContext ctx) {
        Double times = visitExpr(ctx.expr());
        for (double i = 0; i < times; i++) {
            visitStmts(ctx.stmts());
        }
        return 0.0;
    }

    @Override
    public Double visitPrintstmt(CalcLangParser.PrintstmtContext ctx) {
        if (options.getOptionValue("target").toLowerCase().equals("c")) {
            return 0.0;
        }
        if (ctx.STRING() == null) {
            ParseTree arg = ctx.condition() != null ? ctx.condition() : ctx.expr();
            if (!silent)
                output.println(visit(arg));
            return 0.0;
        }

        String text = ctx.STRING().getText().replaceAll("(^\")|(\"$)", "");

        // Interpolation ;)
        int interpolatedGroups;
        Pattern interpolator = Pattern.compile(".*(\\$\\{([^\\}]+)\\}).*");

        do {
            Matcher matcher = interpolator.matcher(text);
            interpolatedGroups = matcher.groupCount();
            if (matcher.matches()) {
                String program = "fn main()\nret " + matcher.group(2) + "\nend main";
                //String program = "fn main()\nret 2\nend main";
                ANTLRInputStream input = new ANTLRInputStream(program);
                CalcLangLexer lexer = new CalcLangLexer(input);
                CommonTokenStream tokens = new CommonTokenStream(lexer);
                CalcLangParser parser = new CalcLangParser(tokens);
                parser.setBuildParseTree(true);
                CalcLangParser.CompilationUnitContext script = parser.compilationUnit();
                for (String func : currentScript.functions.keySet()) {
                    if (!script.functions.containsKey(func))
                        script.functions.put(func, currentScript.functions.get(func));
                }
                script.globals.putAll(currentScript.globals);
                script.globals.putAll(currentBlock.symbols);
                Double result = visitCompilationUnit(script);
                text = text.replace(matcher.group(1), result.toString());
            }
        } while (interpolatedGroups > 2);

        if (!silent)
            output.println(text);
        return 0.0;
    }

    @Override
    public Double visitReturnstmt(CalcLangParser.ReturnstmtContext ctx) {
        currentBlock.execute = false;
        if (currentBlock.returnValue == null) {
            currentBlock.returnValue = ctx.expr();
            return 0.0;
        }
        return super.visitReturnstmt(ctx);
    }

    @Override
    public Double visitStmt(CalcLangParser.StmtContext ctx) {
        if (!currentBlock.execute) return 0.0;
        return super.visitStmt(ctx);
    }
}
