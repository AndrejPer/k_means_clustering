import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.viewer.WaypointPainter;
import java.awt.*;
import java.awt.geom.Point2D;

public class SwingWaypointOverlayPainter extends WaypointPainter<SwingWaypoint> {

    @Override
    protected void doPaint(Graphics2D g, JXMapViewer jxMapViewer, int width, int height) {
        int counter = 0;
        for (SwingWaypoint swingWaypoint : getWaypoints()) {
            if (swingWaypoint.getID() == -1) continue;
            Point2D point = jxMapViewer.getTileFactory().geoToPixel(
                    swingWaypoint.getPosition(), jxMapViewer.getZoom());
            Rectangle rectangle = jxMapViewer.getViewportBounds();
            int pinX = (int) (point.getX() - rectangle.getX());
            int pinY = (int) (point.getY() - rectangle.getY());

            g.setColor(swingWaypoint.getColor());
            g.fillOval(pinX, pinY, swingWaypoint.getSize(), swingWaypoint.getSize());
        }

        for (SwingWaypoint swingWaypoint: getWaypoints()) {
            if (swingWaypoint.getID() != -1) continue;
            Point2D point = jxMapViewer.getTileFactory().geoToPixel(
                    swingWaypoint.getPosition(), jxMapViewer.getZoom());
            Rectangle rectangle = jxMapViewer.getViewportBounds();

            int pinX = (int) (point.getX() - rectangle.getX());
            int pinY = (int) (point.getY() - rectangle.getY());
            g.setColor(swingWaypoint.getColor());
            g.fillOval(pinX, pinY, swingWaypoint.getSize(), swingWaypoint.getSize());
            g.setColor(Color.black);
            g.setStroke(new BasicStroke(3));
            g.drawOval(pinX, pinY, swingWaypoint.getSize(), swingWaypoint.getSize());
            g.setColor(swingWaypoint.getColor().darker());
        }
    }
}
