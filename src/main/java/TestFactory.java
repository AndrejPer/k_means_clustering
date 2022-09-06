import java.io.FileWriter;
import java.io.IOException;
import com.opencsv.CSVWriter;


public class TestFactory {
    static int numberOfIterations = 100,
            siteStep = 10000,
            clusterStep = 100,
            siteFixed = 100000,
            clusterFixed = 100,
            id = 200;

    /*
    TestFactory(int numberOfRepetitions, int numberOfIterations, int siteStep, int clusterStep, int siteFixed, int clusterFixed) {
        this.clusterFixed = clusterFixed;
        this.siteFixed = siteFixed;
        this.numberOfIterations = numberOfIterations;
        this.numberOfRepetitions = numberOfRepetitions;
        this.siteStep = siteStep;
        this.clusterStep = clusterStep;
    }
     */

    public static void main(String[] args) throws IOException {
        CSVWriter file = new CSVWriter(new FileWriter("test_results.csv"));
        file.writeNext(new String[]{"id", "type", "runtime", "memory", "points", "centeroids"}, false);

        //varying sites
        /*
        for(int i = 1; i <= numberOfIterations; i++) {
            //System.out.println("seq loop " + i);
            //call Seq with [i]*[siteStep] [numberOfRepetitions] number of times
            int siteCount = i * siteStep;
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


        for(int i = 1; i <= numberOfIterations; i++) {
            //System.out.println("para loop " + i);
            int siteCount = i * siteStep;
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
        */


        //varying clusters
        for(int i = 1; i <= numberOfIterations; i++) {
            //System.out.println("seq loop " + i);
            int clusterCount = i * clusterStep;
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

        for(int i = 1; i <= numberOfIterations; i++) {
            //System.out.println("para loop " + i);
            int clusterCount = i * clusterStep;
            ParallelComputation parallel = new ParallelComputation(clusterCount, siteFixed);
            parallel.compute();

            file.writeNext(new String[]{
                    Integer.toString(id),
                    "parallel",
                    Double.toString(parallel.getTime()),
                    Double.toString(parallel.getMemory()),
                    Integer.toString(siteFixed),
                    Integer.toString(clusterCount)
            }, false);
            file.flush();
            id++;
        }
        file.close();
    }
}
