package net.ripper;

import net.ripper.carrom.ai.AIPlayerImpl;
import net.ripper.carrom.ai.Shot;
import net.ripper.carrom.managers.GameManager;
import net.ripper.carrom.managers.GameManager.GameState;
import net.ripper.carrom.managers.clients.IGameManagerClient;
import net.ripper.carrom.managers.model.Player;
import net.ripper.carrom.model.Piece;
import net.ripper.carrom.model.components.PolarLine;
import net.ripper.carrom.model.components.Polygon;
import net.ripper.carrom.model.components.Vector2f;
import net.ripper.carrom.renderer.RenderThread;
import net.ripper.util.Clock;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class MainGamePanel extends SurfaceView implements
		SurfaceHolder.Callback, OnGestureListener, IGameManagerClient {
	private static final String TAG = MainGamePanel.class.getSimpleName();

	RenderThread renderThread;
	GameManager gameManager;
	Clock clock;
	Clock clock2;

	static float startX;
	static float startY;
	static float endX;
	static float endY;

	Bitmap carromBoard;
	Bitmap striker;
	Bitmap queen;
	Bitmap black;
	Bitmap white;
	Paint fpsPaint;

	GestureDetector gesture;

	PolarLine guideLine;
	Paint guidePaint;
	Paint l1Paint;
	Paint l2Paint;
	Paint l3Paint;
	Paint l4Paint;
	Paint l5Paint;
	Paint aiRectPaint;
	AIPlayerImpl ai = new AIPlayerImpl();

	public MainGamePanel(Context context) {
		super(context);

		this.getHolder().addCallback(this);
		this.setFocusable(true);

		gameManager = new GameManager(2);

		renderThread = new RenderThread(this.getHolder(), this);

		clock = new Clock();
		clock2 = new Clock();
		gesture = new GestureDetector(this);

		carromBoard = BitmapFactory.decodeResource(this.getResources(),
				R.drawable.carromboard);
		striker = BitmapFactory.decodeResource(this.getResources(),
				R.drawable.striker);
		queen = BitmapFactory.decodeResource(this.getResources(),
				R.drawable.queen_12);

		black = BitmapFactory.decodeResource(this.getResources(),
				R.drawable.black_12);

		white = BitmapFactory.decodeResource(this.getResources(),
				R.drawable.white_12);

		fpsPaint = new Paint();
		fpsPaint.setARGB(255, 255, 255, 255);

		guideLine = new PolarLine(
				this.gameManager.board.shootingRect[0].centerX(),
				this.gameManager.board.shootingRect[0].centerY(),
				this.gameManager.board.boundsRect.height(),
				(float) (235 * Math.PI / 180));

		guidePaint = new Paint();
		guidePaint.setColor(Color.BLACK);
		guidePaint.setAntiAlias(true);
		guidePaint.setPathEffect(new DashPathEffect(new float[] { 10, 10 }, 0));

		l1Paint = new Paint();
		l1Paint.setColor(Color.GREEN);
		l1Paint.setAntiAlias(true);
		l1Paint.setPathEffect(new DashPathEffect(new float[] { 10, 10 }, 0));

		l2Paint = new Paint();
		l2Paint.setColor(Color.RED);
		l2Paint.setAntiAlias(true);
		l2Paint.setPathEffect(new DashPathEffect(new float[] { 10, 10 }, 0));

		l3Paint = new Paint();
		l3Paint.setColor(Color.BLUE);
		l3Paint.setAntiAlias(true);
		l3Paint.setPathEffect(new DashPathEffect(new float[] { 10, 10 }, 0));

		l4Paint = new Paint();
		l4Paint.setColor(Color.RED);
		l4Paint.setAntiAlias(true);
		l4Paint.setPathEffect(new DashPathEffect(new float[] { 10, 10 }, 0));

		l5Paint = new Paint();
		l5Paint.setColor(Color.BLUE);
		l5Paint.setAntiAlias(true);
		l5Paint.setPathEffect(new DashPathEffect(new float[] { 10, 10 }, 0));

		aiRectPaint = new Paint();
		aiRectPaint.setColor(Color.WHITE);
		aiRectPaint.setAntiAlias(true);
		aiRectPaint.setStyle(Style.STROKE);
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		renderThread.running = true;
		renderThread.start();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		while (true) {
			try {
				renderThread.join();
				break;
			} catch (InterruptedException e) {
			}
		}

		// try {
		// FileOutputStream outs = this.getContext().openFileOutput("pos", 0);
		// for (Piece piece : this.gameManager.blackPieces) {
		// String wstr = piece.region.x + "," + piece.region.y + "\n";
		// outs.write(wstr.getBytes());
		// }
		// for (Piece piece : this.gameManager.whitePieces) {
		// String wstr = piece.region.x + "," + piece.region.y + "\n";
		// outs.write(wstr.getBytes());
		// }
		//
		// String wstr = this.gameManager.queen.region.x + ","
		// + this.gameManager.queen.region.y + "\n";
		// outs.write(wstr.getBytes());
		//
		// outs.close();
		// } catch (Exception e) {
		//
		// e.printStackTrace();
		// }
	}

	// the fps to be displayed
	private String avgFps;

	public void setAvgFps(String avgFps) {
		this.avgFps = avgFps;
	}

	private void displayFps(Canvas canvas, String fps) {
		if (canvas != null && fps != null) {
			canvas.drawText(fps, this.getWidth() - 50, 20, fpsPaint);
		}
	}

	// Piece touchedPiece = null;

	@Override
	public boolean onTouchEvent(MotionEvent e) {
		if (e.getY() > this.getHeight() - 50) {
			renderThread.running = false;
			((Activity) this.getContext()).finish();
		}
		if (this.gameManager.gameState != GameState.STRIKER_SHOT_TAKEN) {
			if (e.getX() > this.getWidth() - 50) {
				Shot shot = ai
						.getShot(
								this.gameManager.blackPieces,
								this.gameManager.whitePieces,
								this.gameManager.striker,
								this.gameManager.queen,
								this.gameManager.board,
								this.gameManager.players[this.gameManager.currentPlayerIndex]);

				this.gameManager.striker.region.x = shot.strikerX;
				this.gameManager.striker.region.y = shot.strikerY;

				// Vector2f v = new Vector2f(ai.line2.getFinalX()
				// - ai.line2.originX, ai.line2.getFinalY()
				// - ai.line2.originY);
				// v = v.unitVector().mulScalar(shot.v);
				this.gameManager.gameState = GameState.STRIKER_SHOT_TAKEN;
				this.gameManager.takeShot(shot.strikerVelocity.x,
						shot.strikerVelocity.y);

				Log.d(TAG, "called ai..");
			}
		}

		// for (Piece piece : this.gameManager.blackPieces) {
		// if (piece.region.isPointInCircle(e.getX(), e.getY())) {
		// touchedPiece = piece;
		// }
		// }
		//
		// for (Piece piece : this.gameManager.whitePieces) {
		// if (piece.region.isPointInCircle(e.getX(), e.getY())) {
		// touchedPiece = piece;
		// }
		// }
		//
		// if (e.getAction() == MotionEvent.ACTION_MOVE) {
		// if (touchedPiece != null) {
		// touchedPiece.region.x = e.getX();
		// touchedPiece.region.y = e.getY();
		// }
		// }
		return gesture.onTouchEvent(e);
	}

	public float update() {
		return this.gameManager.update();
	}

	@Override
	public void onDraw(Canvas canvas) {
		// draw board
		// Bitmap bmp = Bitmap.createBitmap(300, 300, Config.ARGB_8888);
		// Canvas c = new Canvas(bmp);
		canvas.drawColor(Color.BLACK);
		canvas.drawBitmap(carromBoard, 0, 0, null);

		canvas.drawBitmap(striker, this.gameManager.striker.region.x
				- this.gameManager.striker.region.radius,
				this.gameManager.striker.region.y
						- this.gameManager.striker.region.radius, null);

		if (!this.gameManager.queen.inHole) {
			canvas.drawBitmap(queen, this.gameManager.queen.region.x
					- this.gameManager.queen.region.radius,
					this.gameManager.queen.region.y
							- this.gameManager.queen.region.radius, null);
		}

		for (Piece piece : this.gameManager.blackPieces) {
			if (!piece.inHole)
				canvas.drawBitmap(black, piece.region.x - piece.region.radius,
						piece.region.y - piece.region.radius, null);
		}

		for (Piece piece : this.gameManager.whitePieces) {
			if (!piece.inHole)
				canvas.drawBitmap(white, piece.region.x - piece.region.radius,
						piece.region.y - piece.region.radius, null);
		}

		displayFps(canvas, avgFps);

		if (gameManager.gameState == GameState.STRIKER_AIMING
				|| gameManager.gameState == GameState.STRIKER_SHOT_POWER) {
			drawGuide(canvas);
		}

		/** AI Visualization **/
		// // drawing rects made by ai
		if (gameManager.gameState != GameState.STRIKER_SHOT_TAKEN) {
			if (ai.polygons.size() > 0) {
				for (Polygon p : ai.polygons) {
					p.drawPolygon(canvas, aiRectPaint);
				}
			}

		}

		// // draw guides
		if (this.ai.line1 != null) {
			canvas.drawLine(ai.line1.originX, ai.line1.originY,
					ai.line1.getFinalX(), ai.line1.getFinalY(), l1Paint);
		}
		if (ai.line2 != null) {
			canvas.drawLine(ai.line2.originX, ai.line2.originY,
					ai.line2.getFinalX(), ai.line2.getFinalY(), l2Paint);
		}

		if (ai.line3 != null) {
			canvas.drawLine(ai.line3.originX, ai.line3.originY,
					ai.line3.getFinalX(), ai.line3.getFinalY(), l3Paint);
		}

		if (ai.line4 != null) {
			canvas.drawLine(ai.line4.originX, ai.line4.originY,
					ai.line4.getFinalX(), ai.line4.getFinalY(), l4Paint);
		}
		if (ai.line5 != null) {
			canvas.drawLine(ai.line5.originX, ai.line5.originY,
					ai.line5.getFinalX(), ai.line5.getFinalY(), l5Paint);
		}
		if (ai.strikerTest != null) {
			canvas.drawBitmap(striker, ai.strikerTest.region.x
					- ai.strikerTest.region.radius, ai.strikerTest.region.y
					- ai.strikerTest.region.radius, null);

		}
	}

	public void drawGuide(Canvas canvas) {
		canvas.drawLine(guideLine.originX, guideLine.originY,
				guideLine.getFinalX(), guideLine.getFinalY(), guidePaint);
	}

	@Override
	public boolean onDown(MotionEvent e) {
		return true;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		if (this.gameManager.gameState == GameState.STRIKER_SHOT_POWER) {
			if (this.gameManager.striker.region.isPointInCircle(e1.getX(),
					e1.getY())) {
				float vx = Math.abs(velocityX) > 500 ? Math.signum(velocityX) * 500
						: velocityX;

				float vy = Math.abs(velocityY) > 500 ? Math.signum(velocityY) * 500
						: velocityY;
				float resultant = (float) Math.sqrt(vx * vx + vy * vy);

				vx = (float) (resultant * Math.cos(guideLine.theta));
				vy = (float) (resultant * Math.sin(guideLine.theta));

				this.gameManager.takeShot(vx / 50, vy / 50);

				this.gameManager.gameState = GameState.STRIKER_SHOT_TAKEN;
			}
			return true;
		}
		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {
		if (this.gameManager.gameState != GameState.STRIKER_SHOT_TAKEN) {
			if (this.gameManager.striker.region.isPointInCircle(e.getX(),
					e.getY())) {
				if (this.gameManager.gameState == GameState.STRIKER_AIMING) {
					this.gameManager.gameState = GameState.STRIKER_POSITIONING;
					Log.d(TAG, "State:" + this.gameManager.gameState.toString());
				} else if (this.gameManager.gameState == GameState.STRIKER_SHOT_POWER) {
					this.gameManager.gameState = GameState.STRIKER_AIMING;
					this.guideLine.originX = this.gameManager.striker.region.x;
					this.guideLine.originY = this.gameManager.striker.region.y;
					Log.d(TAG, "State:" + this.gameManager.gameState.toString());
				}
			}
		}
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {

		if (!this.gameManager.striker.region.isPointInCircle(e1.getX(),
				e1.getY())) {

			if (this.gameManager.gameState == GameState.STRIKER_AIMING) {
				// TODO don't allow shooting backward
				float chX = e2.getX() - guideLine.originX;
				float chY = e2.getY() - guideLine.originY;
				float theta = (float) Math.atan(chY / chX);

				// check in which quadrant the point is
				if (chX < 0 && chY >= 0) {
					// 2nd quadrant
					theta = theta + (float) Math.PI;
				} else if (chX < 0 && chY < 0) {
					// 3rd quadrant
					theta = theta - (float) Math.PI;
				}

				guideLine.rotateTo(theta);
			} else if (this.gameManager.gameState == GameState.STRIKER_POSITIONING) {
				// TODO restrict striker movement within shooting rect
				// TODO don't allow regions where a coin is present
				boolean horizontal = true;
				Player[] players = this.gameManager.players;
				Rect currentShootingRect = this.gameManager.board.shootingRect[players[gameManager.currentPlayerIndex].shootingRectIndex];

				if (currentShootingRect.width() < currentShootingRect.height()) {
					horizontal = false;
				}

				if (horizontal) {
					this.gameManager.striker.region.x = e2.getX();
					guideLine.originX = this.gameManager.striker.region.x;
				} else {
					this.gameManager.striker.region.y = e2.getY();
					guideLine.originY = this.gameManager.striker.region.y;
				}
			}

			return true;
		}
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e) {

	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		if (this.gameManager.gameState != GameState.STRIKER_SHOT_TAKEN) {
			if (this.gameManager.striker.region.isPointInCircle(e.getX(),
					e.getY())) {
				if (this.gameManager.gameState == GameState.STRIKER_POSITIONING) {
					this.guideLine.originX = this.gameManager.striker.region.x;
					this.guideLine.originY = this.gameManager.striker.region.y;

					this.gameManager.gameState = GameState.STRIKER_AIMING;
					Log.d(TAG, "State:" + this.gameManager.gameState.toString());
				} else if (this.gameManager.gameState == GameState.STRIKER_AIMING) {
					this.gameManager.gameState = GameState.STRIKER_SHOT_POWER;
					Log.d(TAG, "State:" + this.gameManager.gameState.toString());
				}
				return true;
			}
		}
		return false;
	}

	@Override
	public void shotFinished() {
		// shot finished
		// reset guide position as of now
		this.guideLine.originX = this.gameManager.striker.region.x;
		this.guideLine.originY = this.gameManager.striker.region.y;

		this.invalidate();
	}

}
