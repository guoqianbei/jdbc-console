package com.ryaltech.utils.jdbcconsole;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.h2.engine.Constants;

public class WebServlet extends org.h2.server.web.WebServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private boolean metaDataEnabled; 

	@Override
	public void init() {
		InitialContext ctx = null;
		OutputStream os = null;
		ServletConfig localServletConfig = getServletConfig();
		String dir = localServletConfig.getInitParameter("properties");
		metaDataEnabled = Boolean.TRUE.toString().equalsIgnoreCase(localServletConfig.getInitParameter("dbMetaDataEnabled"));
		if (dir == null)
			dir = Constants.SERVER_PROPERTIES_DIR;

		try {

			ctx = new InitialContext();
			List<String> dsJndis = extractDataSourceJndi(ctx);

			Properties props = new Properties();
			props.setProperty("webAllowOthers", "true");
			int i = 0;
			for (String dsJndi : dsJndis) {
				props.setProperty(Integer.toString(i++), String.format(
						"%s|javax.naming.InitialContext|%s|", dsJndi, dsJndi));
			}
			os = new FileOutputStream(dir + "/"
					+ Constants.SERVER_PROPERTIES_NAME);
			props.store(os, "#generated by jdbc console");
		} catch (Exception ex) {
			log("Failure to create h2 console properties", ex);
		} finally {
			try {
				ctx.close();
			} catch (Exception ex) {
			}
			try {
				os.close();
			} catch (Exception ex) {
			}

		}

		super.init();
	}

	@Override
	public void doGet(HttpServletRequest paramHttpServletRequest,
			HttpServletResponse paramHttpServletResponse) throws IOException {

		if(!metaDataEnabled && paramHttpServletRequest.getRequestURL().toString().endsWith("tables.do")){
			paramHttpServletResponse.getWriter().println("DB MetaData disabled for performance reasons. If you need it, uncomment section around dbMetaDataEnabled in web.xml.");
			return;
		};
		super.doGet(paramHttpServletRequest, paramHttpServletResponse);
	}

	
	private List<String> extractDataSourceJndi(Context ctx) {
		List<String> dsJndi = new ArrayList<String>();
		extractDataSourceJndi(dsJndi, "", ctx);
		return dsJndi;
	}

	private void extractDataSourceJndi(List<String> dsJndis, String jndi,
			Context ctx) {

		try {
			NamingEnumeration<NameClassPair> ne = ctx.list(jndi);

			while (ne.hasMoreElements()) {
				NameClassPair next = (NameClassPair) ne.nextElement();
				String newJndi = (jndi.length() == 0) ? next.getName() : jndi
						+ "/" + next.getName();
				Object obj = ctx.lookup(newJndi);
				if (obj instanceof DataSource) {
					dsJndis.add(newJndi);
				}
				extractDataSourceJndi(dsJndis, newJndi, ctx);
			}
		} catch (Exception ex) {

		}

	}

}
