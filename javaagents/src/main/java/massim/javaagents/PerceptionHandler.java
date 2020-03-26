package massim.javaagents;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import eis.iilang.*;
import massim.javaagents.utils.Block;
import massim.javaagents.utils.Thing;

public class PerceptionHandler {
	private List<Percept> percepts;
	private String team;

	public PerceptionHandler(List<Percept> percepts) {
		this.percepts = percepts;
		Percept teamPercept = filterByName("team").get(0);
		team = ((Identifier) teamPercept.getParameters().get(0)).getValue().toString();
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
}