import java.util.*;

public class DisjunctiveGraph {
    private Map<Operation, Integer> longestPaths;
    private Solution solution;
    private Problem problem;
    
    public DisjunctiveGraph(Problem problem, Solution solution) {
        this.problem = problem;
        this.solution = solution;
        this.longestPaths = new HashMap<>();
    }
    
    public boolean computeLongestPaths() {
        longestPaths.clear();
        
        for (Job job : problem.getJobs()) {
            for (Operation op : job.getOperations()) {
                longestPaths.put(op, Integer.MIN_VALUE);
            }
        }
        
        boolean changed = true;
        int iterations = 0;
        int maxIterations = longestPaths.size() * 10;
        
        while (changed && iterations < maxIterations) {
            changed = false;
            iterations++;
            
            for (Job job : problem.getJobs()) {
                List<Operation> ops = job.getOperations();
                
                for (int i = 0; i < ops.size(); i++) {
                    Operation op = ops.get(i);
                    Batch batch = solution.getBatchForOperation(op);
                    if (batch == null) continue;
                    
                    Machine machine = problem.getMachine(batch.getMachineId());
                    int newPath;
                    
                    if (i == 0) {
                        newPath = Math.max(job.getReleaseDate(), machine.getSetupTime());
                    } else {
                        Operation prev = ops.get(i - 1);
                        int prevPath = longestPaths.get(prev);
                        if (prevPath == Integer.MIN_VALUE) continue;
                        
                        Batch prevBatch = solution.getBatchForOperation(prev);
                        Machine prevMachine = problem.getMachine(prevBatch.getMachineId());
                        
                        int minLag = prevMachine.getRemovalTime() + prev.getMinTimeLag() + 
                                    machine.getSetupTime() + prev.getProcessingTime();
                        newPath = prevPath + minLag;
                    }
                    
                    if (newPath > longestPaths.get(op)) {
                        longestPaths.put(op, newPath);
                        changed = true;
                    }
                }
            }
            
            for (Machine machine : problem.getMachines()) {
                List<Batch> machineBatches = new ArrayList<>();
                for (Batch b : solution.getBatches()) {
                    if (b.getMachineId() == machine.getId()) {
                        machineBatches.add(b);
                    }
                }
                machineBatches.sort(Comparator.comparingInt(Batch::getPosition));
                
                for (int i = 0; i < machineBatches.size(); i++) {
                    Batch batch = machineBatches.get(i);
                    
                    for (Operation op : batch.getOperations()) {
                        int currentPath = longestPaths.get(op);
                        if (currentPath == Integer.MIN_VALUE) continue;
                        
                        for (Operation op2 : batch.getOperations()) {
                            if (op != op2) {
                                int op2Path = longestPaths.get(op2);
                                if (currentPath > op2Path) {
                                    longestPaths.put(op2, currentPath);
                                    changed = true;
                                }
                            }
                        }
                    }
                    
                    if (i > 0) {
                        Batch prevBatch = machineBatches.get(i - 1);
                        int maxPrevPath = Integer.MIN_VALUE;
                        
                        for (Operation prevOp : prevBatch.getOperations()) {
                            maxPrevPath = Math.max(maxPrevPath, longestPaths.get(prevOp));
                        }
                        
                        if (maxPrevPath != Integer.MIN_VALUE) {
                            int delay = prevBatch.getProcessingTime() + machine.getRemovalTime() + 
                                       machine.getInterBatchDelay() + machine.getSetupTime();
                            int newStart = maxPrevPath + delay;
                            
                            for (Operation op : batch.getOperations()) {
                                if (newStart > longestPaths.get(op)) {
                                    longestPaths.put(op, newStart);
                                    changed = true;
                                }
                            }
                        }
                    }
                }
            }
        }
        
        if (iterations >= maxIterations) {
            return false;
        }
        
        for (Job job : problem.getJobs()) {
            List<Operation> ops = job.getOperations();
            for (int i = 0; i < ops.size() - 1; i++) {
                Operation current = ops.get(i);
                Operation next = ops.get(i + 1);
                
                int currentEnd = longestPaths.get(current) + current.getProcessingTime();
                int nextStart = longestPaths.get(next);
                
                if (nextStart - currentEnd > current.getMaxTimeLag()) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    public void updateSolutionStartTimes() {
        for (Job job : problem.getJobs()) {
            for (Operation op : job.getOperations()) {
                Integer path = longestPaths.get(op);
                if (path != null && path != Integer.MIN_VALUE) {
                    solution.setStartTime(op, path);
                    Batch batch = solution.getBatchForOperation(op);
                    if (batch != null) {
                        batch.setStartTime(path);
                    }
                }
            }
        }
    }
}