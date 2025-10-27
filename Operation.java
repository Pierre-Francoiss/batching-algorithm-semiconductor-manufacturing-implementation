import java.util.List;

public class Operation {
    private int id;
    private Job job;
    private int index;
    private int processingTime;
    private String recipe;
    private List<Integer> eligibleMachines;
    private int minTimeLag;
    private int maxTimeLag;
    
    public Operation(int id, Job job, int index, int processingTime, String recipe, List<Integer> eligibleMachines) {
        this.id = id;
        this.job = job;
        this.index = index;
        this.processingTime = processingTime;
        this.recipe = recipe;
        this.eligibleMachines = eligibleMachines;
        this.minTimeLag = 0;
        this.maxTimeLag = Integer.MAX_VALUE;
    }
    
    public void setTimeLags(int minTimeLag, int maxTimeLag) {
        this.minTimeLag = minTimeLag;
        this.maxTimeLag = maxTimeLag;
    }
    
    public int getId() { return id; }
    public Job getJob() { return job; }
    public int getIndex() { return index; }
    public int getProcessingTime() { return processingTime; }
    public String getRecipe() { return recipe; }
    public List<Integer> getEligibleMachines() { return eligibleMachines; }
    public int getMinTimeLag() { return minTimeLag; }
    public int getMaxTimeLag() { return maxTimeLag; }
}