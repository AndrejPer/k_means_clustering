import java.awt.*;
import java.sql.Timestamp;
import java.util.*;

public class Computation {
    static double r = 6371.0; //Earth's radius
    ArrayList<Site> sitePoints;
    int clusterCount, siteCount, loopCounter;
    ArrayList<Cluster> clusters;
    private int time;
    private long memory;


    public Computation(int cluster,int site) {
        clusterCount = cluster;
        siteCount = site;
        sitePoints = SiteLoader.getInstance().loadSites(siteCount);
        clusters = new ArrayList<>();
    }

    public ArrayList<Cluster> getClusters() {
        return clusters;
    }

    public ArrayList<Site> getSitePoints() {
        return sitePoints;
    }

    public int getTime() {
        return time;
    }

    public long getMemory() {return memory;}

    public void compute() {


        //initializing cluster centers to random sites
        Random rand = new Random(10);
        //using set to assure no repeating numbers
        HashSet<Integer> randInts = new HashSet<>();
        //get random non-repeating indices
        while(randInts.size() < clusterCount) {
            randInts.add(rand.nextInt(sitePoints.size()));
        }
        //GUI RELATED COLORING OF CLUSTERS
        //create a set of colors of corresponding size for coloring the clusters
        HashSet<Color> colors = new HashSet<>();
        for (int i = 0; i < clusterCount; i++) {
            colors.add(new Color(rand.nextFloat(), rand.nextFloat(), rand.nextFloat()));
        }
        Iterator<Color> colorIterator = colors.iterator();



        //add sites at given indexes as initial clusters
        int id = 0;
        for (Integer i: randInts) {
            clusters.add(new Cluster(sitePoints.get(i), colorIterator.next(), id++)); //adding color blue as default
        }



        loopCounter = 0;
        boolean changed;
        Timestamp start = new Timestamp(System.currentTimeMillis());

        //TODO check if start of memory measuring is ok
        //--------------- memory start
        long startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        //-------------------------------
         do {
             //initializing "changed" flag to false at the beginning of each iteration
             changed = false;
             //if(loopCounter > 10000) break;
             loopCounter++;
             changed = bindCluster();

             //update step
             if(changed) for (Cluster cluster: clusters) cluster.updateCentroid();

        } while(changed);

        //TODO check if end of memory measuring is ok
        //--------------- memory end
        long endMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        //-------------------------------

        memory = endMemory - startMemory;

        Timestamp end = new Timestamp(System.currentTimeMillis());
        time = (int) (end.getTime() - start.getTime());
    }

    boolean bindCluster() {
        boolean flag = false;
        for (Cluster cluster: clusters) {
            cluster.setSites(new ArrayList<Site>());
        }
        //assignment step
        for (Site site: sitePoints) {

            double min = Double.MAX_VALUE;
            Cluster currentCluster = clusters.get(site.getClusterID());
            Cluster minCluster = currentCluster;
            double phi1 = site.getLatitude() * Math.PI / 180.0, phi2, //latitudes
                    lambda1 = site.getLongitude() * Math.PI / 180.0, lambda2; //longitudes
            for (Cluster cluster: clusters) {
                phi2 = cluster.getCentroid().getLatitude() * Math.PI / 180.0;
                lambda2 = cluster.getCentroid().getLongitude() * Math.PI / 180.0;
                //double distance = Math.sqrt(Math.pow((cluster.getCentroid().getLatitude() - site.getLatitude()), 2.0) + Math.pow((cluster.getCentroid().getLongitude() - site.getLongitude()), 2.0));
                double haversine = 2 * r * Math.asin(Math.sqrt(Math.pow(Math.sin((phi2 - phi1)/2), 2) + Math.cos(phi1) * Math.cos(phi2) * Math.pow(Math.sin(lambda2 - lambda1), 2)));

                if(haversine < min) {
                    min = haversine;
                    minCluster = cluster;
                }
            }
            //check stopping cond
            if(minCluster != currentCluster) flag = true;

            //binding site and cluster
            minCluster.getSites().ensureCapacity(siteCount/2); minCluster.addSite(site);
            site.setClusterID(minCluster.getClusterID());
        }
        return flag;
    }

    public int getLoopCounter() {
        return loopCounter;
    }
}
