import mpi.MPI;
import java.awt.*;
import java.sql.Timestamp;
import java.util.*;


public class DistributedComputation {
    static ArrayList<Site> sitePoints;
    static ArrayList<Cluster> clusters;
    private static int clusterCount, siteCount, clusterChunkSize, siteChunkSize, time, loopCounter, commSize, rank;
    private static double[] clusterBuffer, siteBuffer;
    private static int[] clusterChunkSizes, clusterDispls, siteChunkSizes, siteDispls, flagChunks, flagDispls;
    private static boolean[] updateFlags;
    private static Timestamp start, end;


    public static void main(String[] args) {
        System.out.println(Arrays.toString(args));
        MPI.Init(args);
        rank = MPI.COMM_WORLD.Rank();
        commSize = MPI.COMM_WORLD.Size();

        //getting the numbers from parameters in the command line
        clusterCount = Integer.parseInt(args[8]);
        siteCount = Integer.parseInt(args[9]);
        clusters = new ArrayList<Cluster>();
        sitePoints = new ArrayList<Site>();
        //[id1, lat1, long1, id2, lat2, long2, id3, lat3, long3,...]
        clusterBuffer = new double[3 * clusterCount];
        //[id1, lat1, long1, w1, clusterID1, id2, lat2, long2, w2, clusterID2, ...]
        siteBuffer = new double[5 * siteCount];
        updateFlags = new boolean[clusterCount];


        //calculating the number of sites one process should deal with
        //rounding up so no element left in case of division with weird numbers
        siteChunkSize = siteCount / commSize;
        clusterChunkSize = clusterCount / commSize;

        //flag
        boolean[] continuing = new boolean[1];
        continuing[0] = false;


        //preparations for varying length scatter and gather
        clusterChunkSizes = new int[commSize];
        clusterDispls = new int[commSize];
        flagChunks = new int[commSize];
        flagDispls = new int[commSize];

        int chunkCounter = 0;
        int remaining = clusterChunkSize == 0 ? clusterCount : clusterCount % commSize;
        //making varying chunk size
        for (int i = 0; i < commSize; i++) {
            clusterChunkSizes[i] = clusterChunkSize * 3;
            flagChunks[i] = clusterChunkSize;
            if (remaining > 0) {
                clusterChunkSizes[i] += 3;
                flagChunks[i]++;
                remaining--;
            }

            clusterDispls[i] = chunkCounter * 3;
            flagDispls[i] = chunkCounter;
            chunkCounter += flagChunks[i];
        }


        siteChunkSizes = new int[commSize];
        siteDispls = new int[commSize];
        chunkCounter = 0;
        remaining = siteChunkSize == 0 ? siteCount : siteCount % commSize;
        //making varying chunk size
        for (int i = 0; i < commSize; i++) {
            siteChunkSizes[i] = siteChunkSize * 5;
            if (remaining > 0) {
                siteChunkSizes[i] += 5;
                remaining--;
            }

            siteDispls[i] = chunkCounter;
            chunkCounter += siteChunkSizes[i];
        }

        double[] siteElements = new double[siteChunkSizes[rank]];
        double[] clusterElements = new double[clusterChunkSizes[rank]];
        boolean[] updateFlagsChunk = new boolean[flagChunks[rank]];


        if (rank == 0) {
            //setting sets up
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
            //System.out.println("site size " + sitePoints.size());

            //SERIALIZE
            //turning cluster and site instances into "triples" and "quintuples"
            //[id1, lat1, long1, id2, lat2, long2, id3, lat3, long3,...]
            for (Cluster cluster : clusters
            ) {
                int id = cluster.getClusterID();
                clusterBuffer[id * 3] = cluster.getClusterID();
                clusterBuffer[id * 3 + 1] = cluster.getCentroid().getLatitude();
                clusterBuffer[id * 3 + 2] = cluster.getCentroid().getLongitude();
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

        start = new Timestamp(System.currentTimeMillis());

        MPI.COMM_WORLD.Barrier();
        do{
            if(rank == 0) loopCounter++;
            updateFlags = new boolean[clusterCount];
            continuing[0] = false;
            //send all clusters to all
            MPI.COMM_WORLD.Bcast(clusterBuffer, 0, clusterBuffer.length, MPI.DOUBLE, 0);
            //send chunks of sites to respective proc
            MPI.COMM_WORLD.Scatterv(siteBuffer, 0, siteChunkSizes, siteDispls, MPI.DOUBLE, siteElements, 0, siteChunkSizes[rank], MPI.DOUBLE, 0);

            //BINDING STEP
            for (int i = 0; i < siteElements.length; i += 5) {
                int minClusterID = -1;
                double min = Double.MAX_VALUE;
                for (int j = 0; j < clusterBuffer.length; j += 3) {
                        double distance = Math.sqrt(Math.pow((clusterBuffer[j + 1] - siteElements[i + 1]), 2)) +
                                Math.sqrt(Math.pow((clusterBuffer[j + 2] - siteElements[i + 2]), 2));
                        //System.out.println("in " + rank + ", distance between site " + i/5 + " and cluster " + j/3 + " is " + distance);
                        if (distance < min) {
                            min = distance;
                            minClusterID = j / 3;
                        }
                }
                siteElements[i + 4] = minClusterID;
            }

            MPI.COMM_WORLD.Allgatherv(siteElements, 0, siteElements.length, MPI.DOUBLE, siteBuffer, 0, siteChunkSizes, siteDispls, MPI.DOUBLE);

            MPI.COMM_WORLD.Scatterv(clusterBuffer, 0, clusterChunkSizes, clusterDispls, MPI.DOUBLE, clusterElements, 0, clusterChunkSizes[rank], MPI.DOUBLE, 0);
            MPI.COMM_WORLD.Scatterv(updateFlags, 0, flagChunks, flagDispls, MPI.BOOLEAN, updateFlagsChunk, 0, flagChunks[rank], MPI.BOOLEAN, 0);

            //UPDATE STEP
            for (int i = 0; i < clusterElements.length; i += 3) { //iterating through cluster
                double initialLat = clusterElements[i + 1], initialLong = clusterElements[i + 2];
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

                updateFlagsChunk[i / 3] = initialLat != clusterElements[i + 1] || initialLong != clusterElements[i + 2];
            }

            MPI.COMM_WORLD.Gatherv(clusterElements, 0, clusterElements.length, MPI.DOUBLE, clusterBuffer, 0, clusterChunkSizes, clusterDispls, MPI.DOUBLE, 0);

            //STOPPING CONDITION
            MPI.COMM_WORLD.Gatherv(updateFlagsChunk, 0, flagChunks[rank], MPI.BOOLEAN, updateFlags, 0, flagChunks, flagDispls, MPI.BOOLEAN, 0);
            if (rank == 0) {
                for (boolean updateFlag : updateFlags) {
                    if (updateFlag) {
                        continuing[0] = true;
                        break;
                    }
                }
            }

            //checking stopping conditions
            if(rank == 0) continuing[0] = continuing[0] && (loopCounter < 1000);
            MPI.COMM_WORLD.Bcast(continuing, 0, 1, MPI.BOOLEAN, 0);

        } while(continuing[0]);


        if (rank == 0) {
            end = new Timestamp(System.currentTimeMillis());
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
            for (int i = 0; i < clusterBuffer.length; i += 3) {
                Cluster cluster = new Cluster(new Site(clusterBuffer[i + 1], clusterBuffer[i + 2], 2000, -1), colorIterator.next(), i/3);
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

        MPI.Finalize();
    }
}

