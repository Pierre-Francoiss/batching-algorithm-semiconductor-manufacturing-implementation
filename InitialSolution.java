import java.util.*;

/* This class represent a first solution created by performing PBIA on the model, we will then perform the simulated annealign based on it*/
public class InitialSolution {
    private Problem problem;
    
    public InitialSolution(Problem problem) {
        this.problem = problem;
    }
    
    public Solution build() {
        Solution solution = new Solution(problem);
        /* Copy and sort jobs based on the priority rules of heuristic  */
        List<Job> sortedJobs = new ArrayList<>(problem.getJobs());
        sortedJobs.sort((j1, j2) -> {
            int lag1 = getMaxLag(j1);
            int lag2 = getMaxLag(j2);
            if (lag1 != lag2) return Integer.compare(lag1, lag2);
            if (j1.getReleaseDate() != j2.getReleaseDate()) 
                return Integer.compare(j1.getReleaseDate(), j2.getReleaseDate());
            return Integer.compare(j2.getPriority(), j1.getPriority());
        });
        
        /* create a map for batchs to machines affectations*/
        Map<Integer, List<Batch>> machineToBatches = new HashMap<>();
        int batchIdCounter = 0;
        
        for (Job job : sortedJobs) {
            for (Operation op : job.getOperations()) {
                boolean inserted = false;
                
                /* for every operations research of an existing batche to add the operations in it */
                for (int machineId : op.getEligibleMachines()) {
                    Machine machine = problem.getMachine(machineId);
                    if (machine == null || !machine.canProcess(op.getRecipe())) continue;
                    
                    List<Batch> batches = machineToBatches.get(machineId);
                    if (batches != null) {
                        for (Batch batch : batches) {
                            if (batch.getRecipe() != null && 
                                batch.getRecipe().equals(op.getRecipe()) && 
                                batch.getOperations().size() < machine.getCapacity()) {
                                batch.addOperation(op);
                                inserted = true;
                                break;
                            }
                        }
                    }
                    if (inserted) break;
                }
                 /* if there is no compatible batches we create a new batch to store this operation */
                if (!inserted) {
                    for (int machineId : op.getEligibleMachines()) {
                        Machine machine = problem.getMachine(machineId);
                        if (machine != null && machine.canProcess(op.getRecipe())) {
                            List<Batch> batches = machineToBatches.computeIfAbsent(machineId, 
                                k -> new ArrayList<>());
                            
                            Batch newBatch = new Batch(batchIdCounter++, machineId, batches.size());
                            newBatch.addOperation(op);
                            batches.add(newBatch);
                            solution.addBatch(newBatch);
                            break;
                        }
                    }
                }
            }
        }
        
        /*Evaluation of this solution */
        calculateStartTimesSimple(solution, machineToBatches);
        solution.evaluate();
        
        return solution;
    }
    
    /* Calcul of the start time of every operations based on the rules descibed in the article */
    private void calculateStartTimesSimple(Solution solution, Map<Integer, List<Batch>> machineToBatches) {
        for (Map.Entry<Integer, List<Batch>> entry : machineToBatches.entrySet()) {
            int machineId = entry.getKey();
            Machine machine = problem.getMachine(machineId);
            if (machine == null) continue;
            
            int currentTime = 0;
            /* for every batch scheduled on a machine */
            for (Batch batch : entry.getValue()) {
                if (batch.getOperations().isEmpty()) continue;
                /* start the operation ASAP */
                int batchStart = currentTime;
                /* calcul of the time diff based on the rules to update currentTime */
                for (Operation op : batch.getOperations()) {
                    /* Release date */
                    int requiredTime = op.getJob().getReleaseDate();
                    /* Last operation must be complete */
                    if (op.getIndex() > 0) {
                        Operation prevOp = op.getJob().getOperations().get(op.getIndex() - 1);
                        int prevStart = solution.getStartTime(prevOp);
                        if (prevStart >= 0) {
                            requiredTime = prevStart + prevOp.getProcessingTime() + prevOp.getMinTimeLag();
                        }
                    }
                    
                    batchStart = Math.max(batchStart, requiredTime);
                }
                
                /* Every operations of the batch should start at the same time */
                batch.setStartTime(batchStart);
                for (Operation op : batch.getOperations()) {
                    solution.setStartTime(op, batchStart);
                }
                
                currentTime = batchStart + batch.getProcessingTime() + machine.getInterBatchDelay();
            }
        }
    }
    
    /*Calcul of the max timelag for a job */
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