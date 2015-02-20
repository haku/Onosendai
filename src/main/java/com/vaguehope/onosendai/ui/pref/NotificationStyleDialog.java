package com.vaguehope.onosendai.ui.pref;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.ToggleButton;

import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.config.NotificationStyle;
import com.vaguehope.onosendai.util.Titleable;

class NotificationStyleDialog implements Titleable {

	private final View llParent;
	private final ToggleButton chkNotify;
	private final CheckBox chkLights;
	private final CheckBox chkVibrate;
	private final CheckBox chkSound;

	public NotificationStyleDialog (final Context context, final NotificationStyle initialValue) {
		final LayoutInflater inflater = LayoutInflater.from(context);
		this.llParent = inflater.inflate(R.layout.notificationstyledialog, null);

		this.chkNotify = (ToggleButton) this.llParent.findViewById(R.id.chkNotify);
		this.chkLights = (CheckBox) this.llParent.findViewById(R.id.chkLights);
		this.chkVibrate = (CheckBox) this.llParent.findViewById(R.id.chkVibrate);
		this.chkSound = (CheckBox) this.llParent.findViewById(R.id.chkSound);

		this.chkNotify.setChecked(initialValue != null);
		this.chkNotify.setOnClickListener(this.chkNotifyClickListener);

		if (initialValue != null) {
			this.chkLights.setChecked(initialValue.isLights());
			this.chkVibrate.setChecked(initialValue.isVibrate());
			this.chkSound.setChecked(initialValue.isSound());
		}
	}

	private final OnClickListener chkNotifyClickListener = new OnClickListener() {
		@Override
		public void onClick (final View v) {
			chkNotifyClick();
		}
	};

	protected void chkNotifyClick () {
		final boolean en = this.chkNotify.isChecked();
		this.chkLights.setEnabled(en);
		this.chkVibrate.setEnabled(en);
		this.chkSound.setEnabled(en);
	}

	@Override
	public String getUiTitle () {
		return "Notification Style"; //ES
	}

	public View getRootView () {
		return this.llParent;
	}

	public NotificationStyle getValue () {
		if (!this.chkNotify.isChecked()) return null;
		return new NotificationStyle(
				this.chkLights.isChecked(),
				this.chkVibrate.isChecked(),
				this.chkSound.isChecked());
	}

}
