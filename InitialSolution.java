import java.util.*;

public class InitialSolution {
    private Problem problem;
    
    public InitialSolution(Problem problem) {
        this.problem = problem;
    }
    
    public Solution build() {
        Solution solution = new Solution(problem);
        
        List<Job> sorted = new ArrayList<>(problem.getJobs());
        sorted.sort((j1, j2) -> {
            int lag1 = getMaxLag(j1);
            int lag2 = getMaxLag(j2);
            if (lag1 != lag2) return Integer.compare(lag1, lag2);
            if (j1.getReleaseDate() != j2.getReleaseDate()) 
                return Integer.compare(j1.getReleaseDate(), j2.getReleaseDate());
            return Integer.compare(j2.getPriority(), j1.getPriority());
        });
        
        Map<Integer, List<Batch>> machineBatches = new HashMap<>();
        int batchId = 0;
        
        for (Job job : sorted) {
            for (Operation op : job.getOperations()) {
                boolean inserted = false;
                
                for (int machineId : op.getEligibleMachines()) {
                    Machine machine = problem.getMachine(machineId);
                    if (machine == null || !machine.canProcess(op.getRecipe())) continue;
                    
                    List<Batch> batches = machineBatches.getOrDefault(machineId, new ArrayList<>());
                    
                    for (Batch batch : batches) {
                        if (batch.getRecipe().equals(op.getRecipe()) && 
                            batch.getOperations().size() < machine.getCapacity()) {
                            batch.addOperation(op);
                            inserted = true;
                            break;
                        }
                    }
                    
                    if (inserted) break;
                }
                
                if (!inserted) {
                    for (int machineId : op.getEligibleMachines()) {
                        Machine machine = problem.getMachine(machineId);
                        if (machine != null && machine.canProcess(op.getRecipe())) {
                            List<Batch> batches = machineBatches.computeIfAbsent(machineId, k -> new ArrayList<>());
                            
                            Batch newBatch = new Batch(batchId++, machineId, batches.size());
                            newBatch.addOperation(op);
                            batches.add(newBatch);
                            solution.addBatch(newBatch);
                            break;
                        }
                    }
                }
            }
        }
        
        DisjunctiveGraph graph = new DisjunctiveGraph(problem, solution);
        if (graph.computeLongestPaths()) {
            graph.updateSolutionStartTimes();
        }
        solution.evaluate();
        
        return solution;
    }
    
    private int getMaxLag(Job job) {
        int max = 0;
        for (Operation op : job.getOperations()) {
            if (op.getMaxTimeLag() < Integer.MAX_VALUE) {
                max = Math.max(max, op.getMaxTimeLag());
            }
        }
        return max;
    }
}