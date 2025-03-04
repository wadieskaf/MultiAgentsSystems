package massim.javaagents;

import eis.AgentListener;
import eis.EnvironmentListener;
import eis.exceptions.ActException;
import eis.exceptions.AgentException;
import eis.exceptions.PerceiveException;
import eis.exceptions.RelationException;
import eis.iilang.EnvironmentState;
import eis.iilang.Percept;
import massim.eismassim.EnvironmentInterface;
import massim.javaagents.agents.Agent;
import massim.javaagents.agents.BasicAgent;

//MINEEEEE
import massim.javaagents.agents.NewAgent;

import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * A scheduler for agent creation and execution.
 * EISMASSim scheduling needs to be enabled (via config), so that getAllPercepts()
 * blocks until new percepts are available!
 * (Also, queued and notifications should be disabled)
 */
public class Scheduler implements AgentListener, EnvironmentListener{

    /**
     * Holds configured agent data.
     */
    private class AgentConf {
        String name;
        String entity;
        String team;
        String className;

        AgentConf(String name, String entity, String team, String className){
            this.name = name;
            this.entity = entity;
            this.team = team;
            this.className = className;
        }
    }

    private EnvironmentInterface eis;
    private List<AgentConf> agentConfigurations = new Vector<>();
    private Map<String, Agent> agents = new HashMap<>();

    /**
     * Create a new scheduler based on the given configuration file
     * @param path path to a java agents configuration file
     */
    Scheduler(String path) {
        parseConfig(path);
    }

    /**
     * Parses the java agents config.
     * @param path the path to the config
     */
    private void parseConfig(String path) {
        try {
            JSONObject config = new JSONObject(new String(Files.readAllBytes(Paths.get(path, "javaagentsconfig.json"))));
            JSONObject agents = config.optJSONObject("agents");
            if(agents != null){
                agents.keySet().forEach(agName -> {
                    JSONObject agConf = agents.getJSONObject(agName);
                    agentConfigurations.add(new AgentConf(agName, agConf.getString("entity"), agConf.getString("team"),
                                                          agConf.getString("class")));
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Connects to an Environment Interface
     * @param ei the interface to connect to
     */
    void setEnvironment(EnvironmentInterface ei) {
        this.eis = ei;
        MailService mailService = new MailService();
        for (AgentConf agentConf: agentConfigurations) {

            Agent agent = null;
            switch(agentConf.className){
                case "NewAgent":
                    agent = new NewAgent(agentConf.name, mailService);
                    break;
                case "BasicAgent":
                    agent = new BasicAgent(agentConf.name, mailService);
                    break;
                // [add further types here]
                default:
                    System.out.println("Unknown agent type/class " + agentConf.className);
            }
            if(agent == null) continue;

            mailService.registerAgent(agent, agentConf.team);

            try {
                ei.registerAgent(agent.getName());
            } catch (AgentException e) {
                e.printStackTrace();
            }

            try {
                ei.associateEntity(agent.getName(), agentConf.entity);
                System.out.println("associated agent \"" + agent.getName() + "\" with entity \"" + agentConf.entity + "\"");
            } catch (RelationException e) {
                e.printStackTrace();
            }

            ei.attachAgentListener(agent.getName(), this);
            agents.put(agentConf.name, agent);
        }
        ei.attachEnvironmentListener(this);
    }

    /**
     * Steps all agents and relevant infrastructure.
     */
    void step() {
        // retrieve percepts for all agents
        List<Agent> newPerceptAgents = new Vector<>();
        agents.values().forEach(ag -> {
            List<Percept> percepts = new Vector<>();
            try {
                eis.getAllPercepts(ag.getName()).values().forEach(percepts::addAll);
                newPerceptAgents.add(ag);
            } catch (PerceiveException e) {
//                System.out.println("No percepts for " + ag.getName());
            }
            ag.setPercepts(percepts);
        });

        // step all agents which have new percepts
        newPerceptAgents.forEach(agent -> {
            eis.iilang.Action action = agent.step();
            try {
                eis.performAction(agent.getName(), action);
            } catch (ActException e) {
                if(action != null)
                    System.out.println("Could not perform action " + action.getName() + " for " + agent.getName());
            }
        });
        //Whiteboard.assignTasks();

        if(newPerceptAgents.size() == 0) try {
            Thread.sleep(1000); // wait a bit in case no agents have been executed
        } catch (InterruptedException ignored) {}
    }

    @Override
    public void handlePercept(String agent, Percept percept) {
        agents.get(agent).handlePercept(percept);
    }

    @Override
    public void handleStateChange(EnvironmentState newState) {}

    @Override
    public void handleFreeEntity(String entity, Collection<String> agents) {}

    @Override
    public void handleDeletedEntity(String entity, Collection<String> agents) {}

    @Override
    public void handleNewEntity(String entity) {}
}
