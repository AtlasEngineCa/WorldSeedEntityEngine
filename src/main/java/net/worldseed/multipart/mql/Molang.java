package net.worldseed.multipart.mql;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A small, dependency-free Molang evaluator for Blockbench keyframe expressions.
 *
 * <p>Replaces the JIT-based {@code dev.hollowcube:mql}, whose runtime class generation reflectively calls
 * {@code ClassLoader.defineClass} — blocked by strong encapsulation on Java 16+, so it throws
 * {@code InaccessibleObjectException} on WSEE's Java 25 target and Molang keyframes never worked.
 *
 * <p>Grammar (case-insensitive; trig in DEGREES, per Bedrock): numbers, {@code + - * /} and unary {@code -},
 * parentheses, comparisons {@code < > <= >= == !=}, logical {@code && || !}, the ternary {@code a ? b : c},
 * the {@code query.}/{@code q.} namespace (anim_time, life_time, delta_time, time) and the {@code math.*}
 * functions Blockbench emits. Unknown identifiers evaluate to 0. Compiled expressions are pure and reusable.
 */
final class Molang {

    static MQLEvaluator compile(String source) {
        try {
            return new Parser(source.toLowerCase(Locale.ROOT)).parseProgram();
        } catch (RuntimeException parseError) {
            return env -> 0; // graceful: a malformed expression contributes 0 rather than crashing the model
        }
    }

    private Molang() {
    }

    private static final class Parser {
        private final String s;
        private int i = 0;

        Parser(String s) {
            this.s = s;
        }

        MQLEvaluator parseProgram() {
            MQLEvaluator e = ternary();
            skipWs();
            // Molang allows ';'-separated statements; keyframes are a single expression, but evaluate the last.
            while (i < s.length() && s.charAt(i) == ';') {
                i++;
                skipWs();
                if (i < s.length()) e = ternary();
                skipWs();
            }
            return e;
        }

        private MQLEvaluator ternary() {
            MQLEvaluator cond = or();
            skipWs();
            if (match('?')) {
                MQLEvaluator a = ternary();
                skipWs();
                expect(':');
                MQLEvaluator b = ternary();
                return env -> cond.evaluate(env) != 0 ? a.evaluate(env) : b.evaluate(env);
            }
            return cond;
        }

        private MQLEvaluator or() {
            MQLEvaluator left = and();
            while (true) {
                skipWs();
                if (match2('|', '|')) {
                    MQLEvaluator l = left, r = and();
                    left = env -> (l.evaluate(env) != 0 || r.evaluate(env) != 0) ? 1 : 0;
                } else return left;
            }
        }

        private MQLEvaluator and() {
            MQLEvaluator left = comparison();
            while (true) {
                skipWs();
                if (match2('&', '&')) {
                    MQLEvaluator l = left, r = comparison();
                    left = env -> (l.evaluate(env) != 0 && r.evaluate(env) != 0) ? 1 : 0;
                } else return left;
            }
        }

        private MQLEvaluator comparison() {
            MQLEvaluator l = additive();
            skipWs();
            if (match2('=', '=')) { MQLEvaluator r = additive(); return env -> l.evaluate(env) == r.evaluate(env) ? 1 : 0; }
            if (match2('!', '=')) { MQLEvaluator r = additive(); return env -> l.evaluate(env) != r.evaluate(env) ? 1 : 0; }
            if (match2('<', '=')) { MQLEvaluator r = additive(); return env -> l.evaluate(env) <= r.evaluate(env) ? 1 : 0; }
            if (match2('>', '=')) { MQLEvaluator r = additive(); return env -> l.evaluate(env) >= r.evaluate(env) ? 1 : 0; }
            if (match('<')) { MQLEvaluator r = additive(); return env -> l.evaluate(env) < r.evaluate(env) ? 1 : 0; }
            if (match('>')) { MQLEvaluator r = additive(); return env -> l.evaluate(env) > r.evaluate(env) ? 1 : 0; }
            return l;
        }

        private MQLEvaluator additive() {
            MQLEvaluator left = multiplicative();
            while (true) {
                skipWs();
                if (match('+')) { MQLEvaluator l = left, r = multiplicative(); left = env -> l.evaluate(env) + r.evaluate(env); }
                else if (match('-')) { MQLEvaluator l = left, r = multiplicative(); left = env -> l.evaluate(env) - r.evaluate(env); }
                else return left;
            }
        }

        private MQLEvaluator multiplicative() {
            MQLEvaluator left = unary();
            while (true) {
                skipWs();
                if (match('*')) { MQLEvaluator l = left, r = unary(); left = env -> l.evaluate(env) * r.evaluate(env); }
                else if (match('/')) { MQLEvaluator l = left, r = unary(); left = env -> { double d = r.evaluate(env); return d == 0 ? 0 : l.evaluate(env) / d; }; }
                else return left;
            }
        }

        private MQLEvaluator unary() {
            skipWs();
            if (match('-')) { MQLEvaluator e = unary(); return env -> -e.evaluate(env); }
            if (match('!')) { MQLEvaluator e = unary(); return env -> e.evaluate(env) == 0 ? 1 : 0; }
            return primary();
        }

        private MQLEvaluator primary() {
            skipWs();
            char c = peek();
            if (c == '(') {
                i++;
                MQLEvaluator e = ternary();
                skipWs();
                expect(')');
                return e;
            }
            if (isDigit(c) || c == '.') return number();
            if (isIdentStart(c)) return identifier();
            throw new RuntimeException("unexpected '" + c + "' at " + i);
        }

        private MQLEvaluator number() {
            int start = i;
            while (i < s.length() && (isDigit(s.charAt(i)) || s.charAt(i) == '.')) i++;
            double value = Double.parseDouble(s.substring(start, i));
            return env -> value;
        }

        private MQLEvaluator identifier() {
            int start = i;
            while (i < s.length() && (isIdentPart(s.charAt(i)) || s.charAt(i) == '.')) i++;
            String name = s.substring(start, i);
            skipWs();
            if (peek() == '(') {
                i++;
                List<MQLEvaluator> args = new ArrayList<>();
                skipWs();
                if (peek() != ')') {
                    args.add(ternary());
                    skipWs();
                    while (match(',')) { args.add(ternary()); skipWs(); }
                }
                expect(')');
                return function(name, args);
            }
            return variable(name);
        }

        private static MQLEvaluator variable(String name) {
            return switch (name) {
                case "query.anim_time", "q.anim_time", "query.time", "q.time" -> env -> env.anim_time();
                case "query.life_time", "q.life_time" -> env -> env.life_time();
                case "query.delta_time", "q.delta_time" -> env -> env.delta_time();
                case "math.pi" -> env -> Math.PI;
                case "true" -> env -> 1;
                case "false" -> env -> 0;
                default -> env -> 0; // unknown query/variable -> 0 (Bedrock-style)
            };
        }

        private static MQLEvaluator function(String name, List<MQLEvaluator> a) {
            return switch (name) {
                case "math.sin" -> env -> Math.sin(Math.toRadians(a.get(0).evaluate(env)));
                case "math.cos" -> env -> Math.cos(Math.toRadians(a.get(0).evaluate(env)));
                case "math.tan" -> env -> Math.tan(Math.toRadians(a.get(0).evaluate(env)));
                case "math.asin" -> env -> Math.toDegrees(Math.asin(a.get(0).evaluate(env)));
                case "math.acos" -> env -> Math.toDegrees(Math.acos(a.get(0).evaluate(env)));
                case "math.atan" -> env -> Math.toDegrees(Math.atan(a.get(0).evaluate(env)));
                case "math.atan2" -> env -> Math.toDegrees(Math.atan2(a.get(0).evaluate(env), a.get(1).evaluate(env)));
                case "math.abs" -> env -> Math.abs(a.get(0).evaluate(env));
                case "math.sqrt" -> env -> Math.sqrt(a.get(0).evaluate(env));
                case "math.pow" -> env -> Math.pow(a.get(0).evaluate(env), a.get(1).evaluate(env));
                case "math.exp" -> env -> Math.exp(a.get(0).evaluate(env));
                case "math.ln" -> env -> Math.log(a.get(0).evaluate(env));
                case "math.mod" -> env -> { double d = a.get(1).evaluate(env); return d == 0 ? 0 : a.get(0).evaluate(env) % d; };
                case "math.min" -> env -> Math.min(a.get(0).evaluate(env), a.get(1).evaluate(env));
                case "math.max" -> env -> Math.max(a.get(0).evaluate(env), a.get(1).evaluate(env));
                case "math.floor" -> env -> Math.floor(a.get(0).evaluate(env));
                case "math.ceil" -> env -> Math.ceil(a.get(0).evaluate(env));
                case "math.round" -> env -> Math.round(a.get(0).evaluate(env));
                case "math.trunc" -> env -> (double) (long) a.get(0).evaluate(env);
                case "math.sign" -> env -> Math.signum(a.get(0).evaluate(env));
                case "math.clamp" -> env -> Math.max(a.get(1).evaluate(env), Math.min(a.get(2).evaluate(env), a.get(0).evaluate(env)));
                case "math.lerp" -> env -> { double x = a.get(0).evaluate(env), y = a.get(1).evaluate(env), t = a.get(2).evaluate(env); return x + (y - x) * t; };
                case "math.random" -> env -> { double lo = a.get(0).evaluate(env), hi = a.get(1).evaluate(env); return lo + Math.random() * (hi - lo); };
                default -> env -> 0;
            };
        }

        private void skipWs() { while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++; }
        private char peek() { return i < s.length() ? s.charAt(i) : '\0'; }
        private boolean match(char c) { skipWs(); if (peek() == c) { i++; return true; } return false; }
        private boolean match2(char a, char b) { skipWs(); if (i + 1 < s.length() && s.charAt(i) == a && s.charAt(i + 1) == b) { i += 2; return true; } return false; }
        private void expect(char c) { if (!match(c)) throw new RuntimeException("expected '" + c + "' at " + i); }

        private static boolean isDigit(char c) { return c >= '0' && c <= '9'; }
        private static boolean isIdentStart(char c) { return c == '_' || (c >= 'a' && c <= 'z'); }
        private static boolean isIdentPart(char c) { return isIdentStart(c) || isDigit(c); }
    }
}
