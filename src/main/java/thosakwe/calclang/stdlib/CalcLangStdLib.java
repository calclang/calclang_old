package thosakwe.calclang.stdlib;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class CalcLangStdLib {
    private static double sin(Double... args) {
        return Math.sin(args[0]);
    }

    private static double cos(Double... args) {
        return Math.cos(args[0]);
    }

    private static double tan(Double... args) {
        return Math.tan(args[0]);
    }

    private static double asin(Double... args) {
        return Math.asin(args[0]);
    }

    private static double acos(Double... args) {
        return Math.acos(args[0]);
    }

    private static double atan(Double... args) {
        return Math.atan(args[0]);
    }

    private static double pow(Double... args) {
        return Math.pow(args[0], args[1]);
    }

    private static double ceil(Double... args) {
        return Math.ceil(args[0]);
    }

    private static double floor(Double... args) {
        return Math.floor(args[0]);
    }

    private static double rnd(Double... args) {
        return Math.random();
    }

    private static double rad(Double... args) {
        return Math.toRadians(args[0]);
    }

    private static double deg(Double... args) {
        return Math.toDegrees(args[0]);
    }

    private static double sum(Double... args) {
        Double sum = 0.0;
        for (Double arg : args) {
            sum += arg;
        }
        return sum;
    }

    private static double percent(Double... args) {
        // pct(number, total)
        return (args[0] / args[1]) * 100.0;
    }

    private static double sqrt(Double... args) {
        // pct(number, total)
        return Math.sqrt(args[0]);
    }

    private static double round(Double... args) {
        return Math.round(args[0]);
    }

    private static double root(Double... args) {
        return Math.pow(Math.E, Math.log(args[0]) / args[1]);
    }

    private static double ln(Double... args) {
        return Math.log(args[0]);
    }

    private static double log(Double... args) {
        //log 31 base 2 -> log(31, 2)
        // Syntax: log(num, base)
        return Math.log(args[0]) / Math.log(args[1]);
    }

    private static double log10(Double... args) {
        return Math.log10(args[0]);
    }

    private static double avg(Double... args) {
        double result = 0;
        for (double arg: args) {
            result += arg;
        }
        return result / args.length;
    }

    public static Map<String, Function<Double[], Double>> get() {
        Map<String, Function<Double[], Double>> stdlib = new HashMap<>();

        stdlib.put("sin", CalcLangStdLib::sin);
        stdlib.put("cos", CalcLangStdLib::cos);
        stdlib.put("tan", CalcLangStdLib::tan);
        stdlib.put("asin", CalcLangStdLib::asin);
        stdlib.put("acos", CalcLangStdLib::acos);
        stdlib.put("atan", CalcLangStdLib::atan);
        stdlib.put("pow", CalcLangStdLib::pow);
        stdlib.put("ceil", CalcLangStdLib::ceil);
        stdlib.put("floor", CalcLangStdLib::floor);
        stdlib.put("rnd", CalcLangStdLib::rnd);
        stdlib.put("sum", CalcLangStdLib::sum);
        stdlib.put("pct", CalcLangStdLib::percent);
        stdlib.put("sqrt", CalcLangStdLib::sqrt);
        stdlib.put("round", CalcLangStdLib::round);
        stdlib.put("root", CalcLangStdLib::root);
        stdlib.put("ln", CalcLangStdLib::ln);
        stdlib.put("log", CalcLangStdLib::log);
        stdlib.put("log10", CalcLangStdLib::log10);
        stdlib.put("rad", CalcLangStdLib::rad);
        stdlib.put("deg", CalcLangStdLib::deg);
        stdlib.put("avg", CalcLangStdLib::avg);

        return stdlib;
    }

    public static Map<String, Double> constants() {
        HashMap<String, Double> constants = new HashMap<>();
        // Some friendly constants :)
        constants.put("PI", Math.PI);
        constants.put("AVOGADRO", 6.022 * Math.pow(10, 23.0));
        constants.put("PLANCK", 6.62607004 * Math.pow(10, -34.0));
        constants.put("SPEED_OF_LIGHT", 2.998 * Math.pow(10, 8));
        constants.put("E", Math.E);
        constants.put("I", Math.sqrt(-1.0));
        constants.put("INFINITY", -1.0 * Math.log(Math.E));
        return constants;
    }
}
