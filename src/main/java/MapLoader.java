
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.OSMTileFactoryInfo;
import org.jxmapviewer.input.*;
import org.jxmapviewer.viewer.DefaultTileFactory;
import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.viewer.TileFactoryInfo;
import org.jxmapviewer.viewer.WaypointPainter;

import javax.swing.JFrame;
import javax.swing.event.MouseInputListener;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class MapLoader {

    public static JXMapViewer getInstance(JFrame frame) {
        JXMapViewer mapViewer = new JXMapViewer();

        //creating tile factory
        TileFactoryInfo info = new OSMTileFactoryInfo();
        DefaultTileFactory tileFactory = new DefaultTileFactory(info);
        mapViewer.setTileFactory(tileFactory);

        //parallelizing loading of tiles
        tileFactory.setThreadPoolSize(Runtime.getRuntime().availableProcessors());

        //initial location - koper
        GeoPosition koper = new GeoPosition(45.54, 13.73);
        mapViewer.setZoom(4);
        mapViewer.setAddressLocation(koper);

        //adding interactions
        MouseInputListener mia = new PanMouseInputListener(mapViewer);
        mapViewer.addMouseListener(mia);
        mapViewer.addMouseMotionListener(mia);
        mapViewer.addMouseListener(new CenterMapListener(mapViewer));
        mapViewer.addMouseWheelListener(new ZoomMouseWheelListenerCursor(mapViewer));
        mapViewer.addKeyListener(new PanKeyListener(mapViewer));


        mapViewer.addPropertyChangeListener("zoom", new PropertyChangeListener()
        {
            @Override
            public void propertyChange(PropertyChangeEvent evt)
            {
                updateWindowTitle(frame, mapViewer);
            }
        });

        mapViewer.addPropertyChangeListener("center", new PropertyChangeListener()
        {
            @Override
            public void propertyChange(PropertyChangeEvent evt)
            {
                updateWindowTitle(frame, mapViewer);
            }
        });

        updateWindowTitle(frame, mapViewer);
        return mapViewer;
}

    protected static void updateWindowTitle(JFrame frame, JXMapViewer mapViewer)
    {
        int zoom = mapViewer.getZoom();
    }

    public static void paintClusters(ArrayList<Cluster> clusters, ArrayList<Site> sites, JXMapViewer mapViewer) {
        GeoPosition koper = new GeoPosition(45.54, 13.73);
        //sway points
        Set<SwingWaypoint> wayPointsSet = new HashSet<SwingWaypoint>();

        Set<GeoPosition> geoPositionSet = new HashSet<GeoPosition>();

        for (Site site : sites) {
            GeoPosition geoPosition = new GeoPosition(site.getLatitude(), site.getLongitude());
            geoPositionSet.add(geoPosition);
            //TODO change if using graphics
            wayPointsSet.add(new SwingWaypoint(site.getSiteID(), geoPosition, Color.BLACK, 5));
        }

        double min = Double.MAX_VALUE, max = Double.MIN_VALUE, totalW = 0;
        for (Cluster cluster: clusters) {
            if (cluster.getWeight() < min) min = cluster.getWeight();
            if(cluster.getWeight() > max) max = cluster.getWeight();
            totalW += cluster.getWeight();
            }

        for (Cluster cluster: clusters) {
            GeoPosition geoPosition = new GeoPosition(cluster.getCentroid().getLatitude(), cluster.getCentroid().getLongitude());
            //TODO change if using graphics
            wayPointsSet.add(new SwingWaypoint(-1, geoPosition, Color.BLACK, (int) (cluster.getWeight()/totalW * 23.0 * clusters.size())));
        }

        //setting overlay painter
        WaypointPainter<SwingWaypoint> swingWaypointPainter = new SwingWaypointOverlayPainter();
        swingWaypointPainter.setWaypoints(wayPointsSet);
        mapViewer.setOverlayPainter(swingWaypointPainter);

        mapViewer.zoomToBestFit(geoPositionSet, 0.95);
    }
}


