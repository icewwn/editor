/**
 * Copyright (c) 2015, rpgtoolkit.net <help@rpgtoolkit.net>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package net.rpgtoolkit.editor.editors;

import java.util.EventListener;

/**
 *
 *
 * @author Joshua Michael Daly
 */
public interface TileSelectionListener extends EventListener {

  /**
   *
   * @param e
   */
  public void tileSelected(TileSelectionEvent e);

  /**
   *
   * @param e
   */
  public void tileRegionSelected(TileRegionSelectionEvent e);
}
