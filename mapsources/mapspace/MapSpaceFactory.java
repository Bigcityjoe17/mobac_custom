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
package mobac.mapsources.mapspace;

import mobac.program.interfaces.MapSpace;

public class MapSpaceFactory {

	/**
	 * @param tileSize
	 * @param isSpherical
	 * @return
	 */
	public static MapSpace getInstance(int tileSize, boolean isSpherical) {
		if (isSpherical) {
			return new MercatorPower2MapSpace(tileSize);
		}
		return new MercatorPower2MapSpaceEllipsoidal(tileSize);

	}

}
