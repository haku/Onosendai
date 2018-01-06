package com.vaguehope.onosendai.images;

import java.io.File;
import java.util.Date;

import com.vaguehope.onosendai.util.DateHelper;
import com.vaguehope.onosendai.util.DialogHelper;
import com.vaguehope.onosendai.util.IoHelper;
import com.vaguehope.onosendai.util.LogWrapper;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.widget.Toast;

public class ImageExporter {

	private static final LogWrapper LOG = new LogWrapper("IE");

	/**
	 * Assumes image has already been downloaded and is in cache.
	 *
	 * @param imgUrl
	 *            URL of image on the internet.
	 * @param prefix e.g. poster user name.
	 * @return If image was actually exported.
	 */
	public static boolean exportToPictures (final Context context, final String imgUrl, final Date date, final String prefix) {
		final File photosDir = new File(Environment.getExternalStorageDirectory().getPath(), "Pictures/Onosendai");
		if (!photosDir.exists()) photosDir.mkdirs();
		return exportToDir(context, imgUrl, date, prefix, photosDir);
	}

	public static boolean exportToDir (final Context context, final String imgUrl, final Date date, final String prefix, final File outputDir) {
		final HybridBitmapCache hybridBitmapCache = new HybridBitmapCache(context, 0);
		final File cacheFile = hybridBitmapCache.getCachedFile(imgUrl);
		if (cacheFile == null) return false;

		final String datePrefix = DateHelper.standardDateTimeFormat().format(date);
		final String extension = CachedImageFileProvider.identifyFileExtension(cacheFile);
		final String displayName = CachedImageFileProvider.makeDisplayName(imgUrl, extension);
		final String outputName = String.format("%s.%s.%s", datePrefix, prefix, displayName);
		final File outputFile = new File(outputDir, outputName);

		new CopyFileToFile(context, cacheFile, outputFile).execute();
		return true;
	}

	private static class CopyFileToFile extends AsyncTask<Void, Void, Exception> {

		private final Context context;
		private final File inputFile;
		private final File outputFile;


		public CopyFileToFile (final Context context, final File inputFile, final File outputFile) {
			this.context = context;
			this.inputFile = inputFile;
			this.outputFile = outputFile;
		}

		@Override
		protected Exception doInBackground (final Void... params) {
			try {
				IoHelper.copy(this.inputFile, this.outputFile);
				return null;
			}
			catch (final Exception e) { // NOSONAR show user all errors.
				return e;
			}
		}

		@Override
		protected void onPostExecute (final Exception result) {
			if (result == null) {
				LOG.i("Exported: %s --> %s", this.inputFile.getAbsolutePath(), this.outputFile.getAbsolutePath());
				Toast.makeText(this.context,
						String.format("Saved Image\n%s", this.outputFile.getName()),
						Toast.LENGTH_SHORT).show(); //ES
			}
			else {
				LOG.e("Failed to export image.", result);
				DialogHelper.alert(this.context, result);
			}
		}

	}

}
