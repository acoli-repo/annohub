package de.unifrankfurt.informatik.acoli.fid.webclient;

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpSession;

import org.apache.tinkerpop.gremlin.driver.Cluster;

import de.unifrankfurt.informatik.acoli.fid.resourceDB.RMServer;
import de.unifrankfurt.informatik.acoli.fid.types.AccountType;
import de.unifrankfurt.informatik.acoli.fid.types.Backup;
import de.unifrankfurt.informatik.acoli.fid.types.UserAccount;
import de.unifrankfurt.informatik.acoli.fid.util.Utils;

/**
 * @author journaldev
 *
 */
	
@ManagedBean(name="auth")
@SessionScoped
public class Authenticate implements Serializable {

	private static final long serialVersionUID = -517015928080158568L;
	private String pwd=null;
	private String msg=null;
	private String user=null;
	
	
	HttpSession session = null;
	
	@ManagedProperty(value="#{execute}")
    private ExecutionBean executionBean;
	
	@ManagedProperty(value="#{account}")
	private UserAccount userAccount=null;

	public String getPwd() {
		return pwd;
	}

	public void setPwd(String pwd) {
		this.pwd = pwd;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	//validate login
	public String validateUsernamePassword() {
		
		boolean valid = ExecutionBean.getPublicExecuter().getResourceManager().userExists(user, pwd);
		boolean isGuest = user.equals("acoli") && pwd.equals("guest");
				
		if (valid || isGuest) {
	
			if (isGuest) {
				user = "guest"+executionBean.getNextFreeGuestAccountId();
				executionBean.getEditManager().removeEditLocks(user);
				userAccount = new UserAccount(user, pwd, "");
			} else {
				// clear previously unclosed edits
				executionBean.getEditManager().removeEditLocks(user);

				userAccount = ExecutionBean.getPublicExecuter().getResourceManager().getUserAccount(user);
				
			}
			
			if(userAccount.getAccountType() == AccountType.RETIRED) {
				Utils.debug("user is retired");
				FacesContext.getCurrentInstance().addMessage(
						null,
						new FacesMessage(FacesMessage.SEVERITY_WARN,
								"Incorrect Username and Password",
								"Please enter correct username and Password"));
				return "login-account";
			}
			
			Utils.debug("login successful");
			session = SessionUtils.getSession();
			session.setAttribute("username", user);
			session.setAttribute("userclass", userAccount.getAccountType());
			session.setMaxInactiveInterval(600); //60min
			
			ExecutionBean.getUserLog().login(user);
			
			System.out.println("===login===");
			System.out.println((new Date()));
			System.out.println(userAccount.getUserID());
			System.out.println(userAccount.getUserEmail());
			System.out.println(userAccount.getAccountType());
			
			try {
				FacesContext.getCurrentInstance().getExternalContext().redirect("login.xhtml"); // works
			    FacesContext.getCurrentInstance().responseComplete();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return "";
		} else {
			Utils.debug("login failed");
			FacesContext.getCurrentInstance().addMessage(
					null,
					new FacesMessage(FacesMessage.SEVERITY_WARN,
							"Incorrect Username and Password",
							"Please enter correct username and Password"));
			return "login-account";
		}
	}

	//logout event, invalidate session
	public String logout() {
		HttpSession session = SessionUtils.getSession();
		session.invalidate();
		
		//Utils.debug("Logged out : "+user);
		
		// clear unclosed edits
		executionBean.getEditManager().removeEditLocks(user);
		ExecutionBean.getUserLog().logout(user);
		
		return "login-account?faces-redirect=true"; //login-account
	}
	
	
	public String restoreBackup() {
		HttpSession session = SessionUtils.getSession();
		session.invalidate();
		
		//Utils.debug("Logged out : "+user);
		
		// clear unclosed edits
		executionBean.getEditManager().removeEditLocks(user);
		ExecutionBean.getUserLog().logout(user);
		
		
		Backup backup = new Backup("test-backup-1");
		//ExecutionBean.getPublicExecuter().stopGremlinServer();//restoreBackup();
		ExecutionBean.getPublicExecuter().makePhysicalBackup(backup);
		//ExecutionBean.getPublicExecuter().startGremlinServer();
		
		// show different page
		return "login-account?faces-redirect=true";
	}
	
	
	public ExecutionBean getExecutionBean() {
		return executionBean;
    }
	
    public void setExecutionBean (ExecutionBean neededBean) {
    	this.executionBean = neededBean;
    }

	public HttpSession getSession() {
		return session;
	}

	public UserAccount getUserAccount() {
		return userAccount;
	}

	public void setUserAccount(UserAccount userAccount) {
		this.userAccount = userAccount;
	}

}

