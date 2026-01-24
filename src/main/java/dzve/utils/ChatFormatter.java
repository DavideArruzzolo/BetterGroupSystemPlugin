package dzve.utils;

import com.hypixel.hytale.protocol.MaybeBool;
import com.hypixel.hytale.server.core.Message;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;

import static java.util.Map.entry;

public final class ChatFormatter {
    private static final Pattern TAG_PATTERN = Pattern.compile("<(/?)([a-zA-Z0-9_]+)(?::([^>]+))?>");
    private static final Map<String, Color> NAMED_COLORS = Map.ofEntries(
            entry("black", new Color(0, 0, 0)),
            entry("dark_blue", new Color(0, 0, 170)),
            entry("dark_green", new Color(0, 170, 0)),
            entry("dark_aqua", new Color(0, 170, 170)),
            entry("dark_red", new Color(170, 0, 0)),
            entry("dark_purple", new Color(170, 0, 170)),
            entry("gold", new Color(255, 170, 0)),
            entry("gray", new Color(170, 170, 170)),
            entry("dark_gray", new Color(85, 85, 85)),
            entry("blue", new Color(85, 85, 255)),
            entry("green", new Color(85, 255, 85)),
            entry("aqua", new Color(85, 255, 255)),
            entry("red", new Color(255, 85, 85)),
            entry("light_purple", new Color(255, 85, 255)),
            entry("yellow", new Color(255, 255, 85)),
            entry("white", new Color(255, 255, 255))
    );

    public static StyledText of(String text) {
        return new StyledText(text);
    }

    public static Message parse(String text) {
        if (!text.contains("<")) {
            return Message.raw(text);
        }

        var root = Message.empty();
        var stateStack = new ArrayDeque<StyleState>();
        stateStack.push(new StyleState());
        var matcher = TAG_PATTERN.matcher(text);

        int lastIndex = 0;
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();

            if (start > lastIndex) {
                var content = text.substring(lastIndex, start);
                root.insert(createStyledMessage(content, stateStack.element()));
            }

            lastIndex = end;
            boolean isClosing = "/".equals(matcher.group(1));
            var tagName = matcher.group(2).toLowerCase();
            var tagArg = matcher.group(3);

            if (isClosing) {
                if (stateStack.size() > 1) {
                    stateStack.pop();
                }
                continue;
            }

            var currentState = stateStack.element();
            var newState = currentState.copy();

            if (NAMED_COLORS.containsKey(tagName)) {
                newState = newState.withColor(NAMED_COLORS.get(tagName));
            } else {
                switch (tagName) {
                    case "color", "c", "colour" -> {
                        var c = parseColorArg(tagArg);
                        if (c != null) newState = newState.withColor(c);
                    }
                    case "bold", "b" -> newState = newState.withBold();
                    case "italic", "i", "em" -> newState = newState.withItalic();
                    case "underline", "u" -> newState = newState.withUnderlined();
                    case "monospace", "mono" -> newState = newState.withMonospace();
                    case "link", "url" -> {
                        if (tagArg != null) newState = newState.withLink(tagArg);
                    }
                    case "reset", "r" -> {
                        stateStack.clear();
                        newState = new StyleState();
                    }
                }
            }
            stateStack.push(newState);
        }

        if (lastIndex < text.length()) {
            var content = text.substring(lastIndex);
            root.insert(createStyledMessage(content, stateStack.element()));
        }

        return root;
    }

    public static Message createStyledMessage(String content, StyleState state) {
        var msg = Message.raw(content);
        if (state.color != null) msg.color(state.color);
        applyCommonStyles(msg, state);

        return msg;
    }

    private static void applyCommonStyles(Message msg, StyleState state) {
        if (state.bold) msg.bold(true);
        if (state.italic) msg.italic(true);
        if (state.monospace) msg.monospace(true);
        if (state.underlined) msg.getFormattedMessage().underlined = MaybeBool.True;
        if (state.link != null) msg.link(state.link);
    }

    private static Color parseColorArg(String arg) {
        if (arg == null) return null;
        return NAMED_COLORS.getOrDefault(arg, parseHexColor(arg));
    }

    private static Color parseHexColor(String hex) {
        try {
            String clean = hex.startsWith("#") ? hex.substring(1) : hex;
            if (clean.length() == 6) {
                var rgb = HexFormat.of().parseHex(clean);
                return new Color(rgb[0] & 0xFF, rgb[1] & 0xFF, rgb[2] & 0xFF);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public static class StyledText {
        private final List<Segment> segments;

        public StyledText(String text) {
            this.segments = List.of(new Segment(text, new StyleState()));
        }

        private StyledText(List<Segment> segments) {
            this.segments = Collections.unmodifiableList(segments);
        }

        private StyledText applyToLast(java.util.function.Function<Segment, Segment> segmentChanger) {
            if (segments.isEmpty()) {
                return this;
            }
            List<Segment> newSegments = new ArrayList<>(segments.subList(0, segments.size() - 1));
            Segment lastSegment = segments.getLast();
            newSegments.add(segmentChanger.apply(lastSegment));
            return new StyledText(newSegments);
        }

        public StyledText withBold() {
            return applyToLast(s -> new Segment(s.text(), s.style().withBold()));
        }

        public StyledText withItalic() {
            return applyToLast(s -> new Segment(s.text(), s.style().withItalic()));
        }

        public StyledText withUnderlined() {
            return applyToLast(s -> new Segment(s.text(), s.style().withUnderlined()));
        }

        public StyledText withMonospace() {
            return applyToLast(s -> new Segment(s.text(), s.style().withMonospace()));
        }

        public StyledText withLink(String link) {
            return applyToLast(s -> new Segment(s.text(), s.style().withLink(link)));
        }

        public StyledText withColor(Color color) {
            return applyToLast(s -> new Segment(s.text(), s.style().withColor(color)));
        }

        public StyledText withGradient(Color startColor, Color endColor) {
            return applyToLast(s -> new Segment(s.text(), s.style().withGradient(startColor, endColor)));
        }

        public StyledText append(String text) {
            List<Segment> newSegments = new ArrayList<>(segments);
            newSegments.add(new Segment(text, new StyleState()));
            return new StyledText(newSegments);
        }

        public Message toMessage() {
            if (segments.isEmpty()) {
                return Message.empty();
            }
            var container = Message.empty();
            for (Segment segment : segments) {
                container.insert(ChatFormatter.createStyledMessage(segment.text(), segment.style()));
            }
            return container;
        }

        private record Segment(String text, StyleState style) {
        }
    }

    public record StyleState(Color color, boolean bold, boolean italic,
                             boolean underlined, boolean monospace, String link,
                             Color gradientStart, Color gradientEnd) {
        public StyleState() {
            this(null, false, false, false, false, null, null, null);
        }

        public StyleState copy() {
            return new StyleState(color, bold, italic, underlined, monospace, link, gradientStart, gradientEnd);
        }

        public StyleState withColor(Color color) {
            return new StyleState(color, bold, italic, underlined, monospace, link, gradientStart, gradientEnd);
        }

        public StyleState withBold() {
            return new StyleState(color, true, italic, underlined, monospace, link, gradientStart, gradientEnd);
        }

        public StyleState withItalic() {
            return new StyleState(color, bold, true, underlined, monospace, link, gradientStart, gradientEnd);
        }

        public StyleState withUnderlined() {
            return new StyleState(color, bold, italic, true, monospace, link, gradientStart, gradientEnd);
        }

        public StyleState withMonospace() {
            return new StyleState(color, bold, italic, underlined, true, link, gradientStart, gradientEnd);
        }

        public StyleState withLink(String link) {
            return new StyleState(color, bold, italic, underlined, monospace, link, gradientStart, gradientEnd);
        }

        public StyleState withGradient(Color startColor, Color endColor) {
            return new StyleState(color, bold, italic, underlined, monospace, link, startColor, endColor);
        }
    }
}
