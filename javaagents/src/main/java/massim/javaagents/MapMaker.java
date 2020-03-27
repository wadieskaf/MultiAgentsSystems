package massim.javaagents;

import massim.javaagents.utils.Block;
import massim.javaagents.utils.Thing;

import java.awt.geom.Point2D;
import java.util.List;

public class MapMaker {
    public int[][] createMap(PerceptionHandler handler, Point2D agentLocation) {
        int[][] map = new int[100][100];
        List<Thing> enemies = handler.getEnemies();
        List<Thing> teammates = handler.getTeammates();
        List<Thing> obstacles = handler.getObstacles();
        List<Thing> goals = handler.getGoals();
        List<Block> blocks = handler.getBlocks();
        List<Block> dispensers = handler.getDispensers();

        int agent_x = (int) agentLocation.getX();
        int agent_y = (int) agentLocation.getY();
        int x;
        int y;

        // Enemies have value 1
        for (var enemy : enemies) {
            x = enemy.getX();
            y = enemy.getY();
            map[agent_x + x][agent_y - y] = 1;
        }
        // Teammates have value 2
        for (var teammate : teammates) {
            x = teammate.getX();
            y = teammate.getY();
            map[agent_x + x][agent_y - y] = 2;
        }
        // Obstacles have value 3
        for (var obstacle : obstacles) {
            x = obstacle.getX();
            y = obstacle.getY();
            map[agent_x + x][agent_y - y] = 3;
        }
        // Goals have value 4
        for (var goal : goals) {
            x = goal.getX();
            y = goal.getY();
            map[agent_x + x][agent_y - y] = 4;
        }
        // Blocks have value 5
        for (var block : blocks) {
            x = block.getX();
            y = block.getY();
            map[agent_x + x][agent_y - y] = 5;
        }
        // Dispensers have value 6
        for (var dispenser : dispensers) {
            x = dispenser.getX();
            y = dispenser.getY();
            map[agent_x + x][agent_y - y] = 6;
        }

        return map;
    }
}
