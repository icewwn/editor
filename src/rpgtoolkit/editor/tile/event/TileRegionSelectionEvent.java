package rpgtoolkit.editor.tile.event;

import java.util.EventObject;
import rpgtoolkit.common.editor.types.Tile;

/**
 * 
 * 
 * @author Joshua Michael Daly
 */
public class TileRegionSelectionEvent extends EventObject
{
    private final Tile[][] tiles;

    /*
     * *************************************************************************
     * Public Constructors
     * *************************************************************************
     */
    public TileRegionSelectionEvent(Object source, Tile[][] tiles)
    {
        super(source);
        this.tiles = tiles;
    }
    
    /*
     * *************************************************************************
     * Public Getters
     * *************************************************************************
     */
    public Tile[][] getTiles()
    {
        return this.tiles;
    }
}
