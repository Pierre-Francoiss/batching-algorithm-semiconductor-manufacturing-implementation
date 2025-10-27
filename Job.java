import java.util.ArrayList;
import java.util.List;

public class Job {
    private int id;
    private List<Operation> operations;
    private int releaseDate;
    private int priority;
    private int waferCount;
    
    public Job(int id, int releaseDate, int priority, int waferCount) {
        this.id = id;
        this.releaseDate = releaseDate;
        this.priority = priority;
        this.waferCount = waferCount;
        this.operations = new ArrayList<>();
    }
    
    public void addOperation(Operation operation) {
        operations.add(operation);
    }
    
    public int getId() { return id; }
    public List<Operation> getOperations() { return operations; }
    public int getReleaseDate() { return releaseDate; }
    public int getPriority() { return priority; }
    public int getWaferCount() { return waferCount; }
}