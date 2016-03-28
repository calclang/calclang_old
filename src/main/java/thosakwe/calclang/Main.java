package thosakwe.calclang;

import org.antlr.v4.runtime.*;
import org.apache.commons.cli.CommandLine;
import thosakwe.calclang.antlr.*;
import thosakwe.calclang.calc2c.CTranspiler;
import thosakwe.calclang.cli.CliArgParser;
import thosakwe.calclang.vm.CalcLangInterpreter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import static java.lang.System.err;
import static java.lang.System.exit;
import static java.lang.System.out;

public class Main {
    public static void main(String[] args) throws IOException {
        CommandLine options = CliArgParser.parseCli(args);
        if (options == null) return;

        CalcLangVisitor visitor;

        if (options.getOptionValue("target", "vm").toLowerCase().equals("c"))
            visitor = new CTranspiler(out, options);
        else
            visitor = new CalcLangInterpreter(out, options);

        out.println("CalcLang Console v1.0.0\n");

        if (options.hasOption("in")) {
            try {
                ANTLRFileStream antlrInputStream = new ANTLRFileStream(options.getOptionValue("in"));
                compile(antlrInputStream, options, visitor,
                        "The reason you are seeing this is because you have encountered a problem that has not been fixed...");
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
                    compile(new ANTLRInputStream(program), options, visitor, program);
                }
            }
        }
    }

    private static void compile(ANTLRInputStream input, CommandLine options, CalcLangVisitor visitor, String originalText) {
        CalcLangLexer lexer = new CalcLangLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        CalcLangParser parser = new CalcLangParser(tokens);
        parser.setBuildParseTree(true);
        CalcLangParser.CompilationUnitContext compilationUnit = parser.compilationUnit();
        Double result;
        if (visitor instanceof CalcLangInterpreter) {
            CalcLangInterpreter interpreter = (CalcLangInterpreter) visitor;
            try {
                result = interpreter.visitCompilationUnit(compilationUnit);
                print(result);
            } catch (Exception e) {
                e.printStackTrace(err);
                compile(new ANTLRInputStream("fn main()\nresult = " + originalText + "\nret result\nend main"), options, visitor, originalText);
            }
        } else visitor.visitCompilationUnit(compilationUnit);
    }

    private static void print(Double result) {
        if (result != null) {
            out.println("Result: " + result);
            exit(result.intValue());
        } else out.println("No result. Perhaps there was an error?");
    }
}
