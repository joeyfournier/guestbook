The guestbook application is  integrated into AppDirect’s Subscription Management and Access Management APIs, and supports OpenId Single Sign On via AppDirect. 

Source:
https://github.com/joeyfournier/guestbook

The application is an example Google App Engine guestbook app which was modified and augmented to be a multi-user web application accessible via AppDirect. In order to support this I creates an additional application (HandleAppDirectServlet.java) to implement endpoints to AppDirects’s subscription management and access management APIs, and another applicaton (ConsumerServlet.java) which handles OpenID SSO.

I utilized Google App Engine as it was one of the recommended deploy platforms on the challenge document and I was impressed by is ease of use and scalability. That said, this decision fed into the design and implementation decisions for my challenge submission:
- GAE does not fully support Java 8 (it officially supports Java 7).
- I chose to use Objectify for persistence as it was part of the Guestbook app and appears to be endorsed by GAE as the straightforward interface to GAE’s powerful (and free) App Engine Datastore, and I wouldn’t have to worry about unsupported caveats of other java frameworks built over it.

Deployed to the internet:
========================
Under the Appdirect user: Joey Fournier  (joey.fournier@wmode.com) product "Guestbook_Multi_User", I was able to buy, cancel, change, assign and unassign users and do SSO. I couldn’t bring myself to pull the trigger on Publishing this “Guestbook” app! I wasn’t sure if anyone would be able to see this publically, and I didn’t want to unleash this Guestbook app that displays EVERYONE’S subscription information! :-)

Here are the deployment details:

Application URLs
----------------
I used Google App Engine and Objectify to deploy the application and endpoints (configured for above).
With the guestbook app, you will have access to a simple guest book which also shows ALL current (not just yours) subscription d/b entries, and it shows a debugging activity log which will has all the incoming xml (successfully signed) received from all endpoint urls that are hit. I deployed a SSO version and a non-SSO version (accessible anytime by anyone) which is pretty much an exact replica except that it does not require SSO via AppDirect
The Application URLs:
- Requires SSO: https://guestbook-1027.appspot.com/guestbook.jsp (see below for credentials to login)
- Non-SSO: https://guestbook-1027.appspot.com/guestbook2.jsp, if you attempt to use the guestbook it may point you back to the SSO version, but you can just add the “2” to get full access.

OpenID SSO endpoint
-------------------
I deployed an OpenID SSO endpoint (ConsumerServlet.java) at:
https://guestbook-1027.appspot.com/login
I’ve created and assigned to the App a test user you can utilize if you wish to use for testing:
greenhippo77@gmail.com
p/w: (send in an email to Laura N.)
 (NOTE: currently there is no limit to a session so once a session is validated with a valid login, you need to hit the “AppDirect Logout” button on the app to invalidate your session and force a new login with AppDirect.)
ConsumerServlet.java is basically openid4java's consumer servlet example modified to support SSO for the submitted guestbook application.

Event endpoints
---------------
The AppDirect event endpoints (HandleAppDirectServlet.java) are deployed at the following locations (configured for the above application). Deployed endpoint urls:
- https://guestbook-1027.appspot.com/ad?action=SUBSCRIPTION_ORDER&eUrl={eventUrl}
- https://guestbook-1027.appspot.com/ad?action=SUBSCRIPTION_CHANGE&eUrl={eventUrl}
- https://guestbook-1027.appspot.com/ad?action=SUBSCRIPTION_CANCEL&eUrl={eventUrl}
- https://guestbook-1027.appspot.com/ad?action=SUBSCRIPTION_NOTIFY&eUrl={eventUrl}
- https://guestbook-1027.appspot.com/ad?action=USER_ASSIGNMENT&eUrl={eventUrl}
- https://guestbook-1027.appspot.com/ad?action=USER_UNASSIGNMENT&eUrl={eventUrl}

I was not able to implement verifying AppDirect's OAuth signature validation after much googling and trying various strategies, I gave up after I asked AppDirect about it and receiving the following response:
Verifying OAuth signatures of outgoing HTTP calls from AppDirect is probably the most confusing part of our challenge for most developers.  Part of it is because there aren't that many libraries that can verify signatures and actually implementing the verification part is error-prone.  So we usually tell candidates that they can skip this part.

To build war and run the source locally
=======================================
HTTPS clone url: https://github.com/joeyfournier/guestbook.git

Config file
-----------
The config file located in src/main/webapp/WEB-INF/HandleAppDirectServlet.properties contains the properties: 
oauth.consumer.key = <oauthConsumerKey from AppDirect>
oauth.consumer.secret = <oauthConsumerSecret from AppDirect> 
And you can add the following property to successfully process the AppDirect example xml files (see below for local test to create a local subscription using this xml):
process.stateless.flag = true

Building
--------
Go to the main directory and run:

mvn clean install

will generate: ./target/guestbook-1.0-SNAPSHOT.war

Run Locally
-----------
To run a local copy (there may be Google App Engine dependencies needed locally?) run:

mvn appengine:devserver

and go to: 
http://localhost:8080/guestbook.jsp (SSO) or http://localhost:8080/guestbook2.jsp (No SSO)
to see guestbook app. 

You can create a test subscription(if process.stateless.flag = true is present in config) by going to:

http://localhost:8080/ad?action=SUBSCRIPTION_ORDER&eUrl=https%3A%2F%2Fwww.appdirect.com%2Fapi%2Fintegration%2Fv1%2Fevents%2FdummyOrder

You can create another test subscription that will work with the Other AppDirect example events xml by hitting:
http://localhost:8080/ad?action=SUBSCRIPTION_ORDER&eUrl=http%3A%2F%2Flocalhost%3A8080%2FdummyADSignUp.xml
which creates a subscription with accountId ‘dummy-account’ which work with other events such as:
http://localhost:8080/ad?action=SUBSCRIPTION_CHANGE&eUrl=https%3A%2F%2Fwww.appdirect.com%2Fapi%2Fintegration%2Fv1%2Fevents%2FdummyChange

Would like to have dones
========================
And finally, given more time, I would like to have added the following to the submission (some of these can be seen in code as TODO):
Wrap and d/b read and write operations in a transaction. GAE and Objectify support transactions and I would like to have added them around each of the events updating the d/b to protect the integrity of the d/b in a multi-user environnment.

Any questions can be sent to joey.fournier@wmode.com

Regards,
Joey
