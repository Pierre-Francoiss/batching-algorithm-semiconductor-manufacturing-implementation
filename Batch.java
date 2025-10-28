import java.util.ArrayList;
import java.util.List;

/* As explained in the article a batch consist of one or several operations on a single machine at a precise time */
public class Batch {
    private int id;
    private List<Operation> operations;
    private int machineId;
    private int position;
    private int startTime;
    
    public Batch(int id, int machineId, int position) {
        this.id = id;
        this.machineId = machineId;
        this.position = position;
        this.operations = new ArrayList<>();
        this.startTime = -1;
    }
    
    public void addOperation(Operation op) {
        operations.add(op);
    }
    
    /* Every operations have the same recipe so we can just grap the first one to cover every cases */
    public String getRecipe() {
        return operations.isEmpty() ? null : operations.get(0).getRecipe();
    }
    
    /* same logic than for recipe */
    public int getProcessingTime() {
        return operations.isEmpty() ? 0 : operations.get(0).getProcessingTime();
    }
    
    public Batch clone(int newId) {
        Batch b = new Batch(newId, machineId, position);
        b.operations = new ArrayList<>(operations);
        b.startTime = startTime;
        return b;
    }
    
    public int getId() { return id; }
    public List<Operation> getOperations() { return operations; }
    public int getMachineId() { return machineId; }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
    public int getStartTime() { return startTime; }
    public void setStartTime(int startTime) { this.startTime = startTime; }
}