package com.lwlee2608.vertx.grpc.plugin;

import java.util.Arrays;
import java.util.List;

public class Util {

    // java keywords from: https://docs.oracle.com/javase/specs/jls/se8/html/jls-3.html#jls-3.9
    private static final List<CharSequence> JAVA_KEYWORDS = Arrays.asList(
            "abstract",
            "assert",
            "boolean",
            "break",
            "byte",
            "case",
            "catch",
            "char",
            "class",
            "const",
            "continue",
            "default",
            "do",
            "double",
            "else",
            "enum",
            "extends",
            "final",
            "finally",
            "float",
            "for",
            "goto",
            "if",
            "implements",
            "import",
            "instanceof",
            "int",
            "interface",
            "long",
            "native",
            "new",
            "package",
            "private",
            "protected",
            "public",
            "return",
            "short",
            "static",
            "strictfp",
            "super",
            "switch",
            "synchronized",
            "this",
            "throw",
            "throws",
            "transient",
            "try",
            "void",
            "volatile",
            "while",
            // additional ones added by us
            "true",
            "false"
    );

    /**
     * Adjust a method name prefix identifier to follow the JavaBean spec:
     * - decapitalize the first letter
     * - remove embedded underscores & capitalize the following letter
     * <p>
     * Finally, if the result is a reserved java keyword, append an underscore.
     *
     * @param word method name
     * @return lower name
     */
    public static String camelCase(String word) {
        StringBuffer w = new StringBuffer();
        w.append(Character.toLowerCase(word.charAt(0)));

        boolean afterUnderscore = false;

        for (int i = 1; i < word.length(); ++i) {
            char c = word.charAt(i);

            if (c == '_') {
                afterUnderscore = true;
            } else {
                if (afterUnderscore) {
                    w.append(Character.toUpperCase(c));
                } else {
                    w.append(c);
                }
                afterUnderscore = false;
            }
        }

        if (JAVA_KEYWORDS.contains(w)) {
            w.append('_');
        }

        return w.toString();
    }

    public static String getSimpleClass(String fullName) {
        String[] token = fullName.split("\\.");
        return token[token.length - 1];
    }
}
