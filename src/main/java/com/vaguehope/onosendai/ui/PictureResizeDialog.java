package com.vaguehope.onosendai.ui;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import local.apache.ByteArrayOutputStream;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.util.ImageMetadata;
import com.vaguehope.onosendai.util.IoHelper;
import com.vaguehope.onosendai.util.Titleable;

public class PictureResizeDialog implements Titleable {

	private static final List<Scale> SCALES = Scale.setOf(100, 75, 50, 25);
	private static final int DEFAULT_SCALES_POS = 0;

	private static final List<Scale> QUALITIES = Scale.setOf(100, 95, 90, 80, 70, 50, 30, 10);
	private static final int DEFAULT_QUALITY_POS = 2;

	private final Context context;
	private final ImageMetadata srcMetadata;

	private final View llParent;
	private final ImageView imgPreview;
	private final Spinner spnScale;
	private final Spinner spnQuality;
	private final ProgressBar prgRedrawing;
	private final TextView txtSummary;

	public PictureResizeDialog (final Context context, final Uri pictureUri) throws IOException {
		this.context = context;
		this.srcMetadata = new ImageMetadata(context, pictureUri);

		final Bitmap srcBmp = this.srcMetadata.readBitmap(); // FIXME do not do this on UI thread.
		if (srcBmp == null) throw new IllegalStateException("Failed to read: " + this.srcMetadata); // FIXME handle this better?

		final LayoutInflater inflater = LayoutInflater.from(context);
		this.llParent = inflater.inflate(R.layout.pictureresizedialog, null);

		this.imgPreview = (ImageView) this.llParent.findViewById(R.id.imgPreview);
		this.spnScale = (Spinner) this.llParent.findViewById(R.id.spnScale);
		this.spnQuality = (Spinner) this.llParent.findViewById(R.id.spnQuality);
		this.prgRedrawing = (ProgressBar) this.llParent.findViewById(R.id.prgRedrawing);
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
			if (shrunk != src) shrunk.recycle();
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
		new PreviewResizeTask(this.txtSummary, this.imgPreview, this.prgRedrawing, this.srcMetadata, this).execute();
	}

	private File getTempOutputFile () {
		final File baseDir = new File(this.context.getCacheDir(), "scaled");
		if (!baseDir.exists() && !baseDir.mkdirs()) throw new IllegalStateException("Failed to create temp directory: " + baseDir.getAbsolutePath());
		final String name = "shrunk_" + this.srcMetadata.getName();
		return new File(baseDir, name);
	}

	protected int scaleDimension (final int from) {
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

	private static class PreviewResizeTask extends AsyncTask<Void, Void, Bitmap> {

		private final TextView txtSummary;
		private final ImageView imgPreview;
		private final ProgressBar prgRedrawing;
		private final StringBuilder summary;
		private final ImageMetadata srcMetadata;
		private final PictureResizeDialog dlg;

		public PreviewResizeTask (final TextView txtSummary, final ImageView imgPreview, final ProgressBar prgRedrawing, final ImageMetadata srcMetadata, final PictureResizeDialog dlg) {
			this.txtSummary = txtSummary;
			this.imgPreview = imgPreview;
			this.prgRedrawing = prgRedrawing;
			this.srcMetadata = srcMetadata;
			this.dlg = dlg;
			this.summary = new StringBuilder();
		}

		@Override
		protected void onPreExecute () {
			this.prgRedrawing.setVisibility(View.VISIBLE);
		}

		@Override
		protected Bitmap doInBackground (final Void... params) {
			try {
				final Bitmap src = this.srcMetadata.readBitmap();
				final int w = this.dlg.scaleDimension(src.getWidth());
				final int h = this.dlg.scaleDimension(src.getHeight());
				final Bitmap scaled = Bitmap.createScaledBitmap(src, w, h, true);
				try {
					final ByteArrayOutputStream compOut = new ByteArrayOutputStream(512 * 1024);
					if (scaled.compress(Bitmap.CompressFormat.JPEG, this.dlg.getQuality().getPercentage(), compOut)) {
						this.summary
								.append(src.getWidth()).append(" x ").append(src.getHeight())
								.append(" (").append(IoHelper.readableFileSize(this.srcMetadata.getSize())).append(")")
								.append(" --> ").append(w).append(" x ").append(h)
								.append(" (").append(IoHelper.readableFileSize(compOut.size())).append(")");
						return BitmapFactory.decodeStream(compOut.toBufferedInputStream());
					}
					this.summary.append("Failed to compress image.");
					return null;
				}
				finally {
					if (scaled != src) scaled.recycle();
				}
			}
			catch (final IOException e) {
				this.summary.append(e.toString());
				return null;
			}
		}

		@Override
		protected void onPostExecute (final Bitmap result) {
			this.txtSummary.setText(this.summary.toString());
			if (result != null) {
				this.imgPreview.setImageBitmap(result);
			}
			else {
				this.imgPreview.setImageResource(R.drawable.exclamation_red);
			}
			this.prgRedrawing.setVisibility(View.INVISIBLE);
		}

	}

}
