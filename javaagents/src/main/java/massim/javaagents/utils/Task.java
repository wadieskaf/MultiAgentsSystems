package massim.javaagents.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Task {
    private String name;
    private Integer deadLine;
    private Integer reward;
    private Map<Block, Boolean> requirements;

    public String getName() {
        return this.name;
    }

    public Integer getDeadLine() {
        return this.deadLine;
    }

    public Integer getReward() {
        return this.reward;
    }
    
    public Map<Block, Boolean> getRequirements() {
        return this.requirements;
    }

    public Task(List<Block> requirements, String name, Integer deadLine, Integer reward){
        this.requirements = new HashMap<>();
        for(var req : requirements){
            this.requirements.put(req, false);
        }
        this.name = name;
        this.deadLine = deadLine;
        this.reward = reward;
    }
    
    public void setRequirementStatus(Block block){
        setRequirementStatus(block, true);
    }
    
    public void setRequirementStatus(Block block, Boolean status){
        this.requirements.replace(block, status);
    }
    
    
}