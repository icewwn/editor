/**
 * Copyright (c) 2015, rpgtoolkit.net <help@rpgtoolkit.net>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package net.rpgtoolkit.editor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.beans.PropertyVetoException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JDesktopPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import net.rpgtoolkit.common.assets.Animation;
import net.rpgtoolkit.common.assets.AssetDescriptor;
import net.rpgtoolkit.common.assets.AssetException;
import net.rpgtoolkit.common.assets.AssetHandle;
import net.rpgtoolkit.common.assets.AssetManager;
import net.rpgtoolkit.common.assets.Board;
import net.rpgtoolkit.common.assets.Enemy;
import net.rpgtoolkit.common.assets.Item;
import net.rpgtoolkit.common.assets.Player;
import net.rpgtoolkit.common.assets.Project;
import net.rpgtoolkit.common.assets.SpecialMove;
import net.rpgtoolkit.common.assets.Tile;
import net.rpgtoolkit.common.assets.TileSet;
import net.rpgtoolkit.editor.editors.AnimationEditor;
import net.rpgtoolkit.editor.editors.BoardEditor;
import net.rpgtoolkit.editor.editors.board.AbstractBrush;
import net.rpgtoolkit.editor.editors.board.ShapeBrush;
import net.rpgtoolkit.editor.editors.board.VectorBrush;
import net.rpgtoolkit.editor.editors.ProjectEditor;
import net.rpgtoolkit.editor.ui.listeners.TileSelectionListener;
import net.rpgtoolkit.editor.editors.board.NewBoardDialog;
import net.rpgtoolkit.editor.ui.resources.Icons;
import net.rpgtoolkit.editor.editors.board.ProgramBrush;
import net.rpgtoolkit.common.utilities.CoreProperties;
import net.rpgtoolkit.common.utilities.TileSetCache;
import net.rpgtoolkit.editor.editors.CharacterEditor;
import net.rpgtoolkit.editor.editors.EnemyEditor;
import net.rpgtoolkit.editor.editors.ItemEditor;
import net.rpgtoolkit.editor.editors.tileset.NewTilesetDialog;
import net.rpgtoolkit.editor.ui.EditorFactory;
import net.rpgtoolkit.editor.ui.LayerPanel;
import net.rpgtoolkit.editor.ui.menu.MainMenuBar;
import net.rpgtoolkit.editor.ui.toolbar.MainToolBar;
import net.rpgtoolkit.editor.ui.ProjectPanel;
import net.rpgtoolkit.editor.ui.PropertiesPanel;
import net.rpgtoolkit.editor.ui.TileSetTabbedPane;
import net.rpgtoolkit.editor.ui.ToolkitDesktopManager;
import net.rpgtoolkit.editor.ui.ToolkitEditorWindow;
import net.rpgtoolkit.editor.ui.listeners.TileSetSelectionListener;
import net.rpgtoolkit.editor.utilities.EditorFileManager;
import net.rpgtoolkit.editor.utilities.FileTools;
import net.rpgtoolkit.editor.utilities.TileSetRipper;

/**
 * Currently opening TileSets, tiles, programs, boards, animations, characters etc.
 *
 * @author Geoff Wilson
 * @author Joshua Michael Daly
 */
public class MainWindow extends JFrame implements InternalFrameListener {

  // Singleton.
  private static final MainWindow INSTANCE = new MainWindow();

  public static final int TILE_SIZE = 32;

  private final JDesktopPane desktopPane;

  private final MainMenuBar menuBar;
  private final MainToolBar toolBar;

  private final JPanel toolboxPanel;
  private final JTabbedPane upperTabbedPane;
  private final JTabbedPane lowerTabbedPane;
  private final ProjectPanel projectPanel;
  private final TileSetTabbedPane tileSetPanel;
  private final PropertiesPanel propertiesPanel;
  private final LayerPanel layerPanel;

  // Project Related.
  private Project activeProject;

  // Board Related.
  private boolean showGrid;
  private boolean showVectors;
  private boolean showPrograms;
  private boolean showCoordinates;
  private boolean snapToGrid;
  private AbstractBrush currentBrush;
  private Tile lastSelectedTile;

  // Listeners.
  private final TileSetSelectionListener tileSetSelectionListener;

  private MainWindow() {
    super("RPG Toolkit 4.0");

    this.desktopPane = new JDesktopPane();
    this.desktopPane.setBackground(Color.LIGHT_GRAY);
    this.desktopPane.setDesktopManager(new ToolkitDesktopManager());

    this.projectPanel = new ProjectPanel();
    this.tileSetPanel = new TileSetTabbedPane();
    this.upperTabbedPane = new JTabbedPane();
    this.upperTabbedPane.addTab("Project", this.projectPanel);
    this.upperTabbedPane.addTab("Tileset", this.tileSetPanel);

    this.propertiesPanel = new PropertiesPanel();
    this.layerPanel = new LayerPanel();
    this.lowerTabbedPane = new JTabbedPane();
    this.lowerTabbedPane.addTab("Properties", this.propertiesPanel);
    this.lowerTabbedPane.addTab("Layers", this.layerPanel);

    this.toolboxPanel = new JPanel(new GridLayout(2, 1));
    this.toolboxPanel.setPreferredSize(new Dimension(352, 0));
    this.toolboxPanel.add(this.upperTabbedPane);
    this.toolboxPanel.add(this.lowerTabbedPane);

    // Application icon.
    this.setIconImage(Icons.getLargeIcon("application").getImage());

    this.menuBar = new MainMenuBar(this);
    this.toolBar = new MainToolBar();

    this.currentBrush = new ShapeBrush();
    ((ShapeBrush) this.currentBrush).makeRectangleBrush(new Rectangle(0, 0, 1, 1));

    this.lastSelectedTile = new Tile();

    this.tileSetSelectionListener = new TileSetSelectionListener();

    JPanel parent = new JPanel(new BorderLayout());
    parent.add(this.desktopPane, BorderLayout.CENTER);

    this.setLayout(new BorderLayout());
    this.add(this.toolBar, BorderLayout.NORTH);
    this.add(parent, BorderLayout.CENTER);
    this.add(this.toolboxPanel, BorderLayout.EAST);

    this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    this.setExtendedState(JFrame.MAXIMIZED_BOTH);
    this.setSize(new Dimension(1024, 768));
    this.setLocationByPlatform(true);
    this.setJMenuBar(this.menuBar);
  }

  public static MainWindow getInstance() {
    return INSTANCE;
  }

  public JDesktopPane getDesktopPane() {
    return this.desktopPane;
  }

  public boolean isShowGrid() {
    return showGrid;
  }

  public void setShowGrid(boolean isShowGrid) {
    this.showGrid = isShowGrid;
  }

  public boolean isShowVectors() {
    return showVectors;
  }

  public void setShowVectors(boolean showVectors) {
    this.showVectors = showVectors;
  }

  public boolean isShowPrograms() {
    return showPrograms;
  }

  public void setShowPrograms(boolean showPrograms) {
    this.showPrograms = showPrograms;
  }

  public boolean isShowCoordinates() {
    return showCoordinates;
  }

  public void setShowCoordinates(boolean isShowCoordinates) {
    this.showCoordinates = isShowCoordinates;
  }

  public boolean isSnapToGrid() {
    return snapToGrid;
  }

  public void setSnapToGrid(boolean snapToGrid) {
    this.snapToGrid = snapToGrid;
  }

  public AbstractBrush getCurrentBrush() {
    return this.currentBrush;
  }

  public void setCurrentBrush(AbstractBrush brush) {
    this.currentBrush = brush;
  }

  public Tile getLastSelectedTile() {
    return this.lastSelectedTile;
  }
  
  public void setLastSelectedTile(Tile tile) {
    lastSelectedTile = tile;
  }

  public MainMenuBar getMainMenuBar() {
    return this.menuBar;
  }

  public MainToolBar getMainToolBar() {
    return this.toolBar;
  }

  public PropertiesPanel getPropertiesPanel() {
    return this.propertiesPanel;
  }

  public BoardEditor getCurrentBoardEditor() {
    if (this.desktopPane.getSelectedFrame() instanceof BoardEditor) {
      return (BoardEditor) this.desktopPane.getSelectedFrame();
    }

    return null;
  }

  public Project getActiveProject() {
    return activeProject;
  }

  public TileSelectionListener getTileSetSelectionListener() {
    return tileSetSelectionListener;
  }

  @Override
  public void internalFrameOpened(InternalFrameEvent e) {
    if (e.getInternalFrame() instanceof AnimationEditor) {
      AnimationEditor editor = (AnimationEditor) e.getInternalFrame();
      propertiesPanel.setModel(editor.getAnimation());
      lowerTabbedPane.setSelectedComponent(propertiesPanel);
    } else if (e.getInternalFrame() instanceof BoardEditor) {
      BoardEditor editor = (BoardEditor) e.getInternalFrame();
      upperTabbedPane.setSelectedComponent(tileSetPanel);
      lowerTabbedPane.setSelectedComponent(layerPanel);
      propertiesPanel.setModel(editor.getBoard());
    } else if (e.getInternalFrame() instanceof CharacterEditor) {
      CharacterEditor editor = (CharacterEditor) e.getInternalFrame();
      propertiesPanel.setModel(editor.getPlayer());
      lowerTabbedPane.setSelectedComponent(propertiesPanel);
    }
  }

  @Override
  public void internalFrameClosing(InternalFrameEvent e) {

  }

  @Override
  public void internalFrameClosed(InternalFrameEvent e) {

  }

  @Override
  public void internalFrameIconified(InternalFrameEvent e) {

  }

  @Override
  public void internalFrameDeiconified(InternalFrameEvent e) {

  }

  @Override
  public void internalFrameActivated(InternalFrameEvent e) {
    if (e.getInternalFrame() instanceof AnimationEditor) {
      AnimationEditor editor = (AnimationEditor) e.getInternalFrame();
      propertiesPanel.setModel(editor.getAnimation());
      lowerTabbedPane.setSelectedComponent(propertiesPanel);
    } else if (e.getInternalFrame() instanceof BoardEditor) {
      BoardEditor editor = (BoardEditor) e.getInternalFrame();
      this.layerPanel.setBoardView(editor.getBoardView());

      if (editor.getSelectedObject() != null) {
        this.propertiesPanel.setModel(editor.getSelectedObject());
      } else {
        this.propertiesPanel.setModel(editor.getBoard());
      }
    } else if (e.getInternalFrame() instanceof CharacterEditor) {
      CharacterEditor editor = (CharacterEditor) e.getInternalFrame();
      propertiesPanel.setModel(editor.getPlayer());
      lowerTabbedPane.setSelectedComponent(propertiesPanel);
    } else if (e.getInternalFrame() instanceof ItemEditor) {
      ItemEditor editor = (ItemEditor) e.getInternalFrame();
      propertiesPanel.setModel(editor.getItem());
      lowerTabbedPane.setSelectedComponent(propertiesPanel);
    }
  }

  @Override
  public void internalFrameDeactivated(InternalFrameEvent e) {
    if (e.getInternalFrame() instanceof AnimationEditor) {
      AnimationEditor editor = (AnimationEditor) e.getInternalFrame();

      if (propertiesPanel.getModel() == editor.getAnimation()) {
        this.propertiesPanel.setModel(null);
      }
    } else if (e.getInternalFrame() instanceof BoardEditor) {
      BoardEditor editor = (BoardEditor) e.getInternalFrame();

      if (this.layerPanel.getBoardView().equals(editor.getBoardView())) {
        this.layerPanel.clearTable();
      }

      if (this.propertiesPanel.getModel() == editor.getSelectedObject()
              || propertiesPanel.getModel() == editor.getBoard()) {
        this.propertiesPanel.setModel(null);
      }

      // So we do not end up drawing the vector or program on the other 
      // board after it has been deactivated.
      if (this.currentBrush instanceof VectorBrush
              || this.currentBrush instanceof ProgramBrush) {
        VectorBrush brush = (VectorBrush) this.currentBrush;

        if (brush.isDrawing() && brush.getBoardVector() != null) {
          brush.finish();
        }
      }
    } else if (e.getInternalFrame() instanceof CharacterEditor) {
      CharacterEditor editor = (CharacterEditor) e.getInternalFrame();

      if (propertiesPanel.getModel() == editor.getPlayer()) {
        this.propertiesPanel.setModel(null);
      }
    }
  }

  /**
   * Adds a new file format editor to our desktop pane.
   *
   * @param editor
   */
  public void addToolkitEditorWindow(ToolkitEditorWindow editor) {
    editor.addInternalFrameListener(this);
    editor.setVisible(true);
    editor.toFront();
    desktopPane.add(editor);
    selectToolkitWindow(editor);
  }

  public void checkFileExtension(File file) {
    String fileName = file.getName().toLowerCase();

    if (fileName.endsWith(CoreProperties.getDefaultExtension(Animation.class))) {
      addToolkitEditorWindow(EditorFactory.getEditor(openAnimation(file)));
    } else if (fileName.endsWith(CoreProperties.getDefaultExtension(Board.class))) {
      addToolkitEditorWindow(EditorFactory.getEditor(openBoard(file)));
    } else if (fileName.endsWith(CoreProperties.getDefaultExtension(Enemy.class))) {
      addToolkitEditorWindow(EditorFactory.getEditor(openEnemy(file)));
    } else if (fileName.endsWith(CoreProperties.getDefaultExtension(Item.class))) {
      addToolkitEditorWindow(EditorFactory.getEditor(openItem(file)));
    } else if (fileName.endsWith(CoreProperties.getDefaultExtension(Player.class))) {
      addToolkitEditorWindow(EditorFactory.getEditor(openCharacter(file)));
    } else if (fileName.endsWith(CoreProperties.getDefaultExtension(TileSet.class))) {
      openTileset(file);
    } else if (fileName.endsWith(CoreProperties.getDefaultExtension(SpecialMove.class))) {
      addToolkitEditorWindow(EditorFactory.getEditor(openSpecialMove(file)));
    }
  }

  public void openProject() {
    EditorFileManager.getFileChooser().resetChoosableFileFilters();
    FileNameExtensionFilter filter = new FileNameExtensionFilter(
            "Toolkit Project", CoreProperties.getDefaultExtension(Project.class));
    EditorFileManager.getFileChooser().setFileFilter(filter);

    File mainFolder = new File(CoreProperties.getProjectsDirectory() + File.separator
            + CoreProperties.getProperty("toolkit.directory.main"));

    if (mainFolder.exists()) {
      EditorFileManager.getFileChooser().setCurrentDirectory(mainFolder);
    }

    if (EditorFileManager.getFileChooser().showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
      File file = EditorFileManager.getFileChooser().getSelectedFile();
      String fileName = file.getName().substring(0, file.getName().indexOf('.'));
      System.setProperty("project.path",
              file.getParentFile().getParent()
              + File.separator
              + CoreProperties.getProperty("toolkit.directory.game")
              + File.separator
              + fileName + File.separator);

      try {
        AssetHandle handle = AssetManager.getInstance().deserialize(
                new AssetDescriptor(file.toURI()));
        activeProject = (Project) handle.getAsset();
      } catch (IOException | AssetException ex) {
        Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
      }

      setupProject();
    }
  }

  public void createNewProject() {
    String projectName = JOptionPane.showInputDialog(this,
            "Project Name:",
            "Create Project",
            JOptionPane.QUESTION_MESSAGE);

    if (projectName != null) {
      boolean result = FileTools.createDirectoryStructure(CoreProperties.getProjectsDirectory(), projectName);

      if (result) {
        Project project = new Project(
                null,
                CoreProperties.getProjectsDirectory()
                + File.separator
                + CoreProperties.getProperty("toolkit.directory.main"),
                projectName);

        try {
          // Write out new project file.
          AssetManager.getInstance().serialize(AssetManager.getInstance().getHandle(project));
          System.setProperty("project.path",
                  System.getProperty("user.home")
                  + File.separator
                  + CoreProperties.getProperty("toolkit.directory.projects")
                  + File.separator
                  + CoreProperties.getProperty("toolkit.directory.game")
                  + File.separator
                  + projectName + File.separator);

          activeProject = project;
          setupProject();
        } catch (IOException | AssetException ex) {
          Logger.getLogger(Board.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
    }
  }

  public void createNewAnimation() {
    addToolkitEditorWindow(EditorFactory.getEditor(new Animation(null)));
  }

  /**
   * Creates an animation editor window for modifying the specified animation file.
   *
   * @param file
   * @return
   */
  public Animation openAnimation(File file) {
    try {
      if (file.canRead()) {
        Animation animation;

        AssetHandle handle = AssetManager.getInstance().deserialize(
                new AssetDescriptor(file.toURI()));
        animation = (Animation) handle.getAsset();

        return animation;
      }
    } catch (IOException | AssetException ex) {
      Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
    }

    return null;
  }

  public void createNewBoard() {
    NewBoardDialog dialog = new NewBoardDialog();
    dialog.setLocationRelativeTo(this);
    dialog.setVisible(true);

    if (dialog.getValue() != null) {
      BoardEditor boardEditor = new BoardEditor(
              "Untitled", dialog.getValue()[0], dialog.getValue()[1]);
      boardEditor.addInternalFrameListener(this);
      boardEditor.setVisible(true);
      boardEditor.toFront();

      this.desktopPane.add(boardEditor);
      this.selectToolkitWindow(boardEditor);
    }
  }

  public Board openBoard(File file) {
    try {
      if (file.canRead()) {
        AssetHandle handle = AssetManager.getInstance().deserialize(
                new AssetDescriptor(EditorFileManager.getFileChooser().getSelectedFile().toURI()));
        Board board = (Board) handle.getAsset();

        return board;
      }
    } catch (IOException | AssetException ex) {
      Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
    }

    return null;
  }

  public void createNewEnemy() {
    Enemy enemy = new Enemy(null);
    enemy.setName("Untitled");

    EnemyEditor enemyEditor = new EnemyEditor(enemy);
    enemyEditor.addInternalFrameListener(this);
    enemyEditor.setVisible(true);
    enemyEditor.toFront();

    this.desktopPane.add(enemyEditor);
    this.selectToolkitWindow(enemyEditor);
  }

  /**
   * Creates an animation editor window for modifying the specified animation file.
   *
   * @param file
   * @return
   */
  public Enemy openEnemy(File file) {
    try {
      if (file.canRead()) {
        AssetHandle handle = AssetManager.getInstance().deserialize(
                new AssetDescriptor(EditorFileManager.getFileChooser().getSelectedFile().toURI()));
        Enemy enemy = (Enemy) handle.getAsset();

        return enemy;
      }
    } catch (IOException | AssetException ex) {
      Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
    }

    return null;
  }
  
  public void createNewItem() {
    Item item = new Item(null);
    item.setName("Untitled");
    
    ItemEditor itemEditor = new ItemEditor(item);
    itemEditor.addInternalFrameListener(this);
    itemEditor.setVisible(true);
    itemEditor.toFront();
    
    desktopPane.add(itemEditor);
    selectToolkitWindow(itemEditor);
  }
  
  public Item openItem(File file) {
    try {
      if (file.canRead()) {
        AssetHandle handle = AssetManager.getInstance().deserialize(
                new AssetDescriptor(EditorFileManager.getFileChooser().getSelectedFile().toURI()));
        Item item = (Item) handle.getAsset();

        return item;
      }
    } catch (IOException | AssetException ex) {
      Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
    }

    return null;
  }

  public void createNewCharacter() {
    Player player = new Player(null);
    player.setName("Untitled");

    CharacterEditor characterEditor = new CharacterEditor(player);
    characterEditor.addInternalFrameListener(this);
    characterEditor.setVisible(true);
    characterEditor.toFront();

    this.desktopPane.add(characterEditor);
    this.selectToolkitWindow(characterEditor);
  }

  /**
   * Creates a character editor window for modifying the specified character file.
   *
   * @param file
   * @return
   */
  public Player openCharacter(File file) {
    try {
      if (file.canRead()) {
        AssetHandle handle = AssetManager.getInstance().deserialize(
                new AssetDescriptor(EditorFileManager.getFileChooser().getSelectedFile().toURI()));
        Player player = (Player) handle.getAsset();

        return player;
      }
    } catch (IOException | AssetException ex) {
      Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
    }

    return null;
  }

  public void createNewTileset() {
    NewTilesetDialog dialog = new NewTilesetDialog();
    dialog.setLocationRelativeTo(this);
    dialog.setVisible(true);

    if (dialog.getValue() != null) {
      int tileWidth = dialog.getValue()[0];
      int tileHeight = dialog.getValue()[1];

      EditorFileManager.getFileChooser().resetChoosableFileFilters();
      FileNameExtensionFilter filter = new FileNameExtensionFilter(
              "Image Files", EditorFileManager.getImageExtensions());
      EditorFileManager.getFileChooser().setFileFilter(filter);

      if (EditorFileManager.getFileChooser().showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
        File file = EditorFileManager.getFileChooser().getSelectedFile();

        try (FileInputStream fis = new FileInputStream(file)) {
          BufferedImage source = ImageIO.read(fis);

          TileSet tileSet = TileSetRipper.rip(source, tileWidth, tileHeight);

          File tileSetFile = EditorFileManager.saveByType(TileSet.class);
          tileSet.setDescriptor(new AssetDescriptor(tileSetFile.toURI()));
          tileSet.setName(tileSetFile.getName());
          
          AssetManager.getInstance().serialize(
                  AssetManager.getInstance().getHandle(tileSet));
          
          openTileset(tileSetFile);
        } catch (IOException | AssetException ex) {
          Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
    }
  }

  public void openTileset(File file) {
    TileSet tileSet = TileSetCache.addTileSet(file.getName());
    tileSetPanel.addTileSet(tileSet);
    upperTabbedPane.setSelectedComponent(tileSetPanel);
  }

  public SpecialMove openSpecialMove(File file) {
    try {
      if (file.canRead()) {
        AssetHandle handle = AssetManager.getInstance().deserialize(
                new AssetDescriptor(EditorFileManager.getFileChooser().getSelectedFile().toURI()));
        SpecialMove move = (SpecialMove) handle.getAsset();

        return move;
      }
    } catch (IOException | AssetException ex) {
      Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
    }

    return null;
  }

  private void setupProject() {
    ProjectEditor projectEditor = new ProjectEditor(this.activeProject);
    this.desktopPane.add(projectEditor, BorderLayout.CENTER);

    projectEditor.addInternalFrameListener(this);
    projectEditor.toFront();

    this.selectToolkitWindow(projectEditor);
    this.setTitle(this.getTitle() + " - "
            + this.activeProject.getGameTitle());

    this.menuBar.enableMenus(true);
    this.toolBar.toggleButtonStates(true);
  }

  private void selectToolkitWindow(ToolkitEditorWindow window) {
    try {
      window.setSelected(true);
    } catch (PropertyVetoException ex) {
      Logger.getLogger(MainWindow.class.getName()).
              log(Level.SEVERE, null, ex);
    }
  }

}