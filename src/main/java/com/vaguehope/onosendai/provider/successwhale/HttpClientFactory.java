package com.vaguehope.onosendai.provider.successwhale;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLException;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.AbstractVerifier;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParamBean;
import org.apache.http.params.HttpParams;

/**
 * https://hc.apache.org/httpcomponents-client-ga/tutorial/html/index.html
 * https:
 * //hc.apache.org/httpcomponents-client-ga/httpclient/apidocs/org/apache/http
 * /impl/conn/tsccm/ThreadSafeClientConnManager.html
 */
public class HttpClientFactory {

	private static final int CONNECTION_TIMEOUT = 20000;
	private static final int SO_TIMEOUT = 30000;

	private static final String TS_PATH = "/successwhale.bks";
	private static final char[] TS_PASSWORD = "123456".toCharArray();
	private static final Map<String, String> HOST_ALIASES;
	static {
		Map<String, String> m = new HashMap<String, String>();
		m.put("api.successwhale.com", "sparrowhawk.ianrenton.com");
		HOST_ALIASES = Collections.unmodifiableMap(m);
	}

	private volatile HttpClient httpClient;

	public synchronized HttpClient getHttpClient () throws SuccessWhaleException {
		try {
			if (this.httpClient == null) this.httpClient = makeHttpClient();
			return this.httpClient;
		}
		catch (IOException e) {
			throw new SuccessWhaleException("Failed to create HTTP client: " + e.toString(), e);
		}
		catch (GeneralSecurityException e) {
			throw new SuccessWhaleException("Failed to create HTTP client. " + e.toString(), e);
		}
	}

	public synchronized void shutdown () {
		if (this.httpClient != null) this.httpClient.getConnectionManager().shutdown();
	}

	private static HttpClient makeHttpClient () throws IOException, GeneralSecurityException {
		HttpParams params = new BasicHttpParams();
		configureTimeouts(params, CONNECTION_TIMEOUT, SO_TIMEOUT);

		ClientConnectionManager conman = new ThreadSafeClientConnManager(params, new SchemeRegistry());
		addHttpSchema(conman);
		addHttpsSchemaForTrustStore(conman, TS_PATH, TS_PASSWORD);

		return new DefaultHttpClient(conman, params);
	}

	private static void configureTimeouts (final HttpParams params, final int connectionTimeout, final int soTimeout) {
		HttpConnectionParamBean connParam = new HttpConnectionParamBean(params);
		connParam.setConnectionTimeout(connectionTimeout);
		connParam.setSoTimeout(soTimeout);
	}

	private static void addHttpSchema (final ClientConnectionManager conman) {
		Scheme scheme = new Scheme("http", PlainSocketFactory.getSocketFactory(), 80); // NOSONAR 80 is not a magic number.  Its HTTP specification.
		conman.getSchemeRegistry().register(scheme);
	}

	private static void addHttpsSchemaForTrustStore (final ClientConnectionManager connMan, final String tsPath, final char[] password) throws IOException, GeneralSecurityException {
		KeyStore truststore = loadKeyStore(tsPath, password);
		SSLSocketFactory sf = new SSLSocketFactory(truststore);
		sf.setHostnameVerifier(new AliasingVerifier(HOST_ALIASES));
		Scheme scheme = new Scheme("https", sf, 443); // NOSONAR 443 is not a magic number.  Its HTTPS specification.
		connMan.getSchemeRegistry().register(scheme);
	}

	private static KeyStore loadKeyStore (final String tsPath, final char[] password) throws IOException, GeneralSecurityException {
		KeyStore ks = KeyStore.getInstance("BKS");
		InputStream is = HttpClientFactory.class.getResourceAsStream(tsPath);
		try {
			ks.load(is, password);
		}
		finally {
			is.close();
		}
		return ks;
	}

	private static class AliasingVerifier extends AbstractVerifier {

		private final Map<String, String> realHostNameToNameOnCert;

		public AliasingVerifier (final Map<String, String> realHostNameToNameOnCert) {
			this.realHostNameToNameOnCert = realHostNameToNameOnCert;
		}

		@Override
		public void verify (final String host, final String[] cns, final String[] subjectAlts) throws SSLException {
			String nameOnCert = this.realHostNameToNameOnCert.get(host);
			if (nameOnCert != null) {
				for (String c : cns) {
					if (nameOnCert.equals(c)) return;
				}
			}
			verify(host, cns, subjectAlts, false);
		}

	}

}
