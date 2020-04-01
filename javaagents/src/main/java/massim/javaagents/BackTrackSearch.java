package massim.javaagents;

import massim.javaagents.utils.Cell;
import massim.javaagents.utils.CellType;
import massim.javaagents.utils.IntegerPair;

import java.util.ArrayList;
import java.util.List;

public class BackTrackSearch {
    private MapHandler mapHandler;
    private List<IntegerPair> visitedNodes;
    private IntegerPair currentAgentLocation;
    private IntegerPair initialAgentLocation;
    private List<CellType> stuckTypes;
    private List<IntegerPair> path;
    private int agentVision;
    private List<IntegerPair> visionBorders;
    List<IntegerPair> possibleMovementsCoordinates;

    public BackTrackSearch(MapHandler mapHandler, int agentVision){
        this.mapHandler = mapHandler;
        this.visitedNodes = new ArrayList<>();
        this.currentAgentLocation = mapHandler.getAgentLocation();
        this.initialAgentLocation = mapHandler.getAgentLocation();
        this.stuckTypes = new ArrayList<>(
                List.of(CellType.Obstacle)
        );
        this.path = new ArrayList<>();
        this.visitedNodes.add(currentAgentLocation);
        this.agentVision = agentVision;
        this.visionBorders = calculateVisionBorders();
        this.possibleMovementsCoordinates = possibleMovementsCoordinates();
    }

    public IntegerPair getBestMovement(){
        if (this.path.size() == 0){
            return null;
        }
        else {
            return this.path.get(0);
        }
    }
/*    private List<IntegerPair> findNearbyUnknow(){
        List<IntegerPair> nearbyLocations = new ArrayList<>();
        List<Integer> possibleMovements = new ArrayList<>(
                List.of(1,-1)
        );
        Cell[][] map = this.mapHandler.getMap();
        for (int i: possibleMovements){
            int x = mapHandler.getAgentLocation().getX() + i;
            int y = mapHandler.getAgentLocation().getY();
            if (map[x][y].getType() == CellType.Unknown){
                nearbyLocations.add(new IntegerPair(x,y));
            }
            x = mapHandler.getAgentLocation().getX();
            y = mapHandler.getAgentLocation().getY() + i;
            if (map[x][y].getType() == CellType.Unknown){
                nearbyLocations.add(new IntegerPair(x,y));
            }
        }
        return nearbyLocations;
    }*/
    private List<IntegerPair> possibleMovementsCoordinates(){
        List<IntegerPair> result = new ArrayList<>();
        List<Integer> possibleMovements = new ArrayList<>(
                List.of(1,-1)
        );
        for (int i: possibleMovements){
            result.add(new IntegerPair(i,0));
            result.add(new IntegerPair(0,i));
        }
        return result;
    }
    private Boolean checkMovement(IntegerPair movement){
        IntegerPair newLocation = this.currentAgentLocation.add(movement);
        return newLocation.getX() < 100 && newLocation.getX() >= 0 &&
                newLocation.getY() < 100 && newLocation.getY() >= 0;
    }
    private int calculateDistance(){
        return (Math.abs(this.initialAgentLocation.getX() - this.currentAgentLocation.getX()) +
                Math.abs(this.initialAgentLocation.getY() - this.currentAgentLocation.getY()))/2;
    }
    private List<IntegerPair> calculateVisionBorders(){
        List<IntegerPair> visionBorders = new ArrayList<>();
        IntegerPair right = this.initialAgentLocation.add(new IntegerPair(0, this.agentVision));
        IntegerPair top = this.initialAgentLocation.add((new IntegerPair(this.agentVision, 0)));
        IntegerPair left = this.initialAgentLocation.add((new IntegerPair(0, -1 * this.agentVision)));
        IntegerPair bottom = this.initialAgentLocation.add(new IntegerPair(-1 * this.agentVision, 0));
        visionBorders.add(right);
        visionBorders.add(left);
        visionBorders.add(top);
        visionBorders.add(bottom);
        for (int i = 1; i<this.agentVision;i++){
            visionBorders.add(left.add(new IntegerPair(i,i)));
            visionBorders.add(left.add(new IntegerPair(-i,i)));
            visionBorders.add(right.add(new IntegerPair(i,-i)));
            visionBorders.add(right.add(new IntegerPair(-i,-i)));
        }
        return visionBorders;
    }
    public Boolean search(){
        List<IntegerPair> possibleMovements = new ArrayList<>();
        for (IntegerPair movement:this.possibleMovementsCoordinates){
            IntegerPair nextLocation = this.currentAgentLocation.add(movement);
            if (checkMovement(movement) && !this.visitedNodes.contains(nextLocation)){
                possibleMovements.add(movement);
            }
        }
        if (possibleMovements.size() == 0){
            return false;
        }
        if (this.visionBorders.contains(this.currentAgentLocation)) {
            for(IntegerPair movement:possibleMovements){
                if (this.mapHandler.getCell(currentAgentLocation.add(movement)).getType() == CellType.Unknown) {
                    return true;
                }
            }
            return false;
        }
        int stuckCount=0;

        for (IntegerPair movement: possibleMovements) {
            IntegerPair nextLocation = this.currentAgentLocation.add(movement);
            if (stuckTypes.contains(this.mapHandler.getCell(nextLocation).getType())) {
                ++stuckCount;
            }
        }
        if (stuckCount == possibleMovements.size()){
            return false;
        }
        for (IntegerPair movement:possibleMovements){
            IntegerPair nextLocation = currentAgentLocation.add(movement);
            this.path.add(movement);
            this.visitedNodes.add(nextLocation);
            this.currentAgentLocation = nextLocation;
            if (search()){
                return true;
            }
            this.path.remove(path.size() - 1);
            this.currentAgentLocation = this.currentAgentLocation.add(movement.inverse());

        }
        return false;

    }

}
