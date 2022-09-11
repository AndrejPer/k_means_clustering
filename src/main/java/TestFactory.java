import java.io.FileWriter;
import java.io.IOException;
import com.opencsv.CSVWriter;


public class TestFactory {
    static int numberOfIterations = 100,
            numberOfRepetitions = 10,
            siteStep = 10000,
            clusterStep = 100,
            siteFixed = 100000,
            clusterFixed = 100,
            id = 0;


    public static void main(String[] args) throws IOException {
        CSVWriter file = new CSVWriter(new FileWriter("test_results.csv"));
        file.writeNext(new String[]{"id", "type", "runtime", "memory", "points", "centeroids"}, false);

        //VARYING SITES
        for(int i = 1; i <= numberOfIterations; i++) {
            int siteCount = i * siteStep;
            //double totalTime = 0, totalMemory = 0;
            for (int j = 0; j < numberOfRepetitions; j++) {
                Computation computation = new Computation(clusterFixed, siteCount);
                computation.compute();
                file.writeNext(new String[]{
                        Integer.toString(id),
                        "sequential",
                        Double.toString(computation.getTime()),
                        Double.toString(computation.getMemory()),
                        Integer.toString(siteCount),
                        Integer.toString(clusterFixed)
                }, false);
                file.flush();
                id++;
            }
        }

        for(int i = 1; i <= numberOfIterations; i++) {
            int siteCount = i * siteStep;
            double totalTime = 0, totalMemory = 0;
            for (int j = 0; j < numberOfRepetitions; j++) {
                ParallelComputation parallel = new ParallelComputation(clusterFixed, siteCount);
                parallel.compute();

                file.writeNext(new String[]{
                        Integer.toString(id),
                        "parallel",
                        Double.toString(parallel.getTime()),
                        Double.toString(parallel.getMemory()),
                        Integer.toString(siteCount),
                        Integer.toString(clusterFixed)
                }, false);
                file.flush();
                id++;
            }
        }


        //VARYING CLUSTERS
        for(int i = 1; i <= numberOfIterations; i++) {
            int clusterCount = i * clusterStep;
            double totalTime = 0, totalMemory = 0;
            for (int j = 0; j < numberOfRepetitions; j++) {
                Computation computation = new Computation(clusterCount, siteFixed);
                computation.compute();

                file.writeNext(new String[]{
                        Integer.toString(id),
                        "sequential",
                        Double.toString(computation.getTime()),
                        Double.toString(computation.getMemory()),
                        Integer.toString(siteFixed),
                        Integer.toString(clusterCount)
                }, false);
                file.flush();
                id++;
            }
        }

        for(int i = 1; i <= numberOfIterations; i++) {
            int clusterCount = i * clusterStep;
            double totalTime = 0, totalMemory = 0;
            for (int j = 0; j < numberOfRepetitions; j++) {
                ParallelComputation parallel = new ParallelComputation(clusterCount, siteFixed);
                parallel.compute();
                totalTime += parallel.getTime();
                totalMemory += parallel.getMemory();

                file.writeNext(new String[]{
                        Integer.toString(id),
                        "parallel",
                        Double.toString(totalTime / numberOfRepetitions),
                        Double.toString(totalMemory / numberOfRepetitions),
                        Integer.toString(siteFixed),
                        Integer.toString(clusterCount)
                }, false);
                file.flush();
                id++;
            }
        }
        file.close();
    }



}
