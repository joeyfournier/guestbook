package com.example.guestbook;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides response helper methods to send various success/error
 * responses using an HttpServletResponse object.
 * Use as singleton.
 * 
 * @author Joey Fournier
 */
public class ResponseHelper {

	// logger
	final Logger logger = LoggerFactory.getLogger(ResponseHelper.class);

	protected static ResponseHelper theInstance = null;

	private ResponseHelper() {
	}

	public synchronized static ResponseHelper getInstance() {
		if (theInstance == null) {
			theInstance = new ResponseHelper();
		}
		return theInstance;
	}

	/**
	 * Checks if a passed in object is null and uses the passed HttpServletResponse object to
	 * generate an error response with passed in descriptors. Returns true if the value is null
	 * and an error is generated/logged.
	 * 
	 * @param checkObject
	 *            object to check if null
	 * @param description
	 *            a descripton of the object to check
	 * @param action
	 *            the current action that this check is being done for
	 * @param event
	 *            for logging purposes, the event this object is being checked from
	 * @param resp
	 *            HttpServletResponse to send an error if needed
	 * @return true if passed in checkObject is null and an error is generated
	 * @throws IOException
	 */
	protected boolean isNullError(Object checkObject, String description, String action, Object event, String statusCode, HttpServletResponse resp) throws IOException {
		boolean returnValue = false;
		if (checkObject == null) {
			logger.info("Subscription {}: null {} in event xml: {}", action, description, event);
			sendEventProcessErrorResponse(resp, statusCode,
					"Subscription " + action + " failed. Unable to obtain " + description + " from event.");
			returnValue = true;
		}
		return returnValue;
	}

	/**
	 * Checks if a passed in object is null and uses the passed HttpServletResponse object to
	 * generate an error response with passed in descriptors. Returns if true the value is null
	 * and an error is generated/logged.
	 * 
	 * @param checkObject
	 *            object to check if null
	 * @param description
	 *            a descripton of the object to check
	 * @param action
	 *            the current action that this check is being done for
	 * @param resp
	 *            HttpServletResponse to send an error if needed
	 * @return true if passed in checkObject is null and an error is generated
	 * @throws IOException
	 */
	protected boolean isNullError(Object checkObject, String description, String action, String statusCode, HttpServletResponse resp) throws IOException {
		boolean returnValue = false;
		if (checkObject == null) {
			logger.info("{}: null {}.", action, description);
			sendEventProcessErrorResponse(resp, statusCode,
					action + " failed. Unable to obtain " + description + " from event.");
			returnValue = true;
		}
		return returnValue;

	}

	/**
	 * Returns standard appdirect event success xml in the response including a passed in accountIdentifier and message
	 * 
	 * @param resp
	 * @param accountIdentifier
	 * @param successMessage
	 * @throws IOException
	 */
	protected void sendEventProcessSuccessResponse(HttpServletResponse resp, String accountIdentifier, String successMessage) throws IOException {
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
	 * 
	 * @param resp
	 * @param messasge
	 * @throws IOException
	 */
	protected void sendEventProcessSuccessResponse(HttpServletResponse resp, String successMessage) throws IOException {

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
	 * 
	 * @param resp
	 * @throws IOException
	 */
	protected void sendEventProcessSuccessResponse(HttpServletResponse resp) throws IOException {
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
	 * 
	 * @param resp
	 * @param errorCode
	 * @param message
	 * @throws IOException
	 */
	protected void sendEventProcessErrorResponse(HttpServletResponse resp, String errorCode, String message) throws IOException {
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

}
