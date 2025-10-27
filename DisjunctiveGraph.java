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
            machineBatches.computeIfAbsent(batch.getMachineId(), k -> new ArrayList<>()).add(batch);
        }
        
        for (List<Batch> batches : machineBatches.values()) {
            batches.sort(Comparator.comparingInt(Batch::getPosition));
        }
        
        boolean changed = true;
        int iterations = 0;
        
        while (changed && iterations < 10) {
            changed = false;
            iterations++;
            
            for (Map.Entry<Integer, List<Batch>> entry : machineBatches.entrySet()) {
                int machineId = entry.getKey();
                Machine machine = problem.getMachine(machineId);
                if (machine == null) continue;
                
                List<Batch> orderedBatches = entry.getValue();
                
                for (int batchIdx = 0; batchIdx < orderedBatches.size(); batchIdx++) {
                    Batch batch = orderedBatches.get(batchIdx);
                    if (batch.getOperations().isEmpty()) continue;
                    
                    int batchStart = 0;
                    
                    if (batchIdx > 0) {
                        Batch prevBatch = orderedBatches.get(batchIdx - 1);
                        int prevStart = prevBatch.getStartTime();
                        if (prevStart >= 0) {
                            batchStart = prevStart + prevBatch.getProcessingTime() + machine.getInterBatchDelay();
                        }
                    }
                    
                    for (Operation op : batch.getOperations()) {
                        int jobReady = op.getJob().getReleaseDate();
                        
                        if (op.getIndex() > 0) {
                            Operation prevOp = op.getJob().getOperations().get(op.getIndex() - 1);
                            int prevStart = solution.getStartTime(prevOp);
                            if (prevStart >= 0) {
                                jobReady = prevStart + prevOp.getProcessingTime() + prevOp.getMinTimeLag();
                            }
                        }
                        
                        batchStart = Math.max(batchStart, jobReady);
                    }
                    
                    int oldStart = batch.getStartTime();
                    if (oldStart != batchStart) {
                        batch.setStartTime(batchStart);
                        for (Operation op : batch.getOperations()) {
                            solution.setStartTime(op, batchStart);
                        }
                        changed = true;
                    }
                }
            }
        }
        
        for (Job job : problem.getJobs()) {
            List<Operation> ops = job.getOperations();
            for (int i = 0; i < ops.size() - 1; i++) {
                Operation current = ops.get(i);
                Operation next = ops.get(i + 1);
                
                int currentStart = solution.getStartTime(current);
                int nextStart = solution.getStartTime(next);
                
                if (currentStart >= 0 && nextStart >= 0) {
                    int currentEnd = currentStart + current.getProcessingTime();
                    int gap = nextStart - currentEnd;
                    
                    if (gap < current.getMinTimeLag()) {
                        return false;
                    }
                    
                    if (current.getMaxTimeLag() < Integer.MAX_VALUE && gap > current.getMaxTimeLag()) {
                        return false;
                    }
                }
            }
        }
        
        return true;
    }
    
    public void updateSolutionStartTimes() {
    }
}