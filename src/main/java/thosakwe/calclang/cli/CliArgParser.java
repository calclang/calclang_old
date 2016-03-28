package thosakwe.calclang.cli;

import org.apache.commons.cli.*;

public class CliArgParser {
    public static CommandLine parseCli(String[] args) {
        CommandLineParser parser = new DefaultParser();
        Options options = new Options();
        options.addOption("t", "target", true, "A compiler output target. Valid: vm, c");
        options.addOption("i", "in", true, "An input file to be interpreted.");
        options.addOption(null, "debug", false, "Print debug output.");
        options.addOption("h", "help", false, "Show this help message and exit.");
        options.addOption("v", "version", false, "Show program's version number and exit.");
        options.addOption(Option.builder()
                .desc("Disable compiler warnings.")
                .longOpt("no-warn")
                .hasArg(false)
                .build());
        try {
            CommandLine result = parser.parse(options, args);
            if (result.hasOption("help"))
                throw new ParseException(null);
            else if (result.hasOption("version"))
                throw new ParseException("VERSION");
            return result;
        } catch (ParseException e) {
            if (e.getMessage() != null && e.getMessage().equals("VERSION"))
                System.out.println("CalcLang Console v1.0.0");
            else
                new HelpFormatter().printHelp("calclang", "CalcLang Console v1.0.0", options, "", true);
            return null;
        }
    }
}
