import java.io.File;

public class TestFactory {
    int numberOfRepetitions, numberOfIterations, siteStep, clusterStep, siteFixed, clusterFixed;
    File file;

    TestFactory(int numberOfRepetitions, int numberOfIterations, int siteStep, int clusterStep, int siteFixed, int clusterFixed) {
        this.clusterFixed = clusterFixed;
        this.siteFixed = siteFixed;
        this.numberOfIterations = numberOfIterations;
        this.numberOfRepetitions = numberOfRepetitions;
        this.siteStep = siteStep;
        this.clusterStep = clusterStep;
    }

    void run() {
        //varying sites
        for(int i = 1; i <= numberOfIterations; i++) {
            //call Seq with [i]*[siteStep] [numberOfRepetitions] number of times
            for(int j = 1; j <= numberOfRepetitions; j++) {
                Computation computation = new Computation(clusterFixed, i*siteStep);
            }
        }

        for(int i = 1; i <= numberOfIterations; i++) {
            //call Seq with [i]*[siteStep] [numberOfRepetitions] number of times
            for(int j = 1; j <= numberOfRepetitions; j++) {
                ParallelComputation parallel = new ParallelComputation(clusterFixed, i*siteStep);
            }
        }

        for(int i = 1; i <= numberOfIterations; i++) {
            //call Seq with [i]*[siteStep] [numberOfRepetitions] number of times
            for(int j = 1; j <= numberOfRepetitions; j++) {
                //DistributedComputation computation = new Computation(clusterFixed, i*siteStep);
            }
        }


        //varying clusters
        for(int i = 1; i <= numberOfIterations; i++) {
            //call Seq with [i]*[siteStep] [numberOfRepetitions] number of times
            for(int j = 1; j <= numberOfRepetitions; j++) {
                Computation computation = new Computation(i*clusterStep, siteFixed);
            }
        }

        for(int i = 1; i <= numberOfIterations; i++) {
            //call Seq with [i]*[siteStep] [numberOfRepetitions] number of times
            for(int j = 1; j <= numberOfRepetitions; j++) {
                ParallelComputation parallel = new ParallelComputation(i*clusterStep, siteFixed);
            }
        }

        for(int i = 1; i <= numberOfIterations; i++) {
            //call Seq with [i]*[siteStep] [numberOfRepetitions] number of times
            for(int j = 1; j <= numberOfRepetitions; j++) {
                //Computation computation = new Computation(i*clusterStep, siteFixed);
            }
        }
    }
}
