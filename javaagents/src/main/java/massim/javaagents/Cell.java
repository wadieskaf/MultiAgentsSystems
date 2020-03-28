package massim.javaagents;

import massim.javaagents.utils.Block;

abstract class Cell {
    private CellType type;

    Cell(CellType type) {
        this.type = type;
    }

    public CellType getType() {
        return type;
    }
}
