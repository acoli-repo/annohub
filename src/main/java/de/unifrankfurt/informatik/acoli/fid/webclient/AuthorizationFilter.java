package de.unifrankfurt.informatik.acoli.fid.webclient;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import de.unifrankfurt.informatik.acoli.fid.types.AccountType;
import de.unifrankfurt.informatik.acoli.fid.util.Utils;

/**
 * @author journaldev
 *
 */
@WebFilter(filterName = "AuthFilter", urlPatterns = { "*.xhtml" })
public class AuthorizationFilter implements Filter {

	public AuthorizationFilter() {
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {

	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		try {

			HttpServletRequest reqt = (HttpServletRequest) request;
			HttpServletResponse resp = (HttpServletResponse) response;
			HttpSession ses = reqt.getSession(false);
			
			String reqURI = reqt.getRequestURI();
			Utils.debug("reqest-url : "+reqURI);
			
			
			if (reqURI.indexOf("/login-account.xhtml") > 0) {
				chain.doFilter(request, response);
				return;
			}
			
		
			if (ses == null || ses.getAttribute("username") == null || reqURI.contains("..")) {
				resp.sendRedirect(reqt.getContextPath() + "/login-account.xhtml");
				return;
			}
			
			if (reqURI.contains("/annohub/javax.faces.resource")) {
				chain.doFilter(request, response);
				return;
			}
			
			AccountType userclass = (AccountType) ses.getAttribute("userclass");
			
			if (userclass.equals(AccountType.GUEST) && 
				(reqURI.endsWith("/login.xhtml") || reqURI.endsWith("/login-resources.xhtml"))) {
				chain.doFilter(request, response);
				return;
			}
			
			if (userclass.equals(AccountType.MEMBER) && 
					(reqURI.endsWith("/login.xhtml")
					|| reqURI.endsWith("/login-resources.xhtml")
					|| reqURI.endsWith("/login-upload.xhtml")
					|| reqURI.endsWith("/login-my.xhtml")
					)) {
				chain.doFilter(request, response);
				return;
			}
			
			
			if (userclass.equals(AccountType.ADMIN) && 
					(reqURI.endsWith("/login.xhtml")
					|| reqURI.endsWith("/login-resources.xhtml")
					|| reqURI.endsWith("/login-upload.xhtml")
					|| reqURI.endsWith("/login-admin.xhtml")
					|| reqURI.endsWith("/login-backup.xhtml")
					|| reqURI.endsWith("/login-languages.xhtml")
					|| reqURI.endsWith("/login-models.xhtml")
					|| reqURI.endsWith("/login-my.xhtml")
					)) {
				chain.doFilter(request, response);
				return;
			}
			
			// path is not allowed
			resp.sendRedirect(reqt.getContextPath() + "/login-account.xhtml");


		} catch (Exception e) {
			System.out.println("Authentication error");
			System.out.println(e.getMessage());
		}
	}

	@Override
	public void destroy() {

	}

}


//if (reqURI.contains("/annohub/javax.faces.resource/theme.css")
//|| reqURI.contains("/annohub/javax.faces.resource/jquery")
//|| reqURI.contains("/annohub/javax.faces.resource/core") 
//|| reqURI.contains("/annohub/javax.faces.resource/components.css")) {
//chain.doFilter(request, response);
//return;
//}

//if (reqURI.indexOf("/login-account.xhtml") >= 0
//|| (ses != null && ses.getAttribute("username") != null)
//|| reqURI.contains("javax.faces.resource")) {
//chain.doFilter(request, response);
//}
//else {
//resp.sendRedirect(reqt.getContextPath() + "/login-account.xhtml");
//}
