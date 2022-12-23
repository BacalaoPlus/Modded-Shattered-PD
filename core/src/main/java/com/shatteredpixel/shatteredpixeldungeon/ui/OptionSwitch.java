/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2022 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class OptionSwitch extends RedButton {

	private int currentState = 0;
	private int totalStates;

	ArrayList<String> optionLabels = new ArrayList<String>();

	public OptionSwitch(ArrayList<String> optionLabels ) {
		super( optionLabels.isEmpty() ? "" : optionLabels.get(0) );

		this.optionLabels = optionLabels;
		this.totalStates = optionLabels.size();
	}

	@Override
	protected void layout() {
		super.layout();

		/*
		float margin = (height - text.height()) / 2;
		
		text.setPos( x + margin, y + margin);
		PixelScene.align(text);
		*/
	}
	
	public int state() {
		return currentState;
	}
	
	public void setState( int value ) {
		value %= totalStates;

		currentState = value;
		text(optionLabels.get(currentState));

	}

	private void incrementState() {
		setState(currentState+1);
	}
	
	@Override
	protected void onClick() {
		super.onClick();

		incrementState();
	}
}
