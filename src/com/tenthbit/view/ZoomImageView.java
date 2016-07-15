/*******************************************************************************
 * Copyright 2013 Tomasz Zawada
 * 
 * Based on the excellent PhotoView by Chris Banes:
 * https://github.com/chrisbanes/PhotoView
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.tenthbit.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Matrix.ScaleToFit;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.OverScroller;
import android.widget.Scroller;

public class ZoomImageView extends ImageView implements View.OnTouchListener,
        ViewTreeObserver.OnGlobalLayoutListener {

    /**
     * Interface definition for a callback to be invoked when the Photo is
     * tapped with a single tap.
     * 
     * @author tomasz.zawada@gmail.com
     */
    public static interface OnPhotoTapListener {
        /**
         * A callback to receive where the user taps on a photo. You will only
         * receive a callback if the user taps on the actual photo, tapping on
         * 'whitespace' will be ignored.
         * 
         * @param view
         *            - View the user tapped.
         * @param x
         *            - where the user tapped from the of the Drawable, as
         *            percentage of the Drawable width.
         * @param y
         *            - where the user tapped from the top of the Drawable, as
         *            percentage of the Drawable height.
         */
        public void onPhotoTap(View view, float x, float y);
    }

    /**
     * Interface definition for a callback to be invoked when the ImageView is
     * tapped with a single tap.
     * 
     * @author tomasz.zawada@gmail.com
     */
    public static interface OnViewTapListener {
        /**
         * A callback to receive where the user taps on a ImageView. You will
         * receive a callback if the user taps anywhere on the view, tapping on
         * 'whitespace' will not be ignored.
         * 
         * @param view
         *            - View the user tapped.
         * @param x
         *            - where the user tapped from the left of the View.
         * @param y
         *            - where the user tapped from the top of the View.
         */
        public void onViewTap(View view, float x, float y);
    }

    /**
     * 
     * The MultiGestureDetector manages the multi-finger pinch zoom, pan and tap
     * 
     * @author tomasz.zawada@gmail.com
     * 
     */
    private class MultiGestureDetector extends GestureDetector.SimpleOnGestureListener implements
            OnScaleGestureListener {
        private final ScaleGestureDetector scaleGestureDetector;
        private final GestureDetector gestureDetector;

        private VelocityTracker velocityTracker;
        private boolean isDragging;

        private float lastTouchX;
        private float lastTouchY;
        private float lastPointerCount;
        private final float scaledTouchSlop;
        private final float scaledMinimumFlingVelocity;

        public MultiGestureDetector(Context context) {
            scaleGestureDetector = new ScaleGestureDetector(context, this);

            gestureDetector = new GestureDetector(context, this);
            gestureDetector.setOnDoubleTapListener(this);

            final ViewConfiguration configuration = ViewConfiguration.get(context);
            scaledMinimumFlingVelocity = configuration.getScaledMinimumFlingVelocity();
            scaledTouchSlop = configuration.getScaledTouchSlop();
        }

        public boolean isScaling() {
            return scaleGestureDetector.isInProgress();
        }

        public boolean onTouchEvent(MotionEvent event) {
            if (gestureDetector.onTouchEvent(event)) {
                return true;
            }

            scaleGestureDetector.onTouchEvent(event);

            /*
             * Get the center x, y of all the pointers
             */
            float x = 0, y = 0;
            final int pointerCount = event.getPointerCount();
            for (int i = 0; i < pointerCount; i++) {
                x += event.getX(i);
                y += event.getY(i);
            }
            x = x / pointerCount;
            y = y / pointerCount;

            /*
             * If the pointer count has changed cancel the drag
             */
            if (pointerCount != lastPointerCount) {
                isDragging = false;
                if (velocityTracker != null) {
                    velocityTracker.clear();
                }
                lastTouchX = x;
                lastTouchY = y;
            }
            lastPointerCount = pointerCount;

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (velocityTracker == null) {
                        velocityTracker = VelocityTracker.obtain();
                    } else {
                        velocityTracker.clear();
                    }
                    velocityTracker.addMovement(event);

                    lastTouchX = x;
                    lastTouchY = y;
                    isDragging = false;
                    break;

                case MotionEvent.ACTION_MOVE: {
                    final float dx = x - lastTouchX, dy = y - lastTouchY;

                    if (isDragging == false) {
                        // Use Pythagoras to see if drag length is larger than
                        // touch slop
                        isDragging = Math.sqrt((dx * dx) + (dy * dy)) >= scaledTouchSlop;
                    }

                    if (isDragging) {
                        if (getDrawable() != null) {
                            suppMatrix.postTranslate(dx, dy);
                            checkAndDisplayMatrix();

                            /**
                             * Here we decide whether to let the ImageView's
                             * parent to start taking over the touch event.
                             * 
                             * First we check whether this function is enabled.
                             * We never want the parent to take over if we're
                             * scaling. We then check the edge we're on, and the
                             * direction of the scroll (i.e. if we're pulling
                             * against the edge, aka 'overscrolling', let the
                             * parent take over).
                             */
                            if (allowParentInterceptOnEdge && !multiGestureDetector.isScaling()) {
                                if ((scrollEdge == EDGE_BOTH)
                                        || ((scrollEdge == EDGE_LEFT) && (dx >= 1f))
                                        || ((scrollEdge == EDGE_RIGHT) && (dx <= -1f))) {

                                    if (getParent() != null) {
                                        getParent().requestDisallowInterceptTouchEvent(false);
                                    }
                                }
                            }
                        }

                        lastTouchX = x;
                        lastTouchY = y;

                        if (velocityTracker != null) {
                            velocityTracker.addMovement(event);
                        }
                    }
                    break;
                }
                case MotionEvent.ACTION_UP: {
                    if (isDragging) {
                        lastTouchX = x;
                        lastTouchY = y;

                        // Compute velocity within the last 1000ms
			if (velocityTracker != null) {
                        	velocityTracker.addMovement(event);
	                        velocityTracker.computeCurrentVelocity(1000);

        	                final float vX = velocityTracker.getXVelocity(), vY = velocityTracker
                                .getYVelocity();

	                        // If the velocity is greater than minVelocity perform
        	                // a fling
                	        if ((Math.max(Math.abs(vX), Math.abs(vY)) >= scaledMinimumFlingVelocity)
                        	        && (getDrawable() != null)) {
                            		currentFlingRunnable = new FlingRunnable(getContext());
	                            	currentFlingRunnable.fling(getWidth(), getHeight(), (int) -vX,
        	                            (int) -vY);
                	            	post(currentFlingRunnable);
                        	} 
		        }
                    }
		    break;
                }
                case MotionEvent.ACTION_CANCEL:
                    lastPointerCount = 0;
                    if (velocityTracker != null) {
                        velocityTracker.recycle();
                        velocityTracker = null;
                    }
                    break;
            }

            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scale = getScale();
            float scaleFactor = detector.getScaleFactor();

            if ((getDrawable() != null)
                    && (!(((scale >= maxScale) && (scaleFactor > 1f)) || ((scale <= 0.75) && (scaleFactor < 1f))))) {
                suppMatrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(),
                        detector.getFocusY());
                checkAndDisplayMatrix();
            }

            return true;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
        }

        @Override
        public boolean onDoubleTap(MotionEvent event) {
            try {
                float scale = getScale();
                float x = event.getX();
                float y = event.getY();

                if (scale < midScale) {
                    post(new AnimatedZoomRunnable(scale, midScale, x, y));
                } else if ((scale >= midScale) && (scale < maxScale)) {
                    post(new AnimatedZoomRunnable(scale, maxScale, x, y));
                } else {
                    post(new AnimatedZoomRunnable(scale, minScale, x, y));
                }
            } catch (Exception e) {
                // Can sometimes happen when getX() and getY() is called
            }

            return true;
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent event) {
            // Wait for the confirmed onDoubleTap() instead
            return false;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent event) {
            if (photoTapListener != null) {
                final RectF displayRect = getDisplayRect();

                if (null != displayRect) {
                    final float x = event.getX(), y = event.getY();

                    // Check to see if the user tapped on the photo
                    if (displayRect.contains(x, y)) {

                        float xResult = (x - displayRect.left) / displayRect.width();
                        float yResult = (y - displayRect.top) / displayRect.height();

                        photoTapListener.onPhotoTap(ZoomImageView.this, xResult, yResult);
                        return true;
                    }
                }
            }
            if (viewTapListener != null) {
                viewTapListener.onViewTap(ZoomImageView.this, event.getX(), event.getY());
            }

            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            if (longClickListener != null) {
                longClickListener.onLongClick(ZoomImageView.this);
            }
        }
    }

    /**
     * 
     * The ScrollerProxy encapsulates the Scroller and OverScroller classes.
     * OverScroller is available since API 9.
     * 
     * @author tomasz.zawada@gmail.com
     * 
     */
    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private class ScrollerProxy {

        private boolean isOld;
        private Object scroller;

        public ScrollerProxy(Context context) {
            if (VERSION.SDK_INT < VERSION_CODES.GINGERBREAD) {
                isOld = true;
                scroller = new Scroller(context);
            } else {
                isOld = false;
                scroller = new OverScroller(context);
            }
        }

        public boolean computeScrollOffset() {
            return isOld ? ((Scroller) scroller).computeScrollOffset() : ((OverScroller) scroller)
                    .computeScrollOffset();
        }

        public void fling(int startX, int startY, int velocityX, int velocityY, int minX, int maxX,
                int minY, int maxY, int overX, int overY) {

            if (isOld) {
                ((Scroller) scroller).fling(startX, startY, velocityX, velocityY, minX, maxX, minY,
                        maxY);
            } else {
                ((OverScroller) scroller).fling(startX, startY, velocityX, velocityY, minX, maxX,
                        minY, maxY, overX, overY);
            }
        }

        public void forceFinished(boolean finished) {
            if (isOld) {
                ((Scroller) scroller).forceFinished(finished);
            } else {
                ((OverScroller) scroller).forceFinished(finished);
            }
        }

        public int getCurrX() {
            return isOld ? ((Scroller) scroller).getCurrX() : ((OverScroller) scroller).getCurrX();
        }

        public int getCurrY() {
            return isOld ? ((Scroller) scroller).getCurrY() : ((OverScroller) scroller).getCurrY();
        }
    }

    private static final int EDGE_NONE = -1;
    private static final int EDGE_LEFT = 0;
    private static final int EDGE_RIGHT = 1;
    private static final int EDGE_BOTH = 2;

    public static final float DEFAULT_MAX_SCALE = 3.0f;
    public static final float DEFAULT_MID_SCALE = 1.75f;
    public static final float DEFAULT_MIN_SCALE = 1f;

    private float minScale = DEFAULT_MIN_SCALE;
    private float midScale = DEFAULT_MID_SCALE;
    private float maxScale = DEFAULT_MAX_SCALE;

    private boolean allowParentInterceptOnEdge = true;

    private MultiGestureDetector multiGestureDetector;

    // These are set so we don't keep allocating them on the heap
    private final Matrix baseMatrix = new Matrix();
    private final Matrix drawMatrix = new Matrix();
    private final Matrix suppMatrix = new Matrix();
    private final RectF displayRect = new RectF();
    private final float[] matrixValues = new float[9];

    // Listeners
    private OnPhotoTapListener photoTapListener;
    private OnViewTapListener viewTapListener;
    private OnLongClickListener longClickListener;

    private int top, right, bottom, left;
    private FlingRunnable currentFlingRunnable;
    private int scrollEdge = EDGE_BOTH;

    private boolean isZoomEnabled;
    private ScaleType scaleType = ScaleType.FIT_CENTER;

    public ZoomImageView(Context context) {
        this(context, null);
    }

    public ZoomImageView(Context context, AttributeSet attr) {
        this(context, attr, 0);
    }

    public ZoomImageView(Context context, AttributeSet attr, int defStyle) {
        super(context, attr, defStyle);

        super.setScaleType(ScaleType.MATRIX);

        setOnTouchListener(this);

        multiGestureDetector = new MultiGestureDetector(context);

        setIsZoomEnabled(true);
    }

    /**
     * Gets the Display Rectangle of the currently displayed Drawable. The
     * Rectangle is relative to this View and includes all scaling and
     * translations.
     * 
     * @return - RectF of Displayed Drawable
     */
    public final RectF getDisplayRect() {
        checkMatrixBounds();
        return getDisplayRect(getDisplayMatrix());
    }

    /**
     * @return The current minimum scale level. What this value represents
     *         depends on the current {@link android.widget.ImageView.ScaleType}
     */
    public float getMinScale() {
        return minScale;
    }

    /**
     * Sets the minimum scale level. What this value represents depends on the
     * current {@link android.widget.ImageView.ScaleType}.
     */
    public void setMinScale(float minScale) {
        checkZoomLevels(minScale, midScale, maxScale);
        this.minScale = minScale;
    }

    /**
     * @return The current middle scale level. What this value represents
     *         depends on the current {@link android.widget.ImageView.ScaleType}
     */
    public float getMidScale() {
        return midScale;
    }

    /**
     * Sets the middle scale level. What this value represents depends on the
     * current {@link android.widget.ImageView.ScaleType}.
     */
    public void setMidScale(float midScale) {
        checkZoomLevels(minScale, midScale, maxScale);
        this.midScale = midScale;
    }

    /**
     * @return The current maximum scale level. What this value represents
     *         depends on the current {@link android.widget.ImageView.ScaleType}
     */
    public float getMaxScale() {
        return maxScale;
    }

    /**
     * Sets the maximum scale level. What this value represents depends on the
     * current {@link android.widget.ImageView.ScaleType}.
     */
    public void setMaxScale(float maxScale) {
        checkZoomLevels(minScale, midScale, maxScale);
        this.maxScale = maxScale;
    }

    /**
     * Returns the current scale value
     * 
     * @return float - current scale value
     */
    public final float getScale() {
        suppMatrix.getValues(matrixValues);
        return matrixValues[Matrix.MSCALE_X];
    }

    /**
     * Return the current scale type in use by the ImageView.
     */
    @Override
    public final ScaleType getScaleType() {
        return scaleType;
    }

    /**
     * Controls how the image should be resized or moved to match the size of
     * the ImageView. Any scaling or panning will happen within the confines of
     * this {@link android.widget.ImageView.ScaleType}.
     * 
     * @param scaleType
     *            - The desired scaling mode.
     */
    @Override
    public final void setScaleType(ScaleType scaleType) {
        if (scaleType == ScaleType.MATRIX) {
            throw new IllegalArgumentException(scaleType.name()
                    + " is not supported in ZoomImageView");
        }

        if (scaleType != this.scaleType) {
            this.scaleType = scaleType;
            update();
        }
    }

    /**
     * Returns true if the ZoomImageView is set to allow zooming of Photos.
     * 
     * @return true if the ZoomImageView allows zooming.
     */
    public final boolean isZoomEnabled() {
        return isZoomEnabled;
    }

    /**
     * Allows you to enable/disable the zoom functionality on the ImageView.
     * When disable the ImageView reverts to using the FIT_CENTER matrix.
     * 
     * @param isZoomEnabled
     *            - Whether the zoom functionality is enabled.
     */
    public final void setIsZoomEnabled(boolean isZoomEnabled) {
        this.isZoomEnabled = isZoomEnabled;
        update();
    }

    /**
     * Whether to allow the ImageView's parent to intercept the touch event when
     * the photo is scroll to it's horizontal edge.
     */
    public void setAllowParentInterceptOnEdge(boolean allowParentInterceptOnEdge) {
        this.allowParentInterceptOnEdge = allowParentInterceptOnEdge;
    }

    @Override
    public void setImageBitmap(Bitmap bitmap) {
        super.setImageBitmap(bitmap);
        update();
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable);
        update();
    }

    @Override
    public void setImageResource(int resId) {
        super.setImageResource(resId);
        update();
    }

    @Override
    public void setImageURI(Uri uri) {
        super.setImageURI(uri);
        update();
    }

    /**
     * Register a callback to be invoked when the Photo displayed by this view
     * is long-pressed.
     * 
     * @param listener
     *            - Listener to be registered.
     */
    @Override
    public final void setOnLongClickListener(OnLongClickListener listener) {
        longClickListener = listener;
    }

    /**
     * Register a callback to be invoked when the Photo displayed by this View
     * is tapped with a single tap.
     * 
     * @param listener
     *            - Listener to be registered.
     */
    public final void setOnPhotoTapListener(OnPhotoTapListener listener) {
        photoTapListener = listener;
    }

    /**
     * Register a callback to be invoked when the View is tapped with a single
     * tap.
     * 
     * @param listener
     *            - Listener to be registered.
     */
    public final void setOnViewTapListener(OnViewTapListener listener) {
        viewTapListener = listener;
    }

    @Override
    public final void onGlobalLayout() {
        if (isZoomEnabled) {
            final int top = getTop();
            final int right = getRight();
            final int bottom = getBottom();
            final int left = getLeft();

            /**
             * We need to check whether the ImageView's bounds have changed.
             * This would be easier if we targeted API 11+ as we could just use
             * View.OnLayoutChangeListener. Instead we have to replicate the
             * work, keeping track of the ImageView's bounds and then checking
             * if the values change.
             */
            if ((top != this.top) || (bottom != this.bottom) || (left != this.left)
                    || (right != this.right)) {
                // Update our base matrix, as the bounds have changed
                updateBaseMatrix(getDrawable());

                // Update values as something has changed
                this.top = top;
                this.right = right;
                this.bottom = bottom;
                this.left = left;
            }
        }
    }

    @Override
    public final boolean onTouch(View v, MotionEvent ev) {
        boolean handled = false;

        if (isZoomEnabled) {
            switch (ev.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // First, disable the Parent from intercepting the touch
                    // event
                    if (v.getParent() != null) {
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                    }

                    // If we're flinging, and the user presses down, cancel
                    // fling
                    if (currentFlingRunnable != null) {
                        currentFlingRunnable.cancelFling();
                        currentFlingRunnable = null;
                    }
                    break;

                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    // If the user has zoomed less than min scale, zoom back
                    // to min scale
                    if (getScale() < minScale) {
                        RectF rect = getDisplayRect();
                        if (null != rect) {
                            v.post(new AnimatedZoomRunnable(getScale(), minScale, rect.centerX(),
                                    rect.centerY()));
                            handled = true;
                        }
                    }
                    break;
            }

            // Finally, try the scale/drag/tap detector
            if ((multiGestureDetector != null) && multiGestureDetector.onTouchEvent(ev)) {
                handled = true;
            }
        }

        return handled;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        getViewTreeObserver().addOnGlobalLayoutListener(this);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        getViewTreeObserver().removeGlobalOnLayoutListener(this);
    }

    protected Matrix getDisplayMatrix() {
        drawMatrix.set(baseMatrix);
        drawMatrix.postConcat(suppMatrix);
        return drawMatrix;
    }

    private final void update() {
        if (isZoomEnabled) {
            super.setScaleType(ScaleType.MATRIX);
            updateBaseMatrix(getDrawable());
        } else {
            resetMatrix();
        }
    }

    /**
     * Helper method that simply checks the Matrix, and then displays the result
     */
    private void checkAndDisplayMatrix() {
        checkMatrixBounds();
        setImageMatrix(getDisplayMatrix());
    }

    private void checkMatrixBounds() {
        final RectF rect = getDisplayRect(getDisplayMatrix());
        if (null == rect) {
            return;
        }

        final float height = rect.height(), width = rect.width();
        float deltaX = 0, deltaY = 0;

        final int viewHeight = getHeight();
        if (height <= viewHeight) {
            switch (scaleType) {
                case FIT_START:
                    deltaY = -rect.top;
                    break;
                case FIT_END:
                    deltaY = viewHeight - height - rect.top;
                    break;
                default:
                    deltaY = ((viewHeight - height) / 2) - rect.top;
                    break;
            }
        } else if (rect.top > 0) {
            deltaY = -rect.top;
        } else if (rect.bottom < viewHeight) {
            deltaY = viewHeight - rect.bottom;
        }

        final int viewWidth = getWidth();
        if (width <= viewWidth) {
            switch (scaleType) {
                case FIT_START:
                    deltaX = -rect.left;
                    break;
                case FIT_END:
                    deltaX = viewWidth - width - rect.left;
                    break;
                default:
                    deltaX = ((viewWidth - width) / 2) - rect.left;
                    break;
            }
            scrollEdge = EDGE_BOTH;
        } else if (rect.left > 0) {
            scrollEdge = EDGE_LEFT;
            deltaX = -rect.left;
        } else if (rect.right < viewWidth) {
            deltaX = viewWidth - rect.right;
            scrollEdge = EDGE_RIGHT;
        } else {
            scrollEdge = EDGE_NONE;
        }

        // Finally actually translate the matrix
        suppMatrix.postTranslate(deltaX, deltaY);
    }

    /**
     * Helper method that maps the supplied Matrix to the current Drawable
     * 
     * @param matrix
     *            - Matrix to map Drawable against
     * @return RectF - Displayed Rectangle
     */
    private RectF getDisplayRect(Matrix matrix) {
        Drawable d = getDrawable();
        if (null != d) {
            displayRect.set(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
            matrix.mapRect(displayRect);
            return displayRect;
        }

        return null;
    }

    /**
     * Resets the Matrix back to FIT_CENTER, and then displays it.s
     */
    private void resetMatrix() {
        suppMatrix.reset();
        setImageMatrix(getDisplayMatrix());
        checkMatrixBounds();
    }

    /**
     * Calculate Matrix for FIT_CENTER
     * 
     * @param d
     *            - Drawable being displayed
     */
    private void updateBaseMatrix(Drawable d) {
        if (null == d) {
            return;
        }

        final float viewWidth = getWidth();
        final float viewHeight = getHeight();
        final int drawableWidth = d.getIntrinsicWidth();
        final int drawableHeight = d.getIntrinsicHeight();

        baseMatrix.reset();

        final float widthScale = viewWidth / drawableWidth;
        final float heightScale = viewHeight / drawableHeight;

        if (scaleType == ScaleType.CENTER) {
            baseMatrix.postTranslate((viewWidth - drawableWidth) / 2F,
                    (viewHeight - drawableHeight) / 2F);

        } else if (scaleType == ScaleType.CENTER_CROP) {
            float scale = Math.max(widthScale, heightScale);
            baseMatrix.postScale(scale, scale);
            baseMatrix.postTranslate((viewWidth - (drawableWidth * scale)) / 2F,
                    (viewHeight - (drawableHeight * scale)) / 2F);

        } else if (scaleType == ScaleType.CENTER_INSIDE) {
            float scale = Math.min(1.0f, Math.min(widthScale, heightScale));
            baseMatrix.postScale(scale, scale);
            baseMatrix.postTranslate((viewWidth - (drawableWidth * scale)) / 2F,
                    (viewHeight - (drawableHeight * scale)) / 2F);

        } else {
            RectF mTempSrc = new RectF(0, 0, drawableWidth, drawableHeight);
            RectF mTempDst = new RectF(0, 0, viewWidth, viewHeight);

            switch (scaleType) {
                case FIT_CENTER:
                    baseMatrix.setRectToRect(mTempSrc, mTempDst, ScaleToFit.CENTER);
                    break;

                case FIT_START:
                    baseMatrix.setRectToRect(mTempSrc, mTempDst, ScaleToFit.START);
                    break;

                case FIT_END:
                    baseMatrix.setRectToRect(mTempSrc, mTempDst, ScaleToFit.END);
                    break;

                case FIT_XY:
                    baseMatrix.setRectToRect(mTempSrc, mTempDst, ScaleToFit.FILL);
                    break;

                default:
                    break;
            }
        }

        resetMatrix();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void postOnAnimation(View view, Runnable runnable) {
        if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN) {
            view.postOnAnimation(runnable);
        } else {
            view.postDelayed(runnable, 16);
        }
    }

    private void checkZoomLevels(float minZoom, float midZoom, float maxZoom) {
        if (minZoom >= midZoom) {
            throw new IllegalArgumentException("MinZoom should be less than MidZoom");
        } else if (midZoom >= maxZoom) {
            throw new IllegalArgumentException("MidZoom should be less than MaxZoom");
        }
    }

    private class AnimatedZoomRunnable implements Runnable {
        // These are 'postScale' values, means they're compounded each iteration
        static final float ANIMATION_SCALE_PER_ITERATION_IN = 1.07f;
        static final float ANIMATION_SCALE_PER_ITERATION_OUT = 0.93f;

        private final float focalX, focalY;
        private final float targetZoom;
        private final float deltaScale;

        public AnimatedZoomRunnable(final float currentZoom, final float targetZoom,
                final float focalX, final float focalY) {
            this.targetZoom = targetZoom;
            this.focalX = focalX;
            this.focalY = focalY;

            if (currentZoom < targetZoom) {
                deltaScale = ANIMATION_SCALE_PER_ITERATION_IN;
            } else {
                deltaScale = ANIMATION_SCALE_PER_ITERATION_OUT;
            }
        }

        public void run() {
            suppMatrix.postScale(deltaScale, deltaScale, focalX, focalY);
            checkAndDisplayMatrix();

            final float currentScale = getScale();

            if (((deltaScale > 1f) && (currentScale < targetZoom))
                    || ((deltaScale < 1f) && (targetZoom < currentScale))) {
                // We haven't hit our target scale yet, so post ourselves
                // again
                postOnAnimation(ZoomImageView.this, this);

            } else {
                // We've scaled past our target zoom, so calculate the
                // necessary scale so we're back at target zoom
                final float delta = targetZoom / currentScale;
                suppMatrix.postScale(delta, delta, focalX, focalY);
                checkAndDisplayMatrix();
            }
        }
    }

    private class FlingRunnable implements Runnable {
        private final ScrollerProxy scroller;
        private int currentX, currentY;

        public FlingRunnable(Context context) {
            scroller = new ScrollerProxy(context);
        }

        public void cancelFling() {
            scroller.forceFinished(true);
        }

        public void fling(int viewWidth, int viewHeight, int velocityX, int velocityY) {
            final RectF rect = getDisplayRect();
            if (null == rect) {
                return;
            }

            final int startX = Math.round(-rect.left);
            final int minX, maxX, minY, maxY;

            if (viewWidth < rect.width()) {
                minX = 0;
                maxX = Math.round(rect.width() - viewWidth);
            } else {
                minX = maxX = startX;
            }

            final int startY = Math.round(-rect.top);
            if (viewHeight < rect.height()) {
                minY = 0;
                maxY = Math.round(rect.height() - viewHeight);
            } else {
                minY = maxY = startY;
            }

            currentX = startX;
            currentY = startY;

            // If we actually can move, fling the scroller
            if ((startX != maxX) || (startY != maxY)) {
                scroller.fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY, 0, 0);
            }
        }

        @Override
        public void run() {
            if (scroller.computeScrollOffset()) {
                final int newX = scroller.getCurrX();
                final int newY = scroller.getCurrY();

                suppMatrix.postTranslate(currentX - newX, currentY - newY);
                setImageMatrix(getDisplayMatrix());

                currentX = newX;
                currentY = newY;

                // Post On animation
                postOnAnimation(ZoomImageView.this, this);
            }
        }
    }
}
