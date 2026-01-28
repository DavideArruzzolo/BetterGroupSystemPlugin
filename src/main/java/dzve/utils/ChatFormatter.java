package dzve.utils;

import com.hypixel.hytale.protocol.MaybeBool;
import com.hypixel.hytale.server.core.Message;
import lombok.ToString;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ToString
public final class ChatFormatter {

    public static StyledText of(String text) {
        return new StyledText(text);
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

    @ToString
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
                             boolean underlined, boolean monospace, String link) {
        public StyleState() {
            this(null, false, false, false, false, null);
        }

        public StyleState withColor(Color color) {
            return new StyleState(color, bold, italic, underlined, monospace, link);
        }

        public StyleState withBold() {
            return new StyleState(color, true, italic, underlined, monospace, link);
        }

        public StyleState withItalic() {
            return new StyleState(color, bold, true, underlined, monospace, link);
        }

        public StyleState withUnderlined() {
            return new StyleState(color, bold, italic, true, monospace, link);
        }

        public StyleState withMonospace() {
            return new StyleState(color, bold, italic, underlined, true, link);
        }

        public StyleState withLink(String link) {
            return new StyleState(color, bold, italic, underlined, monospace, link);
        }
    }
}
