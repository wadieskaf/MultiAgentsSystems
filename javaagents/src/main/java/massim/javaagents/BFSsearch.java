package massim.javaagents;

import java.sql.SQLOutput;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import massim.javaagents.utils.*;

public class BFSsearch {
    MapHandler mh;
    IntegerPair agentCurrentPos;
    int move_count;
    //how many cells to dequeue in order to take a step
    int cells_left_in_layer;
    //how many cells have been added in the BFS expansion
    int cells_in_next_layer;
    boolean reached_goal;
    Queue<IntegerPair> queue;
    int[][] map; //keep track of visited cells
    List <IntegerPair> path;
    int length;
    int width;

    public BFSsearch(MapHandler mh, int length, int width){
        this.mh = mh;
        this.agentCurrentPos = this.mh.getAgentLocation();
        this.move_count = 0;
        this.cells_left_in_layer = 1;
        this.cells_in_next_layer = 0;
        this.reached_goal = false;
        this.queue = new LinkedList<>();
        this.length = length;
        this.width = width;
        this.map = new int[length][width];
        this.path = new LinkedList<>();
    }


    public List<IntegerPair> bfsByLocation(IntegerPair goal){
        //System.out.println("Starting Position: (" + agentCurrentPos.getX() + "," + agentCurrentPos.getY() + ")");
        queue.add(agentCurrentPos);
        this.map[agentCurrentPos.getX()][agentCurrentPos.getY()] = 1; //mark as visited

        while (queue.size() > 0){

            IntegerPair currentCell = queue.remove();
            //System.out.println("Current: (" + currentCell.getX() + "," + currentCell.getY() + ")");
            if(currentCell.getX() == goal.getX() && currentCell.getY() == goal.getY()){
                path.add(currentCell);
                reached_goal = true;
                break;
            }
            //if not goal cell -> explore neighbours
            explore_neighbours(currentCell);

            cells_left_in_layer -= 1;
            if (cells_left_in_layer == 0){
                cells_left_in_layer = cells_in_next_layer;
                cells_in_next_layer = 0;
                move_count +=1;
                path.add(currentCell);//not sure if this is correct
            }
        }

        /*if(reached_goal){
            return path;
        }*/
        return path;
    }

    public List<IntegerPair> bfsByType(CellType cellType, String detail){
        //System.out.println("Starting Position: (" + agentCurrentPos.getX() + "," + agentCurrentPos.getY() + ")");
        queue.add(agentCurrentPos);
        this.map[agentCurrentPos.getX()][agentCurrentPos.getY()] = 1; //mark as visited

        while (queue.size() > 0){

            IntegerPair currentCell = queue.remove();
            //System.out.println("Current: (" + currentCell.getX() + "," + currentCell.getY() + ")");
            if(mh.getMap()[currentCell.getX()][currentCell.getY()] == new OrdinaryCell(cellType)){
                if(detail==""){
                    path.add(currentCell);
                    reached_goal = true;
                    break;
                }
                else{
                    //check if cell detail is the same as the preferred detail
                    //this applies to blocks and dispensers in which detail is the type -- !!!!!! WAITING METHOD !!!!!!!
                }
            }
            //if not goal cell -> explore neighbours
            explore_neighbours(currentCell);

            cells_left_in_layer -= 1;
            if (cells_left_in_layer == 0){
                cells_left_in_layer = cells_in_next_layer;
                cells_in_next_layer = 0;
                move_count +=1;
                path.add(currentCell);//not sure if this is correct
            }
        }

        /*if(reached_goal){
            return path;
        }*/
        return path;
    }

    public void explore_neighbours(IntegerPair current){

        IntegerPair newCellPos = null;

        //possible directions agent can look
        int [] directionInX = {-1,1,0,0};
        int [] directionInY = {0,0,1,-1};

        //for each direction -> check
        for(int i=0; i<4; i++){

            int newPosX = current.getX() + directionInX[i];
            int newPosY = current.getY() + directionInY[i];
            newCellPos =  new IntegerPair(newPosX, newPosY);
            //System.out.println("New CellPos: (" + newCellPos.getX() + "," + newCellPos.getY() + ")");

            if(newCellPos.getX() < 0 || newCellPos.getX() > this.length - 1 || newCellPos.getY() < 0 || newCellPos.getY() > this.width -1){
                continue;
            }
            //check if position has already been visited
            if(this.map[newCellPos.getX()][newCellPos.getY()] == 1) {
                continue;
            }
            //check if the newPosition is valid to move
            if(mh.getMap()[newCellPos.getX()][newCellPos.getY()] == new OrdinaryCell(CellType.Obstacle) || mh.getMap()[newCellPos.getX()][newCellPos.getY()] == new OrdinaryCell(CellType.Unknown)){
                continue; //then check other direction
            }

            this.queue.add(newCellPos);
            this.map[newCellPos.getX()][newCellPos.getY()] = 1; //mark as visited (altough it was only enqueued we don't want to enqueue it again)
            this.cells_in_next_layer += 1;

        }
    }


}
