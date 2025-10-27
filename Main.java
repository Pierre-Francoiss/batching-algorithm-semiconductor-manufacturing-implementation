import java.util.*;

public class Main {
    
    public static void main(String[] args) {
        System.out.println("=== Simulated Annealing - Batching & Scheduling ===\n");
        
        Problem problem = createTestProblem();
        System.out.println("Probleme: " + problem.getJobs().size() + " jobs, " + 
                          problem.getMachines().size() + " machines\n");
        
        long startTime = System.currentTimeMillis();
        InitialSolution builder = new InitialSolution(problem);
        Solution initial = builder.build();
        long buildTime = System.currentTimeMillis() - startTime;
        
        System.out.println("Solution initiale construite en " + buildTime + " ms");
        printSolution("Initiale", initial);
        System.out.println();
        
        SimulatedAnnealing sa = new SimulatedAnnealing(problem, 1000.0, 0.95, 5000);
        startTime = System.currentTimeMillis();
        Solution finale = sa.solve(initial);
        long saTime = System.currentTimeMillis() - startTime;
        
        System.out.println("\nTemps execution SA: " + saTime + " ms");
        printSolution("Finale", finale);
        
        double improvement = ((finale.getObjectiveValue() - initial.getObjectiveValue()) 
                             / Math.abs(initial.getObjectiveValue())) * 100;
        System.out.println("\nAmelioration globale: " + String.format("%.2f%%", improvement));
        
        System.out.println("\nDetails indicateurs:");
        printIndicatorComparison("fMov", initial.getfMov(), finale.getfMov());
        printIndicatorComparison("fBatch", initial.getfBatch(), finale.getfBatch());
        printIndicatorComparison("fXFac", initial.getfXFac(), finale.getfXFac());
    }
    
    private static void printSolution(String label, Solution s) {
        System.out.println(label + ":");
        System.out.println("  Objectif: " + String.format("%.2f", s.getObjectiveValue()));
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
        
        String[] recipes = {"R1", "R2", "R3"};
        
        Machine m1 = new Machine(1, 4, 10, 10, 5, Arrays.asList("R1", "R2"));
        Machine m2 = new Machine(2, 4, 10, 10, 5, Arrays.asList("R1", "R2", "R3"));
        Machine m3 = new Machine(3, 6, 15, 15, 10, Arrays.asList("R1"));
        Machine m4 = new Machine(4, 6, 15, 15, 10, Arrays.asList("R2"));
        Machine m5 = new Machine(5, 4, 15, 15, 10, Arrays.asList("R3"));
        Machine m6 = new Machine(6, 6, 15, 15, 10, Arrays.asList("R1", "R2", "R3"));
        
        problem.addMachine(m1);
        problem.addMachine(m2);
        problem.addMachine(m3);
        problem.addMachine(m4);
        problem.addMachine(m5);
        problem.addMachine(m6);
        
        Random random = new Random(42);
        for (int i = 0; i < 30; i++) {
            int priority = random.nextInt(10) + 1;
            int releaseDate = random.nextInt(60);
            
            Job job = new Job(i, releaseDate, priority, 25);
            
            String recipe1 = recipes[random.nextInt(recipes.length)];
            Operation op1 = new Operation(i * 10, job, 0, 30, recipe1, Arrays.asList(1, 2));
            op1.setTimeLags(10, 120);
            job.addOperation(op1);
            
            String recipe2 = recipes[random.nextInt(recipes.length)];
            Operation op2 = new Operation(i * 10 + 1, job, 1, 180, recipe2, Arrays.asList(3, 4, 5, 6));
            op2.setTimeLags(0, Integer.MAX_VALUE);
            job.addOperation(op2);
            
            problem.addJob(job);
        }
        
        return problem;
    }
}