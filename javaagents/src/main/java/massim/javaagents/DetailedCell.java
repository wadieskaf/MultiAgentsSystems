package massim.javaagents;

import massim.javaagents.utils.Block;

public class DetailedCell extends Cell {
    String details;
    DetailedCell(CellType type) {
        super(type);
    }
    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
