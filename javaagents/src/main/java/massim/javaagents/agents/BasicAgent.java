package massim.javaagents.agents;

import eis.eis2java.annotation.AsPercept;
import eis.iilang.*;
import massim.javaagents.*;

import java.util.*;

import massim.javaagents.utils.*;

/**
 * A not very basic agent.
 */

//java -jar target\javaagents-2019-1.0-jar-with-dependencies.jar

public class BasicAgent extends Agent {
    private enum State{Exploring, MovingToDispenser, NearDispenser, NearBlock, MovingToGoal, AtGoal}

    private MapHandler mapHandler;
    private PerceptionHandler perceptionHandler;
    private int agent_x, agent_y;//for testing
    private IntegerPair agentMovement;
    private int length, width;
    private Task activeTask;
    private Map.Entry<Block, Boolean> activeRequirement;
    private Action action;
    private boolean requested;
    private List<IntegerPair> activePath;
    private int step;
    private Block requirement;
    private Map<Block, Boolean> requirements;
    private State state;
    private Map<String, Integer> myTaskWeights;
    private Map<String, IntegerPair> teamMatesTrans;
    private boolean alreadySkipped;

    Map<String, int[]> weightsOfOthers;
    private String taskAssigned;

    private List<String> announcedTo = new LinkedList<>();
    private Map<String, Map<String, Integer>> bids = new HashMap<>();

    //CONTROL THE HELPING PART
    private boolean askedForHelp = false;
    private boolean helping = false;


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
        this.requirement = null;
        this.state = State.Exploring;
        this.weightsOfOthers = new HashMap<>();
        myTaskWeights = new HashMap<>();
        this.taskAssigned = "";
        this.teamMatesTrans = new HashMap<>();
        alreadySkipped = false;
    }


    @Override
    public void handlePercept(Percept percept) {}

    @Override
    public void handleMessage(Percept message, String sender) {
        //say("weight: " + ((Numeral)message.getParameters().get(0)).getValue().toString());
        if (message.getName().equals("Weights")) {
            List<Parameter> pars = message.getClonedParameters();
            //int[] weights = new int[this.perceptionHandler.getTasks().size()];
            int[] weights = new int[pars.size()];
            for (int i = 0; i < weights.length; ++i) {
                //List<Parameter> pars = message.getClonedParameters();
                weights[i] = ((Numeral) pars.get(i)).getValue().intValue();
            }
            say("Received " + Arrays.toString(weights) + " from " + sender);
            weightsOfOthers.put(sender, weights);
        }

        //task

        //name
        else if (message.getName().equals("Locations")) {
            List<Parameter> pars = message.getClonedParameters();
            //WADIE MAKE THIS
            IntegerPair teammateLocation = new IntegerPair(((Numeral) pars.get(2)).getValue().intValue(),
                    ((Numeral) pars.get(3)).getValue().intValue());
            IntegerPair possibleRelativeLocation = new IntegerPair(-1 * ((Numeral) pars.get(0)).getValue().intValue(),
                    -1 * ((Numeral) pars.get(1)).getValue().intValue());
            if(perceptionHandler != null){
                for(var teammate: this.perceptionHandler.getTeammates()){
                    IntegerPair teammateRelativeLocation = new IntegerPair(teammate.getX(), teammate.getY());
                    if (possibleRelativeLocation.equals(teammateRelativeLocation)){
                        IntegerPair transform = this.mapHandler.getTeammateTransfer(teammateRelativeLocation,
                                teammateLocation, sender);
                        if(!teamMatesTrans.containsKey(sender)) this.teamMatesTrans.put(sender, transform);
                        break;
                    }
                }
            }
        }

        else if(message.getName().equals("Announcement")){

            //I'm busy... leave me alone
            if(activeTask != null){
                sendMessage(perceptionHandler.makePercept("Bid", "", -1), sender, getName());
            }
            //Ok, let me check
            else{
                List<Parameter> pars = message.getClonedParameters();
                String[] reqs = new String[pars.size()];
                String taskReceived = ((Identifier) pars.get(0)).getValue();
                //say("Received task:" + taskReceived + " from " + sender);
                for (int i = 1; i < reqs.length - 1; i++) {
                    reqs[i] = ((Identifier) pars.get(i)).getValue();
                }

                int ind = perceptionHandler.getTasks().indexOf(taskReceived);
                Task t = perceptionHandler.getTasks().get(ind);

                String req = "";
                int min = 1000;

                for(int i=1; i<reqs.length - 1; i++){
                    if(t.getRequirements().get(reqs[i]) == false){
                        Map<IntegerPair, String> dispensers = this.mapHandler.getDispensersByType(reqs[i]);
                        if(!dispensers.isEmpty()){
                            List<IntegerPair> p = lookForDispenserV2(reqs[i]);
                            if(p.size() < min){
                                req = reqs[i];
                                min = p.size();
                            }
                        }
                    }
                    else{
                        continue;
                    }
                }

                if(!req.equals("")){
                    sendMessage(perceptionHandler.makePercept("Bid", req, min), sender, getName());
                }
                else{
                    sendMessage(perceptionHandler.makePercept("Bid", "", -1), sender, getName());
                }
            }

        }

        else if (message.getName().equals("Bid")) {
            List<Parameter> pars = message.getClonedParameters();

            String bidKey = ((Identifier) pars.get(0)).getValue();
            int bidValue = ((Numeral) pars.get(1)).getValue().intValue();

            Map<String, Integer> bid = new HashMap<>();
            bid.put(bidKey, bidValue);
            bids.put(sender, bid);

            Map<String, String> award = new HashMap<>();

            if(bids.size() == announcedTo.size()){
                for(String agent : bids.keySet()){
                    Map.Entry<String,Integer> entry = bids.get(agent).entrySet().iterator().next();
                    if(entry.getKey() == "" || award.containsValue(agent)){
                        continue;
                    }
                    for(String otherAgent : bids.keySet()){
                        if(agent != otherAgent){
                            Map.Entry<String,Integer> otherEntry = bids.get(otherAgent).entrySet().iterator().next();
                            if(otherEntry.getKey() == "" || entry.getKey() != otherEntry.getKey()){
                                if(!award.containsValue(agent))
                                    award.put(entry.getKey(), agent);
                            }
                            else if(entry.getValue() <= otherEntry.getValue()){
                                if(!award.containsValue(agent))
                                    award.put(entry.getKey(), agent);
                            }
                            else{
                                if(!award.containsValue(otherAgent))
                                    award.put(entry.getKey(), otherAgent);
                            }
                        }

                    }
                }
            }


            if(award.size()>0){
                for(String awardedReq : award.keySet()){
                    for(var r : requirements.keySet()){
                        if(r.getType() == awardedReq){
                            requirements.replace(r,true);
                        }
                    }
                    sendMessage(perceptionHandler.makePercept("Award", activeTask.getName(), awardedReq), award.get(awardedReq), getName());
                }
            }
            //maybe sent "" to the ones that didn't receive anything????

        }

        //Award
        else if (message.getName().equals("Award")) {
            List<Parameter> pars = message.getClonedParameters();
            String taskReceived = ((Identifier) pars.get(0)).getValue(); //the task I was awarded
            String awardedRequirement = ((Identifier) pars.get(1)).getValue(); //the requirement I was awarded

            int ind = perceptionHandler.getTasks().indexOf(taskReceived);
            activeTask = perceptionHandler.getTasks().get(ind);

            for(var req : activeTask.getRequirements().keySet()){
                if(req.getType() == awardedRequirement){
                    requirement = req;
                    break;
                }

            }

            helping = true;
        }

    }

    @Override
    public Action step() {
        //if(getName().equals("agentA1"))say(Whiteboard.getAllAssigned().toString());
        say("teammates: " + teamMatesTrans.toString());
        //PHASE 1 (Sense) - Agent gets perceptions from Environment
        List<Percept> percepts = getPercepts();
        this.perceptionHandler = new PerceptionHandler(percepts);

        //PHASE 2 (Internal State) - Agent updates its internal state
        this.mapHandler.updateMap(perceptionHandler);//needs to check if lastAction was successful before updating...
        
        //If Agent sees a teammate -> share relative positions!
        if (this.perceptionHandler.getTeammates().size() > 0) {
            Map<String, IntegerPair> myInfo = new HashMap<>();
            for (var teamMate : this.perceptionHandler.getTeammates()) {
                broadcast(perceptionHandler.makePercept("Locations",
                        teamMate.getX(), teamMate.getY(), this.mapHandler.getAgentLocation().getX(),
                        this.perceptionHandler.getAgentMovement().getY()),
                        getName());
            }
            if(!alreadySkipped){
                alreadySkipped = true;
                return new Action("skip");
            }
            alreadySkipped = false;
        }

        //PHASE 3 (Deliberate) - Agent tries to figure out what is the best action to perform given his current state and his previous action
        step++;
        Action act = chooseAction();

        //PHASE 4 (ACT) - Execute chosen action to achieve goal
        return  act;
    }


    private Action chooseAction() {

        //CHECK WHERE TO FIT SELECTION PROCESS.... WHEN TO RESTART THE WEIGHTS.. PROBABLY WHEN HE IS DONE..
        //ALSO CHECK WHEN TASKS ARE HANDLED... HOW TO MARK THEM SO THAT AGENTS THAT DIDN'T RECEIVE ANYTHING DON'T INTERFEER....


        switch(state){
            case Exploring:
                return doExplore();
            case MovingToDispenser:
                return moveToDispenser(requirement.getType());
            case NearDispenser:
                return requestBlock(requirement.getType());
            case NearBlock:
                return attachBlock(requirement.getType());
            case MovingToGoal:
                return moveToGoal();
            case AtGoal:
                requirements.replace(requirement, true);

                //if you are an agent who needs help and there is still a requirement not done
                if(activeTask.getRequirements().containsValue(false)){
                    if(teamMatesTrans.size() > 0){
                        for(var teamMate : teamMatesTrans.keySet()){
                            //Block requirement = requirements.keySet().stream().findFirst().get();
                            //TODO make a better percept for the requirement


                            List<String> taskSpecs = new LinkedList<>();

                            taskSpecs.add(activeTask.getName());
                            //int i = 1;
                            for(Map.Entry<Block,Boolean> req : activeTask.getRequirements().entrySet()){
                                if(!req.getValue()){
                                    taskSpecs.add(req.getKey().getType());
                                }
                            }

                            String [] taskSpecsToSend = new String[taskSpecs.size()];
                            taskSpecs.toArray(taskSpecsToSend);

                            //send a message of format ["task1", "b2", "b3"]
                            announcedTo.add(teamMate);
                            sendMessage(perceptionHandler.makePercept("Announcement", taskSpecsToSend), teamMate, getName());
                        }
                    }
                }

                else if(activeTask.getRequirements().containsValue(false) == false){
                    //check if help has arrived
                    /*if(helpArrived){
                        //create pattern and connect
                    }
                    else{
                        //wait
                    }*/
                }

                return new Action("skip");
        }

        return new Action("skip");
    }

    //##################################### HELPER FUNCTIONS #####################################################

    private Map<String, Integer> weightTasks(){
        //check if I have seen all the types of blocks/dispensers specified in the requirements
        List<Task> tasks = this.perceptionHandler.getTasks();
        Map<String, Integer> taskWeights = new HashMap<>();
        //int [] taskWeights = new int[tasks.size()];
        for(var task : tasks){
            taskWeights.put(task.getName(), -1);
        }

        for(Task task : tasks){
            for (Map.Entry<Block,Boolean> requirement : task.getRequirements().entrySet()){
                String detail = requirement.getKey().getType();
                Map<IntegerPair, String> dispensers = this.mapHandler.getDispensersByType(detail);
                if(!dispensers.isEmpty()){
                    List<IntegerPair> p = lookForDispenserV2(detail);
                    taskWeights.replace(task.getName(), taskWeights.get(task.getName()) + p.size());
                }
            }
        }
        return taskWeights;
    }


    private Task chooseAvailableTask(){
        myTaskWeights = weightTasks();
        for(var task : myTaskWeights.keySet()){
            Whiteboard.putWeigth(getName(), task, myTaskWeights.get(task));
        }

        //say("My weights: " + myTaskWeights.toString());

        //if(taskAssigned.equals(""))
        taskAssigned = Whiteboard.assignTask(getName());

        //say("All assigned Tasks: " + Whiteboard.getAllAssigned().toString());

        for(var task : perceptionHandler.getTasks()){
            if(taskAssigned.equals(task.getName()))
                return task;
        }

        return null;
    }

    private Action doExplore(){
        List<Task> tasks = this.perceptionHandler.getTasks();
        if(!tasks.isEmpty()){
            if(activeTask == null){
                activeTask = chooseAvailableTask();
                if(activeTask == null){
                    return explore();
                }
            }

            //requirement = activeTask.getRequirement();
            requirements = activeTask.getRequirements();
            for(Map.Entry<Block,Boolean> req : requirements.entrySet()){
                String detail = req.getKey().getType();
                Map<IntegerPair, String> dispensers = this.mapHandler.getDispensersByType(detail);
                if(!dispensers.isEmpty()){
                    state = State.MovingToDispenser;
                    requirement = req.getKey();
                    return moveToDispenser(detail);
                }
            }

            //String detail = requirement.getType();
            //MAYBE REMOVE THIS!!!!
            //Map<IntegerPair, String> dispensers = this.mapHandler.getDispensersByType(detail);
            //if(!dispensers.isEmpty()) return moveToDispenser(detail);
            //SO THE STATE HAS CHANGED BUT IF DISPENSERS IS EMPTY??? THE STATE NEEDS TO GO BACK TO NORMAL
        }
        return explore();
    }

    private Action moveToDispenser(String detail){
        if(activePath.isEmpty() || perceptionHandler.getFailed()){
            activePath = lookForDispenserV2(detail); //action can be "move" if going for a dispenser | "request" if already near a dispenser | "attach" if we have requested a block from the dispenser || "null" if we haven't seen any dispenser yet
            activePath.remove(0);
        }
        if(!dispenserOnMySide(detail).equals("")){
            state = State.NearDispenser;
            activePath = new LinkedList<>();
            return requestBlock(detail);
        }
        return moveTo(activePath.remove(0));
    }

    private Action requestBlock(String detail){
        String direction = dispenserOnMySide(detail);
        String blockDir = blockOnMySide(detail);

        if(!blockDir.equals("")){
            state = State.NearBlock;
            return attachBlock(detail);
        }

        if(direction.equals("")){
            state = State.Exploring;
            activeTask = null;
            requirement = null;
            return doExplore();
        }
        return new Action("request", new Identifier(direction));
    }

    private Action attachBlock(String detail){
        String blockDir = blockOnMySide(detail);
        if(blockDir.equals("")){
            state = State.NearDispenser;
            return requestBlock(detail);
        }
        state = State.MovingToGoal;
        return new Action("attach", new Identifier(blockDir));
    }

    private Action moveToGoal(){
        if(isGoal()){
            state = State.AtGoal;
            return submit();
        }
        List<IntegerPair> goals = mapHandler.getGoalList();
        if(goals.isEmpty()){
            return explore();
        }
        if(activePath.isEmpty()){
            activePath = getShortestPathByType(CellType.Goal, "");
            activePath.remove(0);
        }
        //in case we our attached block gets stuck
        if(perceptionHandler.getFailed()){
            activePath = new LinkedList<>();
            return moveRandom();
        }
        return moveTo(activePath.remove(0));
    }

    private Action submit(){
        List<Thing> attacheds = perceptionHandler.getAttached();
        if(attacheds.isEmpty()){
            state = State.MovingToDispenser;
            explore();
        }
        Thing attached = attacheds.get(0);
        if(attached.getX() != requirement.getX() || attached.getY() != requirement.getY()){
            return new Action("rotate", new Identifier("cw"));
        }
        state = State.Exploring;
        return new Action("submit", new Identifier(activeTask.getName()));
    }

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

    public String blockOnMySide(String detail){
        int [] directionInX = {-1,1,0,0};
        int [] directionInY = {0,0,1,-1};

        //for each direction -> check
        for(int i=0; i<4; i++){
            int x = directionInX[i];
            int y = directionInY[i];
            IntegerPair pos = new IntegerPair(this.mapHandler.getAgentLocation().getX() + x, this.mapHandler.getAgentLocation().getY() + y);
            if(this.mapHandler.getCell(pos).getType().equals(CellType.Block) && ((DetailedCell)mapHandler.getCell(pos)).getDetails().equals(detail)){
                String direction = coordinatesToDirection(new IntegerPair(x,y));
                return direction;
            }
        }
        return "";
    }

    public String dispenserOnMySide(String detail){
        int [] directionInX = {-1,1,0,0};
        int [] directionInY = {0,0,1,-1};

        //for each direction -> check
        for(int i=0; i<4; i++){
            int x = directionInX[i];
            int y = directionInY[i];
            IntegerPair pos = new IntegerPair(this.mapHandler.getAgentLocation().getX() + x, this.mapHandler.getAgentLocation().getY() + y);
            if(this.mapHandler.getCell(pos).getType().equals(CellType.Dispenser) && ((DetailedCell)mapHandler.getCell(pos)).getDetails().equals(detail)){
                String direction = coordinatesToDirection(new IntegerPair(x,y));
                return direction;
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
