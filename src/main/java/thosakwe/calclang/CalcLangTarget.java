package thosakwe.calclang;

import org.apache.commons.cli.CommandLine;
import thosakwe.calclang.antlr.CalcLangBaseVisitor;
import thosakwe.calclang.antlr.CalcLangParser;
import thosakwe.calclang.vm.CalcLangInterpreter;
import thosakwe.calclang.vm.stdlib.CalcLangStdLib;

import java.io.PrintStream;
import java.util.Map;
import java.util.function.Function;

public class CalcLangTarget<T> extends CalcLangBaseVisitor<T> {
    public PrintStream output;
    public CommandLine options;
    public CalcLangParser.CompilationUnitContext currentScript;
    public CalcLangParser.BlockContext currentBlock;
    public Map<String, Function<Double[], Double>> stdlib = CalcLangStdLib.get();
    public Map<String, Double> constants = CalcLangStdLib.constants();
    public boolean silent = false;

    public CalcLangTarget(PrintStream output, CommandLine options) {
        this.output = output;
        this.options = options;
    }

    public void debug(Object x) {
        if (weAreDebugging()) output.println(x);
    }

    public void warn(String warning) {
        if (!options.hasOption("no-warn"))
            output.printf("Warning: %s\n", warning);
    }

    /**
     * Are we debugging?
     *
     * @return true or false
     */
    public boolean weAreDebugging() {
        return !silent && this.options.hasOption("debug");
    }

    @Override
    public T visitBlock(CalcLangParser.BlockContext ctx) {
        this.currentBlock = ctx;
        return super.visitBlock(ctx);
    }

    @Override
    public T visitCompilationUnit(CalcLangParser.CompilationUnitContext ctx) {
        currentScript = ctx;
        return super.visitCompilationUnit(ctx);
    }
}
