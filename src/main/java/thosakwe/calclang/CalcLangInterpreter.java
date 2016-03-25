package thosakwe.calclang;

import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.cli.CommandLine;
import thosakwe.calclang.antlr.CalcLangBaseVisitor;
import thosakwe.calclang.antlr.CalcLangLexer;
import thosakwe.calclang.antlr.CalcLangParser;
import thosakwe.calclang.stdlib.CalcLangStdLib;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.System.out;

class CalcLangInterpreter extends CalcLangBaseVisitor<Double> {
    private PrintStream output;
    private CommandLine options;
    private CalcLangParser.CompilationUnitContext currentScript;
    private CalcLangParser.BlockContext currentBlock;
    private Map<String, Function<Double[], Double>> stdlib = CalcLangStdLib.get();
    private Map<String, Double> constants = CalcLangStdLib.constants();

    CalcLangInterpreter(PrintStream output, CommandLine options) {
        this.output = output;
        this.options = options;
    }

    /**
     * Runs a function. Magical!
     *
     * @param name The name of the function to invoke.
     * @param args Arguments to invoke the function with.
     * @return Returns the value of the executed function.
     */
    private Double invokeFunction(String name, CalcLangParser.ExprContext... args) {
        if (stdlib.containsKey(name)) {
            List<Double> arguments = new ArrayList<>();
            for (CalcLangParser.ExprContext arg : args) {
                arguments.add(visitExpr(arg));
            }
            return stdlib.get(name).apply(arguments.toArray(new Double[arguments.size()]));
        } else if (currentScript.functions.containsKey(name)) {
            return invokeFunction(currentScript.functions.get(name), args);
        } else {
            output.println("Function " + name + " does not exist.");
            return 0.0;
        }
    }


    private Double invokeFunction(CalcLangParser.FnblockContext ctx, CalcLangParser.ExprContext... args) {
        CalcLangParser.BlockContext oldBlock = currentBlock;
        String name = ctx.name.getText();
        if (weAreDebugging()) output.println("Now calling: " + name);
        Double result;

        CalcLangParser.BlockContext blockContext = (CalcLangParser.BlockContext) ctx.parent;
        currentBlock = blockContext;
        List<TerminalNode> parameterNames = ctx.params().ID();

        for (int i = 0; i < args.length && i < parameterNames.size(); i++) {
            blockContext.symbols.put(parameterNames.get(i).getText(), args[i]);
        }

        blockContext.execute = true;
        visitFnblock(ctx);
        result = visitExpr(currentBlock.returnValue);
        currentBlock = oldBlock;
        if (weAreDebugging()) {
            output.printf("Result of %s: %f\n", name, result);
        }
        return result;
    }

    /**
     * Are we debugging?
     *
     * @return true or false
     */
    private boolean weAreDebugging() {
        return this.options.hasOption("debug");
    }

    private Double resolveSymbol(String symbol, CalcLangParser.BlockContext ctx) {
        if (weAreDebugging()) {
            output.printf("Now looking up %s\n", symbol);
            //output.printf("Current scope has %d symbols.\n", ctx.symbols.size());
        }
        if (constants.containsKey(symbol)) return constants.get(symbol);
        else if (currentScript.globals.containsKey(symbol)) {
            return visitExpr(currentScript.globals.get(symbol));
        }
        else if (ctx.symbols.containsKey(symbol))
            return visitExpr(ctx.symbols.get(symbol));
        else if (ctx.parent != null && ctx.parent != ctx && ctx.parent instanceof CalcLangParser.BlockContext)
            return resolveSymbol(symbol, (CalcLangParser.BlockContext) ctx.parent);

        if (weAreDebugging()) output.printf("Could not resolve symbol %s.\n", symbol);
        return 0.0;
    }

    @Override
    public Double visitBlock(CalcLangParser.BlockContext ctx) {
        // Inject some friendly constants...

        if (!ctx.execute) return 0.0;
        this.currentBlock = ctx;
        Double result = super.visitBlock(ctx);
        if (weAreDebugging())
            ctx.symbols.forEach((key, value) -> output.print(key + ": " + visitExpr(value) + "\n"));
        if (ctx.returnValue != null && weAreDebugging())
            output.println("Block returned: " + visitExpr(ctx.returnValue));
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
        currentScript = ctx;
        Double result = super.visitCompilationUnit(ctx);
        if (ctx.functions.containsKey("main")) {
            CalcLangParser.FnblockContext main = ctx.functions.get("main");
            return invokeFunction(main);
        }
        //System.err.println("CalcLang programs must contain a main function.");
        return result;
    }

    @Override
    public Double visitExpr(CalcLangParser.ExprContext ctx) {
        if (ctx.HEX() != null || ctx.NUMBER() != null)
            return CalcLangResolver.numberFromExpr(ctx);

        else if (ctx.function != null) {
            return invokeFunction(
                    ctx.function.getText().trim(),
                    (CalcLangParser.ExprContext[]) ctx.expr().toArray(new CalcLangParser.ExprContext[ctx.expr().size()]));
        } else if (ctx.operator() != null)
            return CalcLangResolver.operateOnExpr(ctx, ctx.operator(), visitExpr(ctx.expr(0)), visitExpr(ctx.expr(1)));

        else if (ctx.expr().size() > 0)
            return visitExpr(ctx.expr(0));

        else if (ctx.ID() != null && ctx.expr().size() == 0) {
            Double result = resolveSymbol(ctx.ID().getText().trim(), currentBlock);
            if (weAreDebugging()) {
                output.printf("Resolved: %s=%f\n", ctx.ID().getText().trim(), result);
            }
            return result;
        }

        if (weAreDebugging()) {
            output.println("Had to return 0:visitExpr");
        }
        return 0.0;
    }

    @Override
    public Double visitImportstmt(CalcLangParser.ImportstmtContext ctx) {
        CalcLangParser.CompilationUnitContext oldScript = currentScript;
        String path = ctx.STRING().getText().replaceAll("(^\")|(\"$)", "");
        File file = new File(options.getOptionValue("in", "."));
        String absolute = file.toPath().resolve("..").resolve(path).toAbsolutePath().toString();
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

        output.println(text);
        return 0.0;
    }
}
