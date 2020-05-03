package massim.javaagents;

import massim.javaagents.utils.Block;
import massim.javaagents.utils.Cell;
import massim.javaagents.utils.CellType;
import massim.javaagents.utils.DetailedCell;
import massim.javaagents.utils.IntegerPair;
import massim.javaagents.utils.OrdinaryCell;
import massim.javaagents.utils.Thing;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.stream.Collectors;

public class MapHandler {
    int length;
    int width;
    private Cell[][] map;
    private IntegerPair agentLocation;
    private List<IntegerPair> blocksLocationslist;
    private List<IntegerPair> dispensersLocationslist;
    private Map<IntegerPair, String> blocksTypeMap;
    private Map<IntegerPair, String> dispensersTypeMap;
    private List<IntegerPair> goalList;

    public MapHandler() {
        length = 0;
        width = 0;
        blocksLocationslist = new ArrayList<IntegerPair>();
        dispensersLocationslist = new ArrayList<IntegerPair>();
        blocksTypeMap = new HashMap<>();
        dispensersTypeMap = new HashMap<>();
        goalList = new LinkedList<>();
    }

    public List<IntegerPair> getGoalList() {
        return goalList;
    }

    public void printMapToFile(String path) {
        try {
            File myObj = new File(path);
            myObj.createNewFile();
            FileWriter myWriter = new FileWriter(myObj);
            for (var row : map) {
                for (var cell : row) {
                    if (cell.getType().equals(CellType.Unknown)) myWriter.write("x");
                    else if (cell.getType().equals(CellType.Dispenser)) myWriter.write("!");
                    else if (cell.getType().equals(CellType.Teammate)) myWriter.write("1");
                    else if (cell.getType().equals(CellType.Obstacle)) myWriter.write("2");
                    else myWriter.write("0");
                }
                myWriter.write("\n");
            }
            myWriter.close();
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    public Map<IntegerPair, String> getDispensersByType(String type) {
        return dispensersTypeMap.entrySet().stream().filter(p -> p.getValue().equals(type)).collect(Collectors.toMap(map -> map.getKey(), map -> map.getValue()));
    }

    public Map<IntegerPair, String> getBlocksByType(String type) {
        return blocksTypeMap.entrySet().stream().filter(p -> p.getValue().equals(type)).collect(Collectors.toMap(map -> map.getKey(), map -> map.getValue()));
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

    public Cell getCell(IntegerPair location) {
//        System.out.println("Location x= " + location.getX());
//        System.out.println("Location y= " + location.getY());
        return this.map[location.getX()][location.getY()];
    }

    public Map<IntegerPair, String> getBlocksTypeMap() {
        return blocksTypeMap;
    }

    public Map<IntegerPair, String> getDipensersTypeMap() {
        return dispensersTypeMap;
    }

    public void initiateMap(int length, int width, IntegerPair agentLocation) {
        this.length = length;
        this.width = width;
        this.map = new Cell[length][width];
        for (var row : map) {
            Arrays.fill(row, new OrdinaryCell(CellType.Unknown));
        }
        this.agentLocation = agentLocation;
        int x = agentLocation.getX();
        int y = agentLocation.getY();
        map[x][y] = new OrdinaryCell(CellType.Agent);
    }

    public void moveAgent(IntegerPair agentMovement) {
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
            int agent_y = this.agentLocation.getY();
            int x = item.getX() + agent_x;
            int y = item.getY() + agent_y;
            if (itemCellType == CellType.Goal) {
                goalList.add(new IntegerPair(x, y));
            }
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
            int agent_y = this.agentLocation.getY();
            int x = item.getX() + agent_x;
            int y = item.getY() + agent_y;
            this.map[x][y] = new DetailedCell(itemCellType, item.getType());
            switch (itemsType) {
                case "Blocks":
                    blocksLocationslist.add(new IntegerPair(x, y));
                    blocksTypeMap.put(new IntegerPair(x, y), item.getType());
                case "Dispensers":
                    dispensersLocationslist.add(new IntegerPair(x, y));
                    dispensersTypeMap.put(new IntegerPair(x, y), item.getType());
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
        addDetailedItemsToMap(dispensers, "Dispensers");
        addDetailedItemsToMap(blocks, "Blocks");

    }

    public IntegerPair getTeammateTransfer(IntegerPair teammatePercept, IntegerPair teammateLocation) {
        IntegerPair internalTeammateLocation = this.agentLocation.add(teammatePercept);
        IntegerPair transformationVector = internalTeammateLocation.subtract(teammateLocation);
        return transformationVector;
    }

    public Boolean checkTransformation(IntegerPair teammateLocation, IntegerPair transformationVector) {
        IntegerPair result = teammateLocation.add(transformationVector);
        return result.check(this.length, this.width);
    }

    public void makeTransformation(IntegerPair transform, Cell item, IntegerPair location) {
        if (checkTransformation(location, transform)) {
            IntegerPair newLocation = location.add(transform);
            this.map[newLocation.getX()][newLocation.getY()] = item;
            if (item.getType() == CellType.Dispenser) {
                dispensersTypeMap.put(newLocation, ((DetailedCell) item).getDetails());
            }
            if (item.getType() == CellType.Block) {
                blocksTypeMap.put(newLocation, ((DetailedCell) item).getDetails());
            }
        }
    }
}
