package massim.javaagents.agents;

import eis.iilang.*;
import massim.javaagents.MailService;
import massim.javaagents.PerceptionHandler;
import massim.javaagents.utils.Block;
import massim.javaagents.utils.Task;
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

        PerceptionHandler ph = new PerceptionHandler(percepts);
        String team = ph.getTeam();
        List<Thing> enemies = ph.getEnemies();
        List<Thing> teammates = ph.getTeammates();
        List<Thing> obstacles = ph.getObstacles();
        List<Thing> goals = ph.getGoals();
        List<Block> blocks = ph.getBlocks();
        List<Block> dispensers = ph.getDispensers();
        List<Task> tasks = ph.getTasks();
        return new Action("move", new Identifier("n"));
    }

}