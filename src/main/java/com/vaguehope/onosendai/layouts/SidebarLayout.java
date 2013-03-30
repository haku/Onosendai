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

package com.vaguehope.onosendai.layouts;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;

import com.vaguehope.onosendai.R;

public class SidebarLayout extends ViewGroup {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static final int DEFAULT_SIDEBAR_WIDTH = 150;
	private static final int SLIDE_DURATION = 200; // 0.2 seconds?

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
	private boolean pressed = false;
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

	protected void setOpen (final boolean open) {
		this.sidebarOpen = open;
	}

	public void toggleSidebar () {
		if (this.getHostView().getAnimation() != null) return;

		if (this.sidebarOpen) {
			this.animation = new TranslateAnimation(0, this.sidebarWidth, 0, 0);
			this.animation.setAnimationListener(this.closeListener);
		}
		else {
			this.animation = new TranslateAnimation(0, -this.sidebarWidth, 0, 0);
			this.animation.setAnimationListener(this.openListener);
		}
		this.animation.setDuration(SLIDE_DURATION);
		this.animation.setFillAfter(true);
		this.animation.setFillEnabled(true);
		this.getHostView().startAnimation(this.animation);
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
		// The max width of side bar is 90% of Parent.
		if (child == this.getSidebarView()) {
			int mode = MeasureSpec.getMode(parentWSpec);
			int width = (int) (getMeasuredWidth() * 0.9);
			super.measureChild(child, MeasureSpec.makeMeasureSpec(width, mode), parentHSpec);
		}
		else {
			super.measureChild(child, parentWSpec, parentHSpec);
		}
	}

	@Override
	public boolean onInterceptTouchEvent (final MotionEvent ev) {
		if (!isOpen()) return false;

		int action = ev.getAction();
		if (action != MotionEvent.ACTION_UP && action != MotionEvent.ACTION_DOWN) {
			return false;
		}

		// if user press and release both on Content while side bar is opening,
		// call listener. otherwise, pass the event to child.
		int x = (int) ev.getX();
		int y = (int) ev.getY();
		if (this.getHostView().getLeft() < x && this.getHostView().getRight() > x && this.getHostView().getTop() < y && this.getHostView().getBottom() > y) {
			if (action == MotionEvent.ACTION_DOWN) {
				this.pressed = true;
			}

			if (this.pressed && action == MotionEvent.ACTION_UP && this.listener != null) {
				this.pressed = false;
				return this.listener.onContentTouchedWhenOpening(SidebarLayout.this);
			}
		}
		else {
			this.pressed = false;
		}

		return false;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private class OpenListener implements Animation.AnimationListener {

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
			setOpen(!isOpen());
			requestLayout();
			SidebarListener l = getListener();
			if (l != null) l.onSidebarOpened(SidebarLayout.this);
		}

		@Override
		public void onAnimationRepeat (final Animation a) {/* Unused. */}
	}

	private class CloseListener implements Animation.AnimationListener {

		private final SidebarLayout sidebarLayout;

		public CloseListener (final SidebarLayout sidebarLayout) {
			this.sidebarLayout = sidebarLayout;
		}

		@Override
		public void onAnimationEnd (final Animation a) {
			this.sidebarLayout.getHostView().clearAnimation();
			this.sidebarLayout.getSidebarView().setVisibility(View.INVISIBLE);
			setOpen(!isOpen());
			requestLayout();
			SidebarListener l = getListener();
			if (l != null) l.onSidebarClosed(SidebarLayout.this);
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

		boolean onContentTouchedWhenOpening (SidebarLayout sidebar);

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
