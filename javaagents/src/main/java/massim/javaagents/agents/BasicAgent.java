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
    private Task activeTask;
    private Map.Entry<Block, Boolean> activeRequirement;
    private Action action;
    private boolean requested;
    private List<IntegerPair> activePath;
    private int step;
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
        this.activeTask = null;
        this.activeRequirement = null;
        this.action = null;
        this.requested = false;
        this.activePath = new LinkedList<>();
        this.step = 0;
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
        this.mapHandler.updateMap(perceptionHandler);//needs to check if lastAction was successful before updating...
        String mapPath = "maps\\" + step + ".txt";
        mapHandler.printMapToFile(mapPath);

        //PHASE 3 (Deliberate) - Agent tries to figure out what is the best action to perform given his current state and his previous action
        step++;
        return workThoseNeurons();

        /*//PHASE 4 (ACT) - Execute chosen action to achieve goal
        return action;*/
    }


    public Action workThoseNeurons(){
        List<Task> tasks = this.perceptionHandler.getTasks();
        action = null;

        //if there is a task available and the agent was not doing any other task -> let's do this task then
        if(tasks.size() > 0 && activeTask == null){
            activeTask = tasks.get(0);
            activeRequirement = activeTask.getRequirements().entrySet().iterator().next();
        }

        //if the agent is doing a task and the current requirement is done -> check if there is any other requirement and do it! If not, the task is done!!!
        if(activeTask != null && activeRequirement.getValue() == true){
            activeRequirement = null;
            for (Map.Entry<Block,Boolean> requirement : activeTask.getRequirements().entrySet())
                if(requirement.getValue() == false){
                    activeRequirement = requirement;
                }

            if(activeRequirement == null){
                action = lookForGoal(); //action can be "move" if going for goal | "submit" if already in goal cell | "null" if doesn't know where is goal
            }
        }
        
        if(activePath.size() > 0 && !perceptionHandler.getFailed()){
            action = moveTo(activePath.remove(0));
        }
        
        //if we are doing a requirement and it's not done -> the aent should fulfill it
        if((activePath.size() == 0 || perceptionHandler.getFailed()) && activeRequirement != null && activeRequirement.getValue() == false){
            String blockType = activeRequirement.getKey().getType();
            activePath = lookForDispenserV2(blockType); //action can be "move" if going for a dispenser | "request" if already near a dispenser | "attach" if we have requested a block from the dispenser || "null" if we haven't seen any dispenser yet
            activePath.remove(0);
            action = moveTo(activePath.remove(0));
        }
        
        //if none of the previous conditions are met, it means no action has been chosen -> so explore
        if(action == null){
            action = explore();
        }

        if(action != null){
            return action;
        }

        //this means there is nothing else to do
        return new Action("skip");
    }



    //##################################### FUNCTIONS #####################################################
    
    
    private List<IntegerPair> lookForDispenserV2(String detail){
        Map<IntegerPair, String> dispensers = this.mapHandler.getDispensersByType(detail);
        if(dispensers.size() > 0){
            
            List<IntegerPair> path = getShortestPathByType(CellType.Dispenser, detail);
            if(path.size()>0){
                return path;
            }
        }
        return null;
    }

    public Action lookForDispenser(String detail){
        Map<IntegerPair, String> dispensers = this.mapHandler.getDispensersByType(detail);
        if(dispensers.size() > 0 ){
            
            List<IntegerPair> path = getShortestPathByType(CellType.Dispenser, detail);
            if(path.size()>2){
                IntegerPair next_location = path.get(1);
                return moveTo(next_location);
            }
            else if(path.size() == 2){
                String direction = dispenserOnMySide(detail);
                if(requested==false){
                    requested = true;
                    return new Action("request", new Identifier(direction));
                }
                else{
                    requested = false;
                    return new Action("attach", new Identifier(direction));
                }
            }
        }
        return explore();
    }

    /*public Action lookForBlock(String detail){
        List<IntegerPair> blocks = this.mapHandler.getBlocksLocationslist();
        if(blocks.size() > 0 ){
            List<IntegerPair> path = getShortestPathByType(CellType.Block, detail);
            if(path.size() > 2){
                return moveTo(path.get(1));
            }
            else if(path.size() == 2){
                blockOnMySide(detail)
            }
        }
        return null;
    }*/

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
        else if(path.size() == 1){
            return new Action("submit", new Identifier(this.activeTask.getName()));
        }

        return explore();
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
        return explore();
    }

    public boolean hasDoneTask(Map<Block, Boolean> requirementsTocheck){
        for (Map.Entry<Block,Boolean> requirement : requirementsTocheck.entrySet())
            if(requirement.getValue() == false){
                return false;
            }
        return true;
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

    public String dispenserOnMySide(String detail){
        int [] directionInX = {-1,1,0,0};
        int [] directionInY = {0,0,1,-1};

        //for each direction -> check
        for(int i=0; i<4; i++){
            int x = directionInX[i];
            int y = directionInY[i];
            if(this.mapHandler.getMap()[this.mapHandler.getAgentLocation().getX() + x][this.mapHandler.getAgentLocation().getY() + y].getType().equals(CellType.Dispenser)){ //!!!MISSING CONDITION TYPE OF BLOCK (DETAILS)
                String direction = coordinatesToDirection(new IntegerPair(x,y));
                return direction;
                //!!!!!!!!!!HANDLE HOW TO REQUEST IF ALREADY HAVE BLOCKS ATTACHED!!!!!!!!!
                //return new Action("request", new Identifier(direction));
            }
        }
        return "";
    }

    public List<IntegerPair> getShortestPathByType(CellType ct, String detail) {
        //boolean failed = this.perceptionHandler.getFailed();
        BFS2 bfs = new BFS2(this.mapHandler);
        List<IntegerPair> path = bfs.BFS(ct, detail);
        return path;
    }

    public List<IntegerPair> getShortestPathByLocation(IntegerPair goal) {
        BFSsearch bfs = new BFSsearch(this.mapHandler, this.length, this.width);
        List<IntegerPair> path = bfs.bfsByLocation(goal);
        return path;
    }

    public Action explore(){
        /*boolean failed = this.perceptionHandler.getFailed();
        BFSsearch bfs = new BFSsearch(this.mapHandler, this.length, this.width, failed);
        List<IntegerPair> path = bfs.bfsByType(CellType.Unknown,"");
        System.out.println("Agent Location: " + "("+mapHandler.getAgentLocation().getX()+","+mapHandler.getAgentLocation().getY()+")");
        System.out.println("Path: ");
        for(var p: path){
            System.out.print("("+p.getX()+","+p.getY()+"), ");
        }

        if(path.size()>1){
            return moveTo(path.get(1));
        }*/
        IntegerPair agentLocation = this.mapHandler.getAgentLocation();
        int [] directionInX = {-1,1,0,0};
        int [] directionInY = {0,0,1,-1};
        
        int [] unknownInX = {-6,6,0,0};
        int [] unknownInY = {0,0,6,-6};
        for(int i=0; i<4; i++){
            int newPosX = agentLocation.getX() + directionInX[i];
            int newPosY = agentLocation.getY() + directionInY[i];
            IntegerPair newCellPos =  new IntegerPair(newPosX, newPosY);
            int unknownX = agentLocation.getX() + unknownInX[i];
            int unknownY = agentLocation.getY() + unknownInY[i];
            IntegerPair unknownPos =  new IntegerPair(unknownX, unknownY);
            if(!mapHandler.getMap()[newCellPos.getX()][newCellPos.getY()].getType().equals(CellType.Empty)){
                continue;
            }
            if(!mapHandler.getMap()[unknownPos.getX()][unknownPos.getY()].getType().equals(CellType.Unknown)){
                continue;
            }
            return moveTo(newCellPos);
        }
        return moveRandom();
        
    }
    
    private Action moveRandom(){
        Random rand = new Random();
        int r = rand.nextInt(100);
        if(r < 25) return new Action("move", new Identifier("n"));
        if(r < 50) return new Action("move", new Identifier("s"));
        if(r < 75) return new Action("move", new Identifier("e"));
        return new Action("move", new Identifier("w"));
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

/*
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

*/