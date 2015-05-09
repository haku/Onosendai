package com.vaguehope.onosendai.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;

import org.apache.http.conn.scheme.LayeredSocketFactory;
import org.apache.http.conn.ssl.StrictHostnameVerifier;
import org.apache.http.params.HttpParams;

import android.annotation.TargetApi;
import android.net.SSLCertificateSocketFactory;
import android.os.Build;

/**
 * Based on http://blog.dev001.net/post/67082904181/android-using-sni-and-tlsv1-2-with-apache
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
public class TlsSniSocketFactory implements LayeredSocketFactory {

	private static final List<String> ALLOWED_CIPHERS = Arrays.asList(new String[] {
			// allowed secure ciphers according to NIST.SP.800-52r1.pdf Section 3.3.1 (see docs directory).
			// TLS 1.2:
			"TLS_RSA_WITH_AES_256_GCM_SHA384",
			"TLS_RSA_WITH_AES_128_GCM_SHA256",
			"TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
			"TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
			"TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
			"TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256",
			"TLS_ECHDE_RSA_WITH_AES_128_GCM_SHA256",
			// maximum interoperability:
			"TLS_RSA_WITH_3DES_EDE_CBC_SHA",
			"TLS_RSA_WITH_AES_128_CBC_SHA",
			// additionally:
			"TLS_RSA_WITH_AES_256_CBC_SHA",
			"TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA",
			"TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
			"TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA",
			"TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
	});
	private static final HostnameVerifier HOSTNAME_VERIFIER = new StrictHostnameVerifier();
	private static final LogWrapper LOG = new LogWrapper("TSF");

	private final TrustManager[] trustManager;

	public TlsSniSocketFactory (final TrustManager[] trustManager) {
		this.trustManager = trustManager;
	}

	// Plain TCP/IP (layer below TLS)

	@Override
	public Socket connectSocket (final Socket s, final String host, final int port, final InetAddress localAddress, final int localPort, final HttpParams params) throws IOException {
		return null;
	}

	@Override
	public Socket createSocket () throws IOException {
		return null;
	}

	@Override
	public boolean isSecure (final Socket s) throws IllegalArgumentException {
		if (s instanceof SSLSocket)
			return ((SSLSocket) s).isConnected();
		return false;
	}

	// TLS layer

	@Override
	public Socket createSocket (final Socket plainSocket, final String host, final int port, final boolean autoClose) throws IOException, UnknownHostException {
		// we don't need the plainSocket
		if (autoClose) plainSocket.close();

		// create and connect SSL socket, but don't do hostname/certificate verification yet.
		final SSLCertificateSocketFactory sslSocketFactory = (SSLCertificateSocketFactory) SSLCertificateSocketFactory.getDefault(0);
		sslSocketFactory.setTrustManagers(this.trustManager);
		final SSLSocket sock = (SSLSocket) sslSocketFactory.createSocket(InetAddress.getByName(host), port);

		// Protocols...
		final List<String> protocols = new ArrayList<String>();
		for (final String protocol : sock.getSupportedProtocols()) {
			if (!protocol.toUpperCase(Locale.ENGLISH).contains("SSL")) protocols.add(protocol);
		}
		sock.setEnabledProtocols(protocols.toArray(new String[0]));

		// Ciphers...
		final HashSet<String> ciphers = new HashSet<String>(ALLOWED_CIPHERS);
		ciphers.retainAll(Arrays.asList(sock.getSupportedCipherSuites()));
		ciphers.addAll(new HashSet<String>(Arrays.asList(sock.getEnabledCipherSuites()))); // All all already enabled ones for compatibility.
		sock.setEnabledCipherSuites(ciphers.toArray(new String[0]));

		// set up SNI before the handshake.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
			sslSocketFactory.setHostname(sock, host);
		}
		else { // This hack seems to work on my 4.0.4 tablet.
			try {
				final java.lang.reflect.Method setHostnameMethod = sock.getClass().getMethod("setHostname", String.class);
				setHostnameMethod.invoke(sock, host);
			}
			catch (final Exception e) {
				LOG.w("SNI not useable: %s", ExcpetionHelper.causeTrace(e));
			}
		}

		// verify hostname and certificate.
		final SSLSession session = sock.getSession();
		if (!HOSTNAME_VERIFIER.verify(host, session)) throw new SSLPeerUnverifiedException("Cannot verify hostname: " + host);

		LOG.i("Connected %s %s %s.", session.getPeerHost(), session.getProtocol(), session.getCipherSuite());
		return sock;
	}
}
