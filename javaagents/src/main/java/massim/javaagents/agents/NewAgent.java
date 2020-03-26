package massim.javaagents.agents;

import eis.iilang.*;
import massim.javaagents.MailService;
import massim.javaagents.PerceptionHandler;
import massim.javaagents.utils.Block;
import massim.javaagents.utils.Thing;

import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;


public class NewAgent extends Agent {

    public NewAgent(String name, MailService mailbox) {
        super(name, mailbox);
    }

    @Override
    public void handlePercept(Percept percept) {

    }

    @Override
    public void handleMessage(Percept message, String sender) {

    }

    @Override
    public Action step() {
        List<Percept> percepts = getPercepts();
        /*percepts.stream()
                .filter(p -> p.getName().equals("step"))
                .findAny()
                .ifPresent(p -> {
                    Parameter param = p.getParameters().getFirst();
                    if(param instanceof Numeral) say("Step " + ((Numeral) param).getValue());
                });
        List<Percept> obstacles = new ArrayList<Percept>();
        obstacles = percepts.stream().filter(p -> p.getName().equals("obstacle")).collect(Collectors.toList());
        for(Percept obstacle : obstacles){
            int x = ((Numeral)obstacle.getParameters().get(0)).getValue().intValue();
            int y = ((Numeral)obstacle.getParameters().get(1)).getValue().intValue();
            if(x == 0 && y == -1){
                return new Action("move", new Identifier("w"));
            }
        }*/
        PerceptionHandler ph = new PerceptionHandler(percepts);
        String team = ph.getTeam();
        List<Thing> enemies = ph.getEnemies();
        List<Thing> teammates = ph.getTeammates();
        List<Thing> obstacles = ph.getObstacles();
        List<Thing> goals = ph.getGoals();
        List<Block> blocks = ph.getBlocks();
        List<Block> dispensers = ph.getDispensers();
        return new Action("move", new Identifier("n"));
    }

}