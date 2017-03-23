package be.cytomine.utils

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.apache.log4j.Logger;

/**
 * Listener for context events (app start/end) and session events (session start/end).
 *
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
public class ContextListener implements ServletContextListener, HttpSessionListener {

    private static final String START_TIME = "grails.plugins.appinfo.start_time";

    private static ContextListener instance;

    private final Logger log = Logger.getLogger(getClass());

    private final List<HttpSession> sessions = new LinkedList<HttpSession>();
    private final Map<String, HttpSession> sessionsById = new HashMap<String, HttpSession>();

    /**
     * Constructor, called by the container (singleton).
     */
    public ContextListener() {
        instance = this;
    }

    public static ContextListener instance() {
        return instance;
    }

    public void contextInitialized(final ServletContextEvent event) {
        log.debug("app startup");
        event.getServletContext().setAttribute(START_TIME, new Date());
    }

    public void contextDestroyed(final ServletContextEvent event) {
        log.debug("app shutdown");
    }

    public void sessionCreated(final HttpSessionEvent event) {
        HttpSession session = event.getSession();
        synchronized (this) {
            sessions.add(session);
            sessionsById.put(session.getId(), session);
        }
    }

    public void sessionDestroyed(final HttpSessionEvent event) {
        HttpSession session = event.getSession();
        synchronized (this) {
            sessions.remove(session);
            sessionsById.remove(session.getId());
        }
    }

    public synchronized List<HttpSession> getSessions() {
        return new ArrayList<HttpSession>(sessions);
    }

    public synchronized HttpSession getSession(final String id) {
        return sessionsById.get(id);
    }
}