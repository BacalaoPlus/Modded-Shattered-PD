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

package com.watabou.noosa;

import com.watabou.glwrap.Matrix;
import com.watabou.input.GameAction;
import com.watabou.input.KeyBindings;
import com.watabou.input.KeyEvent;
import com.watabou.utils.Point;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;
import com.watabou.utils.Signal;

import java.util.ArrayList;

public class Camera extends Gizmo {

	private static ArrayList<Camera> all = new ArrayList<>();

	public boolean panEnemies = false;

	public String cameraMode;

	public static final String FOLLOW				= "follow";
	public static final String PREDICT_FOLLOW		= "predictFollow";
	public static final String SEMI_LOCKED			= "semiLocked";
	public static final String LOCKED				= "locked";

	public void changeCameraMode(String newCameraMode) {
		cameraMode = newCameraMode;

		resetFollow();
	}

	private void resetFollow() {
		shift = PointF.zero;
		lastMove = PointF.zero;
		followTarget = null;

		isFollowingHero = false;
	}

	protected static float invW2;
	protected static float invH2;
	
	public static Camera main;

	public boolean fullScreen;

	public float zoom;
	
	public int x;
	public int y;
	public int width;
	public int height;

	public static float TILE = 16f;
	
	int screenWidth;
	int screenHeight;
	
	public float[] matrix;

	public boolean scrollable = false;
	public PointF scroll;
	public PointF centerOffset;

	//used for predict follow
	public PointF lastMove = new PointF();
	//used for semi locked
	public PointF shift = new PointF();
	
	private float shakeMagX		= 10f;
	private float shakeMagY		= 10f;
	private float shakeTime		= 0f;
	private float shakeDuration	= 1f;
	
	protected float shakeX;
	protected float shakeY;
	
	public static Camera reset() {
		return reset( createFullscreen( 1 ) );
	}
	
	public static synchronized Camera reset( Camera newCamera ) {
		invW2 = 2f / Game.width;
		invH2 = 2f / Game.height;
		
		int length = all.size();
		for (int i=0; i < length; i++) {
			all.get( i ).destroy();
		}
		all.clear();
		
		return main = add( newCamera );
	}
	
	public static synchronized Camera add( Camera camera ) {
		all.add( camera );
		return camera;
	}
	
	public static synchronized Camera remove( Camera camera ) {
		all.remove( camera );
		return camera;
	}
	
	public static synchronized void updateAll() {
		int length = all.size();
		for (int i=0; i < length; i++) {
			Camera c = all.get( i );
			if (c != null && c.exists && c.active) {
				c.update();
			}
		}
	}
	
	public static Camera createFullscreen( float zoom ) {
		int w = (int)Math.ceil( Game.width / zoom );
		int h = (int)Math.ceil( Game.height / zoom );
		Camera c = new Camera(
				(int)(Game.width - w * zoom) / 2,
				(int)(Game.height - h * zoom) / 2,
				w, h, zoom );
		c.fullScreen = true;
		return c;
	}
	
	public Camera( int x, int y, int width, int height, float zoom ) {
		
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.zoom = zoom;
		changeCameraMode(FOLLOW);

		screenWidth = (int)(width * zoom);
		screenHeight = (int)(height * zoom);
		
		scroll = new PointF();
		centerOffset = new PointF();
		
		matrix = new float[16];
		Matrix.setIdentity( matrix );
	}
	
	@Override
	public void destroy() {
		panIntensity = 0f;
	}
	
	public void zoom( float value ) {
		zoom( value,
			scroll.x + width / 2,
			scroll.y + height / 2 );
	}
	
	public void zoom( float value, float fx, float fy ) {

		PointF offsetAdjust = centerOffset.clone();
		centerOffset.scale(zoom).invScale(value);

		zoom = value;
		width = (int)(screenWidth / zoom);
		height = (int)(screenHeight / zoom);
		
		snapTo( fx - offsetAdjust.x, fy - offsetAdjust.y );
	}
	
	public void resize( int width, int height ) {
		this.width = width;
		this.height = height;
		screenWidth = (int)(width * zoom);
		screenHeight = (int)(height * zoom);
	}
	
	Visual followTarget = null;
	PointF panTarget;
	//camera moves at a speed such that it will pan to its current target in 1/intensity seconds
	//keep in mind though that this speed is constantly decreasing, so actual pan time is higher
	float panIntensity = 0f;

	float intensityMultiplier = 1f;

	//what percentage of the screen to ignore when follow panning.
	// 0% means always keep in the center, 50% would mean pan until target is within center 50% of screen
	//if predict follow is active, this is the percentage of screen to move past the target
	float followDeadzone = 0f;

	private void calculateFollowTarget() {
		float deadX = width * followDeadzone /2f;
		float deadY = height * followDeadzone /2f;

		PointF deadP = new PointF(deadX, deadY);

		if(cameraMode == FOLLOW) {

			panTarget = followTarget.center().offset(centerOffset);

			//--
			panTarget.offset(scrollToCenter(scroll).negate());

			if (panTarget.x > deadX){
				panTarget.x -= deadX;
			} else if (panTarget.x < -deadX){
				panTarget.x += deadX;
			} else {
				panTarget.x = 0;
			}

			if (panTarget.y > deadY){
				panTarget.y -= deadY;
			} else if (panTarget.y < -deadY){
				panTarget.y += deadY;
			} else {
				panTarget.y = 0;
			}

			panTarget.offset(scrollToCenter(scroll));
			//--
		} else if(cameraMode == PREDICT_FOLLOW) {
			panTarget = followTarget.center().offset(centerOffset);

			//lastMove.normalize();
			shift = PointF.mult(lastMove, deadP);
			shift.scale(0.5f);

			panTarget.offset(shift);

		} else if(cameraMode == SEMI_LOCKED) {
			//do nothing
		} else if(cameraMode == LOCKED) {
			//do nothing
		}
	}

	private boolean isFollowingHero = false;

	@Override
	public void update() {
		super.update();

		if (isFollowingHero) {
			calculateFollowTarget();
		} else {
			if(followTarget != null) {
				panTarget = followTarget.center().offset(centerOffset);
			}
		}

		PointF panMove = new PointF();

		if (panIntensity > 0f){
			panMove = PointF.diff(panTarget, scrollToCenter(scroll));
			panMove.scale(Math.min(1f, Game.elapsed * panIntensity));

			scroll.offset(panMove);
		}

		if(cameraMode == SEMI_LOCKED && isFollowingHero) {
			shift.subtract(panMove);
		}
		
		if ((shakeTime -= Game.elapsed) > 0) {
			float damping = shakeTime / shakeDuration;
			shakeX = Random.Float( -shakeMagX, +shakeMagX ) * damping;
			shakeY = Random.Float( -shakeMagY, +shakeMagY ) * damping;
		} else {
			shakeX = 0;
			shakeY = 0;
		}
		
		updateMatrix();
	}
	
	public PointF center() {
		return new PointF( width / 2, height / 2 );
	}
	
	public boolean hitTest( float x, float y ) {
		return x >= this.x && y >= this.y && x < this.x + screenWidth && y < this.y + screenHeight;
	}
	
	public void shift( PointF point ){
		scroll.offset(point);

		panIntensity = 0f;
		resetFollow();
	}

	public void setCenterOffset( float x, float y ){
		scroll.x    += x - centerOffset.x;
		scroll.y    += y - centerOffset.y;
		if (panTarget != null) {
			panTarget.x += x - centerOffset.x;
			panTarget.y += y - centerOffset.y;
		}
		centerOffset.set(x, y);
	}
	
	public void snapTo(float x, float y ) {
		scroll.set( x - width / 2, y - height / 2 ).offset(centerOffset);
		panIntensity = 0f;

		resetFollow();
	}
	
	public void snapTo(PointF point ) {
		snapTo( point.x, point.y );
	}

	public void panTo( PointF dst, float intensity ){
		panTarget = dst.offset(centerOffset);
		panIntensity = intensity;

		resetFollow();
	}

	private PointF scrollToCenter(PointF p) {
		PointF newP = p.clone();
		newP.offset(new PointF(width/2f, height/2f));

		return newP;
	}

	public void panFollow(Visual target, float intensity ){

		resetFollow();

		followTarget = target;
		panIntensity = intensity;
	}

	public void panFollowHero(Visual target, float intensity, PointF panDirection ){
		panFollowHero(target, intensity, panDirection, false);
	}

	public void panFollowHero(Visual target, float intensity, PointF panDirection, boolean movement ){

		isFollowingHero = true;
		lastMove = panDirection;

		followTarget = target;
		panIntensity = intensity * intensityMultiplier;

		if (cameraMode == FOLLOW) {
			//do nothing
		}
		else if(cameraMode == PREDICT_FOLLOW) {
			panIntensity /= 4f;
		}
		else if(cameraMode == SEMI_LOCKED) {
			if(movement) {
				panTarget = scrollToCenter(scroll);
				shift.offset(lastMove.scale(Camera.TILE));

				panTarget.offset(shift);
			}
		}
		else if (cameraMode == LOCKED) {
			panTarget = scrollToCenter(scroll);
		}
	}

	public void panEnemies(Point[] positions, float intensity) {
		if(panEnemies && positions.length > 0) {
			PointF middlePoint = getMiddlePoint(positions);
			middlePoint.scale(Camera.TILE);

			panTo(middlePoint, intensity);
		}
	}

	public void setFollowDeadzone( float deadzone ){
		followDeadzone = deadzone;
	}

	public float intensityMultiplier() {
		return intensityMultiplier;
	}

	public void setIntensityMultiplier(float intensityMultiplier) {
		this.intensityMultiplier = intensityMultiplier;
	}
	
	public PointF screenToCamera( int x, int y ) {
		return new PointF(
			(x - this.x) / zoom + scroll.x,
			(y - this.y) / zoom + scroll.y );
	}
	
	public Point cameraToScreen( float x, float y ) {
		return new Point(
			(int)((x - scroll.x) * zoom + this.x),
			(int)((y - scroll.y) * zoom + this.y));
	}
	
	public float screenWidth() {
		return width * zoom;
	}
	
	public float screenHeight() {
		return height * zoom;
	}
	
	protected void updateMatrix() {

	/*	Matrix.setIdentity( matrix );
		Matrix.translate( matrix, -1, +1 );
		Matrix.scale( matrix, 2f / G.width, -2f / G.height );
		Matrix.translate( matrix, x, y );
		Matrix.scale( matrix, zoom, zoom );
		Matrix.translate( matrix, scroll.x, scroll.y );*/
		
		matrix[0] = +zoom * invW2;
		matrix[5] = -zoom * invH2;
		
		matrix[12] = -1 + x * invW2 - (scroll.x + shakeX) * matrix[0];
		matrix[13] = +1 - y * invH2 - (scroll.y + shakeY) * matrix[5];
		
	}
	
	public void shake( float magnitude, float duration ) {
		shakeMagX = shakeMagY = magnitude;
		shakeTime = shakeDuration = duration;
	}

	private PointF getMiddlePoint(Point[] positions) {
		int size = positions.length;

		Point sumOfAll = new Point();

		for (int i = 0; i < positions.length; i++) {
			sumOfAll.offset(positions[i]);
		}

		PointF middlePoint = new PointF(sumOfAll);
		middlePoint.scale(1f/size);

		return middlePoint;
	}
}
