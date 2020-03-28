package massim.javaagents;

import massim.javaagents.utils.Block;
import massim.javaagents.utils.Thing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MapHandler {
    private Cell[][] map;
    private IntegerPair agentLocation;
    private ArrayList<IntegerPair> blocksLocationslist;
    private ArrayList<IntegerPair> dispensersLocationslist;

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

    public ArrayList<IntegerPair> getBlocksLocationslist() {
        return blocksLocationslist;
    }

    public ArrayList<IntegerPair> getDispensersLocationslist() {
        return dispensersLocationslist;
    }

    public void initiateMap(int length, int width, IntegerPair agentMovement) {
        this.map = new Cell[length][width];
        Cell emptyCell = new OrdinaryCell(CellType.Unknown);
        Arrays.fill(map, emptyCell);
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
        OrdinaryCell itemCell;
        switch (itemsType) {
            case "Enemies":
                itemCell = new OrdinaryCell(CellType.Enemy);
                break;
            case "Teammates":
                itemCell = new OrdinaryCell(CellType.Teammate);
                break;
            case "Obstacles":
                itemCell = new OrdinaryCell(CellType.Obstacle);
                break;
            case "Goals":
                itemCell = new OrdinaryCell(CellType.Goal);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + itemsType);
        }
        for (var item : items) {
            int agent_x = this.agentLocation.getX();
            int agent_y = this.agentLocation.getX();
            int x = item.getX() + agent_x;
            int y = item.getY() + agent_y;
            this.map[x][y] = itemCell;

        }
    }

    private void addDetailedItemsToMap(List<Block> items, String itemsType) {
        DetailedCell itemCell;
        switch (itemsType) {
            case "Blocks":
                itemCell = new DetailedCell(CellType.Block);
                break;
            case "Dispensers":
                itemCell = new DetailedCell(CellType.Dispenser);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + itemsType);
        }
        for (var item : items) {
            int agent_x = this.agentLocation.getX();
            int agent_y = this.agentLocation.getX();
            int x = item.getX() + agent_x;
            int y = item.getY() + agent_y;
            itemCell.setDetails(item.getType());
            this.map[x + agent_x][y + agent_y] = itemCell;
            switch (itemsType) {
                case "Blocks":
                    blocksLocationslist.add(new IntegerPair(x, y));
                case "Dispensers":
                    dispensersLocationslist.add(new IntegerPair(x, y));
            }
        }
    }

    public void updateAgentLocation(IntegerPair agentMovement) {
        this.map[agentLocation.getX()][agentLocation.getY()] = new OrdinaryCell(CellType.Unknown);
        this.moveAgent(agentMovement);
        this.map[agentLocation.getX()][agentLocation.getY()] = new OrdinaryCell(CellType.Agent);
    }

    public void updateMap(PerceptionHandler handler, IntegerPair agentMovement) {
        updateAgentLocation(agentMovement);
        List<Thing> enemies = handler.getEnemies();
        List<Thing> teammates = handler.getTeammates();
        List<Thing> obstacles = handler.getObstacles();
        List<Thing> goals = handler.getGoals();
        List<Block> blocks = handler.getBlocks();
        List<Block> dispensers = handler.getDispensers();

        addOrdinaryItemsToMap(enemies, "Enemies");
        addOrdinaryItemsToMap(teammates, "Teammates");
        addOrdinaryItemsToMap(obstacles, "Obstacles");
        addOrdinaryItemsToMap(goals, "Goals");
        addDetailedItemsToMap(blocks, "Blocks");
        addDetailedItemsToMap(dispensers, "Dispensers");

    }
}
