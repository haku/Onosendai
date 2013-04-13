package com.vaguehope.onosendai.provider;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import org.apache.http.client.HttpClient;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import com.vaguehope.onosendai.provider.successwhale.SuccessWhaleException;

/**
 * https://hc.apache.org/httpcomponents-client-ga/tutorial/html/index.html
 * https:
 * //hc.apache.org/httpcomponents-client-ga/httpclient/apidocs/org/apache/http
 * /impl/conn/tsccm/ThreadSafeClientConnManager.html
 */
public class HttpClientFactory {

	private static final int CONNECTION_TIMEOUT = 20000;
	private static final int SO_TIMEOUT = 30000;
	private static final int SO_BUFFER_SIZE = 8192;

	private final String tsPath;
	private final char[] tsPassword;

	private volatile HttpClient httpClient;

	public HttpClientFactory () {
		this(null, null);
	}

	public HttpClientFactory (final String tsPath, final char[] tsPassword) {
		this.tsPath = tsPath;
		this.tsPassword = tsPassword;
	}

	public synchronized HttpClient getHttpClient () throws SuccessWhaleException {
		try {
			if (this.httpClient == null) this.httpClient = makeHttpClient();
			return this.httpClient;
		}
		catch (final IOException e) {
			throw new SuccessWhaleException("Failed to create HTTP client: " + e.toString(), e);
		}
		catch (final GeneralSecurityException e) {
			throw new SuccessWhaleException("Failed to create HTTP client. " + e.toString(), e);
		}
	}

	public synchronized void shutdown () {
		if (this.httpClient != null) this.httpClient.getConnectionManager().shutdown();
	}

	private HttpClient makeHttpClient () throws IOException, GeneralSecurityException {
		final HttpParams params = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(params, CONNECTION_TIMEOUT);
		HttpConnectionParams.setSoTimeout(params, SO_TIMEOUT);
		HttpConnectionParams.setSocketBufferSize(params, SO_BUFFER_SIZE);
		HttpClientParams.setRedirecting(params, false);

		final ClientConnectionManager conman = new ThreadSafeClientConnManager(params, new SchemeRegistry());

		if (this.tsPath != null) {
			addHttpsSchemaForTrustStore(conman, this.tsPath, this.tsPassword);
		}
		else {
			addHttpsSchema(conman);
		}

		return new DefaultHttpClient(conman, params);
	}

	private static void addHttpsSchemaForTrustStore (final ClientConnectionManager connMan, final String tsPath, final char[] password) throws IOException, GeneralSecurityException {
		final KeyStore truststore = loadKeyStore(tsPath, password);
		final SSLSocketFactory sf = new SSLSocketFactory(truststore);
		final Scheme scheme = new Scheme("https", sf, 443); // NOSONAR 443 is not a magic number.  Its HTTPS specification.
		connMan.getSchemeRegistry().register(scheme);
	}

	private static void addHttpsSchema (final ClientConnectionManager connMan) {
		final SocketFactory sf = SSLSocketFactory.getSocketFactory();
		final Scheme scheme = new Scheme("https", sf, 443); // NOSONAR 443 is not a magic number.  Its HTTPS specification.
		connMan.getSchemeRegistry().register(scheme);
	}

	private static KeyStore loadKeyStore (final String tsPath, final char[] password) throws IOException, GeneralSecurityException {
		final KeyStore ks = KeyStore.getInstance("BKS");
		final InputStream is = HttpClientFactory.class.getResourceAsStream(tsPath);
		try {
			ks.load(is, password);
			return ks;
		}
		finally {
			is.close();
		}
	}

}
