package massim.javaagents.utils;

public class DetailedCell extends Cell {
    String details;
    public DetailedCell(CellType type) {
        super(type);
    }
    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
