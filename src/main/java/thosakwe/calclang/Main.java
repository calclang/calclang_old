package thosakwe.calclang;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;
import org.apache.commons.cli.CommandLine;
import thosakwe.calclang.antlr.*;
import thosakwe.calclang.cli.CliArgParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.BitSet;

import static java.lang.System.out;

public class Main {
    public static void main(String[] args) throws IOException {
        CommandLine options = CliArgParser.parseCli(args);
        if (options == null) return;

        CalcLangInterpreter interpreter = new CalcLangInterpreter(out, options);

        out.println("CalcLang Interpreter v1.0.0\n");

        if (options.hasOption("in")) {
            try {
                ANTLRFileStream antlrInputStream = new ANTLRFileStream(options.getOptionValue("in"));
                compile(antlrInputStream, options, interpreter, "undefined");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            out.println("Interactive REPL Mode\n");
            BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
            String input = "";
            char[] output = new char[0];
            while (!input.equals("quit")) {
                out.print("> ");
                input = stdin.readLine();
                if (!input.equals("quit")) {
                    String program = "fn main()\nresult =" + input + "\nret result\nend main";
                    compile(new ANTLRInputStream(program), options, interpreter, program);
                }
            }
        }
    }

    private static void compile(ANTLRInputStream input, CommandLine options, CalcLangInterpreter interpreter, String originalText) {
        CalcLangLexer lexer = new CalcLangLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        CalcLangParser parser = new CalcLangParser(tokens);
        parser.setBuildParseTree(true);
        CalcLangParser.CompilationUnitContext compilationUnit = parser.compilationUnit();
        Double result;
        try {
            result = interpreter.visitCompilationUnit(compilationUnit);
            print(result);
        } catch (Exception e) {
            compile(new ANTLRInputStream("fn main()\nresult = " + originalText + "\nret result\nend main"), options, interpreter, originalText);
        }
    }

    private static void print(Double result) {
        if (result != null) out.println("Result: " + result);
        else out.println("No result. Perhaps there was an error?");
    }
}
