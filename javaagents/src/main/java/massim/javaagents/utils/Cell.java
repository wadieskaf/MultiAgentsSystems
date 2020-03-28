package massim.javaagents.utils;

public class Cell {
    private CellType type;

    Cell(CellType type) {
        this.type = type;
    }

    public CellType getType() {
        return type;
    }
}
