package massim.javaagents;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class Whiteboard {
	private static Map<String, Map<String, Integer>> tasksWeights = new HashMap<>();
	private static Map<String, String> assignedTasks = new HashMap<>();
	
	public static synchronized void putWeigths(String agent, Map<String, Integer> value){
		tasksWeights.put(agent, value);
	}
	
	public static synchronized void putWeigth(String agent, String task, Integer value){
		Map<String, Integer> taskMap = tasksWeights.get(agent);
		if(!assignedTasks.containsKey(task)) assignedTasks.put(task, null);
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
	
	public static synchronized void assignTasks(){
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
	} 

	public static synchronized Map<String, Map<String, Integer>> get(){
		return tasksWeights;
	}
	
	public static synchronized String getTaskAssigned(String agent){
		return assignedTasks.get(agent);
	}
	public static synchronized Map<String, String> getAllAssigned(){
		return assignedTasks;
	}
}