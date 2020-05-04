package massim.javaagents;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import massim.javaagents.utils.CellType;
import massim.javaagents.utils.DetailedCell;
import massim.javaagents.utils.IntegerPair;

public class BFS2 {
	MapHandler mh;
	Set<IntegerPair> visited;
	Map<IntegerPair, IntegerPair> parent;
	
	public BFS2(MapHandler mh){
		this.mh = mh;
		visited = new HashSet<>();
		parent = new HashMap<>();
	}
	
	public List<IntegerPair> explore(){
		return BFS(CellType.Unknown, "", true);
	}
	
	public List<IntegerPair> BFS(CellType type, String detail){
		return BFS(type, detail, false);
	}
	
	public List<IntegerPair> BFS(IntegerPair desiredLocation){
		IntegerPair goal = searchLocation(desiredLocation);
		if(goal == null) return null;
		return createPath(goal);
	}
	
	public List<IntegerPair> BFS(CellType type, String detail, boolean explore){
		IntegerPair goal = search(type, detail, explore);
		if(goal == null) return null;
		return createPath(goal);
	}
	
	private List<IntegerPair> createPath(IntegerPair goal){
		List<IntegerPair> path = new LinkedList<>();
		IntegerPair current = goal;
		while(current != null){
			path.add(0, current);
			current = parent.get(current);
		}
		return path;
	}
	
	private IntegerPair search(CellType type, String detail, boolean explore)
	{ 
		IntegerPair s = mh.getAgentLocation();
		// Create a queue for BFS 
		LinkedList<IntegerPair> queue = new LinkedList<>(); 
  
		// Mark the current node as visited and enqueue it 
		//visited[s]=true; 
		visited.add(s);
		queue.add(s); 
		parent.put(s, null);
  
		while (queue.size() != 0) 
		{ 
			// Dequeue a vertex from queue and print it 
			s = queue.poll(); 
			
			if(!explore && mh.getCell(s).getType().equals(type)){
				if(detail.equals("")) return s;
				if(((DetailedCell)mh.getCell(s)).getDetails().equals(detail)) return s;
			}
			else if(explore && mh.getCell(s).getType().equals(CellType.Unknown)) return s;
  
			Iterator<IntegerPair> i = possibleNeighbours(s, explore).listIterator(); 
			while (i.hasNext()) 
			{ 
				IntegerPair n = i.next(); 
				if (!visited.contains(n)) 
				{ 
					visited.add(n); 
					queue.add(n);
					parent.put(n,s);
				} 
			} 
		}
		return null;
	}
	
	private IntegerPair searchLocation(IntegerPair location)
	{ 
		IntegerPair s = mh.getAgentLocation();
		// Create a queue for BFS 
		LinkedList<IntegerPair> queue = new LinkedList<>(); 
  
		// Mark the current node as visited and enqueue it 
		//visited[s]=true; 
		visited.add(s);
		queue.add(s); 
		parent.put(s, null);
  
		while (queue.size() != 0) 
		{ 
			// Dequeue a vertex from queue and print it 
			s = queue.poll(); 
			
			if(s.equals(location)){
				return s;
			}
  
			Iterator<IntegerPair> i = possibleNeighbours(s, false).listIterator(); 
			while (i.hasNext()) 
			{ 
				IntegerPair n = i.next(); 
				if (!visited.contains(n)) 
				{ 
					visited.add(n); 
					queue.add(n);
					parent.put(n,s);
				} 
			} 
		}
		return null;
	} 
	
	private List<IntegerPair> possibleNeighbours(IntegerPair pos, boolean explore){
		int [] directionInX = {-1,1,0,0};
		int [] directionInY = {0,0,1,-1};
		List<IntegerPair> res = new LinkedList<>();
		for(int i=0; i<4; i++){
			IntegerPair newPos = new IntegerPair(pos.getX() + directionInX[i], pos.getY() + directionInY[i]);
			//boundaries
			if(newPos.getX() < 0 || newPos.getX() > 100 || newPos.getY() < 0 || newPos.getY() > 100){
				continue;
			}
			//cell type
			if(!(mh.getCell(newPos).getType().equals(CellType.Empty) || mh.getCell(newPos).getType().equals(CellType.Dispenser) || mh.getCell(newPos).getType().equals(CellType.Goal))){
				if(!(mh.getCell(newPos).getType().equals(CellType.Unknown) && explore)) continue;
			}
			res.add(newPos);
		}
		return res;
	}
	
}