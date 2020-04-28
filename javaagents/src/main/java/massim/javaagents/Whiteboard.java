package massim.javaagents;

import java.util.*;


public class Whiteboard {
	private static Map<String, Map<String, Integer>> tasksWeights = new HashMap<>();
	private static Map<String, String> assignedTasks = new HashMap<>();
	
	public static synchronized void putWeigths(String agent, Map<String, Integer> value){
		tasksWeights.put(agent, value);
	}
	
	public static synchronized void putWeigth(String agent, String task, Integer value){
		Map<String, Integer> taskMap = tasksWeights.get(agent);
		//if(!assignedTasks.containsKey(task)) assignedTasks.put(task, null);
		if(taskMap == null){
			Map<String, Integer> newMap = new HashMap<>();
			newMap.put(task, value);
			tasksWeights.put(agent, newMap);
		}
		else{
			taskMap.put(task, value);
			tasksWeights.put(agent, taskMap);
		}
	}

	public static synchronized String assignTask(String agent){
		List<String> tasksReceived = new LinkedList<>();
		for(var otherAgent : tasksWeights.keySet()){
			if(!agent.equals(otherAgent)){
				Map<String, Integer> agentTasksW = tasksWeights.get(agent);
				Map<String, Integer> otherAgentTasksW = tasksWeights.get(otherAgent);
				for(String task : agentTasksW.keySet()){
					if(assignedTasks.size() > 0 && assignedTasks.get(task) != null){
						continue;
					}
					else if(agentTasksW.get(task) < 0){
						continue;
					}
					else if(otherAgentTasksW.get(task) == null){
						if(tasksReceived.contains(task))
							continue;
						else
							tasksReceived.add(task);
					}
					else if(agentTasksW.get(task) >= 0 && otherAgentTasksW.get(task) < 0){
						if(tasksReceived.contains(task))
							continue;
						else
							tasksReceived.add(task);
					}
					else if(agentTasksW.get(task) < otherAgentTasksW.get(task)){
						if(tasksReceived.contains(task))
							continue;
						else
							tasksReceived.add(task);
					}
					else{ //the other guy has better weight
						if(tasksReceived.contains(task))
							tasksReceived.remove(task);
						else
							continue;
					}
				}
			}
		}

		String task = "";
		int min = -1;

		for(String taskReceived : tasksReceived){
			if(min == -1){
				min = tasksWeights.get(agent).get(taskReceived);
				task = taskReceived;
			}
			else if(tasksWeights.get(agent).get(taskReceived) < min){
				min = tasksWeights.get(agent).get(taskReceived);
				task = taskReceived;
			}
			else{
				continue;
			}
		}

		if(!task.equals(""))
			assignedTasks.put(task, agent);

		return task;
	}
	
	/*public static synchronized void assignTasks(){
		for(var agent : tasksWeights.keySet()){
			
			if(assignedTasks.containsValue(agent))
				continue;
			Map<String, Integer> taskW = tasksWeights.get(agent);
			String minTask=null;
			Integer minVal = Integer.MAX_VALUE;
			for(String task : taskW.keySet()){
				if(assignedTasks.get(task) == null && taskW.get(task) < minVal && taskW.get(task) >= 0){
					minVal = taskW.get(task);
					minTask = task;
				}
			}
			if(minTask != null)assignedTasks.put(minTask, agent);
		}
		//Double min = Collections.min(map.values())
	}*/

	public static synchronized Map<String, Map<String, Integer>> get(){
		return tasksWeights;
	}
	
	public static synchronized String getTaskAssigned(String agent){
		return assignedTasks.get(agent);
	}
	public static synchronized Map<String, String> getAllAssigned(){
		return assignedTasks;
	}

	public static synchronized Map<String, Integer> getMyWeightsOnBoard(String agent){
		return tasksWeights.get(agent);
	}

	public static synchronized List<Map<String, Integer>> getOthersWeightsOnBoard(String agent){
		List<Map<String, Integer>> othersW = new LinkedList<>();
		for(String other : tasksWeights.keySet()){
			if(!agent.equals(other)){
				othersW.add(tasksWeights.get(other));
			}
		}
		return othersW;
	}
}