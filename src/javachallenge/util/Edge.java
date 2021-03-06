package javachallenge.util;

import java.io.Serializable;

/**
 * Created by peyman on 2/6/14.
 */
public class Edge implements Serializable {
    private Node[] nodes = new Node[2];
    private Cell[] cells = new Cell[2];
    private EdgeType type;

    public Edge() {
        this.type = EdgeType.NONE;
    }

    public Node[] getNodes() {
        return nodes;
    }

    public void setNodes(Node[] nodes) {
        this.nodes = nodes;
    }

    public Cell[] getCells() {
        return cells;
    }

    public void setCells(Cell[] cells) {
        this.cells = cells;
    }

    public EdgeType getType() {
        return type;
    }

    public void setType(EdgeType type) {
        this.type = type;
    }
}
