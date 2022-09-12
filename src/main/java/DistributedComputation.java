import com.opencsv.CSVWriter;
import mpi.MPI;
import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.*;

public class DistributedComputation {
    static ArrayList<Site> sitePoints;
    static ArrayList<Cluster> clusters;
    private static int time, rank, commSize, siteCount, clusterCount;
    private static long memory, startMemory, endMemory;
    private static int loopCounter;
    private static double[] centroidBuffer, siteBuffer;
    private static int[] centroidChunkSizes, centroidDispls, siteChunkSizes, siteDispls;
    private static boolean[] updateFlags, continuing;
    //params: 100 10 10000 100 100000 100


    public static void main(String[] args) throws IOException {

        //FOR RUNNING IN INTELLIJ
        int numberOfIterations = Integer.parseInt(args[6]);
        int numberOfRepetitions = Integer.parseInt(args[7]);
        int siteStep = Integer.parseInt(args[8]);
        int clusterStep = Integer.parseInt(args[9]);
        int siteFixed = Integer.parseInt(args[10]);
        int clusterFixed = Integer.parseInt(args[11]);



        MPI.Init(args);
        rank = MPI.COMM_WORLD.Rank();
        commSize = MPI.COMM_WORLD.Size();

        CSVWriter file = new CSVWriter(new FileWriter("distributed_test_results.csv"));

        if(Objects.equals(Thread.currentThread().getName(), "0")) {
            file.writeNext(new String[]{"id", "type", "runtime", "memory", "points", "centeroids"}, false);
        }

        int id = 4000; //since 0 - 3999 are ids of sequential and parallel computations

        //VARYING SITES
        for(int i = 1; i <= numberOfIterations; i++) {
            siteCount = i * siteStep;
            //repeating calculation with same parameters to smooth out the outliers
            prep(siteCount, clusterFixed);
            if (rank == 0) load(siteCount, clusterFixed);
            for (int j = 0; j < numberOfRepetitions; j++) {
                compute(args, siteCount, clusterFixed);

                if (rank == 0) {
                    file.writeNext(new String[]{
                            Integer.toString(id),
                            "distributed",
                            Double.toString(time),
                            Double.toString(memory),
                            Integer.toString(siteCount),
                            Integer.toString(clusterFixed)
                    }, false);
                    file.flush();
                    id++;
                }
            }
        }

        //VARYING CLUSTERS
        for(int i = 1; i <= numberOfIterations; i++) {
            clusterCount = i * clusterStep;
            prep(siteFixed, clusterCount);
            if(rank == 0) load(siteFixed, clusterCount);
            //repeating calculation with same parameters to smooth out the outliers
            for (int j = 0; j < numberOfRepetitions; j++) {
                compute(args, siteFixed, clusterCount);

                if (rank == 0) {
                    file.writeNext(new String[]{
                            Integer.toString(id),
                            "distributed",
                            Double.toString(time),
                            Double.toString(memory),
                            Integer.toString(siteFixed),
                            Integer.toString(clusterCount),
                    }, false);
                    file.flush();
                    id++;
                }
            }
        }
        file.close();

        MPI.Finalize();
    }

    static void compute(String[] args, int siteCount, int clusterCount) {

        double[] siteElements = new double[siteChunkSizes[rank]];
        double[] clusterElements = new double[centroidChunkSizes[rank]];
        boolean[] flag = new boolean[1];


        loopCounter = 0;
        Timestamp start = new Timestamp(System.currentTimeMillis());
        do {
            //TODO check if start of memory measuring is ok
            //--------------- memory start
            if(rank == 0) startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            //-------------------------------

            if (rank == 0) loopCounter++;

            updateFlags = new boolean[commSize];
            continuing[0] = false;
            flag[0] = true;


            //send all clusters to all
            MPI.COMM_WORLD.Bcast(centroidBuffer, 0, centroidBuffer.length, MPI.DOUBLE, 0);
            //send chunks of sites to respective proc
            MPI.COMM_WORLD.Scatterv(siteBuffer, 0, siteChunkSizes, siteDispls, MPI.DOUBLE, siteElements, 0, siteChunkSizes[rank], MPI.DOUBLE, 0);
            //send flags to respective process
            MPI.COMM_WORLD.Scatter(updateFlags, 0, 1, MPI.BOOLEAN, flag, 0, 1, MPI.BOOLEAN, 0);




            //BINDING STEP
            for (int i = 0; i < siteElements.length; i += 5) {
                //System.out.print("looking into: " + siteElements[i] + ", "  + siteElements[i + 1] + ", "  + siteElements[i + 2] + ", "  + siteElements[i+3] + ", "  + siteElements[i + 4] + " |");

                double minClusterID = -1.0;
                double min = Double.MAX_VALUE;
                for (int j = 0; j < centroidBuffer.length; j += 3) {
                    double distance = Math.sqrt(Math.pow((centroidBuffer[j + 1] - siteElements[i + 1]), 2) + Math.pow((centroidBuffer[j + 2] - siteElements[i + 2]), 2));
                    if (distance < min) {
                        min = distance;
                        minClusterID = j / 3.0;
                    }

                }
                if(!flag[0]) {
                    if(siteElements[i + 4] != minClusterID) flag[0] = true;
                }



                siteElements[i + 4] = minClusterID;

            }

            MPI.COMM_WORLD.Allgatherv(siteElements, 0, siteElements.length, MPI.DOUBLE, siteBuffer, 0, siteChunkSizes, siteDispls, MPI.DOUBLE);


            //STOPPING CONDITION
            MPI.COMM_WORLD.Gather(flag, 0, 1, MPI.BOOLEAN, updateFlags, 0, 1, MPI.BOOLEAN, 0);
            if (rank == 0) {
                for (boolean updateFlag : updateFlags) {
                    if (updateFlag) {
                        continuing[0] = true;
                        break;
                    }
                }
            }
            MPI.COMM_WORLD.Bcast(continuing, 0, 1, MPI.BOOLEAN, 0);
            if(!continuing[0]) break;

            //UPDATE STEP
            MPI.COMM_WORLD.Scatterv(centroidBuffer, 0, centroidChunkSizes, centroidDispls, MPI.DOUBLE, clusterElements, 0, centroidChunkSizes[rank], MPI.DOUBLE, 0);


            for (int i = 0; i < clusterElements.length; i += 3) { //iterating through cluster
                double sumX = 0, sumY = 0, weight = 0;
                for (int j = 0; j < siteBuffer.length; j += 5) { //incrementing by 5, since each group of five elements is data for one site
                    //check if current site belongs to given cluster
                    if ((int) siteBuffer[j + 4] == clusterElements[i]) {
                        sumX += siteBuffer[j + 1] * siteBuffer[j + 3];
                        sumY += siteBuffer[j + 2] * siteBuffer[j + 3];
                        weight += siteBuffer[j + 3];
                    }
                }
                clusterElements[i + 1] = sumX / weight;
                clusterElements[i + 2] = sumY / weight;
            }


            MPI.COMM_WORLD.Gatherv(clusterElements, 0, clusterElements.length, MPI.DOUBLE, centroidBuffer, 0, centroidChunkSizes, centroidDispls, MPI.DOUBLE, 0);

        } while (continuing[0]); //could put true, since break is introduced in line 157

        if (rank == 0) {

            //TODO check if end of memory measuring is OK
            //------------- memory end
            endMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            //-------------------------

            Timestamp end = new Timestamp(System.currentTimeMillis());
            memory = endMemory - startMemory;
            time = (int) (end.getTime() - start.getTime());

            //BACK TO CLUSTERS
            clusters = new ArrayList<>();
            sitePoints = new ArrayList<>();
            sitePoints.ensureCapacity(siteCount);
            //initializing cluster centers to random sites
            Random rand = new Random(10);
            //using set to assure no repeating numbers
            HashSet<Integer> randInts = new HashSet<>();
            //get random non-repeating indices
            while(randInts.size() < clusterCount) {
                randInts.add(rand.nextInt(siteCount));
            }
            //create a set of colors of corresponding size for coloring the clusters
            HashSet<Color> colors = new HashSet<>();
            for (int i = 0; i < clusterCount; i++) {
                colors.add(new Color(rand.nextFloat(), rand.nextFloat(), rand.nextFloat()));
            }
            //add sites at given indexes as initial clusters
            int id = 0;
            Iterator<Color> colorIterator = colors.iterator();
            for (int i = 0; i < centroidBuffer.length; i += 3) {
                Cluster cluster = new Cluster(new Site(centroidBuffer[i + 1], centroidBuffer[i + 2], 2000, -1), colorIterator.next(), i/3);
                clusters.add(cluster);
            }

            int clusterID;
            double weight;
            int[] clusterWeights = new int[clusterCount];
            for (int i = 0; i < siteBuffer.length; i += 5) {
                clusterID = (int) siteBuffer[i + 4];
                weight = siteBuffer[i + 3];
                Site site = new Site(siteBuffer[i + 1], siteBuffer[i + 2], siteBuffer[i + 3], (int) siteBuffer[i]);
                site.setClusterID(clusterID);
                sitePoints.add(site);
                clusters.get(clusterID).getSites().add(site);

                clusterWeights[clusterID] += weight;
            }

            for (int i = 0; i < clusterWeights.length; i++) {
                clusters.get(i).setWeight(clusterWeights[i]);
            }



            // GUI SETUP
            Window window = new Window(args);
            MapLoader.paintClusters(clusters, sitePoints, window.getMap());
            window.getMapPanel().updateUI();

            window.getReport().setText("Clusters calculated in: " + time + "ms. Performed " + loopCounter + " loops.");

            MapLoader.paintClusters(clusters, sitePoints, window.getMap());
            window.getMapPanel().updateUI();
        }
    }

    static void prep(int siteCount, int clusterCount) {
        clusters = new ArrayList<>();
        sitePoints = new ArrayList<>();
        //[id1, lat1, long1, id2, lat2, long2, id3, lat3, long3,...]
        centroidBuffer = new double[3 * clusterCount];
        //[id1, lat1, long1, w1, clusterID1, id2, lat2, long2, w2, clusterID2, ...]
        siteBuffer = new double[5 * siteCount];
        updateFlags = new boolean[commSize];

        startMemory = 0; endMemory = 0;


        //calculating the number of sites one process should deal with
        //rounding up so no element left in case of division with weird numbers
        int siteChunkSize = siteCount / commSize;
        int clusterChunkSize = clusterCount / commSize;

        //flag
        continuing = new boolean[1];

        centroidChunkSizes = new int[commSize];
        centroidDispls = new int[commSize];
        siteChunkSizes = new int[commSize];
        siteDispls = new int[commSize];


        //PREPS for varying length scatter and gather ---

        //making varying chunk size
        int chunkCounter = 0;
        int remaining = clusterChunkSize == 0 ? clusterCount : clusterCount % commSize;
        //System.out.println("for " + clusterCount + " clusters and " + commSize + " proc, remaining is " + remaining);
        for (int i = 0; i < commSize; i++) {
            centroidChunkSizes[i] = clusterChunkSize * 3;
            if (remaining > 0) {
                centroidChunkSizes[i] += 3;
                remaining--;
            }
            centroidDispls[i] = chunkCounter;
            chunkCounter += centroidChunkSizes[i];

        }

        chunkCounter = 0;
        remaining = siteChunkSize == 0 ? siteCount : siteCount % commSize;

        for (int i = 0; i < commSize; i++) {
            siteChunkSizes[i] = siteChunkSize * 5;
            if (remaining > 0) {
                siteChunkSizes[i] += 5;
                remaining--;
            }
            siteDispls[i] = chunkCounter;
            chunkCounter += siteChunkSizes[i];
        }
        //End of PREPS for varying length scatter and gather ---

    }

    static void load(int siteCount, int clusterCount) {
        //setting sets up
        //System.out.println("Working Directory = " + System.getProperty("user.dir"));
        sitePoints = SiteLoader.getInstance().loadSites(siteCount);

        //getting clusters up
        Random rand = new Random(10);
        HashSet<Integer> randInts = new HashSet<>();
        //get random non-repeating indices
        while (randInts.size() < clusterCount) {
            randInts.add(rand.nextInt(sitePoints.size()));
        }
        //create a set of colors of corresponding size for coloring the clusters
        HashSet<Color> colors = new HashSet<>();
        for (int i = 0; i < clusterCount; i++) {
            colors.add(new Color(rand.nextFloat(), rand.nextFloat(), rand.nextFloat()));
        }
        //add sites at given indexes as initial clusters
        Iterator<Color> colorIterator = colors.iterator();
        int clusterID = 0;
        for (Integer i : randInts) {
            clusters.add(new Cluster(sitePoints.get(i), colorIterator.next(), clusterID++));
        }

        //SERIALIZE
        //turning cluster and site instances into "triples" and "quintuples"
        //[id1, lat1, long1, id2, lat2, long2, id3, lat3, long3,...]
        for (Cluster cluster : clusters
        ) {
            int id = cluster.getClusterID();
            centroidBuffer[id * 3] = cluster.getClusterID();
            centroidBuffer[id * 3 + 1] = cluster.getCentroid().getLatitude();
            centroidBuffer[id * 3 + 2] = cluster.getCentroid().getLongitude();
        }

        //[lat1, long1, clusterID1, weight1, id1, lat2, long2, clusterID2, weight2, id2 ...]
        for (int i = 0; i < siteCount; i++) {
            Site site = sitePoints.get(i);
            siteBuffer[i * 5] = site.getSiteID();
            siteBuffer[i * 5 + 1] = site.getLatitude();
            siteBuffer[i * 5 + 2] = site.getLongitude();
            siteBuffer[i * 5 + 3] = site.getWeight();
            siteBuffer[i * 5 + 4] = site.getClusterID();
        }
    }
}

