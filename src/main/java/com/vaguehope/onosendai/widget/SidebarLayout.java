/* Original author's notice is below.  This class has been heavily modified.
 *
 * Copyright (C) 2012 0xlab - http://0xlab.org/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Authored by Julian Chu <walkingice AT 0xlab.org>
 */

package com.vaguehope.onosendai.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;

import com.vaguehope.onosendai.R;

public class SidebarLayout extends ViewGroup {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static final int DEFAULT_SIDEBAR_WIDTH = 150;
	private static final float SIDEBAR_MAX_WIDTH = 0.9f; // The max width of side bar is 90% of Parent.
	private static final int SLIDE_DURATION = 200; // 0.2 seconds?
	private static final double PROPORTION_THAT_COUNTS_AS_CLOSED = 0.25; // Consider closed if more that % dragged.

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	properties.

	private final int sidebarViewRes;
	private final int hostViewRes;
	private View sidebarView;
	private View hostView;
	private SidebarListener listener;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	state.

	private boolean sidebarOpen;
	private int sidebarWidth = DEFAULT_SIDEBAR_WIDTH; // assign default value. It will be overwrite in onMeasure by Layout XML resource.
	private Animation animation;
	private OpenListener openListener;
	private CloseListener closeListener;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public SidebarLayout (final Context context) {
		this(context, null);
	}

	public SidebarLayout (final Context context, final AttributeSet attrs) {
		super(context, attrs);

		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SidebarLayout);
		this.hostViewRes = a.getResourceId(R.styleable.SidebarLayout_hostView, -1);
		this.sidebarViewRes = a.getResourceId(R.styleable.SidebarLayout_sidebarView, -1);
		a.recycle();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public View getHostView () {
		if (this.hostView == null && this.hostViewRes > 0) {
			this.hostView = findViewById(this.hostViewRes);
		}
		if (this.hostView == null) throw new IllegalStateException("Host view is not set.");
		return this.hostView;
	}

	public void setHostView (final View hostView) {
		if (hostView == null) throw new IllegalArgumentException("Host view can not be null.");
		this.hostView = hostView;
	}

	public View getSidebarView () {
		if (this.sidebarView == null && this.sidebarViewRes > 0) {
			this.sidebarView = findViewById(this.sidebarViewRes);
		}
		if (this.sidebarView == null) throw new IllegalStateException("Side bar view is not set.");
		return this.sidebarView;
	}

	public void setSidebarView (final View sidebarView) {
		if (sidebarView == null) throw new IllegalArgumentException("Side bar view can not be null.");
		this.sidebarView = sidebarView;
	}

	public SidebarListener getListener () {
		return this.listener;
	}

	public void setListener (final SidebarListener l) {
		this.listener = l;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public boolean isOpen () {
		return this.sidebarOpen;
	}

	protected boolean setOpen (final boolean open) {
		if (this.sidebarOpen != open) {
			this.sidebarOpen = open;
			return true;
		}
		return false;
	}

	public void toggleSidebar () {
		animateSidebar(!this.sidebarOpen);
	}

	protected void animateSidebar (final boolean gotoOpen) {
		final View host = getHostView();
		if (host.getAnimation() != null) return;

		float deltaX;
		if (gotoOpen) {
			deltaX = host.getTranslationX() > 0 ? -host.getTranslationX() : -this.sidebarWidth;
			this.animation = new TranslateAnimation(0, deltaX, 0, 0);
			this.animation.setAnimationListener(this.openListener);
		}
		else {
			deltaX = this.sidebarWidth - host.getTranslationX();
			this.animation = new TranslateAnimation(0, deltaX, 0, 0);
			this.animation.setAnimationListener(this.closeListener);
		}
		this.animation.setDuration((long) (SLIDE_DURATION * (Math.abs(deltaX) / this.sidebarWidth)));
		this.animation.setFillAfter(true);
		this.animation.setFillEnabled(true);
		host.startAnimation(this.animation);
	}

	public boolean openSidebar () {
		if (!this.sidebarOpen) {
			toggleSidebar();
			return true;
		}
		return false;
	}

	public boolean closeSidebar () {
		if (this.sidebarOpen) {
			toggleSidebar();
			return true;
		}
		return false;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public void onFinishInflate () {
		super.onFinishInflate();
		this.openListener = new OpenListener(this);
		this.closeListener = new CloseListener(this);
		this.touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
	}

	@Override
	public void onLayout (final boolean changed, final int l, final int t, final int r, final int b) {
		// The title bar assign top padding, drop it.
		this.getSidebarView().layout(r - this.sidebarWidth, 0, r, 0 + this.getSidebarView().getMeasuredHeight());
		if (this.sidebarOpen) {
			this.getHostView().layout(l - this.sidebarWidth, 0, r - this.sidebarWidth, b);
		}
		else {
			this.getHostView().layout(l, 0, r, b);
		}
	}

	@Override
	public void onMeasure (final int w, final int h) {
		super.onMeasure(w, h);
		super.measureChildren(w, h);
		this.sidebarWidth = this.getSidebarView().getMeasuredWidth();
	}

	@Override
	protected void measureChild (final View child, final int parentWSpec, final int parentHSpec) {
		if (child == this.getSidebarView()) {
			int mode = MeasureSpec.getMode(parentWSpec);
			int width = (int) (getMeasuredWidth() * SIDEBAR_MAX_WIDTH);
			super.measureChild(child, MeasureSpec.makeMeasureSpec(width, mode), parentHSpec);
		}
		else {
			super.measureChild(child, parentWSpec, parentHSpec);
		}
	}

	private boolean intercepting = false;
	private int touchSlop;
	private float touchStartX;
	private float touchStartY;
	private boolean dragging = false;
	private boolean clicking = false;

	@Override
	public boolean onInterceptTouchEvent (final MotionEvent ev) {
		if (!isOpen()) return false;
		if (!eventWithinView(ev, getHostView())) return false;

		final int action = MotionEventCompat.getActionMasked(ev);
		if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
			this.intercepting = false;
			return false;
		}
		switch (action) {
			case MotionEvent.ACTION_DOWN:
			case MotionEvent.ACTION_MOVE:
			case MotionEvent.ACTION_UP:
				if (this.intercepting) return true;
				if (eventWithinView(ev, getHostView())) {
					this.intercepting = true;
					return true;
				}
				break;
		}
		return false;
	}

	@Override
	public boolean onTouchEvent (final MotionEvent ev) {
		switch (MotionEventCompat.getActionMasked(ev)) {
			case MotionEvent.ACTION_DOWN:
				this.clicking = true;
				this.dragging = false;
				this.touchStartX = ev.getX();
				this.touchStartY = ev.getY();
				return true;
			case MotionEvent.ACTION_MOVE:
				getParent().requestDisallowInterceptTouchEvent(true);
				if (!this.dragging && (
						Math.abs(ev.getX() - this.touchStartX) > this.touchSlop
						|| Math.abs(ev.getY() - this.touchStartY) > this.touchSlop)) {
					this.clicking = false;
					this.dragging = true;
				}
				if (this.dragging) {
					final float x = ev.getX() - this.touchStartX - this.sidebarWidth;
					if (x >= -this.sidebarWidth && x <= 0) this.getHostView().setX(x);
				}
				return true;
			case MotionEvent.ACTION_UP:
				if (this.clicking) {
					closeSidebar();
					return true;
				}
			case MotionEvent.ACTION_CANCEL:
				if (ev.getX() >= (this.sidebarWidth * PROPORTION_THAT_COUNTS_AS_CLOSED)) {
					closeSidebar();
				}
				else {
					animateSidebar(true);
				}
				getParent().requestDisallowInterceptTouchEvent(false);
				return true;
			default:
		}
		return false;
	}

	private static boolean eventWithinView (final MotionEvent ev, final View view) {
		final int x = (int) ev.getX();
		final int y = (int) ev.getY();
		return view.getLeft() < x && view.getRight() > x && view.getTop() < y && view.getBottom() > y;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static class OpenListener implements Animation.AnimationListener {

		private final SidebarLayout sidebarLayout;

		public OpenListener (final SidebarLayout sidebarLayout) {
			this.sidebarLayout = sidebarLayout;
		}

		@Override
		public void onAnimationStart (final Animation a) {
			this.sidebarLayout.getSidebarView().setVisibility(View.VISIBLE);
		}

		@Override
		public void onAnimationEnd (final Animation a) {
			this.sidebarLayout.getHostView().clearAnimation();
			this.sidebarLayout.getHostView().setTranslationX(0); // Clear offset from manual drag.
			final boolean stateChanged = this.sidebarLayout.setOpen(true);
			this.sidebarLayout.requestLayout();
			if (stateChanged) {
				final SidebarListener l = this.sidebarLayout.getListener();
				if (l != null) l.onSidebarOpened(this.sidebarLayout);
			}
		}

		@Override
		public void onAnimationRepeat (final Animation a) {/* Unused. */}
	}

	private static class CloseListener implements Animation.AnimationListener {

		private final SidebarLayout sidebarLayout;

		public CloseListener (final SidebarLayout sidebarLayout) {
			this.sidebarLayout = sidebarLayout;
		}

		@Override
		public void onAnimationEnd (final Animation a) {
			this.sidebarLayout.getHostView().clearAnimation();
			this.sidebarLayout.getHostView().setTranslationX(0); // Clear offset from manual drag.
			this.sidebarLayout.getSidebarView().setVisibility(View.INVISIBLE);
			final boolean stateChanged = this.sidebarLayout.setOpen(false);
			this.sidebarLayout.requestLayout();
			if (stateChanged) {
				final SidebarListener l = this.sidebarLayout.getListener();
				if (l != null) l.onSidebarClosed(this.sidebarLayout);
			}
		}

		@Override
		public void onAnimationRepeat (final Animation a) {/* Unused. */}

		@Override
		public void onAnimationStart (final Animation a) {/* Unused. */}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public interface SidebarListener {

		void onSidebarOpened (SidebarLayout sidebar);

		void onSidebarClosed (SidebarLayout sidebar);

	}

	public static class ToggleSidebarListener implements OnClickListener {

		private final SidebarLayout sidebar;

		public ToggleSidebarListener (final SidebarLayout sidebar) {
			this.sidebar = sidebar;
		}

		@Override
		public void onClick (final View v) {
			this.sidebar.toggleSidebar();
		}

	}

	public static class BackButtonListener implements OnKeyListener {

		private final SidebarLayout sidebar;

		public BackButtonListener (final SidebarLayout sidebar) {
			this.sidebar = sidebar;
		}

		@Override
		public boolean onKey (final View v, final int keyCode, final KeyEvent event) {
			if (keyCode == KeyEvent.KEYCODE_BACK) {
				return this.sidebar.closeSidebar();
			}
			return false;
		}

	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
