import java.util.*;

public class DisjunctiveGraph {
    private Solution solution;
    private Problem problem;
    
    public DisjunctiveGraph(Problem problem, Solution solution) {
        this.problem = problem;
        this.solution = solution;
    }
    
    public boolean computeLongestPaths() {
        Map<Integer, List<Batch>> machineBatches = new HashMap<>();
        for (Batch batch : solution.getBatches()) {
            if (!batch.getOperations().isEmpty()) {
                machineBatches.computeIfAbsent(batch.getMachineId(), k -> new ArrayList<>()).add(batch);
            }
        }
        
        for (List<Batch> batches : machineBatches.values()) {
            batches.sort(Comparator.comparingInt(Batch::getPosition));
        }
        
        for (int iteration = 0; iteration < 10; iteration++) {
            for (Map.Entry<Integer, List<Batch>> entry : machineBatches.entrySet()) {
                int machineId = entry.getKey();
                Machine machine = problem.getMachine(machineId);
                if (machine == null) continue;
                
                List<Batch> orderedBatches = entry.getValue();
                int machineTime = 0;
                
                for (Batch batch : orderedBatches) {
                    int earliestBatchStart = machineTime;
                    
                    for (Operation op : batch.getOperations()) {
                        int jobReady = op.getJob().getReleaseDate();
                        
                        if (op.getIndex() > 0) {
                            Operation prevOp = op.getJob().getOperations().get(op.getIndex() - 1);
                            int prevStart = solution.getStartTime(prevOp);
                            if (prevStart >= 0) {
                                jobReady = Math.max(jobReady, prevStart + prevOp.getProcessingTime() + prevOp.getMinTimeLag());
                            }
                        }
                        
                        earliestBatchStart = Math.max(earliestBatchStart, jobReady);
                    }
                    
                    batch.setStartTime(earliestBatchStart);
                    for (Operation op : batch.getOperations()) {
                        solution.setStartTime(op, earliestBatchStart);
                    }
                    
                    machineTime = earliestBatchStart + batch.getProcessingTime() + machine.getInterBatchDelay();
                }
            }
        }
        
        return true;
    }
    
    public void updateSolutionStartTimes() {
    }
}