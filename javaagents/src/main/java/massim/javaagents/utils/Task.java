package massim.javaagents.utils;

import java.util.List;

public class Task {
    private String name;
    private Integer deadLine;
    private Integer reward;
    private List<Block> requirements;

    public String getName() {
        return this.name;
    }

    public Integer getDeadLine() {
        return this.deadLine;
    }

    public Integer getReward() {
        return this.reward;
    }

    public List<Block> getRequirements() {
        return this.requirements;
    }

    public Task(List<Block> requirements, String name, Integer deadLine, Integer reward){
        this.requirements = requirements;
        this.name = name;
        this.deadLine = deadLine;
        this.reward = reward;
    }
    
    
}