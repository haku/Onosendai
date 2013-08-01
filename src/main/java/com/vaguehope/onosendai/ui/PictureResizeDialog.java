package com.vaguehope.onosendai.ui;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.util.CountingDevNull;
import com.vaguehope.onosendai.util.ImageMetadata;
import com.vaguehope.onosendai.util.IoHelper;
import com.vaguehope.onosendai.util.Titleable;

public class PictureResizeDialog implements Titleable {

	private static final List<Scale> SCALES = Scale.setOf(100, 75, 50, 25);
	private static final int DEFAULT_SCALES_POS = 0;

	private static final List<Scale> QUALITIES = Scale.setOf(100, 95, 90, 80, 70, 50, 30);
	private static final int DEFAULT_QUALITY_POS = 2;

	private final Context context;
	private final ImageMetadata srcMetadata;

	private final View llParent;
	private final Spinner spnScale;
	private final Spinner spnQuality;
	private final TextView txtSummary;

	public PictureResizeDialog (final Context context, final Uri pictureUri) throws IOException {
		this.context = context;
		this.srcMetadata = new ImageMetadata(context, pictureUri);

		final Bitmap srcBmp = this.srcMetadata.readBitmap(); // FIXME do not do this on UI thread.
		if (srcBmp == null) throw new IllegalStateException("Failed to read: " + this.srcMetadata); // FIXME handle this better?

		final LayoutInflater inflater = LayoutInflater.from(context);
		this.llParent = inflater.inflate(R.layout.pictureresizedialog, null);

		this.spnScale = (Spinner) this.llParent.findViewById(R.id.spnScale);
		this.spnQuality = (Spinner) this.llParent.findViewById(R.id.spnQuality);
		this.txtSummary = (TextView) this.llParent.findViewById(R.id.txtSummary);

		final ArrayAdapter<Scale> scaleAdapter = new ArrayAdapter<Scale>(context, R.layout.numberspinneritem);
		scaleAdapter.addAll(SCALES);
		this.spnScale.setAdapter(scaleAdapter);
		this.spnScale.setSelection(DEFAULT_SCALES_POS);
		this.spnScale.setOnItemSelectedListener(this.spnChangeListener);

		final ArrayAdapter<Scale> qualityAdapter = new ArrayAdapter<Scale>(context, R.layout.numberspinneritem);
		qualityAdapter.addAll(QUALITIES);
		this.spnQuality.setAdapter(qualityAdapter);
		this.spnQuality.setSelection(DEFAULT_QUALITY_POS);
		this.spnQuality.setOnItemSelectedListener(this.spnChangeListener);
	}

	public View getRootView () {
		return this.llParent;
	}

	@Override
	public String getUiTitle () {
		return "Shrink " + this.srcMetadata.getName();
	}

	public Scale getScale () {
		return (Scale) this.spnScale.getSelectedItem();
	}

	public Scale getQuality () {
		return (Scale) this.spnQuality.getSelectedItem();
	}

	public Uri resizeToTempFile () throws IOException {
		final Bitmap src = this.srcMetadata.readBitmap();
		final File tgt = getTempOutputFile();
		final Bitmap shrunk = Bitmap.createScaledBitmap(src,
				scaleDimension(src.getWidth()),
				scaleDimension(src.getHeight()), true);
		final OutputStream tgtOut = new FileOutputStream(tgt);
		try {
			shrunk.compress(Bitmap.CompressFormat.JPEG, getQuality().getPercentage(), tgtOut);
			return Uri.fromFile(tgt);
		}
		finally {
			IoHelper.closeQuietly(tgtOut);
		}
	}

	public void recycle () {
		this.srcMetadata.recycle();
	}

	private final OnItemSelectedListener spnChangeListener = new OnItemSelectedListener() {
		@Override
		public void onItemSelected (final AdapterView<?> parent, final View view, final int position, final long id) {
			updateSummary();
		}

		@Override
		public void onNothingSelected (final AdapterView<?> arg0) {/**/}
	};

	protected void updateSummary () {
		final StringBuilder s = new StringBuilder();
		try {
			final Bitmap src = this.srcMetadata.readBitmap();
			s.append(src.getWidth()).append(" x ").append(src.getHeight())
					.append(" (").append(IoHelper.readableFileSize(this.srcMetadata.getSize())).append(")");

			final int w = scaleDimension(src.getWidth());
			final int h = scaleDimension(src.getHeight());
			s.append(" --> ").append(w).append(" x ").append(h);

			final Bitmap shrunk = Bitmap.createScaledBitmap(src, w, h, true);
			final CountingDevNull cdn = new CountingDevNull();
			shrunk.compress(Bitmap.CompressFormat.JPEG, getQuality().getPercentage(), cdn);
			s.append(" (").append(IoHelper.readableFileSize(cdn.getCount())).append(")");
		}
		catch (final IOException e) {
			s.delete(0, s.length());
			s.append(e.toString());
		}
		this.txtSummary.setText(s.toString());
	}

	private File getTempOutputFile () {
		final File baseDir = new File(this.context.getCacheDir(), "scaled");
		if (!baseDir.exists() && !baseDir.mkdirs()) throw new IllegalStateException("Failed to create temp directory: " + baseDir.getAbsolutePath());
		final String name = "shrunk_" + this.srcMetadata.getName();
		return new File(baseDir, name);
	}

	private int scaleDimension (final int from) {
		final Scale s = getScale();
		if (s.getPercentage() == 100) return from;
		return (int) (from * s.getPercentage() / 100d);
	}

	public static class Scale {

		private final int percentage;
		private final String title;

		public Scale (final int percentage, final String title) {
			this.percentage = percentage;
			this.title = title;
		}

		public int getPercentage () {
			return this.percentage;
		}

		@Override
		public String toString () {
			return this.title;
		}

		public static List<Scale> setOf (final int... a) {
			final List<Scale> ret = new ArrayList<Scale>();
			for (final int i : a) {
				ret.add(new Scale(i, i + "%"));
			}
			return Collections.unmodifiableList(ret);
		}

	}

}
