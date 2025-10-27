import java.util.*;

public class SimulatedAnnealing {
    private Problem problem;
    private double temperature;
    private double coolingRate;
    private int maxIterations;
    private Random random;
    
    public SimulatedAnnealing(Problem problem, double temperature, double coolingRate, int maxIterations) {
        this.problem = problem;
        this.temperature = temperature;
        this.coolingRate = coolingRate;
        this.maxIterations = maxIterations;
        this.random = new Random();
    }
    
    public Solution solve(Solution initial) {
        Solution current = initial.clone();
        Solution best = current.clone();
        
        double temp = temperature;
        int accepted = 0;
        int rejected = 0;
        
        System.out.println("Debut Recuit Simule");
        System.out.println("Solution initiale: " + String.format("%.2f", current.getObjectiveValue()));
        
        for (int iter = 0; iter < maxIterations && temp > 0.1; iter++) {
            Solution neighbor = generateNeighbor(current);
            
            if (neighbor != null) {
                DisjunctiveGraph graph = new DisjunctiveGraph(problem, neighbor);
                
                if (graph.computeLongestPaths()) {
                    graph.updateSolutionStartTimes();
                    neighbor.evaluate();
                    
                    double delta = neighbor.getObjectiveValue() - current.getObjectiveValue();
                    
                    boolean accept = false;
                    if (delta > 0) {
                        accept = true;
                        accepted++;
                    } else {
                        double prob = Math.exp(delta / temp);
                        if (random.nextDouble() < prob) {
                            accept = true;
                            accepted++;
                        } else {
                            rejected++;
                        }
                    }
                    
                    if (accept) {
                        current = neighbor;
                        if (current.getObjectiveValue() > best.getObjectiveValue()) {
                            best = current.clone();
                            System.out.println("Iteration " + iter + " - Nouvelle meilleure: " + 
                                             String.format("%.2f", best.getObjectiveValue()));
                        }
                    }
                } else {
                    rejected++;
                }
            }
            
            temp *= coolingRate;
            
            if (iter % 1000 == 0 && iter > 0) {
                System.out.println("Iteration " + iter + " - T=" + String.format("%.2f", temp) + 
                                 " - Acceptes=" + accepted + " - Rejetes=" + rejected);
            }
        }
        
        System.out.println("Fin Recuit Simule");
        System.out.println("Solution finale: " + String.format("%.2f", best.getObjectiveValue()));
        System.out.println("Taux acceptation: " + String.format("%.2f%%", 
                          100.0 * accepted / (accepted + rejected)));
        
        return best;
    }
    
    private Solution generateNeighbor(Solution current) {
        double rand = random.nextDouble();
        
        try {
            if (rand < 0.50) {
                return batchMove(current);
            } else if (rand < 0.75) {
                return operationMove(current);
            } else {
                return operationSwitch(current);
            }
        } catch (Exception e) {
            return null;
        }
    }
    
    private Solution batchMove(Solution current) {
        Solution neighbor = current.clone();
        List<Batch> batches = neighbor.getBatches();
        
        if (batches.isEmpty()) return null;
        
        Batch batch = batches.get(random.nextInt(batches.size()));
        if (batch.getOperations().isEmpty()) return null;
        
        Operation op = batch.getOperations().get(0);
        List<Integer> eligible = op.getEligibleMachines();
        
        if (eligible.isEmpty()) return null;
        
        int newMachineId = eligible.get(random.nextInt(eligible.size()));
        
        Batch newBatch = new Batch(batch.getId(), newMachineId, 0);
        for (Operation o : batch.getOperations()) {
            newBatch.addOperation(o);
        }
        
        batches.remove(batch);
        batches.add(newBatch);
        
        return neighbor;
    }
    
    private Solution operationMove(Solution current) {
        Solution neighbor = current.clone();
        List<Batch> batches = neighbor.getBatches();
        
        if (batches.isEmpty()) return null;
        
        Batch source = batches.get(random.nextInt(batches.size()));
        if (source.getOperations().isEmpty()) return null;
        
        Operation op = source.getOperations().get(random.nextInt(source.getOperations().size()));
        source.getOperations().remove(op);
        
        if (random.nextBoolean() && batches.size() > 1) {
            for (Batch target : batches) {
                if (target != source && target.getRecipe() != null && 
                    target.getRecipe().equals(op.getRecipe())) {
                    Machine m = problem.getMachine(target.getMachineId());
                    if (target.getOperations().size() < m.getCapacity()) {
                        target.addOperation(op);
                        break;
                    }
                }
            }
        } else {
            List<Integer> eligible = op.getEligibleMachines();
            if (!eligible.isEmpty()) {
                int machineId = eligible.get(random.nextInt(eligible.size()));
                Batch newBatch = new Batch(batches.size(), machineId, 0);
                newBatch.addOperation(op);
                batches.add(newBatch);
            }
        }
        
        if (source.getOperations().isEmpty()) {
            batches.remove(source);
        }
        
        return neighbor;
    }
    
    private Solution operationSwitch(Solution current) {
        Solution neighbor = current.clone();
        List<Batch> batches = neighbor.getBatches();
        
        if (batches.size() < 2) return null;
        
        List<Batch> sameBatches = new ArrayList<>();
        String recipe = null;
        
        for (Batch b : batches) {
            if (!b.getOperations().isEmpty()) {
                if (recipe == null) {
                    recipe = b.getRecipe();
                    sameBatches.add(b);
                } else if (recipe.equals(b.getRecipe())) {
                    sameBatches.add(b);
                }
            }
        }
        
        if (sameBatches.size() < 2) return null;
        
        Batch b1 = sameBatches.get(random.nextInt(sameBatches.size()));
        Batch b2;
        do {
            b2 = sameBatches.get(random.nextInt(sameBatches.size()));
        } while (b1 == b2);
        
        if (b1.getOperations().isEmpty() || b2.getOperations().isEmpty()) return null;
        
        Operation op1 = b1.getOperations().get(random.nextInt(b1.getOperations().size()));
        Operation op2 = b2.getOperations().get(random.nextInt(b2.getOperations().size()));
        
        b1.getOperations().remove(op1);
        b2.getOperations().remove(op2);
        b1.addOperation(op2);
        b2.addOperation(op1);
        
        return neighbor;
    }
}