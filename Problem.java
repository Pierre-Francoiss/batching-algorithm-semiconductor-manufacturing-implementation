import java.util.*;

/* Class modeling the base problem to solve, basically the industrial setup to optimize with the machines, the jobs and the time horizon */
public class Problem {
    private List<Job> jobs;
    private List<Machine> machines;
    private Map<Integer, Machine> machineMap;
    private int horizon;
    
    public Problem(int horizon) {
        this.horizon = horizon;
        this.jobs = new ArrayList<>();
        this.machines = new ArrayList<>();
        this.machineMap = new HashMap<>();
    }
    
    public void addJob(Job job) {
        jobs.add(job);
    }
    
    public void addMachine(Machine machine) {
        machines.add(machine);
        machineMap.put(machine.getId(), machine);
    }
    
    public Machine getMachine(int id) {
        return machineMap.get(id);
    }
    
    public List<Job> getJobs() { return jobs; }
    public List<Machine> getMachines() { return machines; }
    public int getHorizon() { return horizon; }
}