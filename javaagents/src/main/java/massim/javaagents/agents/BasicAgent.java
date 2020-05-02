package massim.javaagents.agents;

import eis.eis2java.annotation.AsPercept;
import eis.iilang.*;
import massim.javaagents.*;

import java.util.*;
import java.util.stream.Collectors;

import massim.javaagents.utils.*;

/**
 * A not very basic agent.
 */

//java -jar target\javaagents-2019-1.0-jar-with-dependencies.jar

//TODO
//WADIE -: FIXING MOVING WITH BLOCKS ... SHARINGMAP
//BRUNO -: CHECK TIME....FOR DROPPING OR NOT TASKS.... IF REQUIREMENTS ARE SAME TYPE... B2 AND B2....SIMPLIFY... DONT NEED THE TASK ... LOOK ONLY FOR BLOCK, AND THE X,Y OF REQ.. CASE HELPING.
//ADAM -: MAKE JAVA CODE OF CONNECTING... 


public class BasicAgent extends Agent {
    private enum State{Exploring, MovingToDispenser, NearDispenser, NearBlock, MovingToGoal, AtGoal, InPosition, Helping, LookingForYou, GoingToYou}

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

    private Set<String> announcedTo;
    private Map<String, Map<String, Integer>> bids = new HashMap<>();

    //CONTROL THE HELPING PART
    private boolean helping = false;

    private List<String> reqsAnn = new LinkedList<>(); //ignore it but don't delete it

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
        announcedTo = new HashSet<>();
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

            List<Parameter> pars = message.getClonedParameters();
            String[] reqs = new String[pars.size()-1];

            String taskReceived = ((Identifier) pars.get(0)).getValue();
            Task t = perceptionHandler.getTasks().stream().filter(p -> p.getName().equals(taskReceived)).findAny().get();

            if(state.equals(State.Helping)){
                //Already Helping, sorry
            }
            else if(activeTask.getDeadLine() < t.getDeadLine()){
                //Sorry, my task is almost done, I need to finish it.
                say("Better time!");
            }
            else{
                say("Your : " + t.getDeadLine() + " is better than mine: " + activeTask.getDeadLine() + ", so I got U :)");
                //say("Ok, got u!");
                //Ok, I am not helping anyone....or....I your task is going to be finished sooner, let me help....or... I'm not doing anything
                for (int i = 0; i < reqs.length; i++) {
                    reqs[i] = ((Identifier) pars.get(i+1)).getValue();
                }

                String req = "";
                int min = 1000;

                for(int i=0; i<reqs.length; i++){
                    Map<IntegerPair, String> dispensers = this.mapHandler.getDispensersByType(reqs[i]);
                    if(!dispensers.isEmpty()){
                        List<IntegerPair> p = lookForDispenserV2(reqs[i]); //!!STRANGE!!!
                        if(p.size() < min){
                            req = reqs[i];
                            min = p.size();
                        }
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
            //say("From " + sender + ": " + message.getParameters().toString());
            List<Parameter> pars = message.getClonedParameters();

            String bidKey = ((Identifier) pars.get(0)).getValue();
            int bidValue = ((Numeral) pars.get(1)).getValue().intValue();

            Map<String, Integer> bid = new HashMap<>();
            bid.put(bidKey, bidValue);
            bids.put(sender, bid);

            Map<String, List<String>> award = new HashMap<>();

            if(bids.size() == announcedTo.size()){
                say("Announced reqs: " + reqsAnn.toString());
                say("Announced to: " + announcedTo.size());
                say("Bids: " + bids.toString());
                Set<String> reqsType = new HashSet<>();
                for (var k : bids.keySet()){
                    if(!bids.get(k).keySet().toArray()[0].toString().equals(""))
                        reqsType.add(bids.get(k).keySet().toArray()[0].toString());
                }

                //say("REQS:::::::  " + reqsType.toString());

                Map<String, List<String>> sameBids = new HashMap<>(); //store the agents that bidded to same requirement and compare them
                for(var reqq : reqsType){
                    List<String> agentsSame = bids.entrySet().stream().filter(a -> a.getValue().keySet().toArray()[0].toString().equals(reqq)).map(Map.Entry::getKey).collect(Collectors.toList());
                    sameBids.put(reqq,agentsSame);
                }

                for(String requirem : reqsAnn){
                    int min = 1000;
                    String awardedAgent = "";
                    say("SAME BIDS::: " + sameBids.toString());
                    if(sameBids.get(requirem) != null){
                        for(String agent : sameBids.get(requirem)){
                            if(bids.get(agent).get(requirem) < min){
                                min = bids.get(agent).get(requirem);
                                awardedAgent = agent;
                            }
                        }
                        if(award.containsKey(requirem)){
                            List<String> as = award.get(requirem);
                            as.add(awardedAgent);
                            award.put(requirem, as);//update it
                        }
                        else{
                            award.put(requirem, new ArrayList<String>(List.of(awardedAgent)));
                        }
                        sameBids.get(requirem).remove(awardedAgent);
                        bids.remove(awardedAgent);
                    }
                }

            }

            if(award.size()>0){
                say("Awards: " + award.toString());

                //award = < "b1" : ["A3"] ,
                //          "b2" : ["A1","A2"],
                //          "b3" : [] >

                for(String awardedReq : award.keySet()){
                    //reqToSend = [Block(b1,1,2)]
                    //reqToSend = [Block(b2,-1,0) , Block(b2,0,2)]
                    List<Block> reqToSend = requirements.keySet().stream().filter(req -> req.getType().equals(awardedReq) && !requirements.get(req)).collect(Collectors.toList());
                    for(var req : reqToSend){
                        if(award.get(awardedReq) != null && award.get(awardedReq).size()>0 && !award.get(awardedReq).get(0).equals("")){
                            requirements.replace(req, true);
                            sendMessage(perceptionHandler.makePercept("Award", awardedReq, req.getX(), req.getY()), award.get(awardedReq).remove(0), getName());
                        }
                    }
                }
                announcedTo.clear();
                bids.clear();
                reqsAnn.clear();
                //award.clear();
            }
        }

        //Award
        else if (message.getName().equals("Award")) {
            List<Parameter> pars = message.getClonedParameters();
            String awardedRequirement = ((Identifier) pars.get(0)).getValue(); //the requirement I was awarded
            Integer reqDetailX = ((Numeral) pars.get(1)).getValue().intValue(); //the X detail of the requirement
            Integer reqDetailY = ((Numeral) pars.get(2)).getValue().intValue(); //the Y detail of the requirement

            say("I GOT REQ: " + awardedRequirement + " (" + reqDetailX + "," + reqDetailY + ").");

            requirement = new Block(reqDetailX, reqDetailY, awardedRequirement);
            state = State.Helping;
            helping = true;
        }

    }

    @Override
    public Action step() {
        //if(getName().equals("agentA1"))say(Whiteboard.getAllAssigned().toString());
        //say("teammates: " + teamMatesTrans.toString());
        //PHASE 1 (Sense) - Agent gets perceptions from Environment
        List<Percept> percepts = getPercepts();
        this.perceptionHandler = new PerceptionHandler(percepts);

        //PHASE 2 (Internal State) - Agent updates its internal state
        this.mapHandler.updateMap(perceptionHandler);//needs to check if lastAction was successful before updating...

        //If Agent sees a teammate -> share relative positions!
        if (this.perceptionHandler.getTeammates().size() > 0) {
            say("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!MET!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
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
                return trySubmition();
            case Helping:
                return doHelp();
            case LookingForYou:
                return lookForYou();
            case GoingToYou:
                return goToYou();
            case InPosition:
                createPattern();
                //remember to set helping = false...
        }

        return new Action("skip");
    }

    //##################################### HELPER FUNCTIONS #####################################################

    private Action lookForYou(){
        return new Action("skip");
    }

    private Action goToYou(){
        return null;
    }

    private Action doHelp(){

        List<Thing> attacheds = perceptionHandler.getAttached();

        if(attacheds.size() > 0){
            String d = coordinatesToDirection(new IntegerPair(attacheds.get(0).getX(), attacheds.get(0).getY()));
            return new Action("detach", new Identifier(d));
        }

        //if(hasBlockAttached) -> drop it first
        String detail = requirement.getType();
        //Map<IntegerPair, String> dispensers = this.mapHandler.getDispensersByType(detail);
        //check Null case in dispensers.. but unlikely... because agent is helping because he can...

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

    private Action trySubmition(){
        requirements.replace(requirement, true);

        //if you are an agent who needs help and there is still a requirement not done
        if(activeTask.getRequirements().containsValue(false)){

            //!!!!!!!!!!!!!!!!!CHECK IF ALREADY ANNOUNCED TO AVOID KEEP ANNOUNCING WHILE WAITING.... JUST CHECK SIZE OF ANNOUNCEDTO!!!!!!!!!!!!!!

            if(teamMatesTrans.size() > 0){
                List<String> taskSpecs = new LinkedList<>();
                taskSpecs.add(activeTask.getName());
                //int i = 1;
                for(Map.Entry<Block,Boolean> req : activeTask.getRequirements().entrySet()){
                    if(!req.getValue()){
                        taskSpecs.add(req.getKey().getType());
                        reqsAnn.add(req.getKey().getType()); //ignore this but don't delete
                    }
                }
                String [] taskSpecsToSend = new String[taskSpecs.size()];
                taskSpecs.toArray(taskSpecsToSend);

                for(var teamMate : teamMatesTrans.keySet()){
                    //send a message of format ["task1", "b2", "b3"]
                    announcedTo.add(teamMate);
                    sendMessage(perceptionHandler.makePercept("Announcement", taskSpecsToSend), teamMate, getName());
                }

                /*for(int i=0; i<4; i++){
                    announcedTo.add("agentA"+i);
                }
                broadcast(perceptionHandler.makePercept("Announcement", taskSpecsToSend), getName());*/
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
                    List<IntegerPair> p = lookForDispenserV2(detail);//!!STRANGE!!!
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
            //TODO check this part
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
        if(!helping)
            state = State.MovingToGoal;
        else
            state = State.LookingForYou;
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

    private Action createPattern(){
        //TODO Connect shit (ADAM)
        //Preconds: 2 blocks/task, help is here, the agent needing help has the first requirement under him, helper on his right side with other block under him.
        /*
            3 cases for 2 blocks: {(0,1),(1,1)}, {(-1,1),(0,1)}, {(0,1),(0,2)} (assume that the order of the requrements are fixed in the percept (e.g. (0,1) always comes before (1,1))
            Algorithm:
                the agent seeking help always goes for the first requirement in the tasks percept
                in the goal (or before) rotates the block to (0,1)
                the helper always moves to the right side of the agent with the othe block under him
                case 1:
                    The blocks are in the correct position relative to the main agent
                    connect the blocks
                    helper detaches
                    main agent submits
                case 2:
                    The blocks are in the correct position relative to the helper
                    connect blocks
                    main agent detaches
                    helper submits
                case 3:
                    helper moves down twice then rotates clockwise (What does the main agent do in the meantime?)
                    The block are in the correct position relative to the main agent
                    connect blocks
                    helper detaches
                    main agent submits
        */
        int patternCase = 0;

        return new Action("skip");
    }

    private List<IntegerPair> lookForDispenserV2(String detail){
        Map<IntegerPair, String> dispensers = this.mapHandler.getDispensersByType(detail);
        if(dispensers != null && dispensers.size() > 0){

            List<IntegerPair> path = getShortestPathByType(CellType.Dispenser, detail);
            if(path != null && path.size()>0){
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
