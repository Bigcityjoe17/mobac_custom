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
package mobac.mapsources.impl;

import mobac.program.interfaces.FileBasedMapSource;

/**
 * A {@link FileBasedMapSource} for debugging and testing purposes
 */
public class DebugRandomLocalMapSource extends DebugRandomMapSource implements FileBasedMapSource {

	@Override
	public String getName() {
		return "DebugRandomLocal";
	}

	@Override
	public String toString() {
		return "Debug Random (local)";
	}

}
