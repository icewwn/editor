/**
 * Copyright (c) 2015, rpgtoolkit.net <help@rpgtoolkit.net>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package net.rpgtoolkit.editor.editors;

import net.rpgtoolkit.editor.editors.board.BoardView2D;
import net.rpgtoolkit.editor.editors.board.BoardMouseAdapter;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.File;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import net.rpgtoolkit.common.assets.Tile;
import net.rpgtoolkit.common.assets.Board;
import net.rpgtoolkit.editor.editors.board.AbstractBrush;
import net.rpgtoolkit.common.Selectable;
import net.rpgtoolkit.common.assets.AssetDescriptor;
import net.rpgtoolkit.editor.MainWindow;
import net.rpgtoolkit.editor.ui.ToolkitEditorWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 *
 * @author Geoff Wilson
 * @author Joshua Michael Daly
 */
public class BoardEditor extends ToolkitEditorWindow {
  
  private static final Logger LOGGER = LoggerFactory.getLogger(BoardEditor.class);

  private JScrollPane scrollPane;

  private BoardView2D boardView;
  private Board board;

  private BoardMouseAdapter boardMouseAdapter;

  private Point cursorTileLocation;
  private Point cursorLocation;
  private Rectangle selection;

  private Tile[][] selectedTiles;

  private Selectable selectedObject;

  /**
   * Default Constructor.
   */
  public BoardEditor() {

  }

  public BoardEditor(Board board) {
    super("Untitled", true, true, true, true);
    boardMouseAdapter = new BoardMouseAdapter(this);
    this.board = board;
    init(board, new File(board.getDescriptor().getURI()).getName());
  }

  /**
   *
   * @param fileName
   * @param width
   * @param height
   */
  public BoardEditor(String fileName, int width, int height) {
    super("Untitled", true, true, true, true);
    boardMouseAdapter = new BoardMouseAdapter(this);
    board = new Board(null, width, height);
    board.addLayer();
    init(board, fileName);
  }

  /**
   *
   * @return
   */
  public JScrollPane getScrollPane() {
    return scrollPane;
  }

  /**
   *
   * @param scrollPane
   */
  public void setScrollPane(JScrollPane scrollPane) {
    this.scrollPane = scrollPane;
  }

  /**
   *
   * @return
   */
  public BoardView2D getBoardView() {
    return boardView;
  }

  /**
   *
   * @param boardView
   */
  public void setBoardView(BoardView2D boardView) {
    this.boardView = boardView;
  }

  /**
   *
   * @return
   */
  public Board getBoard() {
    return board;
  }

  /**
   *
   * @param board
   */
  public void setBoard(Board board) {
    this.board = board;
  }

  /**
   *
   * @return
   */
  public Point getCursorTileLocation() {
    return cursorTileLocation;
  }

  /**
   *
   * @param location
   */
  public void setCursorTileLocation(Point location) {
    cursorTileLocation = location;
  }

  /**
   *
   * @return
   */
  public Point getCursorLocation() {
    return cursorLocation;
  }

  /**
   *
   * @param location
   */
  public void setCursorLocation(Point location) {
    cursorLocation = location;
  }

  /**
   *
   * @return
   */
  public Rectangle getSelection() {
    return selection;
  }

  /**
   *
   * @return
   */
  public Tile[][] getSelectedTiles() {
    return selectedTiles;
  }

  /**
   *
   * @param tiles
   */
  public void setSelectedTiles(Tile[][] tiles) {
    selectedTiles = tiles;
  }

  /**
   *
   * @return
   */
  public Selectable getSelectedObject() {
    return selectedObject;
  }

  /**
   *
   * @param object
   */
  public void setSelectedObject(Selectable object) {
    if (object == null) {
      selectedObject = board;
    } else {
      selectedObject = object;
    }

    MainWindow.getInstance().getPropertiesPanel().setModel(
            selectedObject);
    boardView.repaint();
  }

  /**
   * Zoom in on the board view.
   */
  public void zoomIn() {
    boardView.zoomIn();
    scrollPane.getViewport().revalidate();
  }

  /**
   * Zoom out on the board view.
   */
  public void zoomOut() {
    boardView.zoomOut();
    scrollPane.getViewport().revalidate();
  }

  /**
   *
   * @throws java.lang.Exception
   */
  @Override
  public void save() throws Exception {
    save(board);
  }

  /**
   *
   *
   * @param file
   * @throws java.lang.Exception
   */
  @Override
  public void saveAs(File file) throws Exception {
    board.setDescriptor(new AssetDescriptor(file.toURI()));
    setTitle(file.getName());
    save();
  }

  /**
   *
   * @param rectangle
   */
  public void setSelection(Rectangle rectangle) {
    selection = rectangle;
    boardView.repaint();
  }

  /**
   *
   */
  public static void toggleSelectedOnBoardEditor() {
    BoardEditor editor = MainWindow.getInstance().getCurrentBoardEditor();

    if (editor != null) {
      if (editor.getSelectedObject() != null) {
        editor.getSelectedObject().setSelectedState(false);
      }

      editor.setSelectedObject(null);
    }
  }

  /**
   *
   * @param brush
   * @param point
   * @param selection
   */
  public void doPaint(AbstractBrush brush, Point point, Rectangle selection) {
    try {
      if (brush == null) {
        return;
      }

      brush.startPaint(boardView, boardView.
              getCurrentSelectedLayer().getLayer().getNumber());
      brush.doPaint(point.x, point.y, selection);
      brush.endPaint();
    } catch (Exception ex) {
      LOGGER.error("Failed to paint on the board brush=[{}], point=[{}], selection=[{}]", 
              brush, point,selection, ex);
    }
  }

  /**
   *
   * @param rectangle
   * @return
   */
  public Tile[][] createTileLayerFromRegion(Rectangle rectangle) {
    Tile[][] tiles = new Tile[rectangle.width + 1][rectangle.height + 1];

    for (int y = rectangle.y; y <= rectangle.y + rectangle.height; y++) {
      for (int x = rectangle.x; x <= rectangle.x + rectangle.width; x++) {
        tiles[x - rectangle.x][y - rectangle.y]
                = boardView.getCurrentSelectedLayer().
                getLayer().getTileAt(x, y);
      }
    }

    return tiles;
  }

  /**
   *
   *
   * @param x
   * @param y
   * @return
   */
  public int[] calculateSnapCoordinates(int x, int y) {
    int tileSize = MainWindow.TILE_SIZE;
    int[] coordinates = {0, 0};

    int mx = x % tileSize;
    int my = y % tileSize;

    if (mx < tileSize / 2) {
      coordinates[0] = x - mx;
    } else {
      coordinates[0] = x + (tileSize - mx);
    }

    if (my < tileSize / 2) {
      coordinates[1] = y - my;
    } else {
      coordinates[1] = y + (tileSize - my);
    }

    return coordinates;
  }

  private void init(Board board, String fileName) {
    boardView = new BoardView2D(this, board);
    boardView.addMouseListener(boardMouseAdapter);
    boardView.addMouseMotionListener(boardMouseAdapter);

    scrollPane = new JScrollPane(boardView);
    scrollPane.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

    cursorTileLocation = new Point(0, 0);
    cursorLocation = new Point(0, 0);

    setTitle(fileName);
    add(scrollPane);
    pack();
  }
  
}
