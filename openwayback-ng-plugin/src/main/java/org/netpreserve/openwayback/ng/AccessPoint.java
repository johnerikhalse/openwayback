/*
 *  This file is part of the Wayback archival access software
 *   (http://archive-access.sourceforge.net/projects/wayback/).
 *
 *  Licensed to the Internet Archive (IA) by one or more individual
 *  contributors.
 *
 *  The IA licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.netpreserve.openwayback.ng;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.archive.wayback.ExceptionRenderer;
import org.archive.wayback.QueryRenderer;
import org.archive.wayback.ReplayDispatcher;
import org.archive.wayback.ReplayRenderer;
import org.archive.wayback.accesscontrol.CollectionContext;
import org.archive.wayback.accesscontrol.ExclusionFilterFactory;
import org.archive.wayback.core.CaptureSearchResult;
import org.archive.wayback.core.CaptureSearchResults;
import org.archive.wayback.core.Resource;
import org.archive.wayback.core.SearchResults;
import org.archive.wayback.core.UIResults;
import org.archive.wayback.core.UrlSearchResults;
import org.archive.wayback.core.WaybackRequest;
import org.archive.wayback.exception.AccessControlException;
import org.archive.wayback.exception.AuthenticationControlException;
import org.archive.wayback.exception.BadQueryException;
import org.archive.wayback.exception.BaseExceptionRenderer;
import org.archive.wayback.exception.BetterRequestException;
import org.archive.wayback.exception.ConfigurationException;
import org.archive.wayback.exception.ResourceIndexNotAvailableException;
import org.archive.wayback.exception.ResourceNotAvailableException;
import org.archive.wayback.exception.ResourceNotInArchiveException;
import org.archive.wayback.exception.SpecificCaptureReplayException;
import org.archive.wayback.exception.WaybackException;
import org.archive.wayback.memento.DefaultMementoHandler;
import org.archive.wayback.memento.MementoHandler;
import org.archive.wayback.memento.MementoUtils;
import org.archive.wayback.replay.DefaultReplayCaptureSelector;
import org.archive.wayback.replay.ReplayCaptureSelector;
import org.archive.wayback.replay.html.RewriteDirector;
import org.archive.wayback.util.webapp.AbstractRequestHandler;
import org.archive.wayback.util.webapp.RequestMapper;
import org.archive.wayback.util.webapp.ShutdownListener;
import org.archive.wayback.webapp.LiveWebRedirector.LiveWebState;

/**
 * Retains all information about a particular Wayback configuration
 * within a ServletContext, including holding references to the
 * implementation instances of the primary Wayback classes:
 *
 * 		RequestParser
 *		ResourceIndex(via WaybackCollection)
 *		ResourceStore(via WaybackCollection)
 *		QueryRenderer
 *		ReplayDispatcher
 *		ExceptionRenderer
 *		ResultURIConverter
 *
 *
 * @author brad
 */
public class AccessPoint extends AbstractRequestHandler implements
		ShutdownListener, CollectionContext {
	/** webapp relative location of Interstitial.jsp */
	public final static String INTERSTITIAL_JSP = "jsp/Interstitial.jsp";
	/** argument for Interstitial.jsp target URL */
	public final static String INTERSTITIAL_TARGET = "target";
	/** argument for Interstitial.jsp seconds to delay */
	public final static String INTERSTITIAL_SECONDS = "seconds";

	/** argument for Interstitial.jsp msse for replay date */
	public final static String INTERSTITIAL_DATE = "date";
	/** argument for Interstitial.jsp URL being loaded */
	public final static String INTERSTITIAL_URL = "url";

	public final static String REVISIT_STR = "warc/revisit";
	public final static String EMPTY_VALUE = "-";

	public final static String RUNTIME_ERROR_HEADER = "X-Archive-Wayback-Runtime-Error";
	private final static int MAX_ERR_HEADER_LEN = 300;

	//public final static String NOTFOUND_ERROR_HEADER = "X-Archive-Wayback-Not-Found";

	private static final Logger LOGGER = Logger.getLogger(AccessPoint.class.getName());

	private boolean exactHostMatch = false;
	private boolean exactSchemeMatch = false;
	private boolean useAnchorWindow = false;
	private boolean useServerName = false;
	private boolean serveStatic = true;
	private boolean bounceToReplayPrefix = false;
	private boolean bounceToQueryPrefix = false;
	private boolean forceCleanQueries = true;

	private boolean timestampSearch = false;

	private String errorMsgHeader = RUNTIME_ERROR_HEADER;
	private String perfStatsHeader = "X-Archive-Wayback-Perf";
	private String warcFileHeader = "x-archive-src";

	private boolean enableErrorMsgHeader = false;
	private boolean enablePerfStatsHeader = false;
	private boolean enableWarcFileHeader = false;
	private boolean enableMemento = true;

	private String staticPrefix = null;
	private String queryPrefix = null;
	private String replayPrefix = null;
	private QueryRenderer query = null;

	private String interstitialJsp = INTERSTITIAL_JSP;

	private String refererAuth = null;

	private Properties configs = null;

	private ExceptionRenderer exception = new BaseExceptionRenderer();

	private MementoHandler mementoHandler = new DefaultMementoHandler();

	private ExclusionFilterFactory exclusionFactory = null;
	private RewriteDirector rewriteDirector;

	public void init() {
        System.out.println("INIT");
	}

	protected boolean dispatchLocal(HttpServletRequest httpRequest,
			HttpServletResponse httpResponse) throws ServletException,
			IOException {
		if (LOGGER.isLoggable(Level.FINE)) {
			LOGGER.fine("Local dispatch /" + translateRequestPath(httpRequest));
		}
		if (!serveStatic) {
			return false;
		}
//		String contextRelativePath = httpRequest.getServletPath();
		String translatedNoQuery = "/" + translateRequestPath(httpRequest);
//		String absPath = getServletContext().getRealPath(contextRelativePath);
		String absPath = getServletContext().getRealPath(translatedNoQuery);

		if (this.isEnableMemento()) {
			MementoUtils.addDoNotNegotiateHeader(httpResponse);
		}

		//IK: added null check for absPath, it may be null (ex. on jetty)
		if (absPath != null) {
			File test = new File(absPath);
			if((test != null) && !test.exists()) {
				return false;
			}
		}

		String translatedQ = "/" + translateRequestPathQuery(httpRequest);

        System.out.println("=====> " + translatedQ);

		WaybackRequest wbRequest = new WaybackRequest();
//			wbRequest.setContextPrefix(getUrlRoot());
//		wbRequest.setAccessPoint(this);
		wbRequest.extractHttpRequestInfo(httpRequest);
		UIResults uiResults = new UIResults(wbRequest, uriConverter);
		try {
			uiResults.forward(httpRequest, httpResponse, translatedQ);
			return true;
		} catch(IOException e) {
			// TODO: figure out if we got IO because of a missing dispatcher
		}

		return false;
	}

	/**
	 * @param httpRequest HttpServletRequest which is being handled
	 * @param httpResponse HttpServletResponse which is being handled
	 * @return true if the request was actually handled
	 * @throws ServletException per usual
	 * @throws IOException per usual
	 */
    @Override
	public boolean handleRequest(HttpServletRequest httpRequest,
			HttpServletResponse httpResponse) throws ServletException,
			IOException {

		WaybackRequest wbRequest = null;
		boolean handled = false;

		try {
			String inputPath = RequestMapper.getRequestContextPathQuery(httpRequest);
			Thread.currentThread().setName("Thread " +
					Thread.currentThread().getId() + " " + getBeanName() +
					" handling: " + inputPath);
			LOGGER.fine("Handling translated: " + inputPath);

            System.out.println("INPUT PATH: " + inputPath);
            System.out.println("HTTP URI: " + httpRequest.getRequestURI());
            System.out.println("HTTP URL: " + httpRequest.getRequestURL());
//			wbRequest = getParser().parse(httpRequest, this);

            if (inputPath.isEmpty()) {
				handled = dispatchLocal(httpRequest, httpResponse);
			} else {
                return true;
            }
//			if (wbRequest != null) {
//				handled = true;
//
////				wbRequest.setContextPrefix(getAbsoluteLocalPrefix(httpRequest));
////				wbRequest.setContextPrefix(getUrlRoot());
//				wbRequest.extractHttpRequestInfo(httpRequest);
//				// end of refactor
//
//				if (wbRequest.isReplayRequest()) {
//					if (bounceToReplayPrefix) {
//						// we don't accept replay requests on this AccessPoint
//						// bounce the user to the right place:
//						String suffix = translateRequestPathQuery(httpRequest);
//						String replayUrl = replayPrefix + suffix;
//						httpResponse.sendRedirect(replayUrl);
//						return true;
//					}
//					handleReplay(wbRequest, httpRequest, httpResponse);
//				} else {
//					if (bounceToQueryPrefix) {
//						// we don't accept replay requests on this AccessPoint
//						// bounce the user to the right place:
//						String suffix = translateRequestPathQuery(httpRequest);
//						String replayUrl = queryPrefix + suffix;
//						httpResponse.sendRedirect(replayUrl);
//						return true;
//					}
//					handleQuery(wbRequest, httpRequest, httpResponse);
//				}
//			}
//		} catch (BetterRequestException e) {
//			e.generateResponse(httpResponse, wbRequest);
//			httpResponse.getWriter(); // cause perf headers to be committed
//			handled = true;
//		} catch (WaybackException e) {
//			if (httpResponse.isCommitted()) {
//				return true;
//			}
//
//			logError(httpResponse, errorMsgHeader, e, wbRequest);
//
//			LiveWebState liveWebState = LiveWebState.NOT_FOUND;
//
//			// If not liveweb redirected, then render current exception
//			if (liveWebState != LiveWebState.REDIRECTED) {
//				e.setLiveWebAvailable(liveWebState == LiveWebState.FOUND);
////				getException().renderException(httpRequest, httpResponse, wbRequest, e, getUriConverter());
//			}
//
//			handled = true;
//
		} catch (Exception other) {
			logError(httpResponse, errorMsgHeader, other, wbRequest);
		}

		return handled;
	}

	/**
	 * Default implementation returns {@code null}.
	 */
	@Override
	public String getCollectionContextName() {
		return null;
	}

	public void logError(HttpServletResponse httpResponse, String header,
			Exception e, WaybackRequest request) {
		if (e instanceof ResourceNotInArchiveException) {
			if (LOGGER.isLoggable(Level.INFO)) {
				this.logNotInArchive((ResourceNotInArchiveException)e, request);
			}
		} else if (e instanceof AccessControlException) {
			// While StaticMapExclusionFilter#isExcluded(String) reports
			// exclusion at INFO level, RobotExclusionFilter logs exclusion
			// at FINE level only. I believe here is the better place to log
			// exclusion. Unfortunately, AccessControlException has no
			// detailed info (TODO). we don't need a stack trace.
			if (LOGGER.isLoggable(Level.INFO)) {
				LOGGER.log(Level.INFO, "Access Blocked:" + request.getRequestUrl() + ": "+ e.getMessage());
			}
		} else {
			if (LOGGER.isLoggable(Level.WARNING)) {
				LOGGER.log(Level.WARNING, "Runtime Error", e);
			}
		}

		String message = (e != null ? e.toString() : "");

		if (message == null) {
			message = "";
		} else {
			// Get substring from exception name
			int index = message.indexOf(':');
			if (index > 0) {
				index = message.lastIndexOf('.', index);
				if (index > 0) {
					message = message.substring(index + 1);
				}
			}

			if (message.length() > MAX_ERR_HEADER_LEN) {
				message = message.substring(0, MAX_ERR_HEADER_LEN);
			}
			message = message.replace('\n', ' ');
		}

		httpResponse.setHeader(header, message);
	}

	private void logNotInArchive(ResourceNotInArchiveException e, WaybackRequest r) {
		// TODO: move this into ResourceNotInArchiveException constructor
		String url = r.getRequestUrl();
		StringBuilder sb = new StringBuilder(100);
		sb.append("NotInArchive\t");
		sb.append(getBeanName()).append("\t");
		sb.append(url);
		LOGGER.info(sb.toString());
	}

	protected void handleReplay(WaybackRequest wbRequest,
			HttpServletRequest httpRequest, HttpServletResponse httpResponse)
					throws IOException, ServletException, WaybackException {

		String requestURL = wbRequest.getRequestUrl();
        System.out.println("HANDLE REPLAY OF " + requestURL);
        try {

//		CaptureSearchResults captureResults;
//        captureResults = searchCaptures(wbRequest);
//
//		ReplayCaptureSelector captureSelector = new DefaultReplayCaptureSelector(getReplay());

//		CaptureSearchResult closest = captureSelector.next();
//		CaptureSearchResult closest = captureResults.getResults().getFirst();

		int counter = 0;

//		SingleLoadResourceStore resourceStore = new SingleLoadResourceStore(getCollection().getResourceStore());
//
//		while (true) {
//			// Support for redirect from the CDX redirectUrl field
//			// This was the intended use of the redirect field, but has not actually be tested
//			// To enable this functionality, uncomment the lines below
//			// This is an optimization that allows for redirects to be handled without loading the original content
//			//
//			//String redir = closest.getRedirectUrl();
//			//if ((redir != null) && !redir.equals("-")) {
//			//  String fullRedirect = getUriConverter().makeReplayURI(closest.getCaptureTimestamp(), redir);
//			//  throw new BetterRequestException(fullRedirect, Integer.valueOf(closest.getHttpCode()));
//			//}
//
//			Resource httpHeadersResource = null;
//			Resource payloadResource = null;
//			boolean isRevisit = false;
//
//			try {
//				counter++;
//                System.out.println("COUNTER " + counter);
//
//				if (closest == null) {
//					throw new ResourceNotAvailableException("Self-Redirect: No Closest Match Found", 404);
//				}
//
//				closest.setClosest(true);
//				checkAnchorWindow(wbRequest, closest);
//
//				// Redirect to url for the actual closest capture, if not a retry
//				if (counter == 1) {
////					handleReplayRedirect(wbRequest, httpResponse, captureResults, closest);
//				}
//
//				// If revisit, may load two resources separately
//				if (closest.isRevisitDigest()) {
//					isRevisit = true;
//
//					// If the payload record is known and it failed before with this payload, don't try
//					// loading the header resource even.. outcome will likely be same
//					if (resourceStore.isSkipped(closest.getDuplicatePayloadFile())) {
//						// (XXX cannot simply call SessionResourceStore#retrieveResource() because of this
//						// counter thing - is there a better way?)
//						counter--; //don't really count this as we're not even checking the file anymore
//						throw new ResourceNotAvailableException(
//							"Revisit: Skipping already failed " +
//									closest.getDuplicatePayloadFile());
//					}
//					if ((closest.getDuplicatePayloadFile() == null) && wbRequest.isTimestampSearchKey()) {
//						// If a missing revisit and loaded optimized, try loading the entire timeline again
//
//						wbRequest.setTimestampSearchKey(false);
//
//						captureResults = searchCaptures(wbRequest);
//
//						closest = captureSelector.next();
//						//originalClosest = closest;
//						//maxTimeouts *= 2;
//						//maxMissingRevisits *= 2;
//
//						continue;
//					}
//
//					// If old-style arc revisit (no mimetype, filename is '-'), then don't load
//					// headersResource = payloadResource
//					if (EMPTY_VALUE.equals(closest.getFile())) {
//						closest.setFile(closest.getDuplicatePayloadFile());
//						closest.setOffset(closest.getDuplicatePayloadOffset());
//
//						// See that this is successful
//						httpHeadersResource = resourceStore.retrieveResource(closest);
//
//						// Hmm, since this is a revisit it should not redirect -- was: if both headers and payload are from a different timestamp, redirect to that timestamp
////						if (!closest.getCaptureTimestamp().equals(closest.getDuplicateDigestStoredTimestamp())) {
////							throwRedirect(wbRequest, httpResponse, captureResults, closest.getDuplicateDigestStoredTimestamp(), closest.getOriginalUrl(), closest.getHttpCode());
////						}
//
//						payloadResource = httpHeadersResource;
//
//					} else {
//						httpHeadersResource = resourceStore.retrieveResource(closest);
//
//						CaptureSearchResult payloadLocation = retrievePayloadForIdenticalContentRevisit(wbRequest, httpHeadersResource, closest);
//
//						if (payloadLocation == null) {
//							throw new ResourceNotAvailableException("Revisit: Missing original for revisit record " + closest.toString(), 404);
//						}
//
//						payloadResource = resourceStore.retrieveResource(payloadLocation);
//
//						// If zero length old-style revisit with no headers, then must use payloadResource as headersResource
//						if (httpHeadersResource.getRecordLength() <= 0) {
//							httpHeadersResource.close();
//							httpHeadersResource = payloadResource;
//						}
//					}
//				} else {
//					httpHeadersResource = resourceStore.retrieveResource(closest);
//					payloadResource = httpHeadersResource;
//				}
//
//				// Ensure that we are not self-redirecting!
//				// If the status is a redirect, check that the location or url date's are different from the current request
//				// Otherwise, replay the previous matched capture.
//				// This chain is unlikely to go past one previous capture, but is possible
//				if (isSelfRedirect(httpHeadersResource, closest, wbRequest, requestURL)) {
//					LOGGER.info("Self-Redirect: Skipping " + closest.getCaptureTimestamp() + "/" + closest.getOriginalUrl());
//					//closest = findNextClosest(closest, captureResults, requestMS);
//					closest = captureSelector.next();
//					continue;
//				}
//
//				if (counter > 1) {
//					handleReplayRedirect(wbRequest, httpResponse, captureResults, closest);
//				}
//
//				p.retrieved();
//
//				ReplayRenderer renderer =
//						getReplay().getRenderer(wbRequest, closest, httpHeadersResource, payloadResource);
//
//				if (this.isEnableWarcFileHeader() && (warcFileHeader != null)) {
//					if (isRevisit && (closest.getDuplicatePayloadFile() != null)) {
//						httpResponse.addHeader(warcFileHeader, closest.getDuplicatePayloadFile());
//					} else {
//						httpResponse.addHeader(warcFileHeader, closest.getFile());
//					}
//				}
//
//				if (this.isEnableMemento()) {
//					MementoUtils.addMementoDatetimeHeader(httpResponse, closest);
//					if (wbRequest.isMementoTimegate()) {
//						// URL-G in non-redirect proxy mode (archival-url URL-G
//						// always redirects in handleReplayRedirect()).
//						if (getMementoHandler() != null) {
//							getMementoHandler().addTimegateHeaders(
//								httpResponse, captureResults, wbRequest, true);
//						} else {
//							// bare minimum required for URL-G response [sic]
//							// XXX this lacks Vary: accept-datetime header required for URL-G
//							MementoUtils.addOrigHeader(httpResponse, closest.getOriginalUrl());
//							// Probably this is better - same as DefaultMementoHandler
//							//MementoUtils.addTimegateHeaders(httpResponse, captureResults, wbRequest, true);
//						}
//					} else {
//						// Memento URL-M response (can't be an intermediate resource)
//						MementoUtils.addLinkHeader(httpResponse, captureResults, wbRequest, true, true);
//					}
//				}
//
//				renderer.renderResource(httpRequest, httpResponse, wbRequest,
//					closest, httpHeadersResource, payloadResource, getUriConverter(), captureResults);
//
//				p.rendered();
//				p.write(wbRequest.getReplayTimestamp() + " " +
//						wbRequest.getRequestUrl());
//
//				break;
//
//			} catch (SpecificCaptureReplayException scre) {
//				// Primarily ResourceNotAvailableException from ResourceStore,
//				// but renderer.renderResource(...) above can throw
//				// BadContentException (very rare).
//
//				//final String SOCKET_TIMEOUT_MSG = "java.net.SocketTimeoutException: Read timed out";
//
//				CaptureSearchResult nextClosest = null;
//
//				// if exceed maxRedirectAttempts, stop
//				if ((counter > maxRedirectAttempts) && ((this.getLiveWebPrefix() == null) || !isWaybackReferer(wbRequest, this.getLiveWebPrefix()))) {
//					LOGGER.info("LOADFAIL: Timeout: Too many retries, limited to " + maxRedirectAttempts);
//				} else if ((closest != null) && !wbRequest.isIdentityContext()) {
//					//nextClosest = findNextClosest(closest, captureResults, requestMS);
//					nextClosest = captureSelector.next();
//				}
//
//				// Skip any nextClosest that has the same exact filename?
//				// Removing in case skip something that works..
//				// while ((nextClosest != null) && closest.getFile().equals(nextClosest.getFile())) {
//				//	nextClosest = findNextClosest(nextClosest, captureResults, requestMS);
//				//}
//
//				String msg = null;
//
//				if (closest != null) {
//					msg = scre.getMessage() + " /" + closest.getCaptureTimestamp() + "/" + closest.getOriginalUrl();
//				} else {
//					msg = scre.getMessage() + " /" + wbRequest.getReplayTimestamp() + "/" + wbRequest.getRequestUrl();
//				}
//
//				if (nextClosest != null) {
//					if (msg.startsWith("Self-Redirect")) {
//						LOGGER.info("(" + counter + ")LOADFAIL-> " + msg + " -> " + nextClosest.getCaptureTimestamp());
//					} else {
//						LOGGER.warning("(" + counter + ")LOADFAIL-> " + msg + " -> " + nextClosest.getCaptureTimestamp());
//					}
//
//					closest = nextClosest;
//				} else if (wbRequest.isTimestampSearchKey()) {
//					wbRequest.setTimestampSearchKey(false);
//
//					captureResults = searchCaptures(wbRequest);
//
//					captureSelector.setCaptures(captureResults);
//					closest = captureSelector.next();
//
//					//originalClosest = closest;
//
//					//maxTimeouts *= 2;
//					//maxMissingRevisits *= 2;
//
//					continue;
//				} else {
//					LOGGER.warning("(" + counter + ")LOADFAIL: " + msg);
//					scre.setCaptureContext(captureResults, closest);
//					throw scre;
//				}
//			} finally {
//				closeResources(payloadResource, httpHeadersResource);
//			}
//		}
        } catch (Exception ex) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            pw.print("ERROR WORKING WITH: " + requestURL + "\n");
            ex.printStackTrace(pw);
            System.out.println(sw.toString());
        }
	}

//	protected CaptureSearchResults searchCaptures(WaybackRequest wbr)
//			throws ResourceIndexNotAvailableException,
//			ResourceNotInArchiveException, BadQueryException,
//			AccessControlException, ConfigurationException, ResourceNotAvailableException {
//		SearchResults results = queryIndex(wbr);
//		if (!(results instanceof CaptureSearchResults)) {
//			throw new ResourceNotAvailableException(
//				"Bad results looking up " + wbr.getReplayTimestamp() + " " +
//						wbr.getRequestUrl());
//		}
//		return (CaptureSearchResults)results;
//	}
//
//	protected void handleQuery(WaybackRequest wbRequest,
//			HttpServletRequest httpRequest, HttpServletResponse httpResponse)
//			throws ServletException, IOException, WaybackException {
//
//		PerformanceLogger p = new PerformanceLogger("query");
//
//		// TODO: move this Memento code out of this method.
//		// Memento: render timemap
//		if ((this.getMementoHandler() != null) &&
//				(wbRequest.isMementoTimemapRequest())) {
//			if (this.getMementoHandler().renderMementoTimemap(wbRequest,
//				httpRequest, httpResponse)) {
//				return;
//			}
//		}
//
//		// TODO: should this be applied to Memento Timemap as well?
//
//		// Must call getQueryCollapseTime() because AccessPointAdapter
//		// needs to read parent's value, not the unused field of its
//		// own.
//		wbRequest.setCollapseTime(getQueryCollapseTime());
//
//		SearchResults results = queryIndex(wbRequest);
//
//		p.queried();
//
//		if (results instanceof CaptureSearchResults) {
//			CaptureSearchResults cResults = (CaptureSearchResults)results;
//
//			// The Firefox proxy plugin maks an XML request to populate the
//			// list of available captures, and needs the closest result to
//			// the one being replayed to be flagged as such:
//			CaptureSearchResult closest = cResults.getClosest();
//			if (closest != null) {
//				closest.setClosest(true);
//			}
//
//			getQuery().renderCaptureResults(httpRequest, httpResponse,
//				wbRequest, cResults, getUriConverter());
//
//		} else if (results instanceof UrlSearchResults) {
//			UrlSearchResults uResults = (UrlSearchResults)results;
//			getQuery().renderUrlResults(httpRequest, httpResponse, wbRequest,
//				uResults, getUriConverter());
//		} else {
//			throw new WaybackException("Unknown index format");
//		}
//		p.rendered();
//		p.write(wbRequest.getRequestUrl());
//	}

	/**
	 * Release any resources associated with this AccessPoint, including
	 * stopping any background processing threads
	 */
    @Override
	public void shutdown() {
	}

	/**
	 * @return true if this AccessPoint serves static content
	 */
	public boolean isServeStatic() {
		return serveStatic;
	}

	/**
	 * @param serveStatic if set to true, this AccessPoint will serve static
	 * content, and .jsp files
	 */
	public void setServeStatic(boolean serveStatic) {
		this.serveStatic = serveStatic;
	}

	/**
	 * @param interstitialJsp the interstitialJsp to set
	 */
	public void setInterstitialJsp(String interstitialJsp) {
		this.interstitialJsp = interstitialJsp;
	}

	/**
	 * @return the interstitialJsp
	 */
	public String getInterstitialJsp() {
		return interstitialJsp;
	}

	/**
	 * @return the generic customization Properties used with this AccessPoint,
	 * generally to tune the UI
	 */
	public Properties getConfigs() {
		return configs;
	}

	/**
	 * @param configs the generic customization Properties to use with this
	 * AccessPoint, generally used to tune the UI
	 */

	public void setConfigs(Properties configs) {
		this.configs = configs;
	}

	/**
	 * @return the ExceptionRenderer in use with this AccessPoint
	 */
	public ExceptionRenderer getException() {
		return exception;
	}

	/**
	 * @param exception the ExceptionRender to use with this AccessPoint
	 */
	public void setException(ExceptionRenderer exception) {
		this.exception = exception;
	}

	/**
	 * @return the String url prefix to use when generating self referencing
	 * 			replay URLs
	 */
	public String getReplayPrefix() {
		return this.replayPrefix;
	}

	/**
	 * @param replayPrefix explicit URL prefix to use when creating self referencing
	 * 		replay URLs
	 */
	public void setReplayPrefix(String replayPrefix) {
		this.replayPrefix = replayPrefix;
	}

	/**
	 * @return the QueryRenderer to use with this AccessPoint
	 */
	public QueryRenderer getQuery() {
		return query;
	}

	/**
	 * @param query the QueryRenderer responsible for returning query data to
	 * clients.
	 */
	public void setQuery(QueryRenderer query) {
		this.query = query;
	}

	/**
	 * @return the ReplayDispatcher to use with this AccessPoint, responsible
	 * for returning an appropriate ReplayRenderer given the user request and
	 * the returned document type.
	 */
//	public ReplayDispatcher getReplay() {
//		return replay;
//	}

	/**
	 * @param replay the ReplayDispatcher to use with this AccessPoint.
	 */
//	public void setReplay(ReplayDispatcher replay) {
//		this.replay = replay;
//	}

	public boolean isEnableMemento() {
		return enableMemento;
	}

	public void setEnableMemento(boolean enableMemento) {
		this.enableMemento = enableMemento;
	}

	public MementoHandler getMementoHandler() {
		return mementoHandler;
	}

	public void setMementoHandler(MementoHandler mementoHandler) {
		this.mementoHandler = mementoHandler;
	}

}
