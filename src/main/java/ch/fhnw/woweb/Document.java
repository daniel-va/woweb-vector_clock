package ch.fhnw.woweb;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Document {
    public final List<Node> nodes;

    public Document() {
        this.nodes = new ArrayList<>();
    }

    public Document(Document document) {
        this.nodes = new ArrayList<>(document.nodes);
    }

    public int findNodeIndex(long nodeId) {
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).id == nodeId) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public String toString() {
        return nodes.stream()
                .map(Object::toString)
                .collect(Collectors.joining("\n"));
    }

    public static class Node {
        private final long id;
        public final String author;
        public String text = "";

        public Node(long id, String author) {
            this.id = id;
            this.author = author;
        }

        @Override
        public String toString() {
            return String.format("[%d] %s: %s", id, author, text);
        }
    }
}
