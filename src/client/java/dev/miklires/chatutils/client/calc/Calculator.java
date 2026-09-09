package dev.miklires.chatutils.client.calc;

import java.util.Locale;

public final class Calculator {

    public static class CalculatorException extends RuntimeException {
        public CalculatorException(String message) {
            super(message);
        }
    }

    private final String input;
    private int position;

    private Calculator(String input) {
        this.input = input;
    }

    public static double evaluate(String expression) {
        if (expression == null || expression.isBlank()) {
            throw new CalculatorException("empty expression");
        }

        Calculator calculator = new Calculator(expression);
        double result = calculator.expression();
        calculator.skipSpaces();
        if (calculator.position < calculator.input.length()) {
            throw new CalculatorException("unexpected '" + calculator.input.charAt(calculator.position) + "'");
        }
        if (Double.isNaN(result) || Double.isInfinite(result)) {
            throw new CalculatorException("result is not a number");
        }
        return result;
    }

    private double expression() {
        double value = term();
        while (true) {
            if (consume('+')) {
                value += term();
            } else if (consume('-')) {
                value -= term();
            } else {
                return value;
            }
        }
    }

    private double term() {
        double value = unary();
        while (true) {
            if (consume('*')) {
                value *= unary();
            } else if (consume('/')) {
                double divisor = unary();
                if (divisor == 0.0) {
                    throw new CalculatorException("division by zero");
                }
                value /= divisor;
            } else if (consume('%')) {
                double divisor = unary();
                if (divisor == 0.0) {
                    throw new CalculatorException("division by zero");
                }
                value %= divisor;
            } else {
                return value;
            }
        }
    }

    private double unary() {
        if (consume('-')) {
            return -unary();
        }
        if (consume('+')) {
            return unary();
        }
        return power();
    }

    private double power() {
        double base = primary();
        return consume('^') ? Math.pow(base, unary()) : base;
    }

    private double primary() {
        skipSpaces();
        if (position >= input.length()) {
            throw new CalculatorException("expression ends too early");
        }

        if (consume('(')) {
            double value = expression();
            expect(')');
            return value;
        }

        char current = input.charAt(position);
        if (Character.isDigit(current) || current == '.') {
            return number();
        }
        if (Character.isLetter(current)) {
            return nameExpression();
        }
        throw new CalculatorException("unexpected '" + current + "'");
    }

    private double number() {
        int start = position;
        while (position < input.length()
                && (Character.isDigit(input.charAt(position)) || input.charAt(position) == '.')) {
            position++;
        }
        if (position < input.length() && (input.charAt(position) == 'e' || input.charAt(position) == 'E')) {
            int mark = position;
            position++;
            if (position < input.length() && (input.charAt(position) == '+' || input.charAt(position) == '-')) {
                position++;
            }
            if (position < input.length() && Character.isDigit(input.charAt(position))) {
                while (position < input.length() && Character.isDigit(input.charAt(position))) {
                    position++;
                }
            } else {
                position = mark;
            }
        }

        try {
            return Double.parseDouble(input.substring(start, position));
        } catch (NumberFormatException e) {
            throw new CalculatorException("bad number '" + input.substring(start, position) + "'");
        }
    }

    private double nameExpression() {
        int start = position;
        while (position < input.length() && Character.isLetterOrDigit(input.charAt(position))) {
            position++;
        }
        String name = input.substring(start, position).toLowerCase(Locale.ROOT);

        skipSpaces();
        if (!consume('(')) {
            return constant(name);
        }

        double first = expression();
        if (consume(',')) {
            double second = expression();
            expect(')');
            return function(name, first, second);
        }
        expect(')');
        return function(name, first);
    }

    private double constant(String name) {
        return switch (name) {
            case "pi" -> Math.PI;
            case "e" -> Math.E;
            case "tau" -> Math.TAU;
            default -> throw new CalculatorException("unknown name '" + name + "'");
        };
    }

    private double function(String name, double argument) {
        return switch (name) {
            case "sqrt" -> {
                if (argument < 0.0) {
                    throw new CalculatorException("square root of a negative number");
                }
                yield Math.sqrt(argument);
            }
            case "cbrt" -> Math.cbrt(argument);
            case "abs" -> Math.abs(argument);
            case "floor" -> Math.floor(argument);
            case "ceil" -> Math.ceil(argument);
            case "round" -> Math.round(argument);
            case "sign" -> Math.signum(argument);
            case "sin" -> Math.sin(argument);
            case "cos" -> Math.cos(argument);
            case "tan" -> Math.tan(argument);
            case "sind" -> Math.sin(Math.toRadians(argument));
            case "cosd" -> Math.cos(Math.toRadians(argument));
            case "tand" -> Math.tan(Math.toRadians(argument));
            case "asin" -> Math.asin(argument);
            case "acos" -> Math.acos(argument);
            case "atan" -> Math.atan(argument);
            case "ln" -> logOf(argument, Math.E);
            case "log" -> logOf(argument, 10.0);
            case "exp" -> Math.exp(argument);
            default -> throw new CalculatorException("unknown function '" + name + "'");
        };
    }

    private double function(String name, double first, double second) {
        return switch (name) {
            case "min" -> Math.min(first, second);
            case "max" -> Math.max(first, second);
            case "pow" -> Math.pow(first, second);
            case "hypot" -> Math.hypot(first, second);
            case "atan2" -> Math.atan2(first, second);
            case "log" -> logOf(first, second);
            default -> throw new CalculatorException("'" + name + "' does not take two arguments");
        };
    }

    private static double logOf(double value, double base) {
        if (value <= 0.0) {
            throw new CalculatorException("logarithm of a non-positive number");
        }
        return Math.log(value) / Math.log(base);
    }

    private void skipSpaces() {
        while (position < input.length() && Character.isWhitespace(input.charAt(position))) {
            position++;
        }
    }

    private boolean consume(char expected) {
        skipSpaces();
        if (position < input.length() && input.charAt(position) == expected) {
            position++;
            return true;
        }
        return false;
    }

    private void expect(char expected) {
        if (!consume(expected)) {
            throw new CalculatorException("expected '" + expected + "'");
        }
    }
}
