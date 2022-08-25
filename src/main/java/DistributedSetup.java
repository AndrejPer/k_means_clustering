import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import mpi.*;

public class DistributedSetup {
    ArrayList<Site> sitePoints;
    private int clusterCount, siteCount, time, loopCounter;
    ArrayList<Cluster> clusters;
    String[] args;

    public DistributedSetup(int clusterCount, int site, String[] args) {
        this.clusterCount = clusterCount;
        siteCount = site;
        sitePoints = SiteLoader.getInstance().loadSites(siteCount);
        clusters = new ArrayList<Cluster>(clusterCount);
        this.args = args;
    }

    public ArrayList<Cluster> getClusters() {
        return clusters;
    }

    public int getTime() {
        return time;
    }

    public int getLoopCounter() {return loopCounter;}

    public ArrayList<Site> getSetPoints() {
        return sitePoints;
    }
    public void compute() throws Exception {
        System.out.println("distr comp");
        //DistributedComputation computation = new DistributedComputation(args, 3, 1000);




    }

}
