package local.apache;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.http.util.EncodingUtils;

import com.vaguehope.onosendai.util.IoHelper;

public class InputStreamPart extends PartBase {

	private final String filename;
	private final long length;
	private final InputStream is;

	public InputStreamPart (final String name, final String filename, final long length, final InputStream is) {
		super(name, "binary", null, null);
		this.filename = filename;
		this.length = length;
		this.is = is;
	}

	@Override
	protected void sendData (final OutputStream out) throws IOException {
		IoHelper.copy(this.is, out);
	}

	@Override
	protected long lengthOfData () throws IOException {
		return this.length;
	}

	@Override
	protected void sendDispositionHeader (final OutputStream out) throws IOException {
		super.sendDispositionHeader(out);
		out.write(EncodingUtils.getAsciiBytes("; filename="));
		out.write(QUOTE_BYTES);
		out.write(EncodingUtils.getAsciiBytes(this.filename));
		out.write(QUOTE_BYTES);
	}

}
