package servlets.module.challenge;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.owasp.esapi.ESAPI;
import org.owasp.esapi.Encoder;

import utils.ShepherdLogManager;
import utils.SqlFilter;
import dbProcs.Database;

/**
 * SQL Injection Challenge Four - Does not use user specific key
 * <br/><br/>
 * This file is part of the Security Shepherd Project.
 * 
 * The Security Shepherd project is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.<br/>
 * 
 * The Security Shepherd project is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.<br/>
 * 
 * You should have received a copy of the GNU General Public License
 * along with the Security Shepherd project.  If not, see <http://www.gnu.org/licenses/>. 
 * @author Mark Denihan
 *
 */
public class SqlInjection4 extends HttpServlet
{
	//Sql Challenge 4
	private static final long serialVersionUID = 1L;
	private static org.apache.log4j.Logger log = Logger.getLogger(SqlInjection4.class);
	private static String levelName = "SqlInjection4";
	private static String levelResult = "d316e80045d50bdf8ed49d48f130b4acf4a878c82faef34daff8eb1b98763b6f"; 
	private static String levelHash = "1feccf2205b4c5ddf743630b46aece3784d61adc56498f7603ccd7cb8ae92629";
	/**
	 * Users have to defeat SQL injection that blocks single quotes.
	 * The input they enter is also been filtered.
	 * @param theUserName User name used in database look up.
	 * @param thePassword User password used in database look up
	 */
	public void doPost (HttpServletRequest request, HttpServletResponse response) 
	throws ServletException, IOException
	{
		//Setting IpAddress To Log and taking header for original IP if forwarded from proxy
		ShepherdLogManager.setRequestIp(request.getRemoteAddr(), request.getHeader("X-Forwarded-For"));
		//Attempting to recover user name of session that made request
		try
		{
			if (request.getSession() != null)
			{
				HttpSession ses = request.getSession();
				String userName = (String) ses.getAttribute("decyrptedUserName");
				log.debug(userName + " accessed " + levelName + " Servlet");
			}
		}
		catch (Exception e)
		{
			log.debug(levelName + " Servlet Accessed");
			log.error("Could not retrieve user name from session");
		}
		PrintWriter out = response.getWriter();  
		out.print(getServletInfo());
		String htmlOutput = new String();
		Encoder encoder = ESAPI.encoder();
		try
		{
			String theUserName = request.getParameter("theUserName");
			log.debug("User Submitted - " + theUserName);
			theUserName = SqlFilter.levelFour(theUserName);
			log.debug("Filtered to " + theUserName);
			String thePassword = request.getParameter("thePassword");
			log.debug("thePassword Submitted - " + thePassword);
			thePassword = SqlFilter.levelFour(thePassword);
			log.debug("Filtered to " + thePassword);
			String ApplicationRoot = getServletContext().getRealPath("");
			log.debug("Servlet root = " + ApplicationRoot );
			
			log.debug("Getting Connection to Database");
			Connection conn = Database.getChallengeConnection(ApplicationRoot, "SqlChallengeFour");
			Statement stmt = conn.createStatement();
			log.debug("Gathering result set");
			ResultSet resultSet = stmt.executeQuery("SELECT userName FROM users WHERE userName = '" + theUserName + "' AND userPassword = '" + thePassword + "'");
	
			int i = 0;
			htmlOutput = "<h2 class='title'>Login Result</h2>";
			
			log.debug("Opening Result Set from query");
			if(resultSet.next())
			{
				log.debug("Signed in as " + resultSet.getString(1));
				htmlOutput += "<p>Signed in as " + encoder.encodeForHTML(resultSet.getString(1)) + "</p>";
				if(resultSet.getString(1).equalsIgnoreCase("admin"))
				{
					htmlOutput += "<p>As you are the admin, here is the result key:"
								+ "<a>"	+ encoder.encodeForHTML(levelResult) + "</a>";
				}
				else
				{
					htmlOutput += "<p>But admins have all the fun</p>";
				}
				i++;
			}
			if(i == 0)
			{
				htmlOutput = "<h2 class='title'>Login Result</h2><p>You didn't log in. This site is super secure so hax won't work!</p>";
			}
		}
		catch (SQLException e)
		{
			log.debug("SQL Error caught - " + e.toString());
			htmlOutput += "<p>An error was detected!</p>" +
				"<p>" + encoder.encodeForHTML(e.toString()) + "</p>";
		}
		catch(Exception e)
		{
			out.write("An Error Occurred! You must be getting funky!");
			log.fatal(levelName + " - " + e.toString());
		}
		log.debug("Outputting HTML");
		out.write(htmlOutput);
	}
}