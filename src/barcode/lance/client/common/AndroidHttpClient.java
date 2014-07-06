package barcode.lance.client.common;

import java.io.IOException;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

public final class AndroidHttpClient implements HttpClient {

	/**
	 * Create a new HttpClient with reasonable defaults (which you can update).
	 * 
	 * @param userAgent
	 *            to report in your HTTP requests.
	 * @return AndroidHttpClient for you to use for all your requests.
	 */
	public static AndroidHttpClient newInstance(String userAgent) {
		HttpParams params = new BasicHttpParams();

		// Turn off stale checking. Our connections break all the time anyway,
		// and it's not worth it to pay the penalty of checking every time.
		HttpConnectionParams.setStaleCheckingEnabled(params, false);

		// Default connection and socket timeout of 20 seconds. Tweak to taste.
		HttpConnectionParams.setConnectionTimeout(params, 20 * 1000);
		HttpConnectionParams.setSoTimeout(params, 20 * 1000);
		HttpConnectionParams.setSocketBufferSize(params, 8192);

		// Don't handle redirects -- return them to the caller. Our code
		// often wants to re-POST after a redirect, which we must do ourselves.
		HttpClientParams.setRedirecting(params, false);

		// Set the specified user agent and register standard protocols.
		if (userAgent != null) {
			HttpProtocolParams.setUserAgent(params, userAgent);
		}
		SchemeRegistry schemeRegistry = new SchemeRegistry();
		schemeRegistry.register(new Scheme("http", PlainSocketFactory
				.getSocketFactory(), 80));
		schemeRegistry.register(new Scheme("https", SSLSocketFactory
				.getSocketFactory(), 443));
		ClientConnectionManager manager = new ThreadSafeClientConnManager(
				params, schemeRegistry);

		// We use a factory method to modify superclass initialization
		// parameters without the funny call-a-static-method dance.
		return new AndroidHttpClient(manager, params);
	}

	private final HttpClient delegate;

	private AndroidHttpClient(ClientConnectionManager ccm, HttpParams params) {
		this.delegate = new DelegateHttpClient(ccm, params);
	}

	/**
	 * Release resources associated with this client. You must call this, or
	 * significant resources (sockets and memory) may be leaked.
	 */
	public void close() {
		getConnectionManager().shutdown();
	}

	public HttpParams getParams() {
		return delegate.getParams();
	}

	public ClientConnectionManager getConnectionManager() {
		return delegate.getConnectionManager();
	}

	public HttpResponse execute(HttpUriRequest request) throws IOException {
		return delegate.execute(request);
	}

	public HttpResponse execute(HttpUriRequest request, HttpContext context)
			throws IOException {
		return delegate.execute(request, context);
	}

	public HttpResponse execute(HttpHost target, HttpRequest request)
			throws IOException {
		return delegate.execute(target, request);
	}

	public HttpResponse execute(HttpHost target, HttpRequest request,
			HttpContext context) throws IOException {
		return delegate.execute(target, request, context);
	}

	public <T> T execute(HttpUriRequest request,
			ResponseHandler<? extends T> responseHandler) throws IOException {
		return delegate.execute(request, responseHandler);
	}

	public <T> T execute(HttpUriRequest request,
			ResponseHandler<? extends T> responseHandler, HttpContext context)
			throws IOException {
		return delegate.execute(request, responseHandler, context);
	}

	public <T> T execute(HttpHost target, HttpRequest request,
			ResponseHandler<? extends T> responseHandler) throws IOException {
		return delegate.execute(target, request, responseHandler);
	}

	public <T> T execute(HttpHost target, HttpRequest request,
			ResponseHandler<? extends T> responseHandler, HttpContext context)
			throws IOException {
		return delegate.execute(target, request, responseHandler, context);
	}

	private static class DelegateHttpClient extends DefaultHttpClient {

		private DelegateHttpClient(ClientConnectionManager ccm,
				HttpParams params) {
			super(ccm, params);
		}

		@Override
		protected HttpContext createHttpContext() {
			// Same as DefaultHttpClient.createHttpContext() minus the
			// cookie store.
			HttpContext context = new BasicHttpContext();
			context.setAttribute(ClientContext.AUTHSCHEME_REGISTRY,
					getAuthSchemes());
			context.setAttribute(ClientContext.COOKIESPEC_REGISTRY,
					getCookieSpecs());
			context.setAttribute(ClientContext.CREDS_PROVIDER,
					getCredentialsProvider());
			return context;
		}
	}

}