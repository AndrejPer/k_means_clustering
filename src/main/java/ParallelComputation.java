import java.awt.*;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ParallelComputation {

    private ArrayList<Site> sitePoints;
    private final ArrayList<Cluster> clusters;
    private int clusterCount, siteCount, processorCount, chunkSize, time, loopCounter;
    private long memory;
    static boolean[] changeFlags, assignmentFlags;
    private ExecutorService executorBind, executorCluster;
    boolean[] siteFlags;
    boolean assignmentFlag;

    //constructor
    public ParallelComputation(int cluster,int site) {
        clusterCount = cluster;
        assignmentFlags = new boolean[siteCount];
        siteCount = site;
        clusters = new ArrayList<>();
        processorCount = 20; //Runtime.getRuntime().availableProcessors();
        changeFlags = new boolean[processorCount];
        this.executorBind = Executors.newFixedThreadPool(processorCount);
        this.executorCluster = Executors.newFixedThreadPool(clusterCount);
        chunkSize = (int) Math.ceil((double) siteCount/processorCount);
        siteFlags = new boolean[siteCount];
    }

    //getters
    public ArrayList<Cluster> getClusters() { return clusters;}
    public ArrayList<Site> getSetPoints() { return sitePoints;}
    public int getTime() { return time;}
    public int getLoopCounter() {
        return loopCounter;
    }
    public long getMemory() {return memory;}

    //computation
    public void compute(String[] args) {
        //initializing data points
        sitePoints = SiteLoader.getInstance().loadSites(siteCount);


        //initializing cluster centers to random sites
        Random rand = new Random(10);
        //using set to assure no repeating numbers
        HashSet<Integer> randInts = new HashSet<>();
        //get random non-repeating indices
        while(randInts.size() < clusterCount) {
            randInts.add(rand.nextInt(sitePoints.size()));
        }

        // GUI-RELATED COLOR SETTING
        //create a set of colors of corresponding size for coloring the clusters
        HashSet<Color> colors = new HashSet<>();
        for (int i = 0; i < clusterCount; i++) {
            colors.add(new Color(rand.nextFloat(), rand.nextFloat(), rand.nextFloat()));
        }
        Iterator<Color> colorIterator = colors.iterator();


        //add sites at given indexes as initial clusters
        int id = 0;
        for (Integer i: randInts) {
            clusters.add(new Cluster(sitePoints.get(i), colorIterator.next(), id++));
        }


        //calculating k means
        loopCounter = 0;
        boolean changed;
        Timestamp start = new Timestamp(System.currentTimeMillis());

        //TODO check if start of memory measuring is ok
        //--------------- memory start
        long startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        //-------------------------------


         do {
             loopCounter++;

             //re-setting update flags to false
             Arrays.fill(changeFlags, false);

             //assignment step
             bindCluster();


             //stopping condition check here
             changed = false;
             for (boolean changeFlag : changeFlags) {
                 if (changeFlag) {
                     changed = true;
                     break;
                 }
             }

             //only in case changes happened in the assignment step
             if (changed) {
                 //loop through sites sequentially and add one by one to avoid race condition
                 for (Site site : sitePoints) {
                     clusters.get(site.getClusterID()).addSite(site);
                 }

                 //update step
                 updateCentroid();
             }



         } while(changed);

        //TODO check if start of memory measuring is ok
        //--------------- memory start
        long endMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        //-------------------------------

        memory = endMemory - startMemory;

        Timestamp end = new Timestamp(System.currentTimeMillis());
        time = (int) (end.getTime() - start.getTime());
        executorBind.shutdown();
        executorCluster.shutdown();


    }

    void bindCluster() {
        CountDownLatch barrierBind = new CountDownLatch(processorCount);
        //re-initializing cluster's site array
        for (Cluster cluster: clusters) {
            cluster.setSites(new ArrayList<>());
        }
        //assignment
        for(int t = 0; t < processorCount; t++ ) {
            BindClusterRunnable bindClusterRunnable = new BindClusterRunnable(sitePoints, clusters, barrierBind, t * chunkSize, Math.min((t + 1) * chunkSize, siteCount), t);
            executorBind.execute(bindClusterRunnable);
        }
        try {
            barrierBind.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    class BindClusterRunnable implements Runnable {
        int threadID, start, end;
        CountDownLatch barrierBind;
        ArrayList<Site> sitePoints;
        ArrayList<Cluster> clusters;

        public BindClusterRunnable(ArrayList<Site> sitePoints, ArrayList<Cluster> clusters, CountDownLatch barrierBind, int start, int end, int threadID) {
            this.sitePoints = sitePoints;
            this.clusters = clusters;
            this.barrierBind = barrierBind;
            this.start = start;
            this.end = end;
            this.threadID = threadID;
        }

        @Override
        public void run() {
            for(int i = start; i < end; i++) {
                Site site = sitePoints.get(i);
                Cluster currentCluster = clusters.get(site.getClusterID());
                double min = Double.MAX_VALUE;
                Cluster minCluster = clusters.get(0);
                for (Cluster cluster: clusters) {
                    double distance = Math.sqrt(Math.pow((cluster.getCentroid().getLatitude() - site.getLatitude()), 2.0) + Math.pow((cluster.getCentroid().getLongitude() - site.getLongitude()), 2.0));
                    if(distance < min) {
                        min = distance;
                        minCluster = cluster;
                    }
                }
                //verify stopping condition
                //no need to worry about race condition, they access disjoint set;
                //initial site's cluster ID should be 0
                if(minCluster != currentCluster) {
                    int id = start / chunkSize;
                    if(!changeFlags[id]) {
                        changeFlags[id] = true;
                    }
                }

                //binding site to cluster
                site.setClusterID(minCluster.getClusterID());
            }
            barrierBind.countDown();
        }
    }

    public void updateCentroid() {
        CountDownLatch barrierUpdate = new CountDownLatch(clusterCount);
        
        for (Cluster cluster: clusters) {
            UpdateCentroidRunnable update = new UpdateCentroidRunnable(barrierUpdate, cluster);
            executorCluster.execute(update);
        }
        try {
            barrierUpdate.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    static class UpdateCentroidRunnable implements Runnable {
        CountDownLatch barrierUpdate;
        Cluster cluster;

        public UpdateCentroidRunnable(CountDownLatch barrierUpdate, Cluster cluster) {
            this.barrierUpdate = barrierUpdate;
            this.cluster = cluster;
        }

        @Override
        public void run() {
            cluster.updateCentroid();
            barrierUpdate.countDown();
        }
    }
}

