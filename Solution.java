import java.util.*;

public class Solution {
    private Problem problem;
    private List<Batch> batches;
    private Map<Operation, Batch> operationToBatch;
    private Map<Operation, Integer> startTimes;
    private double objectiveValue;
    private double fMov;
    private double fBatch;
    private double fXFac;
    
    public Solution(Problem problem) {
        this.problem = problem;
        this.batches = new ArrayList<>();
        this.operationToBatch = new HashMap<>();
        this.startTimes = new HashMap<>();
    }
    
    public void addBatch(Batch batch) {
        batches.add(batch);
        for (Operation op : batch.getOperations()) {
            operationToBatch.put(op, batch);
        }
    }
    
    public void setStartTime(Operation op, int time) {
        startTimes.put(op, time);
    }
    
    public int getStartTime(Operation op) {
        return startTimes.getOrDefault(op, -1);
    }
    
    public void evaluate() {
        int horizon = problem.getHorizon();
        double alpha = 601.0;
        double beta = 1500001.0;
        double gamma = 41.0;
        
        fMov = 0;
        for (Job job : problem.getJobs()) {
            for (Operation op : job.getOperations()) {
                int start = getStartTime(op);
                if (start >= 0 && start < horizon) {
                    int end = start + op.getProcessingTime();
                    double completionRatio = Math.min(end, horizon) - start;
                    completionRatio = completionRatio / op.getProcessingTime();
                    fMov += job.getWaferCount() * completionRatio;
                }
            }
        }
        
        fBatch = 0;
        int validBatches = 0;
        for (Batch batch : batches) {
            if (batch.getStartTime() >= 0 && batch.getStartTime() < horizon && 
                !batch.getOperations().isEmpty()) {
                Machine m = problem.getMachine(batch.getMachineId());
                if (m != null) {
                    double denominator = m.getCapacity() + (m.getQualifiedRecipes().size() / 100.0);
                    fBatch += batch.getOperations().size() / denominator;
                    validBatches++;
                }
            }
        }
        if (validBatches > 0) fBatch /= validBatches;
        
        fXFac = 0;
        int completedJobs = 0;
        for (Job job : problem.getJobs()) {
            List<Operation> ops = job.getOperations();
            if (!ops.isEmpty()) {
                Operation lastOp = ops.get(ops.size() - 1);
                int lastStart = getStartTime(lastOp);
                if (lastStart >= 0) {
                    int lastEnd = lastStart + lastOp.getProcessingTime();
                    if (lastEnd <= horizon) {
                        int totalTime = lastEnd - job.getReleaseDate();
                        double xf = (double) totalTime / lastOp.getProcessingTime();
                        fXFac += job.getPriority() * xf;
                        completedJobs++;
                    }
                }
            }
        }
        if (completedJobs > 0) fXFac /= completedJobs;
        
        objectiveValue = alpha * fMov + beta * fBatch - gamma * fXFac;
    }
    
    public Solution clone() {
        Solution s = new Solution(problem);
        
        Map<Batch, Batch> batchMap = new HashMap<>();
        for (Batch b : batches) {
            Batch newB = b.clone(b.getId());
            s.batches.add(newB);
            batchMap.put(b, newB);
        }
        
        for (Batch newB : s.batches) {
            for (Operation op : newB.getOperations()) {
                s.operationToBatch.put(op, newB);
            }
        }
        
        s.startTimes = new HashMap<>(startTimes);
        s.objectiveValue = objectiveValue;
        s.fMov = fMov;
        s.fBatch = fBatch;
        s.fXFac = fXFac;
        return s;
    }
    
    public List<Batch> getBatches() { return batches; }
    public Batch getBatchForOperation(Operation op) { return operationToBatch.get(op); }
    public double getObjectiveValue() { return objectiveValue; }
    public double getfMov() { return fMov; }
    public double getfBatch() { return fBatch; }
    public double getfXFac() { return fXFac; }
}