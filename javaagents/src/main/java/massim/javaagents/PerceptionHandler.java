package massim.javaagents;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import eis.iilang.*;
import massim.javaagents.utils.Block;
import massim.javaagents.utils.IntegerPair;
import massim.javaagents.utils.Task;
import massim.javaagents.utils.Thing;

public class PerceptionHandler {
	private List<Percept> percepts;
	private String team;
	private String name;

	public PerceptionHandler(List<Percept> percepts) {
		this.percepts = percepts;
		Percept teamPercept = filterByName("team").get(0);
		team = ((Identifier) teamPercept.getParameters().get(0)).getValue().toString();
		Percept namePercept = filterByName("name").get(0);
		name = ((Identifier) namePercept.getParameters().get(0)).getValue().toString();
	}
	
	public String getName(){
		return name;
	}
	
	public String getTeam() {
		return team;
	}

	public List<Thing> getEnemies() {
		return perceptToThing(filterEnemies());
	}

	public List<Thing> getTeammates() {
		return perceptToThing(filterTeammates());
	}

	public List<Thing> getObstacles() {
		return getThingsByName("obstacle");
	}

	public List<Thing> getGoals() {
		return getThingsByName("goal");
	}
	
	public List<Thing> getAttached(){
		return getThingsByName("attached");
	}

	public List<Block> getBlocks() {
		List<Percept> blocks = filterByType("block")
				.collect(Collectors.toList());
		return perceptToBlock(blocks);
	}
	
	public List<Block> getDispensers() {
		List<Percept> dispensers = filterByType("dispenser")
				.collect(Collectors.toList());
		return perceptToBlock(dispensers);
	}
	
	public List<Task> getTasks(){
		List<Percept> taskPercepts = filterByName("task");
		return perceptToTask(taskPercepts);
	}
	
	public Boolean getFailed(){
		String actionResult = getStringParameter(filterByName("lastActionResult").get(0), 0);
		if(actionResult.equals("success")) return false;
		return true;
	}
	
	public List<Thing> getEmpty(){
		List<Thing> empties = new LinkedList<>();
		for(int i = -5; i <= 5; ++i){
			int absI = Math.abs(i);
			for(int j = absI-5; j <= 5-absI; ++j){
				Thing t = new Thing(i, j);
				empties.add(t);
			}
		}
		List<Thing> things = new LinkedList<>();
		things.addAll(getBlocks());
		things.addAll(getDispensers());
		things.addAll(getObstacles());
		things.addAll(getGoals());
		things.addAll(getEnemies());
		things.addAll(getTeammates());
		for(Thing thing : things){
			empties.removeIf(p -> p.equals(thing));
		}
		return empties;
	}
	
	public IntegerPair getAgentMovement(){
		String actionResult = getStringParameter(filterByName("lastActionResult").get(0), 0);
		String action = getStringParameter(filterByName("lastAction").get(0), 0);
		if(!actionResult.equals("success") || !action.equals("move")) return new IntegerPair(0, 0);
		//wtf?!?!(but it works)
		String direction = ((Identifier)((ParameterList)filterByName("lastActionParams").get(0).getParameters().get(0)).get(0)).getValue();
		return dirToPos(direction);
	}
	
	public Percept makePercept(String name, Object... params){
		LinkedList<Parameter> parameters = new LinkedList<>();
		for(var param : params){
			parameters.add(javaToParameter(param));
		}
		return new Percept(name, parameters);
	}

	private List<Thing> getThingsByName(String name) {
		List<Percept> filteredPercepts = filterByName(name);
		return perceptToThing(filteredPercepts);
	}

	private List<Percept> filterEnemies() {
		Stream<Percept> entities = filterByType("entity");
		return entities.filter(p -> !getStringParameter(p, 3).equals(team)).collect(Collectors.toList());
	}

	private List<Percept> filterTeammates() {
		Stream<Percept> entities = filterByType("entity");
		return entities.filter(p -> getStringParameter(p, 3).equals(team)).collect(Collectors.toList());
	}

	private List<Percept> filterByName(String name) {
		return percepts.stream().filter(p -> p.getName().equals(name)).collect(Collectors.toList());
	}

	private Stream<Percept> filterByType(String type) {
		return percepts.stream().filter(p -> p.getName().equals("thing"))
				.filter(p -> (((Identifier) p.getParameters().get(2)).getValue()).equals(type));
	}

	private List<Thing> perceptToThing(List<Percept> percepts) {
		List<Thing> res = new LinkedList<>();
		for (var per : percepts) {
			if (per.getParameters().size() < 2)
				continue;
			int x = getIntParameter(per, 0);
			int y = getIntParameter(per, 1);
			Thing t = new Thing(x, y);
			res.add(t);
		}
		return res;
	}

	private List<Block> perceptToBlock(List<Percept> percepts) {
		List<Block> res = new LinkedList<>();
		for (var per : percepts) {
			if (per.getParameters().size() < 4 || !(getStringParameter(per, 2).equals("block")
					|| getStringParameter(per, 2).equals("dispenser"))) {
				continue;
			}
			int x = getIntParameter(per, 0);
			int y = getIntParameter(per, 1);
			String type = getStringParameter(per, 3);
			Block t = new Block(x, y, type);
			res.add(t);
		}
		return res;
	}
	
	private List<Task> perceptToTask(List<Percept> precepts){
		List<Task> res = new LinkedList<>();
		for(var per : percepts){
			if (per.getParameters().size() < 4 || !per.getName().equals("task")) {
				continue;
			}
			String name = getStringParameter(per, 0);
			int deadLine = getIntParameter(per, 1);
			int reward = getIntParameter(per, 2);
			ParameterList requirementPercepts = ((ParameterList)per.getClonedParameters().get(3));
			List<Block> requirements = requirementsToBlocks(requirementPercepts);
			Task task = new Task(requirements, name, deadLine, reward);
			res.add(task);
		}
		return res;
	}
	
	private List<Block> requirementsToBlocks(ParameterList requriements){
		List<Block> res = new LinkedList<>();
		for(Parameter req : requriements){
			if(!(req instanceof Function)) continue;
			//This is fucking ridiculous
			int x = ((Numeral)((Function)req).getParameters().get(0)).getValue().intValue();
			int y = ((Numeral)((Function)req).getParameters().get(1)).getValue().intValue();
			String type = ((Identifier)((Function)req).getParameters().get(2)).getValue();
			Block block = new Block(x, y, type);
			res.add(block);
		}
		return res;
	}
	
	private Parameter javaToParameter(Object object){
		if(object instanceof Number){
			return new Numeral((Number)object);
		}
		if(object instanceof String){
			return new Identifier((String)object);
		}
		if(object instanceof List){
			List<Object> list = (List<Object>)object;
			List<Parameter> paramList = new LinkedList<>();
			for(var par : list){
				paramList.add(javaToParameter(par));
			}
			return new ParameterList(paramList);
		}
		if(object instanceof Percept){
			Percept per = (Percept)object;
			return new Function(per.getName(), per.getClonedParameters());
		}
		return null;
	}

	// get p percept's ith parameter if string
	private String getStringParameter(Percept p, int index) {
		if (p.getParameters().get(index) instanceof Identifier) {
			return ((Identifier) p.getParameters().get(index)).getValue();
		} else
			return "";
	}

	// get p percept's ith parameter if interger
	private Integer getIntParameter(Percept p, int index) {
		if (p.getParameters().get(index) instanceof Numeral) {
			return ((Numeral) p.getParameters().get(index)).getValue().intValue();
		} else
			return 0;
	}
	
	private IntegerPair dirToPos(String direction){
		if(direction.equals("n")) return new IntegerPair(0, -1);
		if(direction.equals("s")) return new IntegerPair(0, 1);
		if(direction.equals("e")) return new IntegerPair(1, 0);
		if(direction.equals("w")) return new IntegerPair(-1, 0);
		return new IntegerPair(0, 0);
	}
}