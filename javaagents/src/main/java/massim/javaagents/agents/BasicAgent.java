package massim.javaagents.agents;

import eis.iilang.*;
import massim.javaagents.*;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Random;
import java.util.LinkedList;
import java.util.Queue;

/**
 * A very basic agent.
 */
public class BasicAgent extends Agent {

    private MapHandler mapHandler;
    private int agent_x, agent_y;
    private IntegerPair agentMovement;
    private boolean hasBlockAttached;
    private boolean hasDoneTask;
    private int length, width;
    /**
     * Constructor.
     * @param name    the agent's name
     * @param mailbox the mail facility
     */
    public BasicAgent(String name, MailService mailbox) {
        super(name, mailbox);
        this.agent_x = 24;
        this.agent_y = 24;
        this.length = 50;
        this.width = 50;
        this.agentMovement = new IntegerPair(0,0);
        this.hasBlockAttached = false;
        this.hasDoneTask = false;
        this.mapHandler = new MapHandler();
        this.mapHandler.initiateMap(this.length,this.width, new IntegerPair(this.agent_x,this.agent_y));
    }

    @Override
    public void handlePercept(Percept percept) {}

    @Override
    public void handleMessage(Percept message, String sender) {}

    @Override
    public Action step() {

        //PHASE 1 (Sense) - Agent gets perceptions from Environment
        List<Percept> percepts = getPercepts();
        PerceptionHandler perceptionHandler = new PerceptionHandler(percepts);

        //PHASE 2 (Internal State) - Agent updates its internal state
        this.mapHandler.updateMap(perceptionHandler, this.agentMovement);//needs to check if lastAction was successful before updating...
        System.out.println("Agent Location: (" + this.mapHandler.getAgentLocation().getX() + "," + this.mapHandler.getAgentLocation().getY() + ")");

        //PHASE 3 (Deliberate) - Agent tries to figure out what is the best action to perform given his current state and his previous action
        Action action = workThoseNeurons2(this.mapHandler, perceptionHandler);

        //PHASE 4 (ACT) - Execute chosen action to achieve goal
        return action;
    }





    public boolean isGoal(MapHandler mh){
        if(mh.getMap()[mh.getAgentLocation().getX()][mh.getAgentLocation().getY()] == new OrdinaryCell(CellType.Goal)){
            return true;
        }
        return false;
    }

    public Action moveTo(IntegerPair next){
        //next = new IntegerPair(agent_x + next.getX(), agent_y = next.getY())
        /*IntegerPair diff = new IntegerPair(agent_x,agent_y).diff(next);
        if(diff.getX()<0){
            //agent_x -= 1;
            return new Action("move", new Identifier("w"));
        }
        if(diff.getX()>0){
            //agent_x += 1;
            return new Action("move", new Identifier("e"));
        }
        if(diff.getY()<0){
            //agent_y -= 1;
            return new Action("move", new Identifier("n"));
        }
        if(diff.getY()>0){
            //agent_y += 1;
            return new Action("move", new Identifier("s"));
        }*/

        return new Action("skip");
    }

    public Action moveRandom(){
        Random rand = new Random();
        int randn = rand.nextInt(4);
        if(randn==1){
            //!!!check if there is something in the way before moving!!!
            this.agentMovement = new IntegerPair(0,-1);
            return new Action("move", new Identifier("n"));
        }
        if(randn==2){
            //!!!check if there is something in the way before moving!!!
            this.agentMovement = new IntegerPair(0,1);
            return new Action("move", new Identifier("s"));
        }
        if(randn==3){
            //!!!check if there is something in the way before moving!!!
            this.agentMovement = new IntegerPair(-1,0);
            return new Action("move", new Identifier("w"));
        }
        //!!!check if there is something in the way before moving!!!
        this.agentMovement = new IntegerPair(1,0);
        return new Action("move", new Identifier("e"));
    }

    /*##### USING BFS #####*/
    public List<IntegerPair> getShortestPath(CellType ct) {
        BFSsearch bfs = new BFSsearch(this.mapHandler, this.length, this.width, ct);
        List<IntegerPair> path = bfs.solve();
        return path;
    }

    public boolean blockOnMySide(){
        return false;
    }

    public boolean hasBlockAttached(){
        return false;
    }

    public boolean dispenserOnMySide(){
        return false;
    }

    /*############ TO BE GIVEN BY ADAM#######################*/
    public List<Percept> getTasks(){  return null;  }

    //aka use your brain (think) - try to choose best action
    //##### MISSING - handle task deadline and action failed(shouldn't this be handled on the MapHandler???)
    public Action workThoseNeurons2(MapHandler mh, PerceptionHandler ph){

        //if you are in a goal cell and your done with your task... -> Submit!!!
        if(isGoal(mh)){
            //for now hasDoneTask is only a boolean function.... need to check how to get that info
            if(hasDoneTask){ return new Action("submit", new Identifier(getTasks().get(0).getName())); } //task name will come from ADAM's percepts
        }

        //if I understood correctly -> it goes for one requirement of a task A, once it does that... it looks for next requirement.. until there is no more requirements
        if(hasBlockAttached()){

        }

        //if there is a block in one of my sides
        if(blockOnMySide()){
            //get the block only if it is the same type as requirements
        }

        //if there is a dispenser in one of my sides
        if(dispenserOnMySide()){
            //request a block from dispenser only if it is the same type as requirements
        }

        //if there is an available task -> get the requirements
        List<Percept> tasks = getTasks();
        if(!tasks.isEmpty()){

            //check the requirements
            List<Parameter> requirements = tasks.get(0).getParameters(); //get from ADAM...
            if(!requirements.isEmpty()){
                //if I understood correctly, if there is a requirement.. we should check the type of the block specified in the req..
                //and then look in our "known" map if there is such block type
                String blockType = "b1"; //#### GET IT FROM ADAM's PERCEPT SO######

                //if there is such block type in the map...go for it
                //there should be something from the map like -> mh.getBlocks() and from that I can select the blockType... right now it only has the locations of the blocks
                List<IntegerPair> blocks = mh.getBlocksLocationslist();
                if(!blocks.isEmpty()){
                    //look for the specific type of block within the blocks in my map
                    //for example -> int found = block.findByType(blockType)
                    boolean found = false;
                    if(found){
                        //if there is, so let's look for the shortest path from my loc to the block
                        //from that path take the first step (index 0)
                        IntegerPair next_movement = getShortestPath(CellType.Block).get(0);
                        return moveTo(next_movement);
                    }
                }
                //if we don't find that block in our map... look for a dispenser
                //!!!!!again there should be something that gives me the types of dispensers!!!!
                List<IntegerPair> dispensers = mh.getDispensersLocationslist();
                if(!dispensers.isEmpty()){
                    //look for the specific type of dispenser within the dispensers in my map
                    //for example -> int found = dispenser.findByType(blockType)
                    boolean found = false;
                    if(found){
                        //if there is, so let's look for the shortest path from my loc to the dispenser
                        //from that path take the first step (index 0)
                        IntegerPair next_movement = getShortestPath(CellType.Dispenser).get(0);
                        return moveTo(next_movement);
                    }
                }

                //if neither block or dispenser type is found... then keep moving randomly....
            }

            else{
                //this is the case where requirements are empty.. meaning you already followed them???
                //in this case set hasDoneTask = true
                hasDoneTask = true;
                //look for shortest path to goal area now.... there should be a mh.getGoalArea() !!
                List<IntegerPair> goalAreas = null;
                //if we already know the goal area -> go for it
                if(!goalAreas.isEmpty()){
                    IntegerPair next_movement = getShortestPath(CellType.Goal).get(0);
                    return moveTo(next_movement);
                }
                //else keep moving randomly
            }
        }

        //No task -> ... just move random.. maybe we can consider skip action(doing nothing)
        return moveRandom();
    }

}
