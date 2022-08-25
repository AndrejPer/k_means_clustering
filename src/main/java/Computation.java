import java.awt.*;
import java.sql.Timestamp;
import java.util.*;

public class Computation {
    ArrayList<Site> sitePoints;
    int clusterCount, siteCount, loopCounter;
    ArrayList<Cluster> clusters;
    private int time;

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

    public void compute() {
        //initializing cluster centers to random sites
        Random rand = new Random();
        //using set to assure no repeating numbers
        HashSet<Integer> randInts = new HashSet<>();
        //get random non-repeating indices
        while(randInts.size() < clusterCount) {
            randInts.add(rand.nextInt(sitePoints.size()));
        }
        //create a set of colors of corresponding size for coloring the clusters
        HashSet<Color> colors = new HashSet<>();
        for (int i = 0; i < clusterCount; i++) {
            colors.add(new Color(rand.nextFloat(), rand.nextFloat(), rand.nextFloat()));
        }
        //add sites at given indexes as initial clusters
        int id = 0;
        Iterator<Color> colorIterator = colors.iterator();
        for (Integer i: randInts) {
            clusters.add(new Cluster(sitePoints.get(i), colorIterator.next(), id++));
        }


        loopCounter = 0;
        boolean changed;
        Timestamp start = new Timestamp(System.currentTimeMillis());
         do {
             //initializing "changed" flag to false at the beginning of each iteration
             changed = false;
             if(loopCounter > 10000) break;
             loopCounter++;
             changed = bindCluster();

             //update step
             for (Cluster cluster: clusters) cluster.updateCentroid();

        } while(changed);
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
            for (Cluster cluster: clusters) {
                double distance = Math.sqrt(Math.pow((cluster.getCentroid().getLatitude() - site.getLatitude()), 2.0) + Math.pow((cluster.getCentroid().getLongitude() - site.getLongitude()), 2.0));

                if(distance < min) {
                    min = distance;
                    minCluster = cluster;
                }
            }
            //check stopping cond
            if(minCluster.getClusterID() != currentCluster.getClusterID()) {
                flag = true;
            }


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
