import java.util.*;

public class Main {
    
    /* Testing tools methods */
    private static void printSolution(String label, Solution s) {
        System.out.println(label + ":");
        System.out.println("  Goal: " + String.format("%.2f", s.getObjectiveValue()));
        System.out.println("  fMov: " + String.format("%.2f", s.getfMov()));
        System.out.println("  fBatch: " + String.format("%.4f", s.getfBatch()));
        System.out.println("  fXFac: " + String.format("%.2f", s.getfXFac()));
        System.out.println("  Batches: " + s.getBatches().size());
    }
    
    private static void printIndicatorComparison(String name, double initial, double finale) {
        double change = ((finale - initial) / Math.abs(initial)) * 100;
        System.out.println("  " + name + ": " + String.format("%.2f", initial) + " -> " + 
                          String.format("%.2f", finale) + " (" + 
                          String.format("%.2f%%", change) + ")");
    }
    
    private static Problem createTestProblem() {
        Problem problem = new Problem(1440);
        
        String[] recipes = new String[50];
        for (int i = 0; i < 50; i++) {
            recipes[i] = "R" + (i + 1);
        }
        
        List<Machine> cleaningMachines = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            List<String> qualifiedRecipes = new ArrayList<>();
            int numRecipes = 8 + new Random(i).nextInt(12);
            for (int j = 0; j < numRecipes; j++) {
                qualifiedRecipes.add(recipes[(i * 3 + j) % 50]);
            }
            Machine m = new Machine(i, 2 + new Random(i).nextInt(3), 10 + new Random(i).nextInt(20), 
                                   10 + new Random(i).nextInt(20), 5, qualifiedRecipes);
            cleaningMachines.add(m);
            problem.addMachine(m);
        }
        
        List<Machine> furnaces = new ArrayList<>();
        for (int i = 13; i <= 82; i++) {
            List<String> qualifiedRecipes = new ArrayList<>();
            int numRecipes = 5 + new Random(i).nextInt(10);
            for (int j = 0; j < numRecipes; j++) {
                qualifiedRecipes.add(recipes[(i * 2 + j) % 50]);
            }
            Machine m = new Machine(i, 4 + new Random(i).nextInt(3), 15 + new Random(i).nextInt(15), 
                                   15 + new Random(i).nextInt(15), 5 + new Random(i).nextInt(10), 
                                   qualifiedRecipes);
            furnaces.add(m);
            problem.addMachine(m);
        }
        
        Random random = new Random(42);
        for (int i = 0; i < 700; i++) {
            int priority = random.nextInt(10) + 1;
            int releaseDate = random.nextInt(240);
            
            Job job = new Job(i, releaseDate, priority, 25);
            
            String recipe1 = recipes[random.nextInt(50)];
            List<Integer> eligibleCleaning = new ArrayList<>();
            for (Machine m : cleaningMachines) {
                if (m.canProcess(recipe1)) {
                    eligibleCleaning.add(m.getId());
                }
            }
            if (eligibleCleaning.isEmpty()) {
                eligibleCleaning.add(1 + random.nextInt(12));
            }
            
            Operation op1 = new Operation(i * 10, job, 0, 20 + random.nextInt(20), recipe1, eligibleCleaning);
            op1.setTimeLags(10, 240);
            job.addOperation(op1);
            
            String recipe2 = recipes[random.nextInt(50)];
            List<Integer> eligibleFurnaces = new ArrayList<>();
            for (Machine m : furnaces) {
                if (m.canProcess(recipe2)) {
                    eligibleFurnaces.add(m.getId());
                }
            }
            if (eligibleFurnaces.isEmpty()) {
                eligibleFurnaces.add(13 + random.nextInt(70));
            }
            
            int duration2 = 180 + random.nextInt(420);
            Operation op2 = new Operation(i * 10 + 1, job, 1, duration2, recipe2, eligibleFurnaces);
            op2.setTimeLags(5, 240);
            job.addOperation(op2);
            
            if (random.nextDouble() > 0.7) {
                String recipe3 = recipes[random.nextInt(50)];
                eligibleFurnaces = new ArrayList<>();
                for (Machine m : furnaces) {
                    if (m.canProcess(recipe3)) {
                        eligibleFurnaces.add(m.getId());
                    }
                }
                if (eligibleFurnaces.isEmpty()) {
                    eligibleFurnaces.add(13 + random.nextInt(70));
                }
                
                int duration3 = 120 + random.nextInt(300);
                Operation op3 = new Operation(i * 10 + 2, job, 2, duration3, recipe3, eligibleFurnaces);
                op3.setTimeLags(5, 240);
                job.addOperation(op3);
            }
            
            problem.addJob(job);
        }
        
        return problem;
    }

    /* Global simulation */
    public static void main(String[] args) {
        System.out.println("Java inplementation of the Simulated annealing method described in the article: A batching and scheduling algorithm for the diffusion area in semiconductor manufacturing\n");
        
        Problem problem = createTestProblem();
        System.out.println("Problem: " + problem.getJobs().size() + " jobs, " + 
                          problem.getMachines().size() + " machines\n");
        
        long startTime = System.currentTimeMillis();
        InitialSolution builder = new InitialSolution(problem);
        Solution initial = builder.build();
        long buildTime = System.currentTimeMillis() - startTime;
        
        System.out.println("Initial solution build in " + buildTime + " ms");
        printSolution("Initial", initial);
        

        // SIMULATION 1: Article values
        System.out.println("SIMULATION 1: T0=5000, alpha=0.95");
        SimulatedAnnealing sa1 = new SimulatedAnnealing(problem, 5000.0, 0.95, 50000);
        startTime = System.currentTimeMillis();
        Solution finale1 = sa1.solve(initial);
        long saTime1 = System.currentTimeMillis() - startTime;
        
        System.out.println("execution time SA: " + saTime1 + " ms");
        printSolution("Final 1", finale1);
        
        double improvement1 = ((finale1.getObjectiveValue() - initial.getObjectiveValue()) 
                             / Math.abs(initial.getObjectiveValue())) * 100;
        System.out.println("Improvement: " + String.format("%.2f%%", improvement1));
        
        System.out.println("\nindicators:");
        printIndicatorComparison("fMov", initial.getfMov(), finale1.getfMov());
        printIndicatorComparison("fBatch", initial.getfBatch(), finale1.getfBatch());
        printIndicatorComparison("fXFac", initial.getfXFac(), finale1.getfXFac());
        
        // SIMULATION 2: Adjusted values for articles results
        System.out.println("\nSIMULATION 2: T0=5000000, alpha=0.995\n");
        SimulatedAnnealing sa2 = new SimulatedAnnealing(problem, 5000000.0, 0.995, 50000);
        startTime = System.currentTimeMillis();
        Solution finale2 = sa2.solve(initial);
        long saTime2 = System.currentTimeMillis() - startTime;
        
        System.out.println("execution time SA: " + saTime2 + " ms");
        printSolution("Final 2", finale2);
        
        double improvement2 = ((finale2.getObjectiveValue() - initial.getObjectiveValue()) 
                             / Math.abs(initial.getObjectiveValue())) * 100;
        System.out.println("improvement: " + String.format("%.2f%%", improvement2));
        
        System.out.println("\nindicators:");
        printIndicatorComparison("fMov", initial.getfMov(), finale2.getfMov());
        printIndicatorComparison("fBatch", initial.getfBatch(), finale2.getfBatch());
        printIndicatorComparison("fXFac", initial.getfXFac(), finale2.getfXFac());
        
        // COMPARAISON
        System.out.println("\nComparaison bitween artcile values (temperature and cooling rate) and adjusted values:\n");
        System.out.println("Simulation 1 (5000, 0.95):");
        System.out.println("  - goal: " + String.format("%.2f", finale1.getObjectiveValue()));
        System.out.println("  - improvement: " + String.format("%.2f%%", improvement1));
        System.out.println("  - time: " + saTime1 + " ms");
        System.out.println("\nSimulation 2 (5000000, 0.995):");
        System.out.println("  - goal: " + String.format("%.2f", finale2.getObjectiveValue()));
        System.out.println("  - improvement: " + String.format("%.2f%%", improvement2));
        System.out.println("  - time: " + saTime2 + " ms");
    }
    
   
}