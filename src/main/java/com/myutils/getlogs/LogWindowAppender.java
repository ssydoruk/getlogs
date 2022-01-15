/**
 *
 */
package com.myutils.getlogs;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.*;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

import java.io.Serializable;

@Plugin(name = "LogWindowAppender", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE)
public class LogWindowAppender extends AbstractAppender {

    private final SettingsForm sf;

    protected LogWindowAppender(String name,
                                Filter filter,
                                Layout<? extends Serializable> layout,
                                boolean _ignoreExceptions, Property[] properties, SettingsForm settingsForm) {
        super(name, filter, layout, _ignoreExceptions, properties);
        this.sf=settingsForm;
    }

    protected LogWindowAppender(String name,
                                Filter filter, SettingsForm settingsForm) {
        this(name, filter, null, false, null,
                settingsForm);
    }


    @PluginFactory
    public static LogWindowAppender createAppender(@PluginAttribute("name") String name, @PluginElement("Filter") final Filter filter) {
        return new LogWindowAppender(name, filter, null);
    }

    @Override
    public void append(LogEvent event) {
        if (event.getLevel().isMoreSpecificThan(Level.INFO)) {
            if(sf!=null){
                sf.postLogEvent(event);
            }
//            System.out.println("------------------ APPEND---------------" + event.getMessage().getFormattedMessage());
        }
    }
}
