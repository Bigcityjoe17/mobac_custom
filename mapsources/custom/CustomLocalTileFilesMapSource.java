/*******************************************************************************
 * Copyright (c) MOBAC developers
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 ******************************************************************************/
package mobac.mapsources.custom;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import mobac.exceptions.TileException;
import mobac.gui.mapview.PreviewMap;
import mobac.mapsources.MapSourceTools;
import mobac.mapsources.mapspace.MapSpaceFactory;
import mobac.program.interfaces.FileBasedMapSource;
import mobac.program.interfaces.MapSpace;
import mobac.program.jaxb.BooleanAdapter;
import mobac.program.jaxb.ColorAdapter;
import mobac.program.model.MapSourceLoaderInfo;
import mobac.program.model.TileImageType;
import mobac.utilities.I18nUtils;
import mobac.utilities.Utilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@XmlRootElement(name = "localTileFiles")
public class CustomLocalTileFilesMapSource implements FileBasedMapSource {

	private static final Logger log = LoggerFactory.getLogger(CustomLocalTileFilesMapSource.class);
	private final MapSpace mapSpace = MapSpaceFactory.getInstance(256, true);
	private final AtomicBoolean initialized = new AtomicBoolean(false);
	private MapSourceLoaderInfo loaderInfo = null;
	private String fileSyntax = null;

	private TileImageType tileImageType = null;

	@XmlElement(nillable = false, defaultValue = "CustomLocal")
	private String name = "Custom";

	private int minZoom = PreviewMap.MIN_ZOOM;

	private int maxZoom = PreviewMap.MAX_ZOOM;

	@XmlElement(required = true)
	private File sourceFolder = null;

	@XmlElement()
	private CustomMapSourceType sourceType = CustomMapSourceType.DIR_ZOOM_X_Y;

	@XmlElement(defaultValue = "false")
	@XmlJavaTypeAdapter(value = BooleanAdapter.class, type = boolean.class)
	private boolean invertYCoordinate = false;

	@XmlElement(defaultValue = "#000000")
	@XmlJavaTypeAdapter(ColorAdapter.class)
	private Color backgroundColor = Color.BLACK;

	public CustomLocalTileFilesMapSource() {
		super();
	}

	public synchronized void initialize() {
		if (initialized.get()) {
			return;
		}
		reinitialize();
	}

	public void reinitialize() {
		try {
			if (!sourceFolder.isDirectory()) {
				JOptionPane.showMessageDialog(null,
						String.format(I18nUtils.localizedStringForKey("msg_environment_invalid_source_folder"), name,
								sourceFolder.toString()),
						I18nUtils.localizedStringForKey("msg_environment_invalid_source_folder_title"),
						JOptionPane.ERROR_MESSAGE);
				initialized.set(true);
				return;
			}
			switch (sourceType) {
				case DIR_ZOOM_X_Y :
				case DIR_ZOOM_Y_X :
					initializeDirType();
					break;
				case QUADKEY :
					initializeQuadKeyType();
					break;
				default :
					throw new RuntimeException("Invalid source type");
			}
		} finally {
			initialized.set(true);
		}
	}

	private void initializeDirType() {
		/* Update zoom levels */
		FileFilter ff = new NumericDirFileFilter();
		File[] zoomDirs = sourceFolder.listFiles(ff);
		if (zoomDirs.length < 1) {
			JOptionPane.showMessageDialog(null,
					String.format(I18nUtils.localizedStringForKey("msg_environment_invalid_source_folder_zoom"), name,
							sourceFolder),
					I18nUtils.localizedStringForKey("msg_environment_invalid_source_folder_title"),
					JOptionPane.ERROR_MESSAGE);
			initialized.set(true);
			return;
		}
		int min = PreviewMap.MAX_ZOOM;
		int max = PreviewMap.MIN_ZOOM;
		for (File file : zoomDirs) {
			int z = Integer.parseInt(file.getName());
			min = Math.min(min, z);
			max = Math.max(max, z);
		}
		minZoom = min;
		maxZoom = max;

		for (File zDir : zoomDirs) {
			for (File xDir : zDir.listFiles(ff)) {
				try {
					xDir.listFiles(new FilenameFilter() {

						String syntax = "%d/%d/%d";

						public boolean accept(File dir, String name) {
							String[] parts = name.split("\\.");
							if (parts.length < 2 || parts.length > 3) {
								return false;
							}
							syntax += "." + parts[1];
							if (parts.length == 3) {
								syntax += "." + parts[2];
							}
							tileImageType = TileImageType.getTileImageType(parts[1]);
							fileSyntax = syntax;
							log.debug("Detected file syntax: " + fileSyntax + " tileImageType=" + tileImageType);
							throw new RuntimeException("break");
						}
					});
				} catch (RuntimeException e) {
				} catch (Exception e) {
					log.error(e.getMessage());
				}
				return;
			}
		}
	}

	private void initializeQuadKeyType() {
		String[] files = sourceFolder.list();
		Pattern p = Pattern.compile("([0123]+)\\.(png|gif|jpg)", Pattern.CASE_INSENSITIVE);
		String fileExt = null;
		for (String file : files) {
			Matcher m = p.matcher(file);
			if (!m.matches()) {
				continue;
			}
			fileExt = m.group(2);
			break;
		}
		if (fileExt == null) {
			return; // Error no suitable file found
		}
		fileSyntax = "%s." + fileExt;

		tileImageType = TileImageType.getTileImageType(fileExt);
		p = Pattern.compile("([0123]+)\\.(" + fileExt + ")", Pattern.CASE_INSENSITIVE);

		int min = PreviewMap.MAX_ZOOM;
		int max = 1;

		for (String file : files) {
			Matcher m = p.matcher(file);
			if (!m.matches()) {
				continue;
			}
			if (fileSyntax == null) {
				fileSyntax = "%s." + m.group(2);
			}
			int z = m.group(1).length();
			min = Math.min(min, z);
			max = Math.max(max, z);
		}
		minZoom = min;
		maxZoom = max;
	}

	public byte[] getTileData(int zoom, int x, int y, LoadMethod loadMethod) throws IOException {
		if (!initialized.get()) {
			initialize();
		}
		if (fileSyntax == null) {
			return null;
		}
		if (log.isTraceEnabled()) {
			log.trace(String.format("Loading tile z=%d x=%d y=%d", zoom, x, y));
		}

		if (invertYCoordinate) {
			y = ((1 << zoom) - y - 1);
		}
		String fileName;
		switch (sourceType) {
			case DIR_ZOOM_X_Y :
				fileName = String.format(fileSyntax, zoom, x, y);
				break;
			case DIR_ZOOM_Y_X :
				fileName = String.format(fileSyntax, zoom, y, x);
				break;
			case QUADKEY :
				fileName = String.format(fileSyntax, MapSourceTools.encodeQuadTree(zoom, x, y));
				break;
			default :
				throw new RuntimeException("Invalid source type");
		}
		File file = new File(sourceFolder, fileName);
		try {
			return Utilities.getFileBytes(file);
		} catch (FileNotFoundException e) {
			log.debug("Map tile file not found: \"" + file.getAbsolutePath() + "\"");
			return null;
		}
	}

	public BufferedImage getTileImage(int zoom, int x, int y, LoadMethod loadMethod)
			throws IOException, TileException, InterruptedException {
		byte[] data = getTileData(zoom, x, y, loadMethod);
		if (data == null) {
			return null;
		}
		return ImageIO.read(new ByteArrayInputStream(data));
	}

	public TileImageType getTileImageType() {
		return tileImageType;
	}

	public int getMaxZoom() {
		return maxZoom;
	}

	public int getMinZoom() {
		return minZoom;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return name;
	}

	public MapSpace getMapSpace() {
		return mapSpace;
	}

	public Color getBackgroundColor() {
		return backgroundColor;
	}

	@XmlTransient
	public MapSourceLoaderInfo getLoaderInfo() {
		return loaderInfo;
	}

	public void setLoaderInfo(MapSourceLoaderInfo loaderInfo) {
		this.loaderInfo = loaderInfo;
	}

	private static class NumericDirFileFilter implements FileFilter {

		private final Pattern p = Pattern.compile("^\\d+$");

		public boolean accept(File f) {
			if (!f.isDirectory()) {
				return false;
			}
			return p.matcher(f.getName()).matches();
		}

	}
}
