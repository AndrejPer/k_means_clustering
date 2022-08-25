import mpi.MPI;

import java.awt.*;
import java.util.*;

public class DistributedComputation2 {
    static ArrayList<Site> sitePoints;
    static ArrayList<Cluster> clusters;
    private static int clusterCount, siteCount, chunkSize, time, loopCounter, commSize, rank;
    private static final int[] array = {1, 2, 3, 4, 5, 6, 7, 8, 9};
    private static double[] clusterBuffer, siteBuffer;
    static int[] chunkSizes, displs;

    //flags
    static boolean changed;


    public static void main(String[] args) {
        MPI.Init(args);
        rank = MPI.COMM_WORLD.Rank();
        commSize = MPI.COMM_WORLD.Size();
        //System.out.println("jaz sem "+rank+" od "+commSize);

        //getting the numbers from parameters in the command line
        clusterCount = 3; //Integer.parseInt(args[6]);
        siteCount = 6; //Integer.parseInt(args[7]);
        clusters = new ArrayList<Cluster>();
        sitePoints = new ArrayList<Site>();


        //calculating the number of sites one process should deal with
        //rounding up so no element left in case of division with weird numbers
        chunkSize = array.length/commSize;

        chunkSizes = new int[commSize];
        displs = new int[commSize];
        int chunkCounter = 0;
        int remaining =  array.length % chunkSize;

        //making varying chunk size
        for(int i = 0; i < commSize; i++) {
            chunkSizes[i] = chunkSize;
            if(remaining > 0) {
                chunkSizes[i]++;
                remaining--;
            }

            displs[i] = chunkCounter;
            chunkCounter += chunkSizes[i];
        }

        System.out.println("chunk sizes: " + Arrays.toString(chunkSizes));
        System.out.println("displs: " + Arrays.toString(displs));


        //setting up flags to monitor changes in the assignment step
        //different approach than in parallel (clusters there), due to limitations of MPI

        //attempt to ennect to one function
        System.out.println("in " + rank + " with shunk size " + chunkSize);
        boolean[] cont = new boolean[1];
        cont[0] = true;
        int[] elements = new int[chunkSizes[rank]];
        System.out.println("elements size in " + rank + " is " + elements.length);


        while (cont[0]) {
            MPI.COMM_WORLD.Scatterv(array, 0, chunkSizes, displs, MPI.INT, elements, 0, chunkSizes[rank], MPI.INT, 0);
            System.out.println("in " + rank + " after scatter with array " + Arrays.toString(elements));
            System.out.println("in work of " + rank + " with flag " + cont[0]);

            //work
            for (int i = 0; i<elements.length; i++) elements[i] *= 2;
            MPI.COMM_WORLD.Allgatherv(elements, 0, elements.length, MPI.INT, array, 0, chunkSizes, displs, MPI.INT);

            //check if continue
            if(rank == 0) {
                for (int elt: array
                ) {
                    if (elt == 36) {
                        cont[0] = false;
                        break;
                    }
                }
            }

            MPI.COMM_WORLD.Bcast(cont, 0, 1, MPI.BOOLEAN, 0);


            System.out.println("in work of " + rank + " with cont: " + cont[0]);
        }
        //end of attempt to connect to one function

        /*
        if(rank == 0) {
            coordinate();
        }
        else {
            work();
        }
         */

        System.out.println("before finalize in " + rank + " : " + Arrays.toString(array));
        MPI.Finalize();

    }

    private static void coordinate() {
        System.out.println("in " + rank + " with shunk size " + chunkSize);
        boolean[] cont = new boolean[1];
        boolean[] change = new boolean[1];
        int[] elements = new int[chunkSizes[rank]];
        System.out.println("elements size in " + rank + " is " + elements.length);



        cont[0] = true;
        while(cont[0]) {
            System.out.println("in coord while -----");
            //MPI.COMM_WORLD.Bcast(cont, 0, 1, MPI.BOOLEAN, 0);
            MPI.COMM_WORLD.Scatterv(array, 0, chunkSizes, displs, MPI.INT, elements, 0, chunkSizes[rank], MPI.INT, 0);
            System.out.println("in " + rank + " after scatter with array " + Arrays.toString(elements));


            System.out.println("in " + rank + " after scatter with array " + Arrays.toString(elements));

            for (int i = 0; i<elements.length; i++) elements[i] *= 2;

            MPI.COMM_WORLD.Gatherv(elements, 0, elements.length, MPI.INT, array, 0, chunkSizes, displs, MPI.INT, 0);


            /*
            System.out.println("gathering with size " + commSize);
            for(int i = 1; i < commSize; i++) {
                MPI.COMM_WORLD.Recv(change, 0, 1, MPI.BOOLEAN, MPI.ANY_SOURCE, 999);
                System.out.println("change at " + i + " is " + change[0]);
                changed = changed || change[0];
            }
            */

            for (int elt: array
                 ) {
                if(elt == 39) changed = false;
            }
            System.out.println("final val is " + changed);
            cont[0] = changed;
            MPI.COMM_WORLD.Bcast(cont, 0, 1, MPI.BOOLEAN, 0);
        }
    }

    private static void work() {
        boolean[] cont = new boolean[1];
        boolean[] change = new boolean[1];
        int[] elements = new int[chunkSizes[rank]];

        for(;;){


            MPI.COMM_WORLD.Scatterv(array, 0, chunkSizes, displs, MPI.INT, elements, 0, 3, MPI.INT, 0);
            System.out.println("in " + rank + " after scatter with array " + Arrays.toString(elements));
            System.out.println("in work of " + rank + " with flag " + cont[0]);


            for (int i = 0; i<elements.length; i++) elements[i] *= 2;

            //work

            MPI.COMM_WORLD.Gatherv(elements, 0, elements.length, MPI.INT, array, 0, chunkSizes, displs, MPI.INT, 0);

            change[0] = false;
            System.out.println(rank + " sending " + change[0]);
            System.out.println("about to send from " + rank);
            //MPI.COMM_WORLD.Send(change, 0, 1, MPI.BOOLEAN, 0, 999);

            //check if continue
            MPI.COMM_WORLD.Bcast(cont, 0, 1, MPI.BOOLEAN, 0);
            System.out.println("in work of " + rank + " with cont: " + cont[0]);
            if(!cont[0]) return;
            else continue;
        }


    }
}
