import org.jxmapviewer.viewer.DefaultWaypoint;
import org.jxmapviewer.viewer.GeoPosition;
import java.awt.*;

public class SwingWaypoint extends DefaultWaypoint {
    private final Color color;
    private final int size, id;

    public SwingWaypoint(int id, GeoPosition coordinates, Color color, int size) {
        super(coordinates);
        this.color = color;
        this.size = size == 0 ? 5 : size;
        this.id = id;
    }

    public Color getColor() {
        return color;
    }

    public int getSize() {
        return size;
    }

    public int getID() {
        return id;
    }
}




