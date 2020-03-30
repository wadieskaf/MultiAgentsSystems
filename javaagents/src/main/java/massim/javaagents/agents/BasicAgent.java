package massim.javaagents.agents;

import eis.iilang.*;
import massim.javaagents.*;

import java.util.*;

import massim.javaagents.utils.*;

/**
 * A very basic agent.
 */
public class BasicAgent extends Agent {

    private MapHandler mapHandler;
    private PerceptionHandler perceptionHandler;
    private int agent_x, agent_y;//for testing
    private IntegerPair agentMovement;
    private boolean hasBlockAttached;
    private int length, width;
    private int[][] tempMap; //just testing some stuff.. will be deleted
    /**
     * Constructor.
     * @param name    the agent's name
     * @param mailbox the mail facility
     */
    public BasicAgent(String name, MailService mailbox) {
        super(name, mailbox);
        this.agent_x = 49;
        this.agent_y = 49;
        this.length = 100;
        this.width = 100;
        this.agentMovement = new IntegerPair(0,0);
        this.mapHandler = new MapHandler();
        this.mapHandler.initiateMap(this.length,this.width, new IntegerPair(this.agent_x,this.agent_y));
        //this.tempMap = new int[length][width]; //temporary... just testing stuff
        //Arrays.fill(tempMap, 0);
        //this.tempMap[this.agent_x][this.agent_y] = 1;
    }

    @Override
    public void handlePercept(Percept percept) {}

    @Override
    public void handleMessage(Percept message, String sender) {}

    @Override
    public Action step() {

        //PHASE 1 (Sense) - Agent gets perceptions from Environment
        List<Percept> percepts = getPercepts();
        this.perceptionHandler = new PerceptionHandler(percepts);

        //PHASE 2 (Internal State) - Agent updates its internal state
        this.mapHandler.updateMap(perceptionHandler, this.agentMovement);//needs to check if lastAction was successful before updating...

        //PHASE 3 (Deliberate) - Agent tries to figure out what is the best action to perform given his current state and his previous action
        Action action = workThoseNeurons3();

        //PHASE 4 (ACT) - Execute chosen action to achieve goal
        return action;
    }


    public Action workThoseNeurons3(){
        Action action = null;

        //If there is a task -> Check it
        if(this.perceptionHandler.getTasks().size() > 0){

            //if in a goal area and task is done -> submit it
            if(hasDoneTask()){
                if(isGoal()){
                    action = new Action("submit", new Identifier(this.perceptionHandler.getTasks().get(0).getName())); //!!!!!HANDLE WHAT TASK TO SUBMIT!!!!
                }
                else {
                    action = lookForGoal();
                }
            }

            //this means you couldn't submit it because you didn't finish your task
            else {
                action = blockOnMySide("");
                if(action == null){
                    //if there isn't any block next to me (1 cell away) -> look and move to the closest block
                    action = lookForBlock("");

                    if(action == null){

                    }
                }
                action = lookForRequirement();
            }







            //if not in goal area, but has (a) block(s) attached and has completed the task -> go for the goal area to submit it
            //if(hasBlockAttached && hasDoneTask()){
            action = lookForGoal();

            //else if has (a) block(s) attached, but hasn't finished the task (didn't fulfill all the requirements) -> go for the requirements location
            //else if(hasBlockAttached){
            action = lookForRequirement();

            //if doesn't have a block attached or haven't finished task and there is a block next to the agent, if it is what he needs, get it
            //if(!hasBlockAttached || !hasDoneTask()){
            action = blockOnMySide("");

            //if there isn't any block next to me (1 cell away) -> look and move to the closest block
            action = lookForBlock(""); //!!!MISSING - GET DETAIL

            //if there is no block of my interest in my map -> check dispenser next to me...
            action = dispenserOnMySide("");

            //if no dispenser is next to me (1 cell away) -> look and go for the closest...
            action = lookForDispenser(""); //!!!MISSING - GET DETAIL

            //if none of those conditions apply, just explore the place - explore means going for cells marked as unknowns
            action = explore();
        }

        //if no task is available -> just explore
        else{
            action = explore();
        }

        //OK - NOW DO IT!
        if(action != null){
            return action;
        }

        return new Action("skip");
    }



    //##################################### FUNCTIONS #####################################################

    public Action readyToSubmit(){
        if(hasDoneTask()){
            return new Action("submit", new Identifier(this.perceptionHandler.getTasks().get(0).getName())); //!!!!!HANDLE WHAT TASK TO SUBMIT!!!!
        }
        return null;
    }

    public Action lookForDispenser(String detail){
        List<IntegerPair> dispensers = this.mapHandler.getDispensersLocationslist(); //!!!!MISSING DISPENSER TYPE!!!
        if(dispensers.size() > 0 ){
            List<IntegerPair> path = getShortestPathByType(CellType.Dispenser, detail);
            if(path.size()>1){
                IntegerPair next_location = path.get(1);
                return moveTo(next_location);
            }
        }
        return null;
    }

    public Action lookForBlock(String detail){
        List<IntegerPair> blocks = this.mapHandler.getBlocksLocationslist();
        if(blocks.size() > 0 ){
            List<IntegerPair> path = getShortestPathByType(CellType.Block, detail);
            if(path.size()>1){
                IntegerPair next_location = path.get(1);
                return moveTo(next_location);
            }
        }
        return null;
    }

    public boolean isGoal(){
        if(mapHandler.getMap()[mapHandler.getAgentLocation().getX()][mapHandler.getAgentLocation().getY()].getType().equals(CellType.Goal)){
            return true;
        }
        return false;
    }

    public Action lookForGoal(){
        List <IntegerPair> path = getShortestPathByType(CellType.Goal, "");
        if(path.size()>1){
            IntegerPair next_location = path.get(1);
            return moveTo(next_location);
        }
        return null;
    }

    public Action lookForRequirement(){
        Map<Block, Boolean> reqs = perceptionHandler.getTasks().get(0).getRequirements();

        List <IntegerPair> path = new LinkedList<>();
        int shortestPathSize = 1000;

        for (Map.Entry<Block,Boolean> req: reqs.entrySet()){
            Block b = req.getKey();
            List <IntegerPair> p = getShortestPathByLocation(new IntegerPair(b.getX(), b.getY()));
            if(p.size() > 1 && p.size() < shortestPathSize){
                path = p;
            }
        }

        if(path.size() > 1){
            IntegerPair nextLoc = path.get(0);
            return moveTo(nextLoc);
        }
        return null;
    }

    public boolean hasDoneTask(){
        List<Task> tasks = this.perceptionHandler.getTasks();
        if( tasks.size() > 0){
            Task task = tasks.get(0);
            Map<Block, Boolean> requirements = task.getRequirements();
            for (Map.Entry<Block,Boolean> requirement : requirements.entrySet())
                if(!requirement.getValue()){
                    return false;
                }
        }
        return true; //check scenario of no task
    }

    public String coordinatesToDirection(IntegerPair coordinates){
        if(coordinates.getX()==-1 && coordinates.getY()==0){
            return "w";
        }
        else if(coordinates.getX()==1 && coordinates.getY()==0){
            return "e";
        }
        else if(coordinates.getX()==0 && coordinates.getY()==-1){
            return "n";
        }
        else if(coordinates.getX()==0 && coordinates.getY()==1){
            return "s";
        }
        return "";
    }

    public Action blockOnMySide(String detail){
        int [] directionInX = {-1,1,0,0};
        int [] directionInY = {0,0,1,-1};

        //for each direction -> check
        for(int i=0; i<4; i++){
            int x = directionInX[i];
            int y = directionInY[i];
            if(this.mapHandler.getMap()[this.mapHandler.getAgentLocation().getX() + x][this.mapHandler.getAgentLocation().getY() + y].getType().equals(CellType.Block)){ //!!!MISSING CONDITION TYPE OF BLOCK (DETAILS)
                String direction = coordinatesToDirection(new IntegerPair(x,y));
                //!!!!!!!!!!HANDLE HOW TO ATTACH IF ALREADY HAVE BLOCKS ATTACHED!!!!!!!!!
                return new Action("attach", new Identifier(direction));
            }
        }
        return null;
    }

    public Action dispenserOnMySide(String detail){
        int [] directionInX = {-1,1,0,0};
        int [] directionInY = {0,0,1,-1};

        //for each direction -> check
        for(int i=0; i<4; i++){
            int x = directionInX[i];
            int y = directionInY[i];
            if(this.mapHandler.getMap()[this.mapHandler.getAgentLocation().getX() + x][this.mapHandler.getAgentLocation().getY() + y].getType().equals(CellType.Dispenser)){ //!!!MISSING CONDITION TYPE OF BLOCK (DETAILS)
                String direction = coordinatesToDirection(new IntegerPair(x,y));
                //!!!!!!!!!!HANDLE HOW TO REQUEST IF ALREADY HAVE BLOCKS ATTACHED!!!!!!!!!
                return new Action("request", new Identifier(direction));
            }
        }
        return null;
    }

    public List<IntegerPair> getShortestPathByType(CellType ct, String detail) {
        BFSsearch bfs = new BFSsearch(this.mapHandler, this.length, this.width);
        List<IntegerPair> path = bfs.bfsByType(ct, detail);
        return path;
    }

    public List<IntegerPair> getShortestPathByLocation(IntegerPair goal) {
        BFSsearch bfs = new BFSsearch(this.mapHandler, this.length, this.width);
        List<IntegerPair> path = bfs.bfsByLocation(goal);
        return path;
    }

    public Action explore(){
        BFSsearch bfs = new BFSsearch(this.mapHandler, this.length, this.width);
        List<IntegerPair> path = bfs.bfsByType(CellType.Unknown,"");
        System.out.println("Agent Location: " + "("+mapHandler.getAgentLocation().getX()+","+mapHandler.getAgentLocation().getY()+")");
        System.out.println("Path: ");
        for(var p: path){
            System.out.print("("+p.getX()+","+p.getY()+"), ");
        }

        if(path.size()>1){
            return moveTo(path.get(1));
        }
        return null;
    }


    public Action moveTo(IntegerPair nextLocation){
        int x_movement = nextLocation.getX() - this.mapHandler.getAgentLocation().getX();
        int y_movement = nextLocation.getY() - this.mapHandler.getAgentLocation().getY();

        this.agentMovement = new IntegerPair(x_movement, y_movement);

        if(this.agentMovement.getX() == 0 && this.agentMovement.getY() == 0)
            return new Action("skip");

        String direction = coordinatesToDirection(this.agentMovement);
        return new Action("move", new Identifier(direction));
    }

}


//###############################################################################################

//!!!!!!!!!!HANDLE WHAT TASK TO AIM FOR!!!!!!!!!!!!!!!!!
        /*
        //if in a goal area and task is done -> submit it
        if(isGoal()){
            if(hasDoneTask()){
                //!!!!!HANDLE WHAT TASK TO SUBMIT!!!!
                return new Action("submit", new Identifier(this.perceptionHandler.getTasks().get(0).getName()));
            }
        }

        //if not in goal area, but has (a) block(s) attached and has completed the task -> go for the goal area to submit it
        if(hasBlockAttached && hasDoneTask()){
            List <IntegerPair> path = getShortestPathByType(CellType.Goal, "");
            if(path.size()>1){
                IntegerPair next_location = path.get(1);
                return moveTo(next_location);
            }
        }
        //else if has (a) block(s) attached, but hasn't finished the task (didn't fulfill all the requirements) -> go for the requirements location
        else if(hasBlockAttached){
            //check which one of the requirements is closest to go for... (btw, code needs improvement :P, write id around 4am)
            Map<Block, Boolean> reqs = perceptionHandler.getTasks().get(0).getRequirements();

            List <IntegerPair> path = new LinkedList<>();
            int shortestPathSize = 1000;

            for (Map.Entry<Block,Boolean> req: reqs.entrySet()){
                Block b = req.getKey();
                List <IntegerPair> p = getShortestPathByLocation(new IntegerPair(b.getX(), b.getY()));
                if(p.size() > 1 && p.size() < shortestPathSize){
                    path = p;
                }
            }

            if(path.size() > 1){
                IntegerPair nextLoc = path.get(0);
                return moveTo(nextLoc);
            }

        }

        //if doesn't have a block attached or haven't finished task and there is a block next to the agent, if it is what he needs, get it
        if(!hasBlockAttached || !hasDoneTask()){
            IntegerPair blockLoc = blockOnMySide();
            if(blockLoc != null){
                String direction = coordinatesToDirection(blockLoc);
                //!!!!!!!!!!HANDLE HOW TO ATTACH IF ALREADY HAVE BLOCKS ATTACHED!!!!!!!!!
                return new Action("attach", new Identifier(direction));
            }
            //if there isn't any block next to me (1 cell away) -> look and move to the closest block
            else{
                //!!!!!!MISSING GET ONLY BLOCKS OF TYPE "DETAILS"

// Li eh por location e se kel lista for vazio -> untom xpia p type....

                List<IntegerPair> blocks = this.mapHandler.getBlocksLocationslist();
                if(blocks.size() > 0 ){
                    List<IntegerPair> path = getShortestPathByType(CellType.Block, ""); //!!!DETAIL MISSING
                    if(path.size()>1){
                        IntegerPair next_location = path.get(1);
                        return moveTo(next_location);
                    }
                }
            }
        }

        //if there is no block of my interest in my map -> go for a dispenser of the type of my interest

        //but first, check dispenser next to me...
        IntegerPair dispenserLoc = dispenserOnMySide();
        if(dispenserLoc != null){
            String direction = coordinatesToDirection(dispenserLoc);
            //!!!!!!!!!!HANDLE HOW TO REQUEST IF ALREADY HAVE BLOCKS ATTACHED!!!!!!!!!
            return new Action("request", new Identifier(direction));
        }

        //if no dispenser is next to me -> look and go for 1....

        else{

            List<IntegerPair> dispensers = this.mapHandler.getDispensersLocationslist(); //!!!!MISSING DISPENSER TYPE!!!
            if(dispensers.size() > 0 ){
                List<IntegerPair> path = getShortestPathByType(CellType.Dispenser, "");
                if(path.size()>1){
                    IntegerPair next_location = path.get(1);
                    return moveTo(next_location);
                }
            }
        }
        */