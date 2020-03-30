package massim.javaagents;

import massim.javaagents.utils.Block;
import massim.javaagents.utils.Cell;
import massim.javaagents.utils.CellType;
import massim.javaagents.utils.DetailedCell;
import massim.javaagents.utils.IntegerPair;
import massim.javaagents.utils.OrdinaryCell;
import massim.javaagents.utils.Thing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MapHandler {
    private Cell[][] map;
    private IntegerPair agentLocation;
    private List<IntegerPair> blocksLocationslist;
    private List<IntegerPair> dispensersLocationslist;

    public MapHandler() {
        blocksLocationslist = new ArrayList<IntegerPair>();
        dispensersLocationslist = new ArrayList<IntegerPair>();
    }

    public IntegerPair getAgentLocation() {
        return agentLocation;
    }

    public Cell[][] getMap() {
        return map;
    }

    public List<IntegerPair> getBlocksLocationslist() {
        return blocksLocationslist;
    }

    public List<IntegerPair> getDispensersLocationslist() {
        return dispensersLocationslist;
    }

    public void initiateMap(int length, int width, IntegerPair agentMovement) {
        this.map = new Cell[length][width];
        for(var row : map){
            Arrays.fill(row, new OrdinaryCell(CellType.Unknown));
        }
        this.agentLocation = agentMovement;
        int x = agentMovement.getX();
        int y = agentMovement.getY();
        map[x][y] = new OrdinaryCell(CellType.Agent);
    }

    private void moveAgent(IntegerPair agentMovement) {
        int new_x = this.agentLocation.getX() + agentMovement.getX();
        int new_y = this.agentLocation.getY() + agentMovement.getY();
        this.agentLocation = new IntegerPair(new_x, new_y);
    }

    private void addOrdinaryItemsToMap(List<Thing> items, String itemsType) {
        CellType itemCellType;
        switch (itemsType) {
            case "Enemies":
                itemCellType = CellType.Enemy;
                break;
            case "Teammates":
                itemCellType = CellType.Teammate;
                break;
            case "Obstacles":
                itemCellType = CellType.Obstacle;
                break;
            case "Goals":
                itemCellType = CellType.Goal;
                break;
            case "Empty":
                itemCellType = CellType.Empty;
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + itemsType);
        }
        for (var item : items) {
            int agent_x = this.agentLocation.getX();
            int agent_y = this.agentLocation.getX();
            int x = item.getX() + agent_x;
            int y = item.getY() + agent_y;
            this.map[x][y] = new OrdinaryCell(itemCellType);

        }
    }

    private void addDetailedItemsToMap(List<Block> items, String itemsType) {
        CellType itemCellType;
        switch (itemsType) {
            case "Blocks":
                itemCellType = CellType.Block;
                break;
            case "Dispensers":
                itemCellType = CellType.Dispenser;
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + itemsType);
        }
        for (var item : items) {
            int agent_x = this.agentLocation.getX();
            int agent_y = this.agentLocation.getX();
            int x = item.getX() + agent_x;
            int y = item.getY() + agent_y;
            this.map[x][y] = new DetailedCell(itemCellType, item.getType());
            switch (itemsType) {
                case "Blocks":
                    blocksLocationslist.add(new IntegerPair(x, y));
                case "Dispensers":
                    dispensersLocationslist.add(new IntegerPair(x, y));
            }
        }
    }

    public void updateAgentLocation(IntegerPair agentMovement) {
        this.map[agentLocation.getX()][agentLocation.getY()] = new OrdinaryCell(CellType.Empty);
        this.moveAgent(agentMovement);
        this.map[agentLocation.getX()][agentLocation.getY()] = new OrdinaryCell(CellType.Agent);
    }

    public void updateMap(PerceptionHandler handler) {
        IntegerPair agentMovement = handler.getAgentMovement();
        updateAgentLocation(agentMovement);
        List<Thing> enemies = handler.getEnemies();
        List<Thing> teammates = handler.getTeammates();
        List<Thing> obstacles = handler.getObstacles();
        List<Thing> goals = handler.getGoals();
        List<Thing> empty = handler.getEmpty();
        List<Block> blocks = handler.getBlocks();
        List<Block> dispensers = handler.getDispensers();

        addOrdinaryItemsToMap(enemies, "Enemies");
        addOrdinaryItemsToMap(teammates, "Teammates");
        addOrdinaryItemsToMap(obstacles, "Obstacles");
        addOrdinaryItemsToMap(goals, "Goals");
        addOrdinaryItemsToMap(empty, "Empty");
        addDetailedItemsToMap(blocks, "Blocks");
        addDetailedItemsToMap(dispensers, "Dispensers");

    }
}
