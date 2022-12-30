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

package com.shatteredpixel.shatteredpixeldungeon.items.stones;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Tengu;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.ShadowCaster;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.BArray;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Point;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;

public class StoneOfDisarming extends Runestone {
	
	private static final int DIST = 3;
	
	{
		image = ItemSpriteSheet.STONE_DISARM;

		//so traps do not activate before the effect
		pressesCell = false;
	}

	@Override
	protected void activate(final int cell) {
		boolean[] FOV = new boolean[Dungeon.level.length()];
		Point c = Dungeon.level.cellToPoint(cell);
		ShadowCaster.castShadow(c.x, c.y, FOV, Dungeon.level.losBlocking, DIST);
		
		int sX = Math.max(0, c.x - DIST);
		int eX = Math.min(Dungeon.level.width()-1, c.x + DIST);
		
		int sY = Math.max(0, c.y - DIST);
		int eY = Math.min(Dungeon.level.height()-1, c.y + DIST);
		
		ArrayList<Trap> disarmCandidates = new ArrayList<>();

		ArrayList<Tengu.BombAbility.BombItem> tenguBombs = new ArrayList<>();
		ArrayList<Tengu.ShockerAbility.ShockerItem> tenguShockers = new ArrayList<>();
		ArrayList<Tengu.FireAbility> tenguFire = Tengu.FireAbility.instances;

		for (int y = sY; y <= eY; y++){
			for ( int x = sX; x <= eX; x++){
				int curr = y*Dungeon.level.width() + x;
				
				if (FOV[curr]){

					Heap h = Dungeon.level.heaps.get(curr);
					if(h != null) {
						for (int i = 0; i < h.size(); i++) {
							Item item = h.get(i);

							if(item instanceof Tengu.BombAbility.BombItem) {
								tenguBombs.add((Tengu.BombAbility.BombItem) item);
							}
							else if(item instanceof Tengu.ShockerAbility.ShockerItem) {
								tenguShockers.add((Tengu.ShockerAbility.ShockerItem) item);
							}
							else {
								break;
							}
						}
					}

					Trap t = Dungeon.level.traps.get(curr);
					if (t != null && t.active){
						disarmCandidates.add(t);
					}
					
				}
			}
		}
		
		Collections.sort(disarmCandidates, new Comparator<Trap>() {
			@Override
			public int compare(Trap o1, Trap o2) {
				float diff = Dungeon.level.trueDistance(cell, o1.pos) - Dungeon.level.trueDistance(cell, o2.pos);
				if (diff < 0){
					return -1;
				} else if (diff == 0){
					return Random.Int(2) == 0 ? -1 : 1;
				} else {
					return 1;
				}
			}
		});
		
		//disarms at most nine traps
		int maxTraps = 9;

		//Tengu bombs
		for(Tengu.BombAbility.BombItem b : tenguBombs) {
			b.deactivate(false);
			maxTraps--;
		}
		//Tengu shockers
		for(Tengu.ShockerAbility.ShockerItem s : tenguShockers) {
			s.deactivate();
			maxTraps--;
		}

		//Tengu fire
		ArrayList<Tengu.FireAbility> toRemove = new ArrayList<>();
		for(Tengu.FireAbility fire : Tengu.FireAbility.instances) {

			if(fire.clearCells(FOV)) {
				toRemove.add(fire);
			}

			maxTraps--;
		}

		for (Tengu.FireAbility fire : toRemove) {
			Tengu.FireAbility.instances.remove(fire);
		}


		//Traps
		if(maxTraps < 0) maxTraps = 0;
		while(disarmCandidates.size() > maxTraps) {
			disarmCandidates.remove(maxTraps);
		}

		for ( Trap t : disarmCandidates){
			t.reveal();
			t.disarm();
			CellEmitter.get(t.pos).burst(Speck.factory(Speck.STEAM), 6);
		}
		
		Sample.INSTANCE.play( Assets.Sounds.TELEPORT );
	}
}
