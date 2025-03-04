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

//TODO:
//BRUNO: CONNECTION
//WADIE: STUCK, MAP SHARIN
//ADAM: MAP SHARING, BFS(LOCATION)


public class BasicAgent extends Agent {
    private enum State {Exploring, MovingToDispenser, NearDispenser, NearBlock, MovingToGoal, AtGoal, InPosition, Helping, BuildPattern, LookingForYou, GoingToYou}


    private MapHandler mapHandler;
    private PerceptionHandler perceptionHandler;
    private int agent_x, agent_y;//for testing
    private IntegerPair agentMovement;
    private int length, width;
    private Task activeTask;
    private Action action;
    private List<IntegerPair> activePath;
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

    private List<Action> activeActions;

    //CONTROL THE HELPING PART
    private boolean helping = false;
    String helpingWho = "";

    private List<String> reqsAnn = new LinkedList<>(); //ignore it but don't delete it
    private List<IntegerPair> occupiedPositions = new LinkedList<>(); //IMPORTANT
    private IntegerPair myBlockPos;
    
    String failedMate="";
    
    private Map<String, IntegerPair> myMates = new HashMap<>();
    /**
     * Constructor.
     *
     * @param name    the agent's name
     * @param mailbox the mail facility
     */
    public BasicAgent(String name, MailService mailbox) {
        super(name, mailbox);
        this.agent_x = 49;
        this.agent_y = 49;
        this.length = 100;
        this.width = 100;
        this.agentMovement = new IntegerPair(0, 0);
        this.mapHandler = new MapHandler();
        this.mapHandler.initiateMap(this.length, this.width, new IntegerPair(this.agent_x, this.agent_y));
        this.activeTask = null;
        this.action = null;
        this.activePath = new LinkedList<>();
        this.requirement = null;
        this.state = State.Exploring;
        this.weightsOfOthers = new HashMap<>();
        myTaskWeights = new HashMap<>();
        this.taskAssigned = "";
        this.teamMatesTrans = new HashMap<>();
        alreadySkipped = false;
        announcedTo = new HashSet<>();
        activeActions = new LinkedList<>();
    }


    @Override
    public void handlePercept(Percept percept) {
    }

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

        //Locations
        else if (message.getName().equals("Locations")) {
            List<Parameter> pars = message.getClonedParameters();
            //WADIE MAKE THIS
            IntegerPair teammateLocation = new IntegerPair(((Numeral) pars.get(2)).getValue().intValue(),
                    ((Numeral) pars.get(3)).getValue().intValue());
            IntegerPair possibleRelativeLocation = new IntegerPair(-1 * ((Numeral) pars.get(0)).getValue().intValue(),
                    -1 * ((Numeral) pars.get(1)).getValue().intValue());
            if (perceptionHandler != null) {
                for (var teammate : this.perceptionHandler.getTeammates()) {
                    IntegerPair teammateRelativeLocation = new IntegerPair(teammate.getX(), teammate.getY());
                    if (possibleRelativeLocation.equals(teammateRelativeLocation)) {
                        IntegerPair transform = this.mapHandler.getTeammateTransfer(teammateRelativeLocation, teammateLocation);
                        if (!teamMatesTrans.containsKey(sender)) {
                            this.teamMatesTrans.put(sender, transform);
                            say(sender + " in my memory.");
                            sendMessage(perceptionHandler.makePercept("ReadyToShare"), sender, getName());
                        }
                        break;
                    }
                }
            }
        } else if (message.getName().equals("ReadyToShare")) {
            shareMap(sender);
        } else if (message.getName().equals("Announcement")) {

            List<Parameter> pars = message.getClonedParameters();
            String[] reqs = new String[pars.size() - 1];

            String taskReceived = ((Identifier) pars.get(0)).getValue();
            Task t = perceptionHandler.getTasks().stream().filter(p -> p.getName().equals(taskReceived)).findAny().get();

            if (state.equals(State.Helping)) {
                //Already Helping, sorry
            } else if (activeTask != null && activeTask.getDeadLine() < t.getDeadLine()) {
                //Sorry, my task is almost done, I need to finish it.
                say("Better time!");
            } else {
                say("Yours is better :)");
                //say("Ok, got u!");
                //Ok, I am not helping anyone....or....I your task is going to be finished sooner, let me help....or... I'm not doing anything
                for (int i = 0; i < reqs.length; i++) {
                    reqs[i] = ((Identifier) pars.get(i + 1)).getValue();
                }

                String req = "";
                int min = 1000;

                for (int i = 0; i < reqs.length; i++) {
                    Map<IntegerPair, String> dispensers = this.mapHandler.getDispensersByType(reqs[i]);
                    if (!dispensers.isEmpty()) {
                        List<IntegerPair> p = lookForDispenserV2(reqs[i]); //!!STRANGE!!!
                        if (p.size() < min) {
                            req = reqs[i];
                            min = p.size();
                        }
                    }
                }

                if (!req.equals("")) {
                    sendMessage(perceptionHandler.makePercept("Bid", req, min), sender, getName());
                } else {
                    sendMessage(perceptionHandler.makePercept("Bid", "", -1), sender, getName());
                }

            }
        } else if (message.getName().equals("Bid")) {
            //say("From " + sender + ": " + message.getParameters().toString());
            List<Parameter> pars = message.getClonedParameters();

            String bidKey = ((Identifier) pars.get(0)).getValue();
            int bidValue = ((Numeral) pars.get(1)).getValue().intValue();

            Map<String, Integer> bid = new HashMap<>();
            bid.put(bidKey, bidValue);
            bids.put(sender, bid);

            Map<String, List<String>> award = new HashMap<>();

            if (bids.size() == announcedTo.size()) {
                say("Announced reqs: " + reqsAnn.toString());
                say("Announced to: " + announcedTo.size());
                say("Bids: " + bids.toString());
                Set<String> reqsType = new HashSet<>();
                for (var k : bids.keySet()) {
                    if (!bids.get(k).keySet().toArray()[0].toString().equals(""))
                        reqsType.add(bids.get(k).keySet().toArray()[0].toString());
                }

                //say("REQS:::::::  " + reqsType.toString());

                Map<String, List<String>> sameBids = new HashMap<>(); //store the agents that bidded to same requirement and compare them
                for (var reqq : reqsType) {
                    List<String> agentsSame = bids.entrySet().stream().filter(a -> a.getValue().keySet().toArray()[0].toString().equals(reqq)).map(Map.Entry::getKey).collect(Collectors.toList());
                    sameBids.put(reqq, agentsSame);
                }

                for (String requirem : reqsAnn) {
                    int min = 1000;
                    String awardedAgent = "";
                    say("SAME BIDS::: " + sameBids.toString());
                    if (sameBids.get(requirem) != null) {
                        for (String agent : sameBids.get(requirem)) {
                            if (bids.get(agent).get(requirem) < min) {
                                min = bids.get(agent).get(requirem);
                                awardedAgent = agent;
                            }
                        }
                        if (award.containsKey(requirem)) {
                            List<String> as = award.get(requirem);
                            as.add(awardedAgent);
                            award.put(requirem, as);//update it
                        } else {
                            award.put(requirem, new ArrayList<String>(List.of(awardedAgent)));
                        }
                        sameBids.get(requirem).remove(awardedAgent);
                        bids.remove(awardedAgent);
                    }
                }

            }

            if (award.size() > 0) {
                say("Awards: " + award.toString());

                //award = < "b1" : ["A3"] ,
                //          "b2" : ["A1","A2"],
                //          "b3" : [] >

                for (String awardedReq : award.keySet()) {
                    //reqToSend = [Block(b1,1,2)]
                    //reqToSend = [Block(b2,-1,0) , Block(b2,0,2)]
                    List<Block> reqToSend = requirements.keySet().stream().filter(req -> req.getType().equals(awardedReq) && !requirements.get(req)).collect(Collectors.toList());
                    for (var req : reqToSend) {
                        if (award.get(awardedReq) != null && award.get(awardedReq).size() > 0 && !award.get(awardedReq).get(0).equals("")) {
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
            helpingWho = sender;
        }

        //Go
        else if (message.getName().equals("GO")) {

            sendMessage(perceptionHandler.makePercept("HERE", this.mapHandler.getAgentLocation().getX(),
                    this.mapHandler.getAgentLocation().getY(), activeTask.getName()), sender, this.getName());

        }
        //HERE
        else if (message.getName().equals("HERE")) {
            List<Parameter> pars = message.getClonedParameters();
            IntegerPair teammateLocation = new IntegerPair(((Numeral) pars.get(0)).getValue().intValue(),
                    ((Numeral) pars.get(1)).getValue().intValue());
            String taskReceived = ((Identifier) pars.get(2)).getValue();

            IntegerPair transformation = teamMatesTrans.get(sender);
            IntegerPair teamMatePos = teammateLocation.add(transformation);

            IntegerPair blockXY = new IntegerPair(requirement.getX(), requirement.getY());
            myBlockPos = teamMatePos.add(blockXY);

            occupiedPositions.add(teamMatePos);

            Task t = perceptionHandler.getTasks().stream().filter(ta -> ta.getName().equals(taskReceived)).findFirst().get();
            for(var req : t.getRequirements().keySet()){
                IntegerPair xy = new IntegerPair(req.getX(), req.getY());
                occupiedPositions.add(xy.add(teamMatePos));
            }

            activePath.clear();
            state = State.GoingToYou;

        }
        else if(message.getName().equals("MapSharing")){
            ParameterList cellsPercept = (ParameterList)message.getParameters().get(0);
            List<Cell> cells = new ArrayList<>();
            List<IntegerPair> cellLocations = new ArrayList<>();
            for(var c : cellsPercept){
                Function cellPercept = (Function)c;
                List<Parameter> pars = cellPercept.getClonedParameters();
                CellType cellType = CellType.valueOf(((Identifier)pars.get(0)).getValue());
                String cellClass = ((Identifier)pars.get(1)).getValue();
                IntegerPair cellLocation = new IntegerPair(((Numeral) pars.get(2)).getValue().intValue(),
                        ((Numeral) pars.get(3)).getValue().intValue());
                Cell cell;
                if (cellClass == "Detailed"){
                    String details = ((Identifier)pars.get(4)).getValue();
                    cell = new DetailedCell(cellType, details);
                } else {
                    cell = new OrdinaryCell(cellType);
                }
                cells.add(cell);
                cellLocations.add(cellLocation);
            }
            IntegerPair transform = teamMatesTrans.get(sender);
            Boolean success = mapHandler.shareMap(transform, cells, cellLocations);
            if(success) say(sender + " shared his map with me");
            else {
                teamMatesTrans.remove(sender);
                say(sender + " didn't share his map with me");
            }
            /*
            List<Parameter> pars = message.getClonedParameters();
            CellType cellType = CellType.valueOf(((Identifier) pars.get(0)).getValue());
            String cellClass = ((Identifier) pars.get(1)).getValue();
            IntegerPair cellLocation = new IntegerPair(((Numeral) pars.get(2)).getValue().intValue(),
                    ((Numeral) pars.get(3)).getValue().intValue());
            Cell cell;
            if (cellClass.equals("Detailed")) {
                String details = ((Identifier) pars.get(4)).getValue();
                cell = new DetailedCell(cellType, details);
            } else {
                cell = new OrdinaryCell(cellType);
            }
            IntegerPair transform = this.teamMatesTrans.get(sender);
            this.mapHandler.makeTransformation(transform, cell, cellLocation);*/
            mapHandler.printMapToFile("maps\\" + getName() + "mapSharedWith" + sender + ".txt");
        }
        
        else if(message.getName().equals("ME")){
            List<Parameter> pars = message.getParameters();
            Integer reqDetailX = ((Numeral) pars.get(0)).getValue().intValue(); //the X detail of the requirement
            Integer reqDetailY = ((Numeral) pars.get(1)).getValue().intValue();
            
            myMates.put(sender, new IntegerPair(reqDetailX, reqDetailY));
        }
        
        else if(message.getName().equals("Thank you")){
            reset();
        }

    }

    @Override
    public Action step() {
        if(getName().equals("agentA1"))say(Whiteboard.getAllAssigned().toString());
        say(state.toString());
        //PHASE 1 (Sense) - Agent gets perceptions from Environment
        List<Percept> percepts = getPercepts();
        this.perceptionHandler = new PerceptionHandler(percepts);

        //PHASE 2 (Internal State) - Agent updates its internal state
        this.mapHandler.updateMap(perceptionHandler);//needs to check if lastAction was successful before updating...

        //if(perceptionHandler.getStep() % 100 == 0)mapHandler.printMapToFile("maps\\" + getName() + perceptionHandler.getStep() + ".txt");
        //if(true)return explore();

        if(perceptionHandler.getFailedMove()){
            activePath.clear();
            return moveRandom();
        }
        
        if(perceptionHandler.getFailedAction("connect") && state == State.AtGoal){
            return new Action("connect", new Identifier(failedMate), new Numeral(perceptionHandler.getAttached().get(0).getX()), new Numeral(perceptionHandler.getAttached().get(0).getY()));
        }
        
        if(perceptionHandler.getFailedAction("connect") && state == State.InPosition){
            return new Action("connect", new Identifier(helpingWho), new Numeral(perceptionHandler.getAttached().get(0).getX()), new Numeral(perceptionHandler.getAttached().get(0).getY()));
        }
        
        if(perceptionHandler.getSuccessfulAction("submit")){
            say("Look at me Mom, I fucking did it!!!!!!!");
            sendMessage(perceptionHandler.makePercept("Thank you"), failedMate, getName());
            reset();
        }
        
        //If Agent sees a teammate -> share relative positions!
        if (this.perceptionHandler.getTeammates().size() > 0) {
            //say("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!MET!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            for (var teamMate : this.perceptionHandler.getTeammates()) {
                broadcast(perceptionHandler.makePercept("Locations",
                        teamMate.getX(), teamMate.getY(), this.mapHandler.getAgentLocation().getX(),
                        this.mapHandler.getAgentLocation().getY()),
                        getName());
            }
            if (!alreadySkipped) {
                alreadySkipped = true;
                return new Action("skip");
            }
            alreadySkipped = false;

        }

        //if(true)return explore();

        /*if (check()){
            return moveRandom();
        } */

        

        //PHASE 3 (Deliberate) - Agent tries to figure out what is the best action to perform given his current state and his previous action
        Action act = chooseAction();

        //PHASE 4 (ACT) - Execute chosen action to achieve goal
        return act;
    }


    private Action chooseAction() {

        //CHECK WHERE TO FIT SELECTION PROCESS.... WHEN TO RESTART THE WEIGHTS.. PROBABLY WHEN HE IS DONE..
        //ALSO CHECK WHEN TASKS ARE HANDLED... HOW TO MARK THEM SO THAT AGENTS THAT DIDN'T RECEIVE ANYTHING DON'T INTERFEER....


        switch (state) {
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
            case GoingToYou:
                return goToYou();
            case InPosition:
                //activeActions = createPattern(helping, new IntegerPair(0, 0) /* requirement place*/)
                sendMessage(perceptionHandler.makePercept("ME",requirement.getX(), requirement.getY()), helpingWho, getName());
                return new Action("connect", new Identifier(helpingWho), new Numeral(perceptionHandler.getAttached().get(0).getX()), new Numeral(perceptionHandler.getAttached().get(0).getY()));
                //remember to helping -> false
        }

        return new Action("skip");
    }

    //##################################### HELPER FUNCTIONS #####################################################
    
    private void reset(){
        state = State.Exploring;
        activePath.clear();
        activeTask = null;
        requirement = null;
        helping = false;
        helpingWho = "";
        action = null;
        requirements = null;
        myTaskWeights.clear();
        alreadySkipped = false;
        reqsAnn.clear();
        occupiedPositions.clear();
        failedMate ="";
    }

    private Action lookForYou() {
        sendMessage(perceptionHandler.makePercept("GO"), helpingWho, getName());
        return new Action("skip");
    }

    private Action prepareToGo(IntegerPair agentPos, IntegerPair attch, List<IntegerPair> occupiedPositions){
        for(var occupiedPos : occupiedPositions){
            if(agentPos.equals(occupiedPos)){
                return new Action("rotate", new Identifier("cw"));
            }
        }
        return null;
    }

    private Action goToYou(){

        IntegerPair possibleAgentPos = null;
        IntegerPair attached = null;
        
        action = null;
        
        //CALCULATE WHERE TO GO
        if(activePath.isEmpty()){
            Thing attachedObj = perceptionHandler.getAttached().get(0);
            attached = new IntegerPair(attachedObj.getX(), attachedObj.getY());
            possibleAgentPos = attached.inverse().add(myBlockPos);
            action = prepareToGo(possibleAgentPos, attached, occupiedPositions);
        }
        if(action!=null){
            return action;
        }
        if(!activePath.isEmpty()){
            if(activePath.size() == 1){
                state = State.InPosition;
            }
            return moveTo(activePath.remove(0));
        }

        //IF IT IS NULL, MEANS NO NEED TO ROTATE... SO LET'S CALL BFS AND BUILD TAHT PATH
        
        BFS2 bfs = new BFS2(mapHandler);
        activePath = bfs.BFS(possibleAgentPos);
        activePath.remove(0);
        
        return moveTo(activePath.remove(0));
    }

    private Action doHelp() {

        List<Thing> attacheds = perceptionHandler.getAttached();

        if (attacheds.size() > 0) {
            String d = coordinatesToDirection(new IntegerPair(attacheds.get(0).getX(), attacheds.get(0).getY()));
            if(!d.equals(""))return new Action("detach", new Identifier(d));
        }

        //if(hasBlockAttached) -> drop it first
        String detail = requirement.getType();
        //Map<IntegerPair, String> dispensers = this.mapHandler.getDispensersByType(detail);
        //check Null case in dispensers.. but unlikely... because agent is helping because he can...

        if (activePath.isEmpty() || perceptionHandler.getFailedMove()) {
            activePath = lookForDispenserV2(detail); //action can be "move" if going for a dispenser | "request" if already near a dispenser | "attach" if we have requested a block from the dispenser || "null" if we haven't seen any dispenser yet
            activePath.remove(0);
        }
        if (!dispenserOnMySide(detail).equals("")) {
            state = State.NearDispenser;
            activePath = new LinkedList<>();
            return requestBlock(detail);
        }

        return moveTo(activePath.remove(0));
    }

    private Action trySubmition() {
        requirements.replace(requirement, true);
        //if you are an agent who needs help and there is still a requirement not done
        if (activeTask.getRequirements().containsValue(false)) {

            //!!!!!!!!!!!!!!!!!CHECK IF ALREADY ANNOUNCED TO AVOID KEEP ANNOUNCING WHILE WAITING.... JUST CHECK SIZE OF ANNOUNCEDTO!!!!!!!!!!!!!!
            IntegerPair attached = new IntegerPair(perceptionHandler.getAttached().get(0).getX(), perceptionHandler.getAttached().get(0).getY());
            if (attached.getX() != requirement.getX() || attached.getY() != requirement.getY()) {
                return new Action("rotate", new Identifier("cw"));
            }
            
            if (teamMatesTrans.size() > 0) {
                List<String> taskSpecs = new LinkedList<>();
                taskSpecs.add(activeTask.getName());
                //int i = 1;
                for (Map.Entry<Block, Boolean> req : activeTask.getRequirements().entrySet()) {
                    if (!req.getValue()) {
                        taskSpecs.add(req.getKey().getType());
                        reqsAnn.add(req.getKey().getType()); //ignore this but don't delete
                    }
                }
                String[] taskSpecsToSend = new String[taskSpecs.size()];
                taskSpecs.toArray(taskSpecsToSend);

                for (var teamMate : teamMatesTrans.keySet()) {
                    //send a message of format ["task1", "b2", "b3"]
                    announcedTo.add(teamMate);
                    shareMap(teamMate);
                    sendMessage(perceptionHandler.makePercept("Announcement", taskSpecsToSend), teamMate, getName());
                }

                /*for(int i=0; i<4; i++){
                    announcedTo.add("agentA"+i);
                }
                broadcast(perceptionHandler.makePercept("Announcement", taskSpecsToSend), getName());*/
            }
        } else if (myMates.size() > 0) {///activeTask.getRequirements().containsValue(false) == false
            for(var mate : myMates.keySet()){
                //IntegerPair mateXY = myMates.get(mate);
                failedMate = mate;
                myMates.remove(mate);
                return new Action("connect", new Identifier(mate), new Numeral(perceptionHandler.getAttached().get(0).getX()), new Numeral(perceptionHandler.getAttached().get(0).getY()));
            }
        }

        return new Action("submit", new Identifier(activeTask.getName()));


    }

    private Map<String, Integer> weightTasks() {
        //check if I have seen all the types of blocks/dispensers specified in the requirements
        List<Task> tasks = this.perceptionHandler.getTasks();
        Map<String, Integer> taskWeights = new HashMap<>();
        //int [] taskWeights = new int[tasks.size()];
        for (var task : tasks) {
            taskWeights.put(task.getName(), -1);
        }
        List<IntegerPair> possiblePlaces = List.of(
            new IntegerPair(0,1), 
            new IntegerPair(0,-1),
            new IntegerPair(1,0),
            new IntegerPair(-1,0)
        );
        
        for (Task task : tasks) {
            for (Map.Entry<Block, Boolean> requirement : task.getRequirements().entrySet()) {
                String detail = requirement.getKey().getType();
                IntegerPair r = new IntegerPair(requirement.getKey().getX(), requirement.getKey().getY());
                Map<IntegerPair, String> dispensers = this.mapHandler.getDispensersByType(detail);
                if (!dispensers.isEmpty() && possiblePlaces.contains(r)) {
                    List<IntegerPair> p = lookForDispenserV2(detail);//!!STRANGE!!!
                    if(p != null)taskWeights.replace(task.getName(), taskWeights.get(task.getName()) + p.size());
                }
            }
        }
        return taskWeights;
    }


    private Task chooseAvailableTask() {
        myTaskWeights = weightTasks();
        for (var task : myTaskWeights.keySet()) {
            Whiteboard.putWeigth(getName(), task, myTaskWeights.get(task));
        }

        //say("My weights: " + myTaskWeights.toString());

        //if(taskAssigned.equals(""))
        taskAssigned = Whiteboard.assignTask(getName());

        //say("All assigned Tasks: " + Whiteboard.getAllAssigned().toString());

        for (var task : perceptionHandler.getTasks()) {
            if (taskAssigned.equals(task.getName()))
                return task;
        }

        return null;
    }

    private Action doExplore() {
        List<Task> tasks = this.perceptionHandler.getTasks();
        if (!tasks.isEmpty()) {
            if (activeTask == null) {
                activeTask = chooseAvailableTask();
                if (activeTask == null) {
                    return explore();
                }
            }

            //requirement = activeTask.getRequirement();
            
            List<IntegerPair> possiblePlaces = List.of(
            new IntegerPair(0,1), 
            new IntegerPair(0,-1),
            new IntegerPair(1,0),
            new IntegerPair(-1,0)
        );
            
            //TODO check this part
            requirements = activeTask.getRequirements();
            for (Map.Entry<Block, Boolean> req : requirements.entrySet()) {
                String detail = req.getKey().getType();
                IntegerPair place = new IntegerPair(req.getKey().getX(), req.getKey().getY());
                Map<IntegerPair, String> dispensers = this.mapHandler.getDispensersByType(detail);
                if (!dispensers.isEmpty() && possiblePlaces.contains(place)) {
                    state = State.MovingToDispenser;
                    requirement = req.getKey();
                    return moveToDispenser(detail);
                }
            }
        }
        return explore();
    }

    private Action moveToDispenser(String detail) {
        if (activePath.isEmpty() || activePath == null || perceptionHandler.getFailedMove()) {
            activePath = lookForDispenserV2(detail); //action can be "move" if going for a dispenser | "request" if already near a dispenser | "attach" if we have requested a block from the dispenser || "null" if we haven't seen any dispenser yet
            activePath.remove(0);
        }
        if (!dispenserOnMySide(detail).equals("")) {
            state = State.NearDispenser;
            activePath = new LinkedList<>();
            return requestBlock(detail);
        }
        return moveTo(activePath.remove(0));
    }

    private Action requestBlock(String detail) {
        String direction = dispenserOnMySide(detail);
        String blockDir = blockOnMySide(detail);

        if (!blockDir.equals("")) {
            state = State.NearBlock;
            return attachBlock(detail);
        }

        if (direction.equals("")) {
            state = State.Exploring;
            activeTask = null;
            requirement = null;
            return doExplore();
        }
        return new Action("request", new Identifier(direction));
    }

    private Action attachBlock(String detail) {
        String blockDir = blockOnMySide(detail);
        if (blockDir.equals("")) {
            state = State.NearDispenser;
            return requestBlock(detail);
        }
        if (!helping){
            state = State.MovingToGoal;
        }
        else{
            //state = State.LookingForYou;
            sendMessage(perceptionHandler.makePercept("GO"), helpingWho, getName());
        }
        return new Action("attach", new Identifier(blockDir));
    }

    private Action moveToGoal() {
        if (isGoal()) {
            state = State.AtGoal;
            //return trySubmition();
            return new Action("skip");
        }
        List<IntegerPair> goals = mapHandler.getGoalList();
        if (goals.isEmpty()) {
            return explore();
        }
        if (activePath.isEmpty()) {
            activePath = getShortestPathByType(CellType.Goal, "");
            activePath.remove(0);
        }
        //in case we our attached block gets stuck
        if (perceptionHandler.getFailedMove()) {
            activePath = new LinkedList<>();
            return moveRandom();
        }
        return moveTo(activePath.remove(0));
    }

    @Deprecated
    private Action submit() {
        List<Thing> attacheds = perceptionHandler.getAttached();
        if (attacheds.isEmpty()) {
            state = State.MovingToDispenser;
            explore();
        }
        Thing attached = attacheds.get(0);
        if (attached.getX() != requirement.getX() || attached.getY() != requirement.getY()) {
            return new Action("rotate", new Identifier("cw"));
        }
        state = State.Exploring;
        return new Action("submit", new Identifier(activeTask.getName()));
    }


    private List<Action> createPattern(boolean amHelping, IntegerPair blockPlacement) {
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
        if ((amHelping && blockPlacement.equals(new IntegerPair(1, 1)))
                || (!amHelping && activeTask.getRequrementList().get(1).equals(new IntegerPair(1, 1)))) {//check the helper's requirement (we can compare a block with an integerPair)
            patternCase = 1;
        } else if ((amHelping && blockPlacement.equals(new IntegerPair(0, 1)))
                || (!amHelping && blockPlacement.equals(new IntegerPair(1, 1)))) {
            patternCase = 2;
        } else {
            patternCase = 3;
        }
        state = State.BuildPattern;

        switch (patternCase) {
            case 1:
                if (amHelping) return List.of(
                        new Action("connect", new Identifier("main agent"), new Numeral(0), new Numeral(1)),
                        new Action("detach", new Identifier("s"))
                );
                return List.of(
                        new Action("connect", new Identifier("helper"), new Numeral(0), new Numeral(1)),
                        new Action("skip"),
                        new Action("submit", new Identifier(activeTask.getName()))
                );
            case 2:
                if (amHelping) return List.of(
                        new Action("connect", new Identifier("main agent"), new Numeral(0), new Numeral(1)),
                        new Action("skip"),
                        new Action("submit", new Identifier("{taskName}"))
                );
                return List.of(
                        new Action("connect", new Identifier("helper"), new Numeral(0), new Numeral(1)),
                        new Action("detach", new Identifier("s"))
                );
            case 3:
                if (amHelping) return List.of(
                        new Action("move", new Identifier("s")),
                        new Action("move", new Identifier("s")),
                        new Action("rotate", new Identifier("cw")),
                        new Action("connect", new Identifier("main agent"), new Numeral(-1), new Numeral(0)),
                        new Action("detach", new Identifier("w"))
                );
                return List.of(
                        new Action("skip"),
                        new Action("skip"),
                        new Action("skip"),
                        new Action("connect", new Identifier("helper"), new Numeral(0), new Numeral(1)),
                        new Action("skip"),
                        new Action("submit", new Identifier(activeTask.getName()))
                );
        }

        say("Something fucked up");
        return List.of(new Action("skip"));
    }

    private Action buildPattern() {
        if (activeActions.isEmpty()) {
            state = State.Exploring;
            return new Action("skip");
        }
        return activeActions.remove(0);
    }

    private List<IntegerPair> lookForDispenserV2(String detail) {
        Map<IntegerPair, String> dispensers = this.mapHandler.getDispensersByType(detail);
        if (dispensers != null && dispensers.size() > 0) {

            List<IntegerPair> path = getShortestPathByType(CellType.Dispenser, detail);
            if (path != null && path.size() > 0) {
                return path;
            }
        }
        return null;
    }


    public boolean isGoal() {
        if (mapHandler.getMap()[mapHandler.getAgentLocation().getX()][mapHandler.getAgentLocation().getY()].getType().equals(CellType.Goal)) {
            return true;
        }
        return false;
    }

    public Action lookForGoal() {
        List<IntegerPair> path = getShortestPathByType(CellType.Goal, "");
        if (path.size() > 1) {
            IntegerPair next_location = path.get(1);
            return moveTo(next_location);
        } else if (path.size() == 1) {
            return new Action("submit", new Identifier(this.activeTask.getName()));
        }

        return explore();
    }

    public String coordinatesToDirection(IntegerPair coordinates) {
        if (coordinates.getX() == -1 && coordinates.getY() == 0) {
            return "w";
        } else if (coordinates.getX() == 1 && coordinates.getY() == 0) {
            return "e";
        } else if (coordinates.getX() == 0 && coordinates.getY() == -1) {
            return "n";
        } else if (coordinates.getX() == 0 && coordinates.getY() == 1) {
            return "s";
        }
        return "";
    }

    public String blockOnMySide(String detail) {
        int[] directionInX = {-1, 1, 0, 0};
        int[] directionInY = {0, 0, 1, -1};

        //for each direction -> check
        for (int i = 0; i < 4; i++) {
            int x = directionInX[i];
            int y = directionInY[i];
            IntegerPair pos = new IntegerPair(this.mapHandler.getAgentLocation().getX() + x, this.mapHandler.getAgentLocation().getY() + y);
            if (this.mapHandler.getCell(pos).getType().equals(CellType.Block) && ((DetailedCell) mapHandler.getCell(pos)).getDetails().equals(detail)) {
                String direction = coordinatesToDirection(new IntegerPair(x, y));
                return direction;
            }
        }
        return "";
    }

    public String dispenserOnMySide(String detail) {
        int[] directionInX = {-1, 1, 0, 0};
        int[] directionInY = {0, 0, 1, -1};

        //for each direction -> check
        for (int i = 0; i < 4; i++) {
            int x = directionInX[i];
            int y = directionInY[i];
            IntegerPair pos = new IntegerPair(this.mapHandler.getAgentLocation().getX() + x, this.mapHandler.getAgentLocation().getY() + y);
            if (this.mapHandler.getCell(pos).getType().equals(CellType.Dispenser) && ((DetailedCell) mapHandler.getCell(pos)).getDetails().equals(detail)) {
                String direction = coordinatesToDirection(new IntegerPair(x, y));
                return direction;
            }
        }
        return "";
    }

    public List<IntegerPair> getShortestPathByType(CellType ct, String detail) {
        //boolean failed = this.perceptionHandler.getFailedMove();
        BFS2 bfs = new BFS2(this.mapHandler);
        List<IntegerPair> path = bfs.BFS(ct, detail);
        return path;
    }

    public List<IntegerPair> getShortestPathByLocation(IntegerPair goal) {
        BFSsearch bfs = new BFSsearch(this.mapHandler, this.length, this.width);
        List<IntegerPair> path = bfs.bfsByLocation(goal);
        return path;
    }

    public Action explore() {
        /*BFS2 bfs = new BFS2(mapHandler);
        List<IntegerPair> path = bfs.explore();
        if(path.isEmpty()) return moveRandom();
        path.remove(0);
        return moveTo(path.remove(0));*/
        IntegerPair agentLocation = this.mapHandler.getAgentLocation();
        int[] directionInX = {-1, 1, 0, 0};
        int[] directionInY = {0, 0, 1, -1};

        int[] unknownInX = {-6, 6, 0, 0};
        int[] unknownInY = {0, 0, 6, -6};
        for (int i = 0; i < 4; i++) {
            int newPosX = agentLocation.getX() + directionInX[i];
            int newPosY = agentLocation.getY() + directionInY[i];
            IntegerPair newCellPos = new IntegerPair(newPosX, newPosY);
            int unknownX = agentLocation.getX() + unknownInX[i];
            int unknownY = agentLocation.getY() + unknownInY[i];
            IntegerPair unknownPos = new IntegerPair(unknownX, unknownY);
            if (!mapHandler.getMap()[newCellPos.getX()][newCellPos.getY()].getType().equals(CellType.Empty)) {
                continue;
            }
            if (!mapHandler.getMap()[unknownPos.getX()][unknownPos.getY()].getType().equals(CellType.Unknown)) {
                continue;
            }
            return moveTo(newCellPos);
        }
        return moveRandom();

    }

    private Action moveRandom() {
        Random rand = new Random();
        int r = rand.nextInt(100);
        if (r < 25) return new Action("move", new Identifier("n"));
        if (r < 50) return new Action("move", new Identifier("s"));
        if (r < 75) return new Action("move", new Identifier("e"));
        return new Action("move", new Identifier("w"));
    }


    public Action moveTo(IntegerPair nextLocation) {
        int x_movement = nextLocation.getX() - this.mapHandler.getAgentLocation().getX();
        int y_movement = nextLocation.getY() - this.mapHandler.getAgentLocation().getY();

        this.agentMovement = new IntegerPair(x_movement, y_movement);

        if (this.agentMovement.getX() == 0 && this.agentMovement.getY() == 0)
            return new Action("skip");

        String direction = coordinatesToDirection(this.agentMovement);
        return new Action("move", new Identifier(direction));
    }

    private Boolean check() {
        boolean stuck = false;
        if (!this.activePath.isEmpty()) {
            String nextStep = coordinatesToDirection(this.activePath.get(0));
            List<Thing> attachedItemsList = this.perceptionHandler.getAttached();
            if (!attachedItemsList.isEmpty()) {
                for (var attachedItem : attachedItemsList) {
                    IntegerPair relativePosition = new IntegerPair(attachedItem.getX(), attachedItem.getY());
                    String attachmentDirection = coordinatesToDirection(relativePosition);
                    IntegerPair direction = new IntegerPair(9, 9);
                    Cell checkCell;
                    switch (attachmentDirection) {
                        case "n":
                            if (nextStep.equals("e") || nextStep.equals("w")) {
                                if (nextStep.equals("e")) {
                                    direction = new IntegerPair(1, -1);
                                } else {
                                    direction = new IntegerPair(-1, -1);
                                }
                            }
                            break;
                        case "w":
                            if (nextStep.equals("n") || nextStep.equals("s")) {
                                if (nextStep.equals("n")) {
                                    direction = new IntegerPair(-1, -1);
                                } else {
                                    direction = new IntegerPair(-1, 1);
                                }
                            }
                            break;
                        case "s":
                            if (nextStep.equals("w") || nextStep.equals("e")) {
                                if (nextStep.equals("w")) {
                                    direction = new IntegerPair(-1, 1);
                                } else {
                                    direction = new IntegerPair(1, 1);
                                }
                            }
                            break;
                        case "e":
                            if (nextStep.equals("n") || nextStep.equals("s")) {
                                if (nextStep.equals("n")) {
                                    direction = new IntegerPair(1, -1);
                                } else {
                                    direction = new IntegerPair(1, 1);
                                }
                            }
                            break;
                    }
                    if (!direction.equals(new IntegerPair(9, 9))) {
                        checkCell = this.mapHandler.getCell(this.mapHandler.getAgentLocation().add(direction));
                        if (checkCell.getType() != CellType.Empty) {
                            stuck = true;
                            activePath.clear();
                        }
                    }

                }

            }
        }
        return stuck;
    }

    private void shareMap(String receiver){
        List<Percept> cells = new LinkedList<>();
        for (int i = 0; i < this.length; i++) {
            for (int j = 0; j < this.width; j++) {
                Cell item = this.mapHandler.getCell(new IntegerPair(i, j));
                CellType cellType;
                cellType = item.getType();
                if (cellType.equals(CellType.Unknown) || cellType.equals(CellType.Teammate)) continue;
                String cellClass;
                String details;
                if (cellType == CellType.Dispenser || cellType == CellType.Block) {
                    cellClass = "Detailed";
                    details = ((DetailedCell) item).getDetails();
                } else {
                    cellClass = "Ordinal";
                    details = "";
                }
                /*sendMessage(this.perceptionHandler.makePercept("MapSharing",
                        cellType.toString(), cellClass, i, j, details)
                        , receiver, this.getName());*/
                Percept c = this.perceptionHandler.makePercept("Cell",
                            cellType.toString(), cellClass, i, j, details);
                cells.add(c);
            }
        }
        sendMessage(perceptionHandler.makePercept("MapSharing", cells), receiver, getName());
    }

}
