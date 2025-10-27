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
        int validNeighbors = 0;
        int invalidNeighbors = 0;
        
        System.out.println("Debut Recuit Simule");
        System.out.println("Solution initiale: " + String.format("%.2f", current.getObjectiveValue()));
        System.out.println("Temperature: " + temp + ", CoolingRate: " + coolingRate + ", MaxIter: " + maxIterations);
        
        for (int iter = 0; iter < maxIterations && temp > 0.1; iter++) {
            Solution neighbor = generateNeighbor(current);
            
            if (neighbor != null) {
                DisjunctiveGraph graph = new DisjunctiveGraph(problem, neighbor);
                graph.computeLongestPaths();
                
                validNeighbors++;
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
            }
            
            temp *= coolingRate;
            
            if (iter % 1000 == 0) {
                System.out.println("Iteration " + iter + " - T=" + String.format("%.2f", temp) + 
                                 " - Acceptes=" + accepted + " - Rejetes=" + rejected +
                                 " - Valides=" + validNeighbors + " - Invalides=" + invalidNeighbors);
            }
        }
        
        System.out.println("Fin Recuit Simule - " + (accepted + rejected) + " tentatives");
        System.out.println("Solution finale: " + String.format("%.2f", best.getObjectiveValue()));
        System.out.println("Voisins valides: " + validNeighbors + ", invalides: " + invalidNeighbors);
        if (accepted + rejected > 0) {
            System.out.println("Taux acceptation: " + String.format("%.2f%%", 
                              100.0 * accepted / (accepted + rejected)));
        }
        
        return best;
    }
    
    private Solution generateNeighbor(Solution current) {
        double rand = random.nextDouble();
        
        Solution neighbor = null;
        int attempts = 0;
        int maxAttempts = 5;
        
        while (neighbor == null && attempts < maxAttempts) {
            try {
                if (rand < 0.50) {
                    neighbor = batchSwap(current);
                } else if (rand < 0.75) {
                    neighbor = operationMove(current);
                } else {
                    neighbor = operationSwitch(current);
                }
            } catch (Exception e) {
                neighbor = null;
            }
            attempts++;
        }
        
        return neighbor;
    }
    
    private Solution batchSwap(Solution current) {
        Solution neighbor = current.clone();
        List<Batch> batches = neighbor.getBatches();
        
        if (batches.size() < 2) return null;
        
        Map<Integer, List<Batch>> machineBatches = new HashMap<>();
        for (Batch b : batches) {
            if (!b.getOperations().isEmpty()) {
                machineBatches.computeIfAbsent(b.getMachineId(), k -> new ArrayList<>()).add(b);
            }
        }
        
        List<Integer> machinesWithMultipleBatches = new ArrayList<>();
        for (Map.Entry<Integer, List<Batch>> entry : machineBatches.entrySet()) {
            if (entry.getValue().size() >= 2) {
                machinesWithMultipleBatches.add(entry.getKey());
            }
        }
        
        if (machinesWithMultipleBatches.isEmpty()) return null;
        
        int machineId = machinesWithMultipleBatches.get(random.nextInt(machinesWithMultipleBatches.size()));
        List<Batch> machineBatchList = machineBatches.get(machineId);
        
        int idx1 = random.nextInt(machineBatchList.size());
        int idx2 = random.nextInt(machineBatchList.size());
        
        if (idx1 == idx2) {
            idx2 = (idx2 + 1) % machineBatchList.size();
        }
        
        Batch b1 = machineBatchList.get(idx1);
        Batch b2 = machineBatchList.get(idx2);
        
        int pos1 = b1.getPosition();
        b1.setPosition(b2.getPosition());
        b2.setPosition(pos1);
        
        machineBatchList.sort(Comparator.comparingInt(Batch::getPosition));
        
        return neighbor;
    }
    
    private Solution operationMove(Solution current) {
        Solution neighbor = current.clone();
        List<Batch> batches = neighbor.getBatches();
        
        List<Batch> candidateBatches = new ArrayList<>();
        for (Batch b : batches) {
            if (b.getOperations().size() >= 2) {
                candidateBatches.add(b);
            }
        }
        
        if (candidateBatches.isEmpty()) return null;
        
        Batch source = candidateBatches.get(random.nextInt(candidateBatches.size()));
        Operation op = source.getOperations().get(random.nextInt(source.getOperations().size()));
        
        source.getOperations().remove(op);
        
        List<Batch> compatibleBatches = new ArrayList<>();
        for (Batch b : batches) {
            if (b != source && b.getMachineId() == source.getMachineId() && 
                b.getRecipe() != null && b.getRecipe().equals(op.getRecipe())) {
                Machine m = problem.getMachine(b.getMachineId());
                if (m != null && b.getOperations().size() < m.getCapacity()) {
                    compatibleBatches.add(b);
                }
            }
        }
        
        if (!compatibleBatches.isEmpty()) {
            Batch target = compatibleBatches.get(random.nextInt(compatibleBatches.size()));
            target.addOperation(op);
        } else {
            source.addOperation(op);
        }
        
        return neighbor;
    }
    
    private Solution operationSwitch(Solution current) {
        Solution neighbor = current.clone();
        List<Batch> batches = neighbor.getBatches();
        
        if (batches.size() < 2) return null;
        
        List<Batch> nonEmptyBatches = new ArrayList<>();
        for (Batch b : batches) {
            if (!b.getOperations().isEmpty()) {
                nonEmptyBatches.add(b);
            }
        }
        
        if (nonEmptyBatches.size() < 2) return null;
        
        Batch b1 = nonEmptyBatches.get(random.nextInt(nonEmptyBatches.size()));
        Batch b2;
        int attempts = 0;
        do {
            b2 = nonEmptyBatches.get(random.nextInt(nonEmptyBatches.size()));
            attempts++;
            if (attempts > 10) return null;
        } while (b1 == b2 || !b1.getRecipe().equals(b2.getRecipe()));
        
        Operation op1 = b1.getOperations().get(random.nextInt(b1.getOperations().size()));
        Operation op2 = b2.getOperations().get(random.nextInt(b2.getOperations().size()));
        
        b1.getOperations().remove(op1);
        b2.getOperations().remove(op2);
        b1.addOperation(op2);
        b2.addOperation(op1);
        
        return neighbor;
    }
}