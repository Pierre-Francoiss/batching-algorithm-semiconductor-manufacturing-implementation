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
        Solution initialSolution = initial.clone();
        
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
                neighbor.rebuildOperationToBatch();
                DisjunctiveGraph graph = new DisjunctiveGraph(problem, neighbor);
                
                if (graph.computeLongestPaths()) {
                    validNeighbors++;
                    neighbor.evaluate();
                    
                    double delta = neighbor.getObjectiveValue() - current.getObjectiveValue();
                    
                    if (iter < 10 && delta != 0) {
                        System.out.println("  Voisin " + iter + ": Obj=" + String.format("%.2f", neighbor.getObjectiveValue()) + 
                                         ", Delta=" + String.format("%.2f", delta));
                    }
                    
                    boolean accept = false;
                    if (delta > 0) {
                        accept = true;
                        accepted++;
                    } else {
                        double prob = Math.exp(delta / temp);
                        if (iter < 10) {
                            System.out.println("    Proba acceptation: " + String.format("%.4f", prob));
                        }
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
                                             String.format("%.2f", best.getObjectiveValue()) +
                                             " (amélioration: " + String.format("%.2f", 
                                             best.getObjectiveValue() - initialSolution.getObjectiveValue()) + ")");
                        }
                    }
                } else {
                    invalidNeighbors++;
                    rejected++;
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
        System.out.println("Solution initiale: " + String.format("%.2f", initialSolution.getObjectiveValue()));
        System.out.println("Solution finale (best): " + String.format("%.2f", best.getObjectiveValue()));
        System.out.println("Solution courante (current): " + String.format("%.2f", current.getObjectiveValue()));
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
        String moveType = "";
        
        while (neighbor == null && attempts < 20) {
            try {
                if (rand < 0.50) {
                    moveType = "BatchMove";
                    neighbor = batchMove(current);
                } else if (rand < 0.75) {
                    moveType = "OperationMove";
                    neighbor = operationMove(current);
                } else {
                    moveType = "OperationSwitch";
                    neighbor = operationSwitch(current);
                }
            } catch (Exception e) {
                System.out.println("Exception in " + moveType + ": " + e.getMessage());
                neighbor = null;
            }
            attempts++;
            if (neighbor == null) {
                rand = random.nextDouble();
            }
        }
        
        if (neighbor == null && attempts >= 20) {
            System.out.println("ECHEC: Impossible de generer un voisin apres 20 tentatives");
        }
        
        return neighbor;
    }
    
    private Solution batchMove(Solution current) {
        Solution neighbor = current.clone();
        List<Batch> batches = neighbor.getBatches();
        
        if (batches.isEmpty()) return null;
        
        Batch batch = batches.get(random.nextInt(batches.size()));
        if (batch.getOperations().isEmpty()) return null;
        
        boolean changeMachine = random.nextDouble() < 0.3;
        
        if (changeMachine) {
            Operation op = batch.getOperations().get(0);
            List<Integer> eligible = op.getEligibleMachines();
            
            if (eligible.size() <= 1) {
                changeMachine = false;
            } else {
                int newMachineId = eligible.get(random.nextInt(eligible.size()));
                if (newMachineId == batch.getMachineId() && eligible.size() > 1) {
                    do {
                        newMachineId = eligible.get(random.nextInt(eligible.size()));
                    } while (newMachineId == batch.getMachineId());
                }
                
                List<Operation> ops = new ArrayList<>(batch.getOperations());
                batches.remove(batch);
                
                Batch newBatch = new Batch(batch.getId(), newMachineId, 0);
                for (Operation o : ops) {
                    newBatch.addOperation(o);
                }
                batches.add(newBatch);
                
                reassignPositions(neighbor, newMachineId);
                return neighbor;
            }
        }
        
        if (!changeMachine) {
            Map<Integer, List<Batch>> machineBatches = new HashMap<>();
            for (Batch b : batches) {
                if (!b.getOperations().isEmpty()) {
                    machineBatches.computeIfAbsent(b.getMachineId(), k -> new ArrayList<>()).add(b);
                }
            }
            
            List<Batch> sameMachineBatches = machineBatches.get(batch.getMachineId());
            if (sameMachineBatches == null || sameMachineBatches.size() <= 1) return null;
            
            sameMachineBatches.sort(Comparator.comparingInt(Batch::getPosition));
            
            int currentIndex = sameMachineBatches.indexOf(batch);
            if (currentIndex < 0) return null;
            
            int newIndex = random.nextInt(sameMachineBatches.size());
            if (newIndex == currentIndex) {
                newIndex = (newIndex + 1) % sameMachineBatches.size();
            }
            
            sameMachineBatches.remove(currentIndex);
            sameMachineBatches.add(newIndex, batch);
            
            for (int i = 0; i < sameMachineBatches.size(); i++) {
                sameMachineBatches.get(i).setPosition(i);
            }
        }
        
        return neighbor;
    }
    
    private Solution operationMove(Solution current) {
        Solution neighbor = current.clone();
        List<Batch> batches = neighbor.getBatches();
        
        List<Batch> candidateBatches = new ArrayList<>();
        for (Batch b : batches) {
            if (b.getOperations().size() >= 1) {
                candidateBatches.add(b);
            }
        }
        
        if (candidateBatches.isEmpty()) return null;
        
        Batch source = candidateBatches.get(random.nextInt(candidateBatches.size()));
        Operation op = source.getOperations().get(random.nextInt(source.getOperations().size()));
        
        source.getOperations().remove(op);
        
        if (random.nextBoolean()) {
            List<Batch> compatibleBatches = new ArrayList<>();
            for (Batch b : batches) {
                if (b != source && b.getRecipe() != null && b.getRecipe().equals(op.getRecipe())) {
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
        } else {
            List<Integer> eligible = op.getEligibleMachines();
            if (!eligible.isEmpty()) {
                int machineId = eligible.get(random.nextInt(eligible.size()));
                Batch newBatch = new Batch(batches.size(), machineId, 0);
                newBatch.addOperation(op);
                batches.add(newBatch);
                reassignPositions(neighbor, machineId);
            } else {
                source.addOperation(op);
            }
        }
        
        if (source.getOperations().isEmpty()) {
            batches.remove(source);
            reassignPositions(neighbor, source.getMachineId());
        }
        
        return neighbor;
    }
    
    private Solution operationSwitch(Solution current) {
        Solution neighbor = current.clone();
        List<Batch> batches = neighbor.getBatches();
        
        Map<String, List<Batch>> recipeGroups = new HashMap<>();
        for (Batch b : batches) {
            if (!b.getOperations().isEmpty() && b.getRecipe() != null) {
                recipeGroups.computeIfAbsent(b.getRecipe(), k -> new ArrayList<>()).add(b);
            }
        }
        
        List<String> recipesWithMultipleBatches = new ArrayList<>();
        for (Map.Entry<String, List<Batch>> entry : recipeGroups.entrySet()) {
            if (entry.getValue().size() >= 2) {
                recipesWithMultipleBatches.add(entry.getKey());
            }
        }
        
        if (recipesWithMultipleBatches.isEmpty()) return null;
        
        String recipe = recipesWithMultipleBatches.get(random.nextInt(recipesWithMultipleBatches.size()));
        List<Batch> sameBatches = recipeGroups.get(recipe);
        
        Batch b1 = sameBatches.get(random.nextInt(sameBatches.size()));
        Batch b2;
        do {
            b2 = sameBatches.get(random.nextInt(sameBatches.size()));
        } while (b1 == b2);
        
        Operation op1 = b1.getOperations().get(random.nextInt(b1.getOperations().size()));
        Operation op2 = b2.getOperations().get(random.nextInt(b2.getOperations().size()));
        
        b1.getOperations().remove(op1);
        b2.getOperations().remove(op2);
        b1.addOperation(op2);
        b2.addOperation(op1);
        
        return neighbor;
    }
    
    private void reassignPositions(Solution solution, int machineId) {
        List<Batch> machineBatches = new ArrayList<>();
        for (Batch b : solution.getBatches()) {
            if (b.getMachineId() == machineId && !b.getOperations().isEmpty()) {
                machineBatches.add(b);
            }
        }
        
        machineBatches.sort(Comparator.comparingInt(Batch::getPosition));
        
        for (int i = 0; i < machineBatches.size(); i++) {
            machineBatches.get(i).setPosition(i);
        }
    }
}