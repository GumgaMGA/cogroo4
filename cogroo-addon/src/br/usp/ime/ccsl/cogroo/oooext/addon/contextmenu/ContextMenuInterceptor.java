package br.usp.ime.ccsl.cogroo.oooext.addon.contextmenu;

import br.usp.ime.ccsl.cogroo.oooext.LoggerImpl;
import br.usp.ime.ccsl.cogroo.oooext.i18n.I18nLabelsLoader;
import com.sun.star.ui.*;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XIndexContainer;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.uno.UnoRuntime;
import java.util.logging.Logger;

// from http://user.services.openoffice.org/en/forum/viewtopic.php?f=20&t=27115&start=0
public class ContextMenuInterceptor implements XContextMenuInterceptor {

    private static Logger LOGGER = LoggerImpl.getLogger(ContextMenuInterceptor.class.getCanonicalName());

    public ContextMenuInterceptorAction notifyContextMenuExecute(
            com.sun.star.ui.ContextMenuExecuteEvent aEvent) throws RuntimeException {

        try {

            // Retrieve context menu container and query for service factory to
            // create sub menus, menu entries and separators
            XIndexContainer xContextMenu = aEvent.ActionTriggerContainer;
            XMultiServiceFactory xMenuElementFactory =
                    (XMultiServiceFactory) UnoRuntime.queryInterface(
                    XMultiServiceFactory.class, xContextMenu);
            if (xMenuElementFactory != null) {
                // create root menu entry for sub menu and sub menu
                XPropertySet xRootMenuEntry =
                        (XPropertySet) UnoRuntime.queryInterface(
                        com.sun.star.beans.XPropertySet.class,
                        xMenuElementFactory.createInstance("com.sun.star.ui.ActionTrigger"));

                // create a line separator for our new help sub menu
                XPropertySet xSeparator =
                        (XPropertySet) UnoRuntime.queryInterface(
                        com.sun.star.beans.XPropertySet.class,
                        xMenuElementFactory.createInstance("com.sun.star.ui.ActionTriggerSeparator"));

                Short aSeparatorType = new Short(ActionTriggerSeparatorType.LINE);
                xSeparator.setPropertyValue("SeparatorType", (Object) aSeparatorType);

                // intialize root menu entry
                xRootMenuEntry.setPropertyValue("Text", new String(I18nLabelsLoader.ADDON_REPORT_ERROR_CONTEXTMENU_GC));
                xRootMenuEntry.setPropertyValue("CommandURL", "br.usp.ime.ccsl.cogroo.oooext:ReportError");

                // intialize help/content menu entry
                // entry "Content"
                XPropertySet xMenuEntry = (XPropertySet) UnoRuntime.queryInterface(
                        XPropertySet.class, xMenuElementFactory.createInstance(
                        "com.sun.star.ui.ActionTrigger"));

                
                int last = xContextMenu.getCount();
                if (last < 0) {
                    last = 0;
                }

                // add new sub menu into the given context menu
                xContextMenu.insertByIndex(last, (Object) xRootMenuEntry);

                // add separator into the given context menu
                xContextMenu.insertByIndex(last, (Object) xSeparator);

                // The controller should execute the modified context menu and stop notifying other
                // interceptors.
                return com.sun.star.ui.ContextMenuInterceptorAction.EXECUTE_MODIFIED;
            }
        } catch (com.sun.star.uno.Exception ex) {
            LOGGER.warning("Error initializing context menu interceptor");
            ex.printStackTrace();
        } catch (java.lang.Throwable ex) {
            LOGGER.warning("Error initializing context menu interceptor");
            ex.printStackTrace();
        }

        return com.sun.star.ui.ContextMenuInterceptorAction.IGNORED;
    }
}
