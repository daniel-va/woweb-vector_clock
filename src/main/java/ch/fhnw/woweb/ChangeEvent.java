package ch.fhnw.woweb;

public interface ChangeEvent {
    void apply(Document document);

    class CreateNode implements ChangeEvent {
        private final String author;

        public CreateNode(String author) {
            this.author = author;
        }

        @Override
        public void apply(Document document) {
            var id = document.nodes.size();
            document.nodes.add(new Document.Node(id, author));
        }
    }

    class UpdateNode implements ChangeEvent {
        private final long nodeId;
        private final String newText;

        public UpdateNode(long nodeId, String newText) {
            this.nodeId = nodeId;
            this.newText = newText;
        }

        @Override
        public void apply(Document document) {
            var i = document.findNodeIndex(nodeId);
            if (i >= 0) {
                document.nodes.get(i).text = newText;
            }
        }
    }

    class MoveNode implements ChangeEvent {
        private final long nodeId;
        private final int newIndex;

        public MoveNode(long nodeId, int newIndex) {
            this.nodeId = nodeId;
            this.newIndex = newIndex;
        }

        @Override
        public void apply(Document document) {
            var i = document.findNodeIndex(nodeId);
            if (i >= 0) {
                document.nodes.add(newIndex, document.nodes.remove(i));
            }
        }
    }

    class RemoveNode implements ChangeEvent {
        private final long nodeId;

        public RemoveNode(long nodeId) {
            this.nodeId = nodeId;
        }

        @Override
        public void apply(Document document) {
            var i = document.findNodeIndex(nodeId);
            if (i >= 0) {
                document.nodes.remove(i);
            }
        }
    }
}
