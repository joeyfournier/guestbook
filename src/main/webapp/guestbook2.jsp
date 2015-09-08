<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.google.appengine.api.users.User" %>
<%@ page import="com.google.appengine.api.users.UserService" %>
<%@ page import="com.google.appengine.api.users.UserServiceFactory" %>
<%@ page import="java.util.List" %>
<%@ page import="com.example.guestbook.model.Greeting" %>
<%@ page import="com.example.guestbook.model.Guestbook" %>
<%@ page import="com.example.guestbook.model.AppActivity" %>
<%@ page import="com.example.guestbook.model.Subscription" %>
<%@ page import="com.googlecode.objectify.Key" %>
<%@ page import="com.googlecode.objectify.ObjectifyService" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<html>
<head>
    <link type="text/css" rel="stylesheet" href="/stylesheets/main.css"/>
</head>

<body>

<%
    String guestbookName = request.getParameter("guestbookName");
    if (guestbookName == null) {
        guestbookName = "default";
    }
    pageContext.setAttribute("guestbookName", guestbookName);
    UserService userService = UserServiceFactory.getUserService();
    User user = userService.getCurrentUser();
    if (user != null) {
        pageContext.setAttribute("user", user);
%>

<p>Hello, ${fn:escapeXml(user.nickname)}! (You can
    <a href="<%= userService.createLogoutURL(request.getRequestURI()) %>">sign out</a>.)</p>
<%
} else {
%>
<p>Hello good earthling!
    <a href="<%= userService.createLoginURL(request.getRequestURI()) %>">Sign in</a>
    to include your name with greetings you post.</p>
<%
    }
%>

<%
    // Create the correct Ancestor key
      Key<Guestbook> theBook = Key.create(Guestbook.class, guestbookName);

    // Run an ancestor query to ensure we see the most up-to-date
    // view of the Greetings belonging to the selected Guestbook.
      List<Greeting> greetings = ObjectifyService.ofy()
          .load()
          .type(Greeting.class) // We want only Greetings
          .ancestor(theBook)    // Anyone in this book
          .order("-date")       // Most recent first - date is indexed.
          .limit(5)             // Only show 5 of them.
          .list();

    if (greetings.isEmpty()) {
%>
<p>Guestbook '${fn:escapeXml(guestbookName)}' has no messages.</p>
<%
    } else {
%>
<p>Messages in Guestbook '${fn:escapeXml(guestbookName)}'.</p>
<%
      // Look at all of our greetings
        for (Greeting greeting : greetings) {
            pageContext.setAttribute("greeting_content", greeting.content);
            String author;
            if (greeting.author_email == null) {
                author = "An anonymous person";
            } else {
                author = greeting.author_email;
                String author_id = greeting.author_id;
                if (user != null && user.getUserId().equals(author_id)) {
                    author += " (You)";
                }
            }
            pageContext.setAttribute("greeting_user", author);
%>
<p><b>${fn:escapeXml(greeting_user)}</b> wrote:</p>
<blockquote>${fn:escapeXml(greeting_content)}</blockquote>
<%
        }
    }
%>

<form action="/sign" method="post">
    <div><textarea name="content" rows="3" cols="60"></textarea></div>
    <div><input type="submit" value="Post Greeting"/></div>
    <input type="hidden" name="guestbookName" value="${fn:escapeXml(guestbookName)}"/>
</form>
<form action="/guestbook.jsp" method="get">
    <div><input type="text" name="guestbookName" value="${fn:escapeXml(guestbookName)}"/></div>
    <div><input type="submit" value="Switch Guestbook"/></div>
</form>

<p></p>

<p> Let's see all our app direct SUBSCRIPTIONS: </p>

<%
    // Run an ancestor query to ensure we see the most up-to-date
    // view of the Greetings belonging to the selected Guestbook.
      List<Subscription> subscriptions = ObjectifyService.ofy()
          .load()
          .type(Subscription.class) // We want only Greetings
          .order("-date")       // Most recent first - date is indexed.
          .limit(50)             // Only show 50 of them.
          .list();

    if (subscriptions.isEmpty()) {
%>
<p>Sadly, there are no appDirect SUBBSCRIPTIONS happening.</p>
<%
    } else {
%>
<p>Well lookie here, we have some subscriptions:</p>
<%
      // Look at all of our subscriptionss
        for (Subscription sub : subscriptions) {
           
%>
<p><b><%=sub.date%></b> ...id:  <%=sub.id%> ...accountId: <%=sub.accountId%> ...Status: <%=sub.status%>
...MarketPlace: <%=sub.marketPlace%>
...Order: <%=sub.order%>  
...Users: <%=sub.theUsers%>  
...creator: <%=sub.creator%> 
...company: <%=sub.company%> </p>
<%
//WARNING TEMPORARY CLEAN UP MAKE SURE YOU COMMENT THIS OUT
//ObjectifyService.ofy().delete().entity(sub).now();
        }
    }
%>
<p> Let's see all the app direct activity: </p>

<%
    // Run an ancestor query to ensure we see the most up-to-date
    // view of the Greetings belonging to the selected Guestbook.
      List<AppActivity> activityList = ObjectifyService.ofy()
          .load()
          .type(AppActivity.class) // We want only Greetings
          .order("-date")       // Most recent first - date is indexed.
          .limit(50)             // Only show 50 of them.
          .list();

    if (activityList.isEmpty()) {
%>
<p>Sadly, there are no appDirect thingies happening.</p>
<%
    } else {
%>
<p>Well lookie here, we have some action:</p>
<%
      // Look at all of our activities
        for (AppActivity activity : activityList) {
           
%>
<p><b><%=activity.date%></b> 
<!--  ...id:  <%=activity.id%> 
...Customer: <%=activity.customer%> ...Action: <%=activity.action%>  ...Version: <%=activity.version%> -->
 ...xml: <%=activity.details%></p>
<%
        }
    }
%>


<p></p>


</body>
</html>