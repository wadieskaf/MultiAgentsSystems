package massim.javaagents;

import massim.javaagents.utils.Block;
import massim.javaagents.utils.Thing;

import java.util.List;

public class MapHandler {
    private int[][] map;

    public IntegerPair getAgentLocation() {
        return agentLocation;
    }

    private IntegerPair agentLocation;

    public int[][] getMap() {
        return map;
    }


    public void initiateMap(int length, int width, IntegerPair agentMovement) {
        this.map = new int[length][width];
        this.agentLocation = agentMovement;
        int x = agentMovement.getX();
        int y = agentMovement.getY();
        //Agent location has value of 7
        map[x][y] = 7;
    }

    private void moveAgent(IntegerPair agentMovement) {
        int new_x = this.agentLocation.getX() + agentMovement.getX();
        int new_y = this.agentLocation.getY() + agentMovement.getY();
        this.agentLocation = new IntegerPair(new_x, new_y);
    }
    public void updateAgentLocation(IntegerPair agentMovement){
        this.map[agentLocation.getX()][agentLocation.getY()] = 0;
        this.moveAgent(agentMovement);
        this.map[agentLocation.getX()][agentLocation.getY()] = 7;
    }

    public void updateMap(PerceptionHandler handler, IntegerPair agentMovement) {
        updateAgentLocation(agentMovement);
        List<Thing> enemies = handler.getEnemies();
        List<Thing> teammates = handler.getTeammates();
        List<Thing> obstacles = handler.getObstacles();
        List<Thing> goals = handler.getGoals();
        List<Block> blocks = handler.getBlocks();
        List<Block> dispensers = handler.getDispensers();

        int agent_x = agentMovement.getX();
        int agent_y = agentMovement.getY();
        int x;
        int y;

        // Enemies have value 1
        for (var enemy : enemies) {
            x = enemy.getX();
            y = enemy.getY();
            this.map[agent_x - x][agent_y + y] = 1;
        }
        // Teammates have value 2
        for (var teammate : teammates) {
            x = teammate.getX();
            y = teammate.getY();
            this.map[agent_x - x][agent_y + y] = 2;
        }
        // Obstacles have value 3
        for (var obstacle : obstacles) {
            x = obstacle.getX();
            y = obstacle.getY();
            this.map[agent_x - x][agent_y + y] = 3;
        }
        // Goals have value 4
        for (var goal : goals) {
            x = goal.getX();
            y = goal.getY();
            this.map[agent_x - x][agent_y + y] = 4;
        }

        // Dispensers have value 5
        for (var dispenser : dispensers) {
            x = dispenser.getX();
            y = dispenser.getY();
            this.map[agent_x + x][agent_y - y] = 5;
        }
    }
}
