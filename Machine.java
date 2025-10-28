import java.util.List;

public class Machine {
    private int id;
    private int capacity;
    private int setupTime;
    private int removalTime;
    private int interBatchDelay;
    /* Listing associated recipes to affect operations to right machines in addition to disponibility */
    private List<String> qualifiedRecipes;
    
    public Machine(int id, int capacity, int setupTime, int removalTime, int interBatchDelay, List<String> qualifiedRecipes) {
        this.id = id;
        this.capacity = capacity;
        this.setupTime = setupTime;
        this.removalTime = removalTime;
        this.interBatchDelay = interBatchDelay;
        this.qualifiedRecipes = qualifiedRecipes;
    }
    
    public boolean canProcess(String recipe) {
        return qualifiedRecipes.contains(recipe);
    }
    
    public int getId() { return id; }
    public int getCapacity() { return capacity; }
    public int getSetupTime() { return setupTime; }
    public int getRemovalTime() { return removalTime; }
    public int getInterBatchDelay() { return interBatchDelay; }
    public List<String> getQualifiedRecipes() { return qualifiedRecipes; }
}