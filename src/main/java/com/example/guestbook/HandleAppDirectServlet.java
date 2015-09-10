package com.example.guestbook;

import java.io.IOException;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.example.guestbook.model.AppActivity;
import com.example.guestbook.model.Company;
import com.example.guestbook.model.MarketPlace;
import com.example.guestbook.model.Order;
import com.example.guestbook.model.Person;
import com.example.guestbook.model.Subscription;
import com.example.guestbook.model.User;
import com.googlecode.objectify.ObjectifyService;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.basic.DefaultOAuthConsumer;




/**
 * This class provides endpoints for AppDirect Subscription and Access management events
 * ands handles them according to Appdirect documentation at:
 * http://info.appdirect.com/developers/docs/api_integration/api_overview
 * 
 * TODO: - add logging 
 * 		 - make HTTPS
 *       - Handle IOException gracefully
 * 		 - see many other TODOs through-out
 * 
 * @author Joey Fournier
 */
public class HandleAppDirectServlet extends HttpServlet {
	
	public static final String FLAG_STATELESS = "STATELESS";

	// logger
	final Logger logger = LoggerFactory.getLogger(HandleAppDirectServlet.class);
			
	// constants
	
	// parameter names
	public static final String PARAM_ACTION = "action";
	public static final String PARAM_EVENT_URL = "eUrl";
	// event types
	public static final String EVENT_TYPE_SUB_ORDER = "SUBSCRIPTION_ORDER";
	public static final String EVENT_TYPE_SUB_CHANGE = "SUBSCRIPTION_CHANGE";
	public static final String EVENT_TYPE_SUB_CANCEL = "SUBSCRIPTION_CANCEL";
	public static final String EVENT_TYPE_SUB_NOTICE = "SUBSCRIPTION_NOTICE";
	public static final String EVENT_TYPE_USER_ASSIGN = "USER_ASSIGNMENT";
	public static final String EVENT_TYPE_USER_UNASSIGN = "USER_UNASSIGNMENT";
	// Notices
	public static final String NOTICE_DEACTIVATED = "DEACTIVATED";
	public static final String NOTICE_REACTIVATED ="REACTIVATED";
	public static final String NOTICE_CLOSED = "CLOSED";
	public static final String NOTICE_UPCOMING_INVOICE = "UPCOMING_INVOICE";
	// subscription statuses
	public static final String STATUS_ACTIVE = "active";
	public static final String STATUS_DEACTIVATED = "deactivated";
	public static final String STATUS_CANCELED = "canceled";
	// error codes
	private static final String ERROR_CODE_USER_ALREADY_EXISTS = "USER_ALREADY_EXISTS";
	private static final String ERROR_CODE_INVALID_RESPONSE = "INVALID_RESPONSE";
	private static final String ERROR_CODE_USER_NOT_FOUND = "USER_NOT_FOUND";
	private static final String ERROR_CODE_ACCOUNT_NOT_FOUND = "ACCOUNT_NOT_FOUND"; 
	private static final String ERROR_CODE_UNKNOWN_ERROR = "UNKNOWN_ERROR";
	
	// config file path
	private static final String CONFIG_FILE_PATH = "WEB-INF/HandleAppDirectServlet.properties";
	// These values can/should be set by properties in CONFIG_FILE_PATH
	public static final  String OAUTH_CONSUMER_KEY_DEFAULT = "dummy";
	public static final  String OAUTH_CONSUMER_SECRET_DEFAULT = "dummy";
	public static  String oauthConsumerKey = OAUTH_CONSUMER_KEY_DEFAULT;
	public static  String oauthConsumerSecret = OAUTH_CONSUMER_SECRET_DEFAULT;
	// can be set in properties to process Stateless events (for testing)
	public static boolean processStatelessFlag = false;
	
	
	/**
	 * Initializes properties {@inheritDoc}
	 */
	public void init(ServletConfig config) throws ServletException {
		super.init(config);

		Configuration propertiesConfig;
		try {
			/* read properties file */
			propertiesConfig = new PropertiesConfiguration(CONFIG_FILE_PATH);
			oauthConsumerKey = propertiesConfig.getString("oauth.consumer.key", OAUTH_CONSUMER_KEY_DEFAULT);
			oauthConsumerSecret = propertiesConfig.getString("oauth.consumer.secret", OAUTH_CONSUMER_SECRET_DEFAULT);
			processStatelessFlag = propertiesConfig.getBoolean("process.stateless.flag", false);
		} catch (ConfigurationException e1) {
			logger.warn("Unable to readconfiguation file {}, using defaults.");
			e1.printStackTrace();
		}
	}

	/* (non-Javadoc) Process incoming AppDirect events. 
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

		// don't process anything that is not Oauth signed by AD
		if (!validateOAuthSignature(req)) {
			// short circuit TODO: make this 401
			resp.setContentType("text/plain");
			resp.getWriter().println("Sorry, you are not Authorized.\n\n");
		} else {
			// we have a legitimate AD request, lets see if its valid and
			// attempt to process it
			if (req.getParameter(PARAM_ACTION) != null) {
				String action = req.getParameter(PARAM_ACTION);
				String eventUrl = req.getParameter(PARAM_EVENT_URL);

				// obtain xml from eventUrl (signed fetch)
				Document dDoc = obtainXmlDoc(eventUrl);

				if (dDoc == null) {
					logger.info("Unable to obtain valid xml from attempted signed fetch on eventUrl: {}", eventUrl);
					sendEventProcessErrorResponse(resp, ERROR_CODE_INVALID_RESPONSE,
							"Unable to obtain valid xml from eventUrl: " + eventUrl);
					return;
				}

				// log xml that comes in, this is just for
				// informational/debugging purposes
				createActivityFromEventXml(dDoc);

				/*
				 * handle these: SUBSCRIPTION_ORDER: fired by AppDirect when a
				 * user buys an app from AppDirect.
				 * 
				 * SUBSCRIPTION_CHANGE: fired by AppDirect when a user
				 * upgrades/downgrades/modifies an existing subscription.
				 * 
				 * SUBSCRIPTION_CANCEL: fired by AppDirect when a user cancels a
				 * subscription.
				 * 
				 * SUBSCRIPTION_NOTICE: fired by AppDirect when a subscription
				 * goes overdue or delinquent.
				 * 
				 * USER_ASSIGNMENT: fired by AppDirect when a user assigns a
				 * user to an app.
				 * 
				 * USER_UNASSIGNMENT: fired by AppDirect when a user unassigns a
				 * user from an app.
				 */
				switch (action) {
				case EVENT_TYPE_SUB_ORDER:
					handleSubOrder(resp, dDoc);
					break;
				case EVENT_TYPE_SUB_CHANGE:
					handleSubChange(resp, dDoc);
					break;
				case EVENT_TYPE_SUB_CANCEL:
					handleSubCancel(resp, dDoc);
					break;
				case EVENT_TYPE_SUB_NOTICE:
					handleSubNotice(resp, dDoc);
					break;
				case EVENT_TYPE_USER_ASSIGN:
					handleUserAssign(resp, dDoc);
					break;
				case EVENT_TYPE_USER_UNASSIGN:
					handleUserUnassign(resp, dDoc);
					break;

				default:
					// unexpected action; TODO: change to 401
					resp.setContentType("text/plain");
					resp.getWriter().println("Unexpected action parameter: " + action + " \n\n");
					break;
				}

			} else {
				// TODO: change to 401?
				resp.setContentType("text/plain");
				resp.getWriter().println("No action parameter provided.\n\n");
			}
		}
	}

	
	/**
	 * Handles User Unassignment xml event, by removing the given user if they exist.
	 * Will either send success or error xml via the passed in HttpServletResponse object.
	 * NOTE: users are solely identified by email in this simple implementation
	 * 
	 * @param resp
	 * @param dDoc xml for User Unassignment xml
	 * @throws IOException
	 */
	private void handleUserUnassign(HttpServletResponse resp, Document dDoc) throws IOException {

		Subscription sub = null;
		String accountId = null;
		String userId = null;	
		logger.info("handleUserUnassign...");

		com.example.guestbook.model.userAssign.EventType event = validateAccessEventXml(resp,dDoc);
		
		if (event==null) return;

		// event type validity check
		if (checkEventTypeMatch(resp,event.getType(),EVENT_TYPE_USER_UNASSIGN)) return;
		
		// deal with flag, true response means a valid error is returned and we can stop processing
		if (handleOrderFlag(resp, event.getFlag())) return;
		
		// user's openid is used for id
		userId = event.getPayload().getUser().getOpenId();
		accountId = event.getPayload().getAccount().getAccountIdentifier();
		
		//TODO: wrap in transaction
		
		// obtains the subscription for accessMangement from event XML, check for missing attributes
		sub = accessManagementValidate(resp, accountId, userId, "User Unassignment");
		
		if (sub!=null)
		{	
	        // don't care about status of subscription for unassignment, just check if user exists
			Map<String,User> users = (HashMap<String,User>) sub.theUsers;
			if ((users == null) || (!users.containsKey(userId))){
	
				logger.info("handleUserUnssign: user does not exist for accountId {}, user id: {}", accountId,userId);
				sendEventProcessErrorResponse(resp, ERROR_CODE_USER_NOT_FOUND,
						"User Unassignment failed. User not found with user id: " + userId);
			} else {
				// we have the user to unassign, go ahead and remove them, and update the sub.
				users.remove(userId);
				sub.theUsers = users;
				ObjectifyService.ofy().save().entity(sub).now();
				logger.info("handleUserUnassign: user removed: {}. Saved subscription: {}", userId, sub);
				// WE BE DONE, send simple success
				sendEventProcessSuccessResponse(resp,"User unassigned successfully.");
			}	
		}
	}

	/**
	 * Handle User Assignment based on passed in xml document. Will return appropriate xml via passed in HttpServletResponse.
	 * NOTE: For this simple implementation Users are simply an email; TODO: save a map of user attributes as needed
	 * @param resp
	 * @param dDoc
	 * @throws IOException
	 */
	private void handleUserAssign(HttpServletResponse resp, Document dDoc) throws IOException {

		Subscription sub = null;
		String accountId = null;
		String userId = null;
		
		logger.info("handleUserAssign...");
		
		com.example.guestbook.model.userAssign.EventType event = validateAccessEventXml(resp,dDoc);
		
		if (event==null) return;
		
		// event type validity check
		if (checkEventTypeMatch(resp,event.getType(),EVENT_TYPE_USER_ASSIGN)) return;
		
		// deal with flag
		if (handleOrderFlag(resp, event.getFlag())) return;
		
		accountId = event.getPayload().getAccount().getAccountIdentifier();
		
		// user's openid is used for id
		userId = event.getPayload().getUser().getOpenId();

		// TODO: wrap in transaction
		
		// obtains the sub for accessMangement from event XML, check for missing attributes
		sub = accessManagementValidate(resp, accountId, userId, "User Assignment");

		if (sub != null) {
			// active sub?
			if (!sub.status.equalsIgnoreCase(STATUS_ACTIVE)) {

				logger.info("handleUserAssign: subscription not active (status={}) for account identifier: {}", sub.getStatus(),accountId);
				sendEventProcessErrorResponse(resp, ERROR_CODE_INVALID_RESPONSE,
						"User assign failed. Suscription not active for account identifier: " + accountId);
			} else {
				Map<String,User> userList = (HashMap<String,User>) sub.theUsers;
				if (userList == null) {
					userList = (Map<String,User>) new HashMap<String,User>();
				}
				// does user already exist
				if (userList.containsKey(userId)) {

					logger.info("handleUserAssign: for accountId {} user already exists for key: {}",accountId, userId);
					sendEventProcessErrorResponse(resp, ERROR_CODE_USER_ALREADY_EXISTS,
							"User assign failed. User already exists with user key: " + userId);
				} else {
					// add user
					User user = new User(event.getPayload().getUser());
					userList.put(userId, user);
					sub.theUsers = userList;
					ObjectifyService.ofy().save().entity(sub).now();
					logger.info("handleUserAssign: user added to accountId ({}) with key: {}. Subscription saved: {}",accountId, userId, sub);
					// WE BE DONE, send simple success
					sendEventProcessSuccessResponse(resp);
				}
			}
		} 
	}
	
	/**
	 * Accepts an xml document and attempt to validate it has User Assign object. The xml
	 * should be a valid AppDirect user assign or unassign events.
	 * @param resp used to return error response (if needed)
	 * @param dDoc the xml document
	 * @param action calling action (i.e. User Assign) to use in response and logs (if needed)
	 * @return a user assign object
	 * @throws IOException
	 */
	private com.example.guestbook.model.userAssign.EventType validateAccessEventXml(HttpServletResponse resp, Document dDoc) throws IOException{
		
		
		com.example.guestbook.model.userAssign.EventType event = null;	
		// extract xml into sub order event objects.
		logger.info("Attempting JAXB unmarshall of dDoc = {}",dDoc);
		// Unmarshall the xml into objects
		JAXBContext jc;
		try {
			jc = JAXBContext.newInstance(com.example.guestbook.model.userAssign.EventType.class);
			//Create unmarshaller
			Unmarshaller um = jc.createUnmarshaller();
			//EventType event = (EventType) um.unmarshal(signedFetch.getInputStream() );
			event = (com.example.guestbook.model.userAssign.EventType) um.unmarshal(dDoc ); 
			logger.info("Created from xml, the following sub order Event={}",event);
			
		} catch (JAXBException e) {
			e.printStackTrace();
			logger.info("XML not formatted correctly for User Assign. Exception message: {}",StringEscapeUtils.escapeXml(e.getMessage()));
			sendEventProcessErrorResponse(resp, ERROR_CODE_INVALID_RESPONSE, "XML not formatted correctly for User Assign. Exception message: "+StringEscapeUtils.escapeXml(e.getMessage()));
		}
		return event;
	}
	
	/**
	 * Validates for user assign/unassign passed in required fields (from xml) and makes sure the subscription
	 * exists on which the assign/unassign is operating on. This routine sends error xml via the passed in
	 * HttpServletResponse if validation errors occur.
	 * @param resp HttpServletResponse used to return error xml if needed
	 * @param accountId the accountId from xml
	 * @param openId the user id from xml
	 * @param action name of the action (i.e. Assignment or Unassignment) taking place to use in error messages
	 * @return returns subscription on which access management is being done if it exists, null otherwise
	 * @throws IOException
	 */
	private Subscription accessManagementValidate(HttpServletResponse resp, String accountId, String userId, String action) throws IOException
	{
		Subscription sub = null;
		logger.info("accessManagementValidate... for accountId {}",accountId);

		// make sure accountId is not null
		if (isNullError (accountId, "account identifier",action,resp)) return null;
		// make we have userId at the very least
		if (isNullError (userId, "user id",action,resp)) return null;

		// we have openid and account id, get the subscription for the account
		// obtain subscription
		sub = getSubScriptionForAccountId(resp, accountId, action);
		
		return sub;
	}
	
	
	/**
	 * Handle subscription notice based on passed in xml document. Will return appropriate xml via passed in HttpServletResponse.
	 * 
	 * SUBSCRIPTION_NOTICE: fired by AppDirect
	 * when a subscription goes overdue or delinquent
	 * @param resp
	 * @param dDoc
	 * @throws IOException
	 */
	private void handleSubNotice(HttpServletResponse resp, Document dDoc) throws IOException {

		Subscription sub = null;
		
		// validate Notice xml
		com.example.guestbook.model.subNotice.EventType event = validateNoticeXml( resp,  dDoc, "Notice");
		
		if (event==null) return;
		
		// event type validity check
		if (checkEventTypeMatch(resp,event.getType(),EVENT_TYPE_SUB_NOTICE)) return;
		
		// deal with flag
		if (handleOrderFlag(resp, event.getFlag())) return;
		
		String accountId = event.getPayload().getAccount().getAccountIdentifier();
		
		//TODO: wrap in transaction
		
		// obtain subscription
		sub = getSubScriptionForAccountId(resp, accountId, "Subscription Notice");
		
		if (sub!=null)
		{
			/*
			 * regardless of the current status, we will act upon the Subscription Notice;
			 * we will only leave status the same if it is already in the correct state
			 * for the notice.
			 */
			
			/*
			 * A DEACTIVATED notice means the account is deactivated. 
			 *   It is recommended that all access to the ISV's application be suspended 
			 *   (but not deleted). Account deactivation may occur if, for example, the account
			 *   holder is overdue in making a payment or if abuse is detected.
			 * A REACTIVATED notice means an account should be considered active and receive
			 *   its typical access. This status will usually indicate that the account holder
			 *   has paid an overdue invoice.
			 * A CLOSED notice means that the account has been in a SUSPENDED or FREE_TRIAL_EXPIRED
			 *   state for a period of time exceeding the grace period (typically 1 or 2 months but
			 *   it may vary by marketplace), and that it should be deleted by the ISV. In most cases,
			 *   this event should trigger the same code on the ISV as the SUBSCRIPTION_CANCELLED event.
			 * An UPCOMING_INVOICE  notice informs a vendor that there is an upcoming invoice 
			 *   that will be computed for this account. This will be issued 24 hours prior
			 *   to the calculation of the account's outstanding bill. The intention of this 
			 *   notice is to give a vendor the opportunity to update the AppDirect-powered 
			 *   marketplace with any usage information via the Billing Usage API, which will 
			 *   be included on the upcoming invoice.
			 */
	
			String notice = event.getPayload().getNotice().getType();
			
			switch (notice.toUpperCase()) {
				case NOTICE_DEACTIVATED:
					if (!sub.getStatus().equalsIgnoreCase(STATUS_DEACTIVATED))
					{
						sub.setStatus(STATUS_DEACTIVATED);
						ObjectifyService.ofy().save().entity(sub).now();
						logger.info("{} Notice applied. Subscription saved: {}",NOTICE_DEACTIVATED, sub);
					}
					sendEventProcessSuccessResponse(resp);					
					break;
				case NOTICE_REACTIVATED:
					if (!sub.getStatus().equalsIgnoreCase(STATUS_ACTIVE))
					{
						sub.setStatus(STATUS_ACTIVE);
						ObjectifyService.ofy().save().entity(sub).now();
						logger.info("{} Notice applied. Subscription saved: {}",NOTICE_REACTIVATED, sub);
					}
					sendEventProcessSuccessResponse(resp);
					break;
				case NOTICE_CLOSED:
					if (!sub.getStatus().equalsIgnoreCase(STATUS_CANCELED))
					{
						sub.setStatus(STATUS_CANCELED);
						ObjectifyService.ofy().save().entity(sub).now();
						logger.info("{} Notice applied. Subscription saved: {}",NOTICE_CLOSED, sub);
					}
					sendEventProcessSuccessResponse(resp);	
					break;
				case NOTICE_UPCOMING_INVOICE:
					handleUpcomingInvoice(resp, sub);
					break;
				default:
					sendEventProcessErrorResponse(resp, ERROR_CODE_INVALID_RESPONSE,
							"Subscription Notice failed. Unrecognized notice: "+notice);
			}
		}
					
	}
	
	/**
	 * Accepts an xml document and attempt to validate it has a Subscription notice object. The xml
	 * should be a valid AppDirect subscription notice event.
	 * @param resp used to return error response (if needed)
	 * @param dDoc the xml document
	 * @param action calling action (i.e. Subscription Notice) to use in response and logs (if needed)
	 * @return a subscription change event object
	 * @throws IOException
	 */
	private com.example.guestbook.model.subNotice.EventType validateNoticeXml(HttpServletResponse resp, Document dDoc, String action) throws IOException {
		
		
		com.example.guestbook.model.subNotice.EventType event = null;
		
		// extract xml into sub order event objects.
		logger.info("Attempting JAXB unmarshall of dDoc = {}", dDoc);
		// Unmarshall the xml into objects
		JAXBContext jc;
		try {
			jc = JAXBContext.newInstance(com.example.guestbook.model.subNotice.EventType.class);
			//Create unmarshaller
			Unmarshaller um = jc.createUnmarshaller();
			//EventType event = (EventType) um.unmarshal(signedFetch.getInputStream() );
			event = (com.example.guestbook.model.subNotice.EventType) um.unmarshal(dDoc ); 
			logger.info("Created from xml, the following Event={}",event);
			
		} catch (JAXBException e) {
			e.printStackTrace();
			logger.info("XML not formatted correctly for Subscription  {}. Exception message: {}",action,StringEscapeUtils.escapeXml(e.getMessage()));
			sendEventProcessErrorResponse(resp, ERROR_CODE_INVALID_RESPONSE, "XML not formatted correctly for Subscription  "+action+". Exception message: "+StringEscapeUtils.escapeXml(e.getMessage()));
			return null;
		}
		
		// verify accountId exists in  xml
		String accountId = event.getPayload().getAccount().getAccountIdentifier();
		if (isNullError (accountId, "account identifier",action,(Object)event,resp)) return null;
		if (isNullError (event.getPayload().getNotice(), "Notice",action,(Object)event, resp)) return null;
		if (isNullError (event.getPayload().getNotice().getType(), "Notice Type",action,(Object)event,resp)) return null;
		
		return event;
	}

	/**
	 * Checks if a passed in object is null and uses the passed HttpServletResponse object to 
	 * generate an error response with passed in descriptors. Returns true the value is null
	 * and an error is generated/logged.
	 * @param checkObject object to check if null
	 * @param description a descripton of the object to check
	 * @param action the current action that this check is being done for
	 * @param event for logging purposes, the event this object is being checked from
	 * @param resp HttpServletResponse to send an error if needed
	 * @return true if passed in checkObject is null and an error is generated
	 * @throws IOException
	 */
	private boolean isNullError(Object checkObject, String description, String action, Object event, HttpServletResponse resp) throws IOException
	{
		boolean returnValue = false;
		if (checkObject == null) {
			logger.info("Subscription {}: null {} in event xml: {}",action,description,event);
			sendEventProcessErrorResponse(resp, ERROR_CODE_INVALID_RESPONSE,
					"Subscription "+action+" failed. Unable to obtain "+description+" from event.");
			returnValue = true;
		}
		return returnValue;
	}
	
	/**
	 * Checks if a passed in object is null and uses the passed HttpServletResponse object to 
	 * generate an error response with passed in descriptors. Returns if true the value is null
	 * and an error is generated/logged.
	 * @param checkObject object to check if null
	 * @param description a descripton of the object to check
	 * @param action the current action that this check is being done for
	 * @param resp HttpServletResponse to send an error if needed
	 * @return true if passed in checkObject is null and an error is generated
	 * @throws IOException
	 */
	private boolean isNullError(Object checkObject, String description, String action, HttpServletResponse resp) throws IOException
	{
		boolean returnValue = false;
		if (checkObject == null) {
			logger.info("{}: null {}.",action,description);
			sendEventProcessErrorResponse(resp, ERROR_CODE_INVALID_RESPONSE,
					action+" failed. Unable to obtain "+description+" from event.");
			returnValue = true;
		}
		return returnValue;
		
	}


	/**
	 * Handle an upcoming invoice notice. For this implementation we won't do anything.
	 * TODO: figure out what we want to do for an upcoming invoice notice and make it so.
	 * @param resp HttpServletResponse to send response
	 * @param sub subscription on which the notice has been sent
	 * @throws IOException
	 */
	private void handleUpcomingInvoice(HttpServletResponse resp, Subscription sub) throws IOException {
		logger.info("{} Notice applied. Subscription left the same: {}",NOTICE_UPCOMING_INVOICE, sub);
		sendEventProcessSuccessResponse(resp);		
	}

	/**
	 * Handle subscription cancel based on passed in xml document. Will return appropriate xml via passed in HttpServletResponse.
	 * @param resp
	 * @param dDoc
	 * @throws IOException
	 */
	private void handleSubCancel(HttpServletResponse resp, Document dDoc) throws IOException {

		Subscription sub = null;

		com.example.guestbook.model.subChange.EventType event  = validateOrderUpdateXml(resp, dDoc,"Cancel");
		
		if (event==null) return;

		// event type validity check
		if (checkEventTypeMatch(resp,event.getType(),EVENT_TYPE_SUB_CANCEL)) return;
		
		// handle flag
		if (handleOrderFlag(resp, event.getFlag())) return;
		
		String accountId = event.getPayload().getAccount().getAccountIdentifier();

		// obtain subscription, TODO: wrap in transaction
		sub = getSubScriptionForAccountId(resp, accountId, "Subscription Cancel");
		if (sub!=null)	
		{
			if (sub.status.equalsIgnoreCase(STATUS_CANCELED)) {
				// already canceled, nothing to do
				logger.info("handleSubCancel: sub already canceled for accountId {}; returning success.", accountId);
			} else {
				logger.info("handleSubCancel: subscription already exists for accountId: {}, with status: {}. Updating to Canceled.",accountId,sub.getStatus());
				sub.setStatus(STATUS_CANCELED);
				ObjectifyService.ofy().save().entity(sub).now();
				logger.info("Subscripton canceled. Subscription saved: {}", sub);
			}
			sendEventProcessSuccessResponse(resp, "Subscription canceled.");
		}
	}

	/**
	 * Returns standard appdirect event success xml in the response including a passed in accountIdentifier and message
	 * @param resp
	 * @param accountIdentifier
	 * @param successMessage
	 * @throws IOException
	 */
	public void sendEventProcessSuccessResponse (HttpServletResponse resp, String accountIdentifier, String successMessage) throws IOException
	{
		resp.setContentType("text/xml;charset=UTF-8");
		StringBuilder writer = new StringBuilder();
		writer.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
		writer.append("<result>");
		writer.append("<success>true</success>");
		writer.append("<message>");
		writer.append(successMessage);
		writer.append("</message>");
		writer.append("<accountIdentifier>");
		writer.append(accountIdentifier);
		writer.append("</accountIdentifier>");
	    writer.append("</result>");
	    resp.getWriter().println(writer.toString());
	    resp.flushBuffer();
	}
	
	/**
	 * Returns standard appdirect event success xml in the response including a passed in message
	 * @param resp
	 * @param messasge
	 * @throws IOException
	 */
	public void sendEventProcessSuccessResponse (HttpServletResponse resp, String successMessage) throws IOException
	{

		resp.setContentType("text/xml;charset=UTF-8");
		StringBuilder writer = new StringBuilder();
		writer.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
		writer.append("<result>");
		writer.append("<success>true</success>");
		writer.append("<message>");
		writer.append(successMessage);
		writer.append("</message>");
	    writer.append("</result>");
	    resp.getWriter().println(writer.toString());
	    resp.flushBuffer();
		
	}
	
	/**
	 * Returns standard appdirect event success xml in the response 
	 * @param resp
	 * @throws IOException
	 */
	public void sendEventProcessSuccessResponse (HttpServletResponse resp) throws IOException
	{

		resp.setContentType("text/xml;charset=UTF-8");
		StringBuilder writer = new StringBuilder();
		writer.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
		writer.append("<result>");
		writer.append("<success>true</success>");
	    writer.append("</result>");
	    resp.getWriter().println(writer.toString());
	    resp.flushBuffer();
	}
	

	/**
	 * Returns standard appdirect event error xml in the response including a passed in code and message
	 * @param resp
	 * @param errorCode
	 * @param message
	 * @throws IOException
	 */
	public void sendEventProcessErrorResponse (HttpServletResponse resp, String errorCode, String message) throws IOException
	{
		resp.setContentType("text/xml;charset=UTF-8");
		StringBuilder writer = new StringBuilder();

		writer.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
		writer.append("<result>");
		writer.append("<success>false</success>");
		writer.append("<errorCode>");
		writer.append(errorCode);
		writer.append("</errorCode>");
		writer.append("<message>");
		writer.append(message);
		writer.append("</message>");
	    writer.append("</result>");
	    resp.getWriter().println(writer.toString());
	    resp.flushBuffer();
	}
	
	/**
	 * Validates request is from AppDirect by checking the Oauth signature
	 * 
	 * WARNING: couldn't find any obvious examples of this, spent some time trying 
	 * various things, but will have to do this later. I sent a questions to 
	 * AppDirect peoples and was told this is one of the more complex and error prone
	 * areas and that they often tell developers to skip this, so i'm skipping it
	 * to work on other integration areas.
	 *
	 * @param req
	 * @return true if Appdirerct Request has valid OAuth Signature 
	 */
	private boolean validateOAuthSignature(HttpServletRequest req) {
		
		// TODO: implement this, couldn't find any obvious examples of this,
		// see above comments.
		return true;

	}
	
	/**
	 * Process a Subscription Order based on passed event xml. If successful
	 * a subscription record will be added to d/b. The HttpServletResponse is used
	 * to return success/fail xml response.
	 * 
	 * @param resp
	 * @param dDoc event xml for sub order
	 * @throws IOException
	 */
	private void handleSubOrder(HttpServletResponse resp, Document dDoc) throws IOException {

		// this method creates a subscription on success and returns the accociated account identifer (null if unsuccessful).
		String accountIdentifier = createSubscriptionFromXml(dDoc,resp);

		// If everything went well we have an accountIdentifier
		if (accountIdentifier!=null) {
			sendEventProcessSuccessResponse(resp, accountIdentifier, "Account creation successful.");
		} 

	}

	
	/**
	 * Returns xml given an eventUrl by turning it into a signed fetch and
	 * returning the resulting xml document. 
     * Turning into a Document is a convenience way to deal with signed fetch
     * xml in all subsequent code.
	 * @param eventUrl
	 * @return xml Document, null on error
	 */
	private Document obtainXmlDoc(String eventUrl)
	{

		DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();

		// sign the eventUrl to obtain xml for event
		HttpURLConnection signedFetch = signRequest(eventUrl);

		if (signedFetch == null)
		{
			logger.info("Unable to obtain signed fetch for eventUrl: {}", eventUrl);
			return null;
		}
		
		/*
		 * uncomment to hack out signing 
		 * URL url = new URL(eventUrl); 
		 * HttpURLConnection request = (HttpURLConnection) url.openConnection(); 
		 * signedFetch = request;
		 */

		// Attempt to obtain xml in form of Document from the signed eventUrl
		DocumentBuilder builder;
		try {
			
			builder = domFactory.newDocumentBuilder();
			
			logger.info("Attempting parse of signedFetch = {} - {}",signedFetch.getURL(),signedFetch.getRequestProperties());
			Document dDoc = builder.parse(signedFetch.getInputStream());
			
			return dDoc;
			
			//TODO: add real handling around exceptions, i.e. logging, etc.
		} catch (ParserConfigurationException e) {
			logger.info("Exception encountered: {}",e.getMessage());
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	
	/**
	 * SUBSCRIPTION_CHANGE: fired by AppDirect when a user upgrades/downgrades/modifies an existing subscription.
	 * Handle subscription change based on passed in xml document. Will return appropriate xml via passed in HttpServletResponse.
	 * NOTE: in this simplified implemenation, we are not tracking sub details to actually effect the changes, so this is
	 * essentially a no-op.
	 * @param resp
	 * @param dDoc
	 * @throws IOException
	 */
	private void handleSubChange(HttpServletResponse resp, Document dDoc) throws IOException {


		Subscription sub = null;

		com.example.guestbook.model.subChange.EventType event  = validateOrderUpdateXml(resp, dDoc,"Change");
		
		if (event==null) return;
		
		// event type validity check
		if (checkEventTypeMatch(resp,event.getType(),EVENT_TYPE_SUB_CHANGE)) return;
		
		if (handleOrderFlag(resp, event.getFlag())) return;
		
		String accountId = event.getPayload().getAccount().getAccountIdentifier();
		
		// obtain subscription TODO: wrap in transaction
		sub = getSubScriptionForAccountId(resp, accountId, "Subscription Change");
			
		if (sub!=null)
		{
			/*
			 * regardless of the current status, we will act upon the Subscription Change;
			 * 
			 * assume change is entirely encapsulated in the order, which makes a sub
			 * change a simple case of swapping out the payload's order. All test
			 * cases validate this assumptions so far.
			 * TODO: validate this assumption and make changes as necessary
			 */
			Order currentOrder = sub.getOrder();
			Order order = new Order(event.getPayload().getOrder());
			logger.info("Replacing accountId {} current subscription order: {}; with changed order: {}",accountId,currentOrder,order);
			sub.setOrder(order);
			ObjectifyService.ofy().save().entity(sub).now();
			logger.info("Subscripton changed. Subscription saved: {}", sub);
			// just send success
			sendEventProcessSuccessResponse(resp);	
		}
								
	}
	
	/**
	 * Takes a flag value and acts accordingly for Order events:
	 * all but STATELESS flags are ignored, we go ahead a process development flags;
	 * for STATELESS, we  short circuit processing and return a valid error value.
	 * Returns true if a response has been returned false otherwise.
	 * @param resp used to send a valid error response if needed
	 * @param flag the flag to respond to, can be null (ignored)
	 * @return true if a valid error response returned
	 * @throws IOException
	 */
	private boolean handleOrderFlag(HttpServletResponse resp,String flag) throws IOException
	{
		boolean isHandled = false;
		
		// for non subscription orders, all but STATELESS flags are ignored, we go ahead a process development flags
		// for testing we will accept stateless as well, uncomment line below to enable stateless short-circuit
		if (flag!=null && flag.equalsIgnoreCase(FLAG_STATELESS))
		{
			logger.info("Stateless flag detected.");
			if (processStatelessFlag)
			{
				logger.info("ProcessStatelessFlag is turned on, we will process this request normally. Set ProcessStatelessFlag to false (see {}) to send immediate valid error response for Stateless events.",CONFIG_FILE_PATH);
			} else
			{
				sendEventProcessErrorResponse(resp, ERROR_CODE_UNKNOWN_ERROR,"STATELESS flag detected, returning UNKNOWN ERROR.");
				isHandled = true;
			}
		}
		else 
		{ 
			// any other flag will be addded to subscription d/b in case we care
			logger.info("Flag ({})detected; will be recorded in the subscripton.", flag);
		}
		return isHandled;
	}

	/**
	 * Accepts an account id and attempts to return a related stored subscription.
	 * @param resp used to return error response (if needed)
	 * @param accountId
	 * @param action calling action to use in response and logs (if needed)
	 * @return subscription stored in d/b with passed in account Id
	 * @throws IOException
	 */
	private Subscription getSubScriptionForAccountId(HttpServletResponse resp, String accountId, String action)
			throws IOException {
		// check if subscription exists TODO: check for multiple sub entries
		// in d/b
		Subscription sub = ObjectifyService.ofy().load().type(Subscription.class).filter("accountId", accountId).first()
				.now();
	
		if (sub == null) {

			// account doesn't exist, can't handle sub change
			logger.info("{} failed, No subscription found for : {}",action, accountId);
			sendEventProcessErrorResponse(resp, ERROR_CODE_ACCOUNT_NOT_FOUND,
					action + " failed. Acccount not found for account identifier: " + accountId);
		}
		return sub;
	}
	
	/**
	 * Accepts an xml document and attempt to validate it has a Subscription change object. The xml
	 * should be a valid AppDirect subscription change or cancel event.
	 * @param resp used to return error response (if needed)
	 * @param dDoc the xml document
	 * @param action calling action (i.e. Subscripton Change) to use in response and logs (if needed)
	 * @return a subscription change event object
	 * @throws IOException
	 */
	private com.example.guestbook.model.subChange.EventType validateOrderUpdateXml(HttpServletResponse resp,
			Document dDoc, String action) throws IOException {

		com.example.guestbook.model.subChange.EventType event = null;

		// extract xml into sub change event objects.
		logger.info("Attempting JAXB unmarshall of dDoc = {}", dDoc);
		// Unmarshall the xml into objects
		JAXBContext jc;
		try {
			jc = JAXBContext.newInstance(com.example.guestbook.model.subChange.EventType.class);
			// Create unmarshaller
			Unmarshaller um = jc.createUnmarshaller();
			event = (com.example.guestbook.model.subChange.EventType) um.unmarshal(dDoc);
			logger.info("Created from xml, the following Event={}", event);

		} catch (JAXBException e) {
			e.printStackTrace();
			logger.info("XML not formatted correctly for Subscription {}. Exception message: {}",action, StringEscapeUtils.escapeXml(e.getMessage()));
			sendEventProcessErrorResponse(resp, ERROR_CODE_INVALID_RESPONSE,
					"XML not formatted correctly for Subscription  " + action + ". Exception message: "
							+ StringEscapeUtils.escapeXml(e.getMessage()));
			return null;
		}

		// verify accountId exists in xml, TODO: add more validation?
		String accountId = event.getPayload().getAccount().getAccountIdentifier();

		if (isNullError (accountId, "account identifier","Subscription "+action,(Object)event,resp)) return null;
		
		return event;
	}
	
	/**
	 * Accepts an xml document and attempt to validate it has a Subscription order object. The xml
	 * should be a valid AppDirect subscription order event.
	 * @param resp used to return error response (if needed)
	 * @param dDoc the xml document
	 * @return a subscription order event object
	 * @throws IOException
	 */
	private com.example.guestbook.model.subOrder.EventType validateOrderXml(HttpServletResponse resp, Document dDoc) throws IOException {
		
		com.example.guestbook.model.subOrder.EventType event = null;
		
		// extract xml into sub order event objects.
		logger.info("Attempting JAXB unmarshall of dDoc = {}", dDoc);
		// Unmarshall the xml into objects
		JAXBContext jc;
		try {
			jc = JAXBContext.newInstance(com.example.guestbook.model.subOrder.EventType.class);
			//Create unmarshaller
			Unmarshaller um = jc.createUnmarshaller();
			//EventType event = (EventType) um.unmarshal(signedFetch.getInputStream() );
			event = (com.example.guestbook.model.subOrder.EventType) um.unmarshal(dDoc ); 
			logger.info("Created from xml, the following sub order Event={}",event);
			
		} catch (JAXBException e) {
			e.printStackTrace();
			logger.info("XML not formatted correctly for Subscription Order. Exception message: {}",StringEscapeUtils.escapeXml(e.getMessage()));
			sendEventProcessErrorResponse(resp, ERROR_CODE_INVALID_RESPONSE, "XML not formatted correctly for Subscription Order. Exception message: "+StringEscapeUtils.escapeXml(e.getMessage()));
			return null;
		}
		// event type validity check
		if (checkEventTypeMatch(resp,event.getType(),EVENT_TYPE_SUB_ORDER)) return null;
		
		return event;
	}
	
	private boolean checkEventTypeMatch(HttpServletResponse resp, String eventTypeIn , String eventTypeExpected) throws IOException
	{
		
		boolean returnValue = false;
		if (!eventTypeExpected.equalsIgnoreCase(eventTypeIn)) {
			logger.info("Expected event type {}, received: {}",eventTypeExpected,eventTypeIn);
			sendEventProcessErrorResponse(resp, ERROR_CODE_INVALID_RESPONSE,
					"Expected event type "+eventTypeExpected+", received "+eventTypeIn);
			returnValue = true;
		}
		return returnValue;
		
	}
	
	
	
	/**
	 * Attempts to create a subscription based on passed in xml. 
	 * If successful, creates an subscription d/b entry and returns the account id.
	 * WARNING: multiple returns todo: refactor out multiple returns.
	 * @param dDoc xml of subscription order
	 * @param resp http servlet response used to return error xml for some error conditions
	 * @return account id 
	 * @throws IOException
	 */
	private String createSubscriptionFromXml(Document dDoc, HttpServletResponse resp) throws IOException {
		String accountId = null;
		String finalAccountId = null;
		Subscription sub = null;
		com.example.guestbook.model.subOrder.EventType event = null;
		logger.info("createSubscriptionFromXml...");
		
		event = validateOrderXml( resp, dDoc);
			
		if (event==null) return null;
		
		// handle flag
		String flag = event.getFlag();
		if (handleOrderFlag(resp, flag)) return null;

		// generate account id based on current subscription event
		accountId = generateNewAccountId(event);
		
		/* 
		 * Check accountId validity and see if subscription already exists
		 * 
		 */
		if (accountId == null) {
			logger.info("createSubscriptionFromXml: unable to generatate account id for subscription xml {}",event);
			sendEventProcessErrorResponse(resp, ERROR_CODE_INVALID_RESPONSE, "Unable to obtain account id for subscription event. ");
			return  null;
		}

		// TODO: WRAP IN TRANSACTION
		
		// check if subscription exists TODO: check for multiple sub entries in d/b
		sub = ObjectifyService.ofy().load().type(Subscription.class).filter("accountId", accountId).first().now();
		
		if (sub!=null)
		{
		    if ((sub.status.equalsIgnoreCase(STATUS_DEACTIVATED)) || (sub.status.equalsIgnoreCase(STATUS_CANCELED)))
			{
				// assume re-purchasing deactivated or canceled? remove the the old status and create a new one
		    	// note, the new sub may have different attributes
				logger.info("createSubscriptionFromXml: deleting and recreating subscription that already exists for (accountId): {}, with status: {}. Delete sub: {}",accountId,sub.status, sub);
				ObjectifyService.ofy().delete().entity(sub).now();
			}
			else
			{					
				// active (or some other status) subscription already exists error
				logger.info("createSubscriptionFromXml: subscription already exists for accoundId: {}",accountId);
				sendEventProcessErrorResponse(resp, ERROR_CODE_USER_ALREADY_EXISTS, "Subscription arleady created for accountId: "+accountId);
				return null;
			}
				
		}
		
		// create the subscription with values from  xml
		MarketPlace marketPlace = new MarketPlace(event.getMarketplace());
		Company company = new Company(event.getPayload().getCompany());
		Person creator = new Person(event.getCreator());
		Order order = new Order(event.getPayload().getOrder());

		String status = STATUS_ACTIVE;
		Map<String,User> theUsers = (Map<String,User>) new HashMap<String,User>(); // no users initially
	
        // create a new subscription object
		sub = new Subscription(accountId,marketPlace,creator,company,order,status, theUsers); 
		// may have a flag
		sub.setFlag(flag);
		
		/*
		 * save the subscription record 
		 */
		ObjectifyService.ofy().save().entity(sub).now();
		logger.info("New subscription created: {}",sub);
		// END TRANSACTION
		
		// passing back non-empty accountId indicates success
		finalAccountId = accountId;
		
		return finalAccountId;
	}
	
	
	
	
	
	public UUID generateUUID() {
		// generate random UUIDs
		UUID idOne = UUID.randomUUID();
		logger.debug("UUID generated: {}" ,idOne);
		return idOne;
	}

	
	/**
	 * Generates a new accountId. 
	 * TODO: Decide what we really want to for strategy for account ids.
	 * This determines if we limit subscriptions per company, etc.
	 * For now, lets use a company uuid which limits a company to 
	 * having only 1 subscription regardless of edition of our application.
	 * This is mostly for testing purposes. May want to append edition.
	 * We could make it unique every time providing the ability to 
	 * have unlimited subscriptions
	 * @param subscription event object
	 * @return accountId
	 */
	private String generateNewAccountId(com.example.guestbook.model.subOrder.EventType event) {
		String accountId = null;
		// below would generate a unique id prefixed by company name (unlimited subscriptions)
		//accountId = event.getPayload().getCompany().getName() + generateUUID().toString();
		
		// this is company uuid + edition which limits a company to having a subscription per edition
		//accountId = event.getPayload().getCompany().getUuid()+event.getPayload().getOrder().getEditionCode();
		
		// this is company uuid  which limits a company to having one subscription regardless of edition
		accountId = event.getPayload().getCompany().getUuid();
		return accountId;
	}



	/**
	 * Converts entire xml document into a string.
	 * @param doc xml document
	 * @return string representation of the xml document , null if error
	 */
	public String getStringFromDocument(Document doc)
	{
	    try
	    {
	       DOMSource domSource = new DOMSource(doc);
	       StringWriter writer = new StringWriter();
	       StreamResult result = new StreamResult(writer);
	       TransformerFactory tf = TransformerFactory.newInstance();
	       Transformer transformer = tf.newTransformer();
	       transformer.transform(domSource, result);
	       return writer.toString();
	    }
	    catch(TransformerException ex)
	    {
	       ex.printStackTrace();
	       return null;
	    }
	} 
	
	/**
	 * For logging/debugging purposes.
	 * Takes xml document and attempts log the xml and could also extract values to create
	 * an activity d/b entry to record activity that we attempted to process 
	 * TODO: record status of attempt
	 * 
	 * @param dDoc
	 *            xml document
	 * @return true if a d/b entry is successfully created
	 */
	private boolean createActivityFromEventXml(Document dDoc) {
		boolean isSuccessful = false;
		if (dDoc != null) {
			/*
			 * Uncomment below (and modify?) if we wish to extract values from xml to put into activity log
			 */
			//String activityType = getXmlElementValueAsString("/event/type/text()", dDoc);
			//String customer = getXmlElementValueAsString("/event/creator/firstName/text()", dDoc) + " "
			//		+ getXmlElementValueAsString("/event/creator/lastName/text()", dDoc);
			//String version = getXmlElementValueAsString("/event/marketplace/partner/text()", dDoc);
			// for detail: display all the xml
			String theXml = getStringFromDocument(dDoc);
			String detail = theXml;
			//AppActivity activity = new AppActivity(activityType, customer, version, detail);
			AppActivity activity = new AppActivity("", "", "", detail);
			/*
			 * Save to d/b
			 * Use Objectify to save the activity and now() is used to make
			 * the call synchronously.
			 */
			ObjectifyService.ofy().save().entity(activity).now();
			isSuccessful = true;
		} else {
			logger.info("Null xml document in createActivityFromEventXml, no activity saved to d/b!");
		}
		return isSuccessful;
	}
	
	/**
	 * Accepts a string URL and returns an HttpUrlConnection that is signed with Oauth Key and Secret
	 * @param theUrl string url
	 * @return HttpURLConnection that is signed with Oauth Key and Secret, null on error
	 */
	private HttpURLConnection signRequest(String theUrl)
	{		
		OAuthConsumer consumer = new DefaultOAuthConsumer(oauthConsumerKey, oauthConsumerSecret);
		URL url;
		try {
			url = new URL(theUrl);
			HttpURLConnection request = (HttpURLConnection) url.openConnection();
			consumer.sign(request);
			return request;		
		} catch (Exception e) {
			logger.warn("An exception occurred try to sign the request {}. Exception message: {}", theUrl, e.getMessage() );
			e.printStackTrace();
		} 
		return null;
		
	}

	/**
	 * Expects a xml document and a valid xPathExpression for the document and
	 * will return the string nodeValue that the xPathExpression points to
	 * TODO: synchronize access to XPathFactory which is not thread safe
	 * 
	 * @param xPathExpression
	 *            references a text node in the passed in Document
	 * @param dDoc
	 *            xml document
	 * @return string nodeValue, null if problem encountered or not found
	 */
	String getXmlElementValueAsString(String xPathExpression, Document dDoc) {
		String theValue = null;
		if (dDoc==null || xPathExpression == null)
		{
			logger.info("ERROR, unexpected null encountered, dDoc={}, xPathExpression={}",dDoc,xPathExpression);
		}
		else
		{	
			try {
				XPath xPath = XPathFactory.newInstance().newXPath();
				Node node = (Node) xPath.evaluate(xPathExpression, dDoc, XPathConstants.NODE);
				theValue = node.getNodeValue();
				logger.info("xPathExpression ({}) returns {}",xPathExpression, theValue);
			} catch (Exception e) {
				logger.info("ERROR! Likely an invalid xPathExpression ({}) was supplied against xml document: {}",xPathExpression,dDoc);
				e.printStackTrace();
			}
		}
		return theValue;
	}

}