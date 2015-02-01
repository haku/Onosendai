package com.vaguehope.onosendai.ui;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import local.apache.ByteArrayOutputStream;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Rect;
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
import com.vaguehope.onosendai.storage.AttachmentStorage;
import com.vaguehope.onosendai.util.DialogHelper.Listener;
import com.vaguehope.onosendai.util.ImageMetadata;
import com.vaguehope.onosendai.util.IoHelper;
import com.vaguehope.onosendai.util.LogWrapper;
import com.vaguehope.onosendai.util.Result;
import com.vaguehope.onosendai.util.Titleable;

public class PictureResizeDialog implements Titleable {

	private static final List<Scale> SCALES = Scale.setOf(100, 50, 25);
	private static final int DEFAULT_SCALES_POS = 1;

	private static final List<Scale> QUALITIES = Scale.setOf(100, 95, 90, 80, 70, 50, 30, 10);
	private static final int DEFAULT_QUALITY_POS = 2;

	private static final LogWrapper LOG = new LogWrapper("PRD");

	private final Context context;
	private final ImageMetadata srcMetadata;

	private final View llParent;
	private final ImageView imgPreview;
	private final Spinner spnScale;
	private final Spinner spnQuality;
	private final ProgressBar prgRedrawing;
	private final TextView txtSummary;

	public PictureResizeDialog (final Context context, final Uri pictureUri) {
		this.context = context;
		this.srcMetadata = new ImageMetadata(this.context, pictureUri);

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
		final Bitmap scaled = this.srcMetadata.getBitmap(getScale().getPercentage());
		final File tgt = AttachmentStorage.getTempFile(this.context, "shrunk_" + this.srcMetadata.getName());
		final OutputStream tgtOut = new FileOutputStream(tgt);
		try {
			scaled.compress(Bitmap.CompressFormat.JPEG, getQuality().getPercentage(), tgtOut);
			return Uri.fromFile(tgt);
		}
		finally {
			IoHelper.closeQuietly(tgtOut);
		}
	}

	public void recycle () {
		this.srcMetadata.recycle();
	}

	protected Context getContext () {
		return this.context;
	}

	private final OnItemSelectedListener spnChangeListener = new OnItemSelectedListener() {
		@Override
		public void onItemSelected (final AdapterView<?> parent, final View view, final int position, final long id) {
			updateSummary();
		}

		@Override
		public void onNothingSelected (final AdapterView<?> arg0) {/**/}
	};

	private Scale lastScale = null;
	private Scale lastQuality = null;

	protected void updateSummary () {
		if (this.lastScale == getScale() && this.lastQuality == getQuality()) return;
		this.lastScale = getScale();
		this.lastQuality = getQuality();
		new PreviewResizeTask(this.txtSummary, this.imgPreview, this.prgRedrawing, this.srcMetadata, this).execute();
	}

	protected int scaleDimension (final int from) {
		final Scale s = getScale();
		if (s.getPercentage() == 100) return from; // NOSONAR 100 not magic number, its how percentage works.
		return (int) (from * s.getPercentage() / 100d); // NOSONAR 100 not magic number, its how percentage works.
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
			LOG.i("Generating preview: s=%s q=%s.", this.dlg.getScale(), this.dlg.getQuality());
			try {
				final Bitmap scaled = this.srcMetadata.getBitmap(this.dlg.getScale().getPercentage());
				final ByteArrayOutputStream compOut = new ByteArrayOutputStream(512 * 1024);
				if (scaled.compress(Bitmap.CompressFormat.JPEG, this.dlg.getQuality().getPercentage(), compOut)) {
					this.summary
							.append(this.srcMetadata.getWidth()).append(" x ").append(this.srcMetadata.getHeight())
							.append(" (").append(IoHelper.readableFileSize(this.srcMetadata.getSize())).append(")")
							.append(" --> ").append(scaled.getWidth()).append(" x ").append(scaled.getHeight())
							.append(" (").append(IoHelper.readableFileSize(compOut.size())).append(")");
					final BitmapRegionDecoder dec = BitmapRegionDecoder.newInstance(compOut.toBufferedInputStream(), true);
					try {
						final int srcW = dec.getWidth();
						final int srcH = dec.getHeight();
						final int tgtW = this.dlg.getRootView().getWidth(); // FIXME Workaround for ImageView width issue.  Fix properly with something like FixedWidthImageView.
						final int tgtH = this.imgPreview.getHeight();
						final int left = srcW > tgtW ? (srcW - tgtW) / 2 : 0;
						final int top = srcH > tgtH ? (srcH - tgtH) / 2 : 0;
						final BitmapFactory.Options options = new BitmapFactory.Options();
						options.inPurgeable = true;
						options.inInputShareable = true;
						return dec.decodeRegion(new Rect(left, top, left + tgtW, top + tgtH), options);
					}
					finally {
						dec.recycle();
					}
				}
				this.summary.append("Failed to compress image.");
				return null;
			}
			// XXX Many cases of OutOfMemoryError have been reported, particularly on low end hardware.
			// Try not to upset the user too much by not dying completely if possible.
			catch (final Throwable e) {
				LOG.e("Failed to generate preview image.", e);
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

	public static class ResizeToTempFileTask extends AsyncTask<Void, Void, Result<Uri>> {

		private final PictureResizeDialog prd;
		private final Listener<Result<Uri>> resultListener;
		private ProgressDialog dialog;

		public ResizeToTempFileTask (final PictureResizeDialog prd, final Listener<Result<Uri>> resultListener) {
			this.prd = prd;
			this.resultListener = resultListener;
		}

		@Override
		protected void onPreExecute () {
			this.dialog = ProgressDialog.show(this.prd.getContext(), "Shrink image", "Resizing image...", true);
		}

		@Override
		protected Result<Uri> doInBackground (final Void... params) {
			try {
				return new Result<Uri>(this.prd.resizeToTempFile());
			}
			catch (final Exception e) { // NOSONAR show user all errors.
				return new Result<Uri>(e);
			}
			// XXX Cases of OutOfMemoryError have been reported, particularly on low end hardware.
			// Try not to upset the user too much by not dying completely if possible.
			catch (final Throwable e) {
				return new Result<Uri>(new ExecutionException("Failed to resize image.", e));
			}
		}

		@Override
		protected void onPostExecute (final Result<Uri> result) {
			this.dialog.dismiss();
			this.resultListener.onAnswer(result);
		}

	}

}
