import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MiniJson {
    private final String text;
    private int index = 0;

    public MiniJson(String text) {
        this.text = text;
    }

    public Object parse() {
        skipWhitespace();
        Object value = parseValue();
        skipWhitespace();
        return value;
    }

    private Object parseValue() {
        skipWhitespace();
        if (peek('{')) {
            return parseObject();
        } else if (peek('[')) {
            return parseArray();
        } else if (peek('"')) {
            return parseString();
        } else {
            return parseNumberOrLiteral();
        }
    }

    private Map<String, Object> parseObject() {
        expect('{');
        Map<String, Object> map = new HashMap<>();
        skipWhitespace();
        if (peek('}')) {
            expect('}');
            return map;
        }
        while (true) {
            skipWhitespace();
            String key = parseString();
            skipWhitespace();
            expect(':');
            Object value = parseValue();
            map.put(key, value);
            skipWhitespace();
            if (peek('}')) {
                expect('}');
                break;
            }
            expect(',');
        }
        return map;
    }

    private List<Object> parseArray() {
        expect('[');
        List<Object> list = new ArrayList<>();
        skipWhitespace();
        if (peek(']')) {
            expect(']');
            return list;
        }
        while (true) {
            Object value = parseValue();
            list.add(value);
            skipWhitespace();
            if (peek(']')) {
                expect(']');
                break;
            }
            expect(',');
        }
        return list;
    }

    private String parseString() {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (index < text.length()) {
            char c = text.charAt(index++);
            if (c == '"') {
                break;
            }
            if (c == '\\' && index < text.length()) {
                char next = text.charAt(index++);
                if (next == '"') {
                    sb.append('"');
                } else if (next == '\\') {
                    sb.append('\\');
                } else if (next == 'n') {
                    sb.append('\n');
                } else if (next == 't') {
                    sb.append('\t');
                } else {
                    sb.append(next);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private Object parseNumberOrLiteral() {
        int start = index;
        while (index < text.length()) {
            char c = text.charAt(index);
            if (Character.isDigit(c) || Character.isLetter(c) || c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E') {
                index++;
            } else {
                break;
            }
        }
        String token = text.substring(start, index).trim();
        if (token.isEmpty()) {
            throw new IllegalStateException("Unexpected token at position " + index);
        }
        if ("true".equals(token)) {
            return Boolean.TRUE;
        }
        if ("false".equals(token)) {
            return Boolean.FALSE;
        }
        if ("null".equals(token)) {
            return null;
        }
        try {
            if (token.contains(".") || token.contains("e") || token.contains("E")) {
                return Double.parseDouble(token);
            }
            return Integer.parseInt(token);
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("Invalid number: " + token);
        }
    }

    private void expect(char c) {
        skipWhitespace();
        if (index >= text.length() || text.charAt(index) != c) {
            throw new IllegalStateException("Expected '" + c + "' at position " + index);
        }
        index++;
    }

    private boolean peek(char c) {
        skipWhitespace();
        return index < text.length() && text.charAt(index) == c;
    }

    private void skipWhitespace() {
        while (index < text.length()) {
            char c = text.charAt(index);
            if (c == ' ' || c == '\n' || c == '\r' || c == '\t') {
                index++;
            } else {
                break;
            }
        }
    }
}
